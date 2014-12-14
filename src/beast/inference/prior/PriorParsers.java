/*
 * PriorParsers.java
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

package beast.inference.prior;

import beast.inference.distribution.DistributionLikelihood;
import beast.inference.model.Likelihood;
import beast.inference.model.Statistic;
import beast.math.distributions.BetaDistribution;
import beast.math.distributions.ExponentialDistribution;
import beast.math.distributions.GammaDistribution;
import beast.math.distributions.HalfTDistribution;
import beast.math.distributions.InverseGammaDistribution;
import beast.math.distributions.LaplaceDistribution;
import beast.math.distributions.LogNormalDistribution;
import beast.math.distributions.NegativeBinomialDistribution;
import beast.math.distributions.NormalDistribution;
import beast.math.distributions.PoissonDistribution;
import beast.math.distributions.UniformDistribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

/**
 */
public class PriorParsers {
    public final static boolean DEBUG = false;

    public static final String UNIFORM_PRIOR = "uniformPrior";
    public static final String EXPONENTIAL_PRIOR = "exponentialPrior";
    public static final String POISSON_PRIOR = "poissonPrior";
    public static final String NEGATIVE_BINOMIAL_PRIOR = "negativeBinomialPrior";
    public static final String NORMAL_PRIOR = "normalPrior";
    public static final String LOG_NORMAL_PRIOR = "logNormalPrior";
    public static final String GAMMA_PRIOR = "gammaPrior";
    public static final String INVGAMMA_PRIOR = "invgammaPrior";
    public static final String INVGAMMA_PRIOR_CORRECT = "inverseGammaPrior";
    public static final String LAPLACE_PRIOR = "laplacePrior";
    public static final String BETA_PRIOR = "betaPrior";
    public static final String UPPER = "upper";
    public static final String LOWER = "lower";
    public static final String MEAN = "mean";
    public static final String MEAN_IN_REAL_SPACE = "meanInRealSpace";
    public static final String STDEV = "stdev";
    public static final String SHAPE = "shape";
    public static final String SHAPEB = "shapeB";
    public static final String SCALE = "scale";
    public static final String DF = "df";
    public static final String OFFSET = "offset";
    public static final String HALF_T_PRIOR = "halfTPrior";

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser UNIFORM_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return UNIFORM_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double lower = xo.getDoubleAttribute(LOWER);
            double upper = xo.getDoubleAttribute(UPPER);

            if (lower == Double.NEGATIVE_INFINITY || upper == Double.POSITIVE_INFINITY)
                throw new XMLParseException("Uniform prior " + xo.getName() + " cannot take a bound at infinity, " +
                        "because it returns 1/(high-low) = 1/inf");

            DistributionLikelihood likelihood = new DistributionLikelihood(new UniformDistribution(lower, upper));
            if (DEBUG) {
                System.out.println("Uniform prior: " + xo.getChildCount());
            }
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (DEBUG) {
                    System.out.println(xo.getChild(j));
                }
                if (xo.getChild(j) instanceof Statistic) {
                    if (DEBUG) {
                        //System.out.println((Statistic) xo.getChild(j));
                        Statistic test = (Statistic) xo.getChild(j);
                        System.out.println(test.getDimension());
                        for (int i = 0; i < test.getDimension(); i++) {
                            System.out.println("  " + test.getDimensionName(i) + " - " + test.getStatisticValue(i));
                        }
                        System.out.println(test.getClass());
                    }
                    likelihood.addData((Statistic) xo.getChild(j));
                    if (DEBUG) {
                    	likelihood.calculateLogLikelihood();
                    	System.out.println("likelihood: " + likelihood.getLogLikelihood());
                    }
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }
            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(LOWER),
                AttributeRule.newDoubleRule(UPPER),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given uniform distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser EXPONENTIAL_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return EXPONENTIAL_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double scale;

            if (xo.hasAttribute(SCALE)) {
                scale = xo.getDoubleAttribute(SCALE);
            } else {
                scale = xo.getDoubleAttribute(MEAN);
            }
            final double offset = xo.hasAttribute(OFFSET) ? xo.getDoubleAttribute(OFFSET) : 0.0;

            DistributionLikelihood likelihood = new DistributionLikelihood(new ExponentialDistribution(1.0 / scale), offset);
            if (DEBUG) {
                System.out.println("Exponential prior: " + xo.getChildCount());
            }
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (DEBUG) {
                    System.out.println(xo.getChild(j));
                }
                if (xo.getChild(j) instanceof Statistic) {
                    if (DEBUG) {
                    	System.out.println("scale: " + scale);
                    	System.out.println("offset: " + offset);
                        //System.out.println((Statistic) xo.getChild(j));
                        Statistic test = (Statistic) xo.getChild(j);
                        System.out.println(test.getDimension());
                        for (int i = 0; i < test.getDimension(); i++) {
                            System.out.println("  " + test.getDimensionName(i) + " - " + test.getStatisticValue(i));
                        }
                        System.out.println(test.getClass());
                    }
                    likelihood.addData((Statistic) xo.getChild(j));
                    if (DEBUG) {
                    	likelihood.makeDirty();
                    	likelihood.calculateLogLikelihood();
                    	System.out.println("likelihood: " + likelihood.getLogLikelihood());
                    }
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new XORRule(
                        AttributeRule.newDoubleRule(SCALE),
                        AttributeRule.newDoubleRule(MEAN)
                ),
                AttributeRule.newDoubleRule(OFFSET, true),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given exponential distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser POISSON_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return POISSON_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double mean = xo.getDoubleAttribute(MEAN);
            double offset = xo.getDoubleAttribute(OFFSET);

            DistributionLikelihood likelihood = new DistributionLikelihood(new PoissonDistribution(mean), offset);
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MEAN),
                AttributeRule.newDoubleRule(OFFSET),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given poisson distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser NEGATIVE_BINOMIAL_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return NEGATIVE_BINOMIAL_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double mean = xo.getDoubleAttribute(MEAN);
            double stdev = xo.getDoubleAttribute(STDEV);

            DistributionLikelihood likelihood = new DistributionLikelihood(new NegativeBinomialDistribution(mean, stdev));
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MEAN),
                AttributeRule.newDoubleRule(STDEV),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given negative binomial distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser HALF_T_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return HALF_T_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double scale = xo.getDoubleAttribute(SCALE);
            double df = xo.getDoubleAttribute(DF);

            DistributionLikelihood likelihood = new DistributionLikelihood(new HalfTDistribution(scale, df));
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(SCALE),
                AttributeRule.newDoubleRule(DF),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given half-T distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser NORMAL_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return NORMAL_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double mean = xo.getDoubleAttribute(MEAN);
            double stdev = xo.getDoubleAttribute(STDEV);

            DistributionLikelihood likelihood = new DistributionLikelihood(new NormalDistribution(mean, stdev));
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MEAN),
                AttributeRule.newDoubleRule(STDEV),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given normal distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     * <p/>
     * If X ~ logNormal, then log(X) ~ Normal.
     * <br>
     * <br>
     * If meanInRealSpace=false, <code>mean</code> specifies the mean of log(X) and
     * <code>stdev</code> specifies the standard deviation of log(X).
     * <br>
     * <br>
     * If meanInRealSpace=true, <code>mean</code> specifies the mean of X, but <code>
     * stdev</code> specifies the standard deviation of log(X).
     * <br>
     */
    public static XMLObjectParser LOG_NORMAL_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return LOG_NORMAL_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double mean = xo.getDoubleAttribute(MEAN);
            final double stdev = xo.getDoubleAttribute(STDEV);
            final double offset = xo.getAttribute(OFFSET, 0.0);
            final boolean meanInRealSpace = xo.getAttribute(MEAN_IN_REAL_SPACE, false);

            if (meanInRealSpace) {
                if (mean <= 0) {
                    throw new IllegalArgumentException("meanInRealSpace works only for a positive mean");
                }
                mean = Math.log(mean) - 0.5 * stdev * stdev;
            }

            final DistributionLikelihood likelihood = new DistributionLikelihood(new LogNormalDistribution(mean, stdev), offset);

            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MEAN),
                AttributeRule.newDoubleRule(STDEV),
                AttributeRule.newDoubleRule(OFFSET, true),
                AttributeRule.newBooleanRule(MEAN_IN_REAL_SPACE, true),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given lognormal distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };


    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser GAMMA_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return GAMMA_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            final double shape = xo.getDoubleAttribute(SHAPE);
            final double scale = xo.getDoubleAttribute(SCALE);
            final double offset = xo.getAttribute(OFFSET, 0.0);

            DistributionLikelihood likelihood = new DistributionLikelihood(new GammaDistribution(shape, scale), offset);
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(SHAPE),
                AttributeRule.newDoubleRule(SCALE),
                AttributeRule.newDoubleRule(OFFSET, true),
                // AttributeRule.newBooleanRule(UNINFORMATIVE, true),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given gamma distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser INVGAMMA_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return INVGAMMA_PRIOR;
        }

        public String[] getParserNames() {
            return new String[]{INVGAMMA_PRIOR, INVGAMMA_PRIOR_CORRECT};
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            final double shape = xo.getDoubleAttribute(SHAPE);
            final double scale = xo.getDoubleAttribute(SCALE);
            final double offset = xo.getDoubleAttribute(OFFSET);

            DistributionLikelihood likelihood = new DistributionLikelihood(new InverseGammaDistribution(shape, scale), offset);

            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(SHAPE),
                AttributeRule.newDoubleRule(SCALE),
                AttributeRule.newDoubleRule(OFFSET),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given inverse gamma distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    public static XMLObjectParser LAPLACE_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return LAPLACE_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            double mean = xo.getDoubleAttribute(MEAN);
            double scale = xo.getDoubleAttribute(SCALE);

            DistributionLikelihood likelihood = new DistributionLikelihood(new LaplaceDistribution(mean, scale));
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MEAN),
                AttributeRule.newDoubleRule(SCALE),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given laplace distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };

    /**
     * A special parser that reads a convenient short form of priors on parameters.
     */
    public static XMLObjectParser BETA_PRIOR_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return BETA_PRIOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            final double shape = xo.getDoubleAttribute(SHAPE);
            final double shapeB = xo.getDoubleAttribute(SHAPEB);
            final double offset = xo.getAttribute(OFFSET, 0.0);

            DistributionLikelihood likelihood = new DistributionLikelihood(new BetaDistribution(shape, shapeB), offset);
            for (int j = 0; j < xo.getChildCount(); j++) {
                if (xo.getChild(j) instanceof Statistic) {
                    likelihood.addData((Statistic) xo.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + xo.getName() + " element");
                }
            }

            return likelihood;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(SHAPE),
                AttributeRule.newDoubleRule(SHAPEB),
                AttributeRule.newDoubleRule(OFFSET, true),
                new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "Calculates the prior probability of some data under a given beta distribution.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }
    };
    
}
