/*
 * ACLikelihood.java
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

package beast.evomodel.clock;

import beast.evomodel.tree.TreeModel;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.math.MathUtils;
import beast.math.distributions.InverseGaussianDistribution;
import beast.math.distributions.LogNormalDistribution;
import beast.math.distributions.NormalDistribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * Calculates the likelihood of a set of rate changes in a tree, assuming a (log)normal or inverse gaussian distributed
 * change in rate at each node, with a mean of the previous (log) rate and a variance proportional to branch length.
 * cf Yang and Rannala 2006
 *
 * @author Michael Defoin Platel
 * @author Wai Lok Sibon Li
 */
public class ACLikelihood extends RateEvolutionLikelihood {

    public enum Distribution {
        LOG_NORMAL("logNormal"),
        NORMAL("normal"),
        INVERSE_GAUSSIAN("inverseGaussian");
        final String name;
        Distribution(final String s) {
            name = s;
        }
        public String toString() {
            return name;
        }

        public static Distribution fromString(String s) {
            for (Distribution d : Distribution.values())
                if (d.toString().equalsIgnoreCase(s))
                    return d;
            return null;
        }

    }


    public ACLikelihood(TreeModel tree, Parameter ratesParameter, Parameter variance, Parameter rootRate,
                        boolean isEpisodic, Distribution distribution) {

        //super((isLogSpace) ? "LogNormally Distributed" : "Normally Distributed", tree, ratesParameter, rootRate, isEpisodic);
        super(distribution.toString(), tree, ratesParameter, rootRate, isEpisodic);

        //this.isLogSpace = isLogSpace;
        this.variance = variance;
        this.distribution = distribution;

        addVariable(variance);
    }

    /**
     * @return the log likelihood of the rate change from the parent to the child.
     */
    double branchRateChangeLogLikelihood(double parentRate, double childRate, double time) {
        double var = variance.getParameterValue(0);

        if (!isEpisodic())
            var *= time;

        //if (isLogSpace) {
        //    double logParentRate = Math.log(parentRate);
        //    double logChildRate = Math.log(childRate);

        //    return NormalDistribution.logPdf(logChildRate, logParentRate - (var / 2.), Math.sqrt(var)) - logChildRate;

        //} else {
        //    return NormalDistribution.logPdf(childRate, parentRate, Math.sqrt(var));
        //}
        switch (distribution) {
            case LOG_NORMAL:
                return LogNormalDistribution.logPdf(childRate, Math.log(parentRate) - (var / 2.), Math.sqrt(var));
            case NORMAL:
                return NormalDistribution.logPdf(childRate, parentRate, Math.sqrt(var));
            case INVERSE_GAUSSIAN:
                double shape = (parentRate * parentRate * parentRate) / var;
                return InverseGaussianDistribution.logPdf(childRate, parentRate, shape);
            default:
                throw new RuntimeException("No distribution specified!");
        }
    }

    double differentiateBranchRateChangeLogLikelihood(double parentRate, double childRate, double time, boolean respectParent) {
        double var = variance.getParameterValue(0);

        if (!isEpisodic())
            var *= time;

        if (respectParent) {
            switch (distribution) {
                case LOG_NORMAL:
                    return LogNormalDistribution.differentiateLogPdfMean(childRate, Math.log(parentRate) - (var / 2.), Math.sqrt(var)) / parentRate;
                case NORMAL:
                    return NormalDistribution.differentiateLogPdfMean(childRate, parentRate, Math.sqrt(var));
                case INVERSE_GAUSSIAN:
                    double shape = (parentRate * parentRate * parentRate) / var;
                    return InverseGaussianDistribution.differentiateLogPdfMean(childRate, parentRate, shape)
                            + 3 * parentRate * parentRate * InverseGaussianDistribution.differentiateLogPdfShape(childRate, parentRate, shape) / var;
                default:
                    throw new RuntimeException("No distribution specified!");
            }
        } else {
            switch (distribution) {
                case LOG_NORMAL:
                    return LogNormalDistribution.differentiateLogPdf(childRate, Math.log(parentRate) - (var / 2.), Math.sqrt(var));
                case NORMAL:
                    return NormalDistribution.differentiateLogPdf(childRate, parentRate, Math.sqrt(var));
                case INVERSE_GAUSSIAN:
                    double shape = (parentRate * parentRate * parentRate) / var;
                    return InverseGaussianDistribution.differentiateLogPdf(childRate, parentRate, shape);
                default:
                    throw new RuntimeException("No distribution specified!");
            }
        }
    }

    double differentiateBranchRateChangeLogLikelihood(double parentRate, double childRate, double time, Variable<Double> v, int index) {
        if (v == variance) {
            double var = variance.getParameterValue(0);
            double chain = 1.0;
            if (!isEpisodic()) {
                var *= time;
                chain *= time;
            }
            switch (distribution) {
                case LOG_NORMAL: {
                    final double mean = Math.log(parentRate) - (var / 2.);
                    final double stdev = Math.sqrt(var);
                    return chain * 0.5 * (-LogNormalDistribution.differentiateLogPdfMean(childRate, mean, stdev)
                            + LogNormalDistribution.differentiateLogPdfStdev(childRate, mean, stdev) / stdev);
                }
                case NORMAL: {
                    final double stdev = Math.sqrt(var);
                    return chain * 0.5 * NormalDistribution.differentiateLogPdfStdev(childRate, parentRate, stdev) / stdev;
                }
                case INVERSE_GAUSSIAN:
                    final double parentRateCubed = parentRate * parentRate * parentRate;
                    final double shape = parentRateCubed / var;
                    return chain * parentRateCubed * InverseGaussianDistribution.differentiateLogPdfShape(childRate, parentRate, shape) / (var * var);
                default:
                    throw new RuntimeException("No distribution specified!");
            }
        } else {
            return 0;
        }
    }

    double differentiateBranchRateChangeLogLikelihood(double parentRate, double childRate, double time) {
        double var = variance.getParameterValue(0);
        double chain = 1.0;
        if (!isEpisodic()) {
            var *= time;
            chain *= var;
        }
        switch (distribution) {
            case LOG_NORMAL: {
                final double mean = Math.log(parentRate) - (var / 2.);
                final double stdev = Math.sqrt(var);
                return chain * 0.5 * (-LogNormalDistribution.differentiateLogPdfMean(childRate, mean, stdev)
                        + LogNormalDistribution.differentiateLogPdfStdev(childRate, mean, stdev) / stdev);
            }
            case NORMAL: {
                final double stdev = Math.sqrt(var);
                return chain * 0.5 * NormalDistribution.differentiateLogPdfStdev(childRate, parentRate, stdev) / stdev;
            }
            case INVERSE_GAUSSIAN:
                final double parentRateCubed = parentRate * parentRate * parentRate;
                final double shape = parentRateCubed / var;
                return chain * parentRateCubed * InverseGaussianDistribution.differentiateLogPdfShape(childRate, parentRate, shape) / (var * var);
            default:
                throw new RuntimeException("No distribution specified!");
        }
    }

    double branchRateSample(double parentRate, double time) {

        double var = variance.getParameterValue(0);

        if (!isEpisodic())
            var *= time;

        //if (isLogSpace) {
        //    final double logParentRate = Math.log(parentRate);

        //    return Math.exp(MathUtils.nextGaussian() * Math.sqrt(var) + logParentRate - (var / 2.));
        //} else {
        //    return MathUtils.nextGaussian() * Math.sqrt(var) + parentRate;
        //}

        switch (distribution) {
            case LOG_NORMAL:
                final double logParentRate = Math.log(parentRate);
                return Math.exp(MathUtils.nextGaussian() * Math.sqrt(var) + logParentRate - (var / 2.));
            case NORMAL:
                return MathUtils.nextGaussian() * Math.sqrt(var) + parentRate;
            case INVERSE_GAUSSIAN:
                //return Math.random()
                //Random rand = new Random();
                double lambda = (parentRate * parentRate * parentRate) / var;
                return MathUtils.nextInverseGaussian(parentRate, lambda);
            default:
                throw new RuntimeException("No distribution specified!");
        }
    }

    private Parameter variance;
    //boolean isLogSpace = false;
    Distribution distribution;

    public static final XMLObjectParser<ACLikelihood> PARSER = new AbstractXMLObjectParser<ACLikelihood>() {
        public static final String AC_LIKELIHOOD = "ACLikelihood";

        public static final String VARIANCE = "variance";
        public static final String SHAPE = "shape";

        public static final String DISTRIBUTION = "distribution";

        public String getParserName() {
            return AC_LIKELIHOOD;
        }

        public ACLikelihood parseXMLObject(XMLObject xo) throws XMLParseException {

            TreeModel tree = (TreeModel) xo.getChild(TreeModel.class);

            Parameter ratesParameter = (Parameter) xo.getElementFirstChild(RateEvolutionLikelihood.RATES);

            Parameter rootRate = (Parameter) xo.getElementFirstChild(RateEvolutionLikelihood.ROOTRATE);

            Parameter variance = (Parameter) xo.getElementFirstChild(VARIANCE);

            boolean isEpisodic = xo.getBooleanAttribute(RateEvolutionLikelihood.EPISODIC);

            //Distribution distributionModel = new InverseGaussianDistribution(0,1);
            //Parameter distribution = (Parameter) xo.getElementFirstChild(DISTRIBUTION);
            Distribution distribution = Distribution.fromString(xo.getStringAttribute(DISTRIBUTION));

            //boolean isLogSpace = xo.getAttribute(LOGSPACE, false);

            //return new ACLikelihood(tree, ratesParameter, variance, rootRate, isEpisodic, isLogSpace);
            return new ACLikelihood(tree, ratesParameter, variance, rootRate, isEpisodic, distribution);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return
                    "This element returns an object that can calculate the likelihood " +
                            "of rate changes in a tree under the assumption of " +
                            "distributed rate changes among lineages. " +
                            //"(log)normally distributed rate changes among lineages. " +
                            "Specifically, each branch is assumed to draw a rate from a " +
                            "distribution with mean of the rate in the " +
                            //"(log)normal distribution with mean of the rate in the " +
                            "parent branch and the given standard deviation (the variance can be optionally proportional to " +
                            "branch length).";
        }

        public Class getReturnType() {
            return ACLikelihood.class;
        }


        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(TreeModel.class),
                new ElementRule(RateEvolutionLikelihood.RATES, Parameter.class, "The branch rates parameter", false),
                AttributeRule.newBooleanRule(RateEvolutionLikelihood.EPISODIC, false, "true if model is branch length independent, false if length-dependent."),
                new StringAttributeRule(DISTRIBUTION, "The distribution to use", Distribution.values(), false),
                //AttributeRule.newBooleanRule(LOGSPACE, true, "true if model considers the log of the rates."),
                new ElementRule(RateEvolutionLikelihood.ROOTRATE, Parameter.class, "The root rate parameter"),
                new ElementRule(VARIANCE, Parameter.class, "The standard deviation of the distribution"),
                //new ElementRule(VARIANCE, Parameter.class, "The standard deviation of the (log)normal distribution"),
                //new ElementRule(DISTRIBUTION, Parameter.class, "The distribution to use"),
                //new ElementRule(DISTRIBUTION, ParametricDistributionModel.class, "The distribution model for rates among branches", false),
        };
    };
}
