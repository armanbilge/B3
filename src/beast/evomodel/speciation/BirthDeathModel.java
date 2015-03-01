/*
 * BirthDeathModel.java
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
import beast.evolution.util.Units;
import beast.evomodel.tree.TreeModel;
import beast.inference.model.CompoundParameter;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.logging.Logger;

import static org.apache.commons.math3.special.Gamma.logGamma;

/**
 * Birth Death model based on Gernhard 2008  "The conditioned reconstructed process"
 * Journal of Theoretical Biology Volume 253, Issue 4, 21 August 2008, Pages 769-778
 * doi:10.1016/j.jtbi.2008.04.005 (http://dx.doi.org/10.1016/j.jtbi.2008.04.005)
 * <p/>
 * This derivation conditions directly on fixed N taxa.
 * <p/>
 * The inference is directly on b-d (strictly positive) and d/b (constrained in [0,1))
 * <p/>
 * Vefified using simulated trees generated by Klass tree sample. (http://www.klaashartmann.com/treesample/)
 * <p/>
 * Sampling proportion not verified via simulation. Proportion set by default to 1, an assignment which makes the expressions
 * identical to the expressions before the change.
 *
 * @author Joseph Heled
 *         Date: 24/02/2008
 */
public class BirthDeathModel extends UltrametricSpeciationModel {

    public enum TreeType {
        UNSCALED,     // no coefficient 
        TIMESONLY,    // n!
        ORIENTED,     // n
        LABELED,      // 2^(n-1)/(n-1)!
                      // conditional on root: 2^(n-1)/n!(n-1)
                      // conditional on origin: 2^(n-1)/n!
    }

    public static final String BIRTH_DEATH_MODEL = "birthDeathModel";
    public static final String CONDITIONAL_ON_ROOT = "conditionalOnRoot";

    /*
     * mu/lambda
     *
     * null means default (0), or pure birth (Yule)
     */
    private Parameter relativeDeathRateParameter;

    /**
     *    lambda - mu
     */
    private Parameter birthDiffRateParameter;
   
    private Parameter sampleProbability;

    private Parameter originHeightParameter;

    private TreeType type;

    private boolean conditionalOnRoot;
    private boolean conditionOnOrigin;

    /**
     * rho *
     */

    public BirthDeathModel(Parameter birthDiffRateParameter,
                           Parameter relativeDeathRateParameter,
                           Parameter sampleProbability,
                           Parameter originHeightParameter,
                           TreeType type,
                           Type units) {

        this(BIRTH_DEATH_MODEL, birthDiffRateParameter, relativeDeathRateParameter, sampleProbability, originHeightParameter, type, units, false);
    }

    public BirthDeathModel(String modelName,
                           Parameter birthDiffRateParameter,
                           Parameter relativeDeathRateParameter,
                           Parameter sampleProbability,
                           Parameter originHeightParameter,
                           TreeType type,
                           Type units, boolean conditionalOnRoot) {

        super(modelName, units);

        this.birthDiffRateParameter = birthDiffRateParameter;
        addVariable(birthDiffRateParameter);
        birthDiffRateParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.relativeDeathRateParameter = relativeDeathRateParameter;
        if( relativeDeathRateParameter != null ) {
          addVariable(relativeDeathRateParameter);
          relativeDeathRateParameter.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));
        }

        this.sampleProbability = sampleProbability;
        if (sampleProbability != null) {
            addVariable(sampleProbability);
            sampleProbability.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));
        }

        this.originHeightParameter = originHeightParameter;
        conditionOnOrigin = originHeightParameter != null;
        if (conditionOnOrigin) addVariable(originHeightParameter);

        this.conditionalOnRoot = conditionalOnRoot;
        if ( conditionalOnRoot && conditionOnOrigin) {
            throw new IllegalArgumentException("Cannot condition on both root and origin!");
        }

        this.type = type;
    }

    @Override
    public boolean isYule() {
        // Yule only
        return (relativeDeathRateParameter == null && sampleProbability == null && !conditionalOnRoot);
    }

    @Override
    public double getMarginal(Tree tree, CalibrationPoints calibration) {
       // Yule only
       return calibration.getCorrection(tree, getR());
    }

    public double getR() {
        return birthDiffRateParameter.getParameterValue(0);
    }

    public double getA() {
        return relativeDeathRateParameter != null ? relativeDeathRateParameter.getParameterValue(0) : 0;
    }

    public double getRho() {
        return sampleProbability != null ? sampleProbability.getParameterValue(0) : 1.0;
    }

    @Override
    public double calculateTreeLogLikelihood(Tree tree) {
        if (conditionOnOrigin && tree.getNodeHeight(tree.getRoot()) > originHeightParameter.getValue(0))
            return Double.NEGATIVE_INFINITY;
        return super.calculateTreeLogLikelihood(tree);
    }

    private double logCoeff(int taxonCount) {
        switch( type ) {
            case UNSCALED: break;
            case TIMESONLY: return logGamma(taxonCount + 1);
            case ORIENTED:  return Math.log(taxonCount);
            case LABELED:   {
                final double two2nm1 = (taxonCount - 1) * Math.log(2.0);
                if( conditionalOnRoot ) {
                    return two2nm1 - Math.log(taxonCount-1) - logGamma(taxonCount+1);
                } else if (conditionOnOrigin) {
                    return two2nm1 - logGamma(taxonCount + 1);
                } else {
                    return two2nm1 - logGamma(taxonCount);
                }
            }
        }
        return 0.0;
    }

    public double logTreeProbability(int taxonCount) {
        double c1 = logCoeff(taxonCount);
        if (conditionOnOrigin) {
            final double height = originHeightParameter.getValue(0);
            c1 += (taxonCount - 1) * logConditioningTerm(height);
        } else if (!conditionalOnRoot) {
            c1 += (taxonCount - 1) * Math.log(getR() * getRho()) + taxonCount * Math.log(1 - getA());
        }
        return c1;
    }

    public double differentiateLogTreeProbability(int taxonCount, Variable<Double> var) {
        if (conditionOnOrigin) {
            final double height = originHeightParameter.getValue(0);
            double deriv = taxonCount - 1;
            if (var == originHeightParameter)
                deriv *= differentiateLogConditioningTerm(height);
            else
                deriv *= differentiateLogConditioningTerm(height, var);
            return deriv;
        } else if (!conditionalOnRoot) {
            if (var == birthDiffRateParameter)
                return (taxonCount - 1) / getR();
            else if (var == relativeDeathRateParameter)
                return taxonCount / (getA() - 1);
            else if (var == sampleProbability)
                return (taxonCount - 1) / getRho();
        }
        return 0;
    }

    public double logNodeProbability(Tree tree, NodeRef node) {
        final double height = tree.getNodeHeight(node);
        final double r = getR();
        final double mrh = -r * height;
        final double a = getA();
        final double rho = getRho();

        if (conditionalOnRoot && tree.isRoot(node)) {
            return (tree.getTaxonCount() - 2) * logConditioningTerm(height);
        }

        final double z = Math.log(rho + ((1 - rho) - a) * Math.exp(mrh));
        double l = -2 * z + mrh;

        if (!conditionOnOrigin && !conditionalOnRoot && tree.isRoot(node)) {
            l += mrh - z;
        }

        return l;
    }

    public double differentiateLogNodeProbability(Tree tree, NodeRef node, Variable<Double> var, int index) {

        if (var instanceof CompoundParameter)
            var = ((CompoundParameter) var).getMaskedParameter(index);

        final double height = tree.getNodeHeight(node);
        final double r = getR();
        final double rh = r * height;
        final double a = getA();
        final double rho = getRho();

        if (var == birthDiffRateParameter) {

            if (conditionalOnRoot && tree.isRoot(node)) {
                return (tree.getTaxonCount() - 2) * differentiateLogConditioningTerm(height, var);
            }

            final double aprhom1 = a + rho - 1;
            final double z = - height * aprhom1 / (aprhom1 - rho * Math.exp(rh));
            double l = -2 * z - height;

            if (!conditionOnOrigin && !conditionalOnRoot && tree.isRoot(node)) {
                l -= height + z;
            }

            return l;

        } else if (var == relativeDeathRateParameter) {

            if (conditionalOnRoot && tree.isRoot(node)) {
                return (tree.getTaxonCount() - 2) * differentiateLogConditioningTerm(height, var);
            }

            final double z = 1 / (a - rho * Math.exp(rh) + rho - 1);
            double l = -2 * z;

            if (!conditionOnOrigin && !conditionalOnRoot && tree.isRoot(node)) {
                l -= z;
            }

            return l;

        } else if (var == sampleProbability) {

            if (conditionalOnRoot && tree.isRoot(node)) {
                return (tree.getTaxonCount() - 2) * differentiateLogConditioningTerm(height, var);
            }

            final double erhm1 = Math.exp(rh) - 1;
            final double z = erhm1 / (-a + rho * erhm1 + 1);
            double l = -2 * z;

            if (!conditionOnOrigin && !conditionalOnRoot && tree.isRoot(node)) {
                l -= z;
            }

            return l;

        } else if (tree instanceof TreeModel && var instanceof Parameter && ((TreeModel) tree).isHeightParameterForNode(node, (Parameter) var)) {

            if (conditionalOnRoot && tree.isRoot(node)) {
                return (tree.getTaxonCount() - 2) * differentiateLogConditioningTerm(height);
            }

            final double aprhom1 = a + rho - 1;
            final double z = r * aprhom1 / (aprhom1 - rho * Math.exp(rh));
            double l = -2 * z - r;

            if (!conditionOnOrigin && !conditionalOnRoot && tree.isRoot(node)) {
                l -= r + z;
            }

            return l;

        } else {
            return 0;
        }
    }

    protected double logConditioningTerm(double height) {
        final double r = getR();
        final double a = getA();
        final double rho = getRho();
        final double ca = 1 - a;
        final double erh = Math.exp(r * height);
        if (erh != 1.0) {
            return Math.log(r * ca * (rho + ca / (erh - 1)));
        } else {  // use exp(x)-1 = x for x near 0
            return Math.log(ca * (r * rho + ca / height));
        }
    }

    protected double differentiateLogConditioningTerm(double height, Variable<Double> var) {
        final double r = getR();
        final double a = getA();
        final double rho = getRho();
        final double ca = 1 - a;
        final double rh = r * height;
        final double erh = Math.exp(rh);
        final double erhm1 = erh - 1;
        final double rhrho = rh * rho;
        if (var == birthDiffRateParameter) {
            if (erh != 1.0) {
                return height * (rho - ca) / (ca + rho * erhm1) - height / erhm1 + 1 / r;
            } else {  // use exp(x)-1 = x for x near 0
                return height * rho / (ca + rhrho);
            }
        } else if (var == relativeDeathRateParameter) {
            final double term;
            if (erh != 1.0) {
                term = rho * erh + rho;
            } else {  // use exp(x)-1 = x for x near 0
                term = rhrho;
            }
            return - (2 * a - term - 2) / (ca * (a - term - 1));
        } else if (var == sampleProbability) {
            if (erh != 1.0) {
                return 1 / (ca / erhm1 + rho);
            } else {  // use exp(x)-1 = x for x near 0
                return rh / (ca + rhrho);
            }
        } else {
            return 0;
        }
    }

    protected double differentiateLogConditioningTerm(double height) {
        final double r = getR();
        final double a = getA();
        final double rho = getRho();
        final double ca = 1 - a;
        final double erh = Math.exp(r * height);
        if (erh != 1.0) {
            final double erhm1 = erh - 1;
            return - ca * r * erh / (erhm1 * (ca + rho * erhm1));
        } else {  // use exp(x)-1 = x for x near 0
            return - ca / (height * (ca + height * rho * r));
        }
    }

    public boolean includeExternalNodesInLikelihoodCalculation() {
        return false;
    }

    public static final XMLObjectParser<BirthDeathModel> PARSER = new AbstractXMLObjectParser<BirthDeathModel>() {

        public static final String BIRTHDIFF_RATE = "birthMinusDeathRate";
        public static final String RELATIVE_DEATH_RATE = "relativeDeathRate";
        public static final String SAMPLE_PROB = "sampleProbability";
        public static final String ORIGIN_HEIGHT = "originHeight";
        public static final String TREE_TYPE = "type";

        public static final String BIRTH_DEATH = "birthDeath";

        public String getParserName() {
            return BIRTH_DEATH_MODEL;
        }

        public BirthDeathModel parseXMLObject(XMLObject xo) throws XMLParseException {

            final Units.Type units = Units.parseUnitsAttribute(xo);

            final String s = xo.getAttribute(TREE_TYPE, TreeType.UNSCALED.toString());
            final TreeType treeType = TreeType.valueOf(s);
            final boolean conditonalOnRoot =  xo.getAttribute(CONDITIONAL_ON_ROOT, false);

            final Parameter birthParameter = (Parameter) xo.getElementFirstChild(BIRTHDIFF_RATE);
            final Parameter deathParameter = (Parameter) xo.getElementFirstChild(RELATIVE_DEATH_RATE);
            final Parameter sampleProbability = xo.hasChildNamed(SAMPLE_PROB) ?
                    (Parameter) xo.getElementFirstChild(SAMPLE_PROB) : null;
            final Parameter originHeightParameter = xo.hasChildNamed(ORIGIN_HEIGHT) ?
                    (Parameter) xo.getElementFirstChild(ORIGIN_HEIGHT) : null;

            Logger.getLogger("beast.evomodel").info(xo.hasChildNamed(SAMPLE_PROB) ? getCitationRHO() : getCitation());

            final String modelName = xo.getId();

            return new BirthDeathModel(modelName, birthParameter, deathParameter, sampleProbability, originHeightParameter,
                    treeType, units, conditonalOnRoot);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************
        public String getCitationRHO() {
            return "Stadler, T; On incomplete sampling under birth-death models and connections to the sampling-based coalescent;\n" +
                    "JOURNAL OF THEORETICAL BIOLOGY (2009) 261:58-66";
        }

        public String getCitation() {
            return "Using birth-death model on tree: Gernhard T (2008) J Theor Biol, Volume 253, Issue 4, Pages 769-778 In press";
        }

        public String getParserDescription() {
            return "Gernhard (2008) model of speciation (equation at bottom of page 19 of draft).";
        }

        public Class getReturnType() {
            return BirthDeathModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newStringRule(TREE_TYPE, true),
                AttributeRule.newBooleanRule(CONDITIONAL_ON_ROOT, true),
                new ElementRule(BIRTHDIFF_RATE, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(RELATIVE_DEATH_RATE, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(SAMPLE_PROB, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
                new ElementRule(ORIGIN_HEIGHT, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
                Units.UNITS_RULE
        };
    };

    public static final XMLObjectParser<BirthDeathModel> YULE_PARSER = new AbstractXMLObjectParser<BirthDeathModel>() {
        public static final String YULE_MODEL = "yuleModel";

        public static final String YULE = "yule";
        public static final String ORIGIN_HEIGHT = "originHeight";
        public static final String BIRTH_RATE = "birthRate";

        public String getParserName() {
            return YULE_MODEL;
        }

        public BirthDeathModel parseXMLObject(XMLObject xo) throws XMLParseException {

            final Units.Type units =Units.parseUnitsAttribute(xo);

            final XMLObject cxo = xo.getChild(BIRTH_RATE);

            final boolean conditonalOnRoot =  xo.getAttribute(CONDITIONAL_ON_ROOT, false);
            final Parameter brParameter = (Parameter) cxo.getChild(Parameter.class);
            final Parameter originHeightParameter = xo.hasChildNamed(ORIGIN_HEIGHT) ?
                    (Parameter) xo.getElementFirstChild(ORIGIN_HEIGHT) : null;

            Logger.getLogger("beast.evomodel").info("Using Yule prior on tree");

            return new BirthDeathModel(xo.getId(), brParameter, null, null, originHeightParameter,
                    TreeType.UNSCALED, units, conditonalOnRoot);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A speciation model of a simple constant rate Birth-death process.";
        }

        public Class getReturnType() {
            return BirthDeathModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newBooleanRule(CONDITIONAL_ON_ROOT, true),
                new ElementRule(BIRTH_RATE,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(ORIGIN_HEIGHT, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}, true),
                Units.UNITS_RULE
        };
    };
}