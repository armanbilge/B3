/*
 * BirthDeathSerialSamplingModel.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
 *
 * BEAST is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BEAST.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.evomodel.speciation;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evolution.util.Taxon;
import beast.evolution.util.Units;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Beginning of tree prior for birth-death + serial sampling + extant sample proportion. More Tanja magic...
 *
 * @author Alexei Drummond
 */
public class BirthDeathSerialSamplingModel extends MaskableSpeciationModel {

    // R0
    Variable<Double> R0;

    // recovery rate
    Variable<Double> recoveryRate;

    // sampling probability
    Variable<Double> samplingProbability;


    // birth rate
    Variable<Double> lambda;

    // death rate
    Variable<Double> mu;

    // serial sampling rate
    Variable<Double> psi;

    // extant sampling proportion
    Variable<Double> p;

    //boolean death rate is relative?
    boolean relativeDeath = false;

    // boolean stating whether sampled individuals remain infectious, or become non-infectious
//    boolean sampledIndividualsRemainInfectious = false; // replaced by r

    //    the additional parameter 0 <= r <= 1 has to be estimated.
    //    for r=1, this is sampledRemainInfectiousProb=0
    //    for r=0, this is sampledRemainInfectiousProb=1
    Variable<Double> r;

    //Variable<Double> finalTimeInterval;

    boolean hasFinalSample = false;

    // the origin of the infection, x0 > tree.getRoot();
    Variable<Double> origin;

    public BirthDeathSerialSamplingModel(
            Variable<Double> lambda,
            Variable<Double> mu,
            Variable<Double> psi,
            Variable<Double> p,
            boolean relativeDeath,
            Variable<Double> r,
            boolean hasFinalSample,
            Variable<Double> origin,
            Type units) {

        this("birthDeathSerialSamplingModel", lambda, mu, psi, p, relativeDeath, r, hasFinalSample, origin, units);
    }

    public BirthDeathSerialSamplingModel(
            String modelName,
            Variable<Double> lambda,
            Variable<Double> mu,
            Variable<Double> psi,
            Variable<Double> p,
            boolean relativeDeath,
            Variable<Double> r,
            boolean hasFinalSample,
            Variable<Double> origin,
            Type units) {

        super(modelName, units);

        this.relativeDeath = relativeDeath;

        this.lambda = lambda;
        addVariable(lambda);
        lambda.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.mu = mu;
        addVariable(mu);
        mu.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.psi = psi;
        addVariable(psi);
        psi.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.p = p;
        addVariable(p);
        p.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));

        this.hasFinalSample = hasFinalSample;

        this.r = r;
        addVariable(r);
        r.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));

        this.origin = origin;
        if (origin != null) {
            addVariable(origin);
            origin.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        }
    }

    public BirthDeathSerialSamplingModel(
            String modelName,
            Variable<Double> R0,
            Variable<Double> recoveryRate,
            Variable<Double> samplingProbability,
            Variable<Double> origin,
            Type units) {

        super(modelName, units);

        this.relativeDeath = false;
        this.hasFinalSample = false;

        this.R0 = R0;
        addVariable(R0);
        R0.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.recoveryRate = recoveryRate;
        addVariable(recoveryRate);
        recoveryRate.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.samplingProbability = samplingProbability;
        addVariable(samplingProbability);
        samplingProbability.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));

        this.origin = origin;
        if (origin != null) {
            addVariable(origin);
            origin.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        }
    }

    /**
     * @param b   birth rate
     * @param d   death rate
     * @param p   proportion sampled at final time point
     * @param psi rate of sampling per lineage per unit time
     * @param t   time
     * @return the probability of no sampled descendants after time, t
     */
    public static double p0(double b, double d, double p, double psi, double t) {
        double c1 = c1(b, d, psi);
        double c2 = c2(b, d, p, psi);

        double expc1trc2 = Math.exp(-c1 * t) * (1.0 - c2);

        return (b + d + psi + c1 * ((expc1trc2 - (1.0 + c2)) / (expc1trc2 + (1.0 + c2)))) / (2.0 * b);
    }

    public static double q(double b, double d, double p, double psi, double t) {
        double c1 = c1(b, d, psi);
        double c2 = c2(b, d, p, psi);
//        double res = 2.0 * (1.0 - c2 * c2) + Math.exp(-c1 * t) * (1.0 - c2) * (1.0 - c2) + Math.exp(c1 * t) * (1.0 + c2) * (1.0 + c2);
        double res = c1 * t + 2.0 * Math.log( Math.exp(-c1 * t) * (1.0 - c2) + (1.0 + c2) ); // operate directly in logspace, c1 * t too big
        return res;
    }

    private static double c1(double b, double d, double psi) {
        return Math.abs(Math.sqrt(Math.pow(b - d - psi, 2.0) + 4.0 * b * psi));
    }

    private static double c2(double b, double d, double p, double psi) {
        return -(b - d - 2.0 * b * p - psi) / c1(b, d, psi);
    }


    public double p0(double t) {
        return p0(birth(), death(), p(), psi(), t);
    }

    public double q(double t) {
        return q(birth(), death(), p(), psi(), t);
    }

    private double c1() {
        return c1(birth(), death(), psi());
    }

    private double c2() {
        return c2(birth(), death(), p(), psi());
    }

    public double birth() {
        if (mask != null) return mask.birth();
        if (lambda != null) {
            return lambda.getValue(0);
        } else {
            double r0 = R0.getValue(0);
            double rr = recoveryRate.getValue(0);
            return r0 * rr;
        }
    }

    public double death() {
        if (mask != null) return mask.death();
        if (mu != null) {
            return relativeDeath ? mu.getValue(0) * birth() : mu.getValue(0);
        } else {
            double rr = recoveryRate.getValue(0);
            double sp = samplingProbability.getValue(0);

            return rr * (1.0 - sp);
        }
    }

    public double psi() {
        if (mask != null) return mask.psi();

        if (psi != null) {
        return psi.getValue(0);
        } else {
            double rr = recoveryRate.getValue(0);
            double sp = samplingProbability.getValue(0);

            return rr * sp;
        }
    }

    /**
     * @return the proportion of population sampled at final sample, or zero if there is no final sample
     */
    public double p() {

        if (mask != null) return mask.p.getValue(0);
        return hasFinalSample ? p.getValue(0) : 0;
    }

    // The mask does not affect the following three methods

    public boolean isSamplingOrigin() {
        return origin != null;
    }

    public double x0() {
        return origin.getValue(0);
    }

    /**
     * Generic likelihood calculation
     *
     * @param tree the tree to calculate likelihood of
     * @return log-likelihood of density
     */
    public final double calculateTreeLogLikelihood(Tree tree) {

        if (isSamplingOrigin() && x0() < tree.getNodeHeight(tree.getRoot())) {
            return Double.NEGATIVE_INFINITY;
//            throw new RuntimeException("Orign value (" + x0() + ") cannot < tree root height (" + tree.getNodeHeight(tree.getRoot()) + ")");
        }

        //System.out.println("calculating tree log likelihood");
        //double time = finalTimeInterval();

        // extant leaves
        int n = 0;
        // extinct leaves
        int m = 0;

        for (int i = 0; i < tree.getExternalNodeCount(); i++) {
            NodeRef node = tree.getExternalNode(i);
            if (tree.getNodeHeight(node) == 0.0) {
                n += 1;
            } else {
                m += 1;
            }
        }

        if (!hasFinalSample && n < 1) {
            throw new RuntimeException(
                    "For sampling-through-time model there must be at least one tip at time zero.");
        }

        double b = birth();
        double p = p();

        double logL = 0;
        if (isSamplingOrigin()) {
//            logL = Math.log(1.0 / q(x0()));
            logL = - q(x0());
            //System.out.println("originLogL=" + logL + " x0");
        } else {
            throw new RuntimeException(
                    "The origin must be sampled, as integrating it out is not implemented!");
            // integrating out the time between the origin and the root of the tree
            //double bottom = c1 * (c2 + 1) * (1 - c2 + (1 + c2) * Math.exp(c1 * x1));
            //logL = Math.log(1 / bottom);
        }
        if (hasFinalSample) {
            logL += n * Math.log(4.0 * p);
        }
        for (int i = 0; i < tree.getInternalNodeCount(); i++) {
            double x = tree.getNodeHeight(tree.getInternalNode(i));
            logL += Math.log(b) - q(x);

            //System.out.println("internalNodeLogL=" + Math.log(b / q(x)));

        }
        for (int i = 0; i < tree.getExternalNodeCount(); i++) {
            double y = tree.getNodeHeight(tree.getExternalNode(i));

            if (y > 0.0) {
                logL += Math.log(psi()) + q(y);

                //System.out.println("externalNodeLogL=" + Math.log(psi() * (r() + (1.0 - r()) * p0(y)) * q(y)));

            } else if (!hasFinalSample) {
                //handle condition ending on final tip in sampling-through-time-only situation
                logL += Math.log(psi()) + q(y);
//                System.out.println("externalNodeLogL=" + Math.log(psi() * q(y)));

            }
        }

        return logL;
    }

    public final double differentiateTreeLogLikelihood(Tree tree, Variable<Double> var, int index) {
        throw new UnsupportedOperationException();
//        if (isSamplingOrigin() && x0() < tree.getNodeHeight(tree.getRoot())) {
//            return 0;
////            throw new RuntimeException("Orign value (" + x0() + ") cannot < tree root height (" + tree.getNodeHeight(tree.getRoot()) + ")");
//        }
//
//        //System.out.println("calculating tree log likelihood");
//        //double time = finalTimeInterval();
//
//        // extant leaves
//        int n = 0;
//        // extinct leaves
//        int m = 0;
//
//        for (int i = 0; i < tree.getExternalNodeCount(); i++) {
//            NodeRef node = tree.getExternalNode(i);
//            if (tree.getNodeHeight(node) == 0.0) {
//                n += 1;
//            } else {
//                m += 1;
//            }
//        }
//
//        if (!hasFinalSample && n < 1) {
//            throw new RuntimeException(
//                    "For sampling-through-time model there must be at least one tip at time zero.");
//        }
//
//        double b = birth();
//        double p = p();
//
//        double logL;
//        if (isSamplingOrigin()) {
////            logL = Math.log(1.0 / q(x0()));
//            logL = - q(x0());
//            //System.out.println("originLogL=" + logL + " x0");
//        } else {
//            throw new RuntimeException(
//                    "The origin must be sampled, as integrating it out is not implemented!");
//            // integrating out the time between the origin and the root of the tree
//            //double bottom = c1 * (c2 + 1) * (1 - c2 + (1 + c2) * Math.exp(c1 * x1));
//            //logL = Math.log(1 / bottom);
//        }
//        if (hasFinalSample) {
//            logL += n * Math.log(4.0 * p);
//        }
//        for (int i = 0; i < tree.getInternalNodeCount(); i++) {
//            double x = tree.getNodeHeight(tree.getInternalNode(i));
//            logL += Math.log(b) - q(x);
//
//            //System.out.println("internalNodeLogL=" + Math.log(b / q(x)));
//
//        }
//        for (int i = 0; i < tree.getExternalNodeCount(); i++) {
//            double y = tree.getNodeHeight(tree.getExternalNode(i));
//
//            if (y > 0.0) {
//                logL += Math.log(psi()) + q(y);
//
//                //System.out.println("externalNodeLogL=" + Math.log(psi() * (r() + (1.0 - r()) * p0(y)) * q(y)));
//
//            } else if (!hasFinalSample) {
//                //handle condition ending on final tip in sampling-through-time-only situation
//                logL += Math.log(psi()) + q(y);
////                System.out.println("externalNodeLogL=" + Math.log(psi() * q(y)));
//
//            }
//        }
//
//        return logL;
    }

    public double calculateTreeLogLikelihood(Tree tree, Set<Taxon> exclude) {
        if (exclude.size() == 0) return calculateTreeLogLikelihood(tree);
        throw new RuntimeException("Not implemented!");
    }

    public double differentiateTreeLogLikelihood(Tree tree, Set<Taxon> exclude, Variable<Double> var, int index) {
        if (exclude.size() == 0) return differentiateTreeLogLikelihood(tree, var, index);
        throw new RuntimeException("Not implemented!");
    }

    public void mask(SpeciationModel mask) {
        if (mask instanceof BirthDeathSerialSamplingModel) {
            this.mask = (BirthDeathSerialSamplingModel) mask;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void unmask() {
        mask = null;
    }

    // if a mask exists then use the mask's parameters instead (except for origin and finalTimeInterval)
    BirthDeathSerialSamplingModel mask = null;

    public static final XMLObjectParser<BirthDeathSerialSamplingModel> PARSER = new AbstractXMLObjectParser<BirthDeathSerialSamplingModel>() {
        public static final String BIRTH_DEATH_SERIAL_MODEL = "birthDeathSerialSampling";
        public static final String LAMBDA = "birthRate";
        public static final String MU = "deathRate";
        public static final String RELATIVE_MU = "relativeDeathRate";
        public static final String PSI = "psi";
        public static final String SAMPLE_PROBABILITY = "sampleProbability"; // default to fix to 0
        public static final String SAMPLE_BECOMES_NON_INFECTIOUS = "sampleBecomesNonInfectiousProb";
        public static final String R = "r";
        //    public static final String FINAL_TIME_INTERVAL = "finalTimeInterval";
        public static final String ORIGIN = "origin";
        public static final String TREE_TYPE = "type";
        public static final String BDSS = "bdss";
        public static final String HAS_FINAL_SAMPLE = "hasFinalSample";

        public String getParserName() {
            return BIRTH_DEATH_SERIAL_MODEL;
        }

        public BirthDeathSerialSamplingModel parseXMLObject(XMLObject xo) throws XMLParseException {

            final String modelName = xo.getId();
            final Units.Type units = Units.parseUnitsAttribute(xo);

            boolean hasFinalSample = xo.getAttribute(HAS_FINAL_SAMPLE, false);

            final Parameter lambda = (Parameter) xo.getElementFirstChild(LAMBDA);

            boolean relativeDeath = xo.hasChildNamed(RELATIVE_MU);
            Parameter mu;
            if (relativeDeath) {
                mu = (Parameter) xo.getElementFirstChild(RELATIVE_MU);
            } else {
                mu = (Parameter) xo.getElementFirstChild(MU);
            }

            final Parameter psi = (Parameter) xo.getElementFirstChild(PSI);
            //Issue 656: fix p=0
            final Parameter p = xo.hasChildNamed(SAMPLE_PROBABILITY) ?
                    (Parameter) xo.getElementFirstChild(SAMPLE_PROBABILITY) : new Parameter.Default(0.0);

            Parameter origin = null;
            if (xo.hasChildNamed(ORIGIN)) {
                origin = (Parameter) xo.getElementFirstChild(ORIGIN);
            }

            final Parameter r = xo.hasChildNamed(SAMPLE_BECOMES_NON_INFECTIOUS) ?
                    (Parameter) xo.getElementFirstChild(SAMPLE_BECOMES_NON_INFECTIOUS) : new Parameter.Default(0.0);
//        r.setParameterValueQuietly(0, 1 - r.getParameterValue(0)); // donot use it, otherwise log is changed improperly

//        final Parameter finalTimeInterval = xo.hasChildNamed(FINAL_TIME_INTERVAL) ?
//                (Parameter) xo.getElementFirstChild(FINAL_TIME_INTERVAL) : new Parameter.Default(0.0);

            Logger.getLogger("beast.evomodel").info(xo.hasChildNamed(SAMPLE_BECOMES_NON_INFECTIOUS) ? getCitationRT() : getCitationPsiOrg());

            return new BirthDeathSerialSamplingModel(modelName, lambda, mu, psi, p, relativeDeath,
                    r, hasFinalSample, origin, units);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getCitationPsiOrg() {
//        return "Stadler, T; Sampling-through-time in birth-death trees; JOURNAL OF THEORETICAL BIOLOGY (2010) 267:396-404";
            return "Stadler T (2010) J Theor Biol 267, 396-404 [Birth-Death with Serial Samples].";
        }

        public String getCitationRT() {
            return "Stadler et al (2011) : Estimating the basic reproductive number from viral sequence data, " +
                    "Mol.Biol.Evol., doi: 10.1093/molbev/msr217, 2011";
        }

        public String getParserDescription() {
            return "Stadler et al (2010) model of speciation.";
        }

        public Class getReturnType() {
            return BirthDeathSerialSamplingModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newStringRule(TREE_TYPE, true),
                AttributeRule.newBooleanRule(HAS_FINAL_SAMPLE, true),
//            new ElementRule(FINAL_TIME_INTERVAL, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
                new ElementRule(ORIGIN, Parameter.class, "The origin of the infection, x0 > tree.rootHeight", true),
                new ElementRule(LAMBDA, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new XORRule(
                        new ElementRule(MU, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                        new ElementRule(RELATIVE_MU, new XMLSyntaxRule[]{new ElementRule(Parameter.class)})),
                new ElementRule(PSI, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(SAMPLE_BECOMES_NON_INFECTIOUS, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
                //Issue 656
//            new ElementRule(SAMPLE_PROBABILITY, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
//            XMLUnits.SYNTAX_RULES[0]
        };
    };
}