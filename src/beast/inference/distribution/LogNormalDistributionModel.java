/*
 * LogNormalDistributionModel.java
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

package beast.inference.distribution;

import beast.inference.model.AbstractModel;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.math.UnivariateFunction;
import beast.math.distributions.NormalDistribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class that acts as a model for log-normally distributed data.
 *
 * @author Alexei Drummond
 * @version $Id: LogNormalDistributionModel.java,v 1.8 2005/05/24 20:25:59 rambaut Exp $
 */

public class LogNormalDistributionModel extends AbstractModel implements ParametricDistributionModel {

    public static final String LOGNORMAL_DISTRIBUTION_MODEL = "logNormalDistributionModel";

    //if mean is not in real space then exponentiate to get value in the lognormal space
    boolean isMeanInRealSpace;
    boolean isStdevInRealSpace;
    boolean usesStDev = true;

    /**
     * Constructor.
     */
    public LogNormalDistributionModel(Parameter meanParameter, Parameter stdevParameter, double offset, boolean meanInRealSpace, boolean stdevInRealSpace) {

        super(LOGNORMAL_DISTRIBUTION_MODEL);

        isMeanInRealSpace = meanInRealSpace;
        isStdevInRealSpace = stdevInRealSpace;

        this.meanParameter = meanParameter;
        this.scaleParameter = stdevParameter;
        this.offset = offset;
        addVariable(meanParameter);
        if (isMeanInRealSpace) {
            meanParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        } else {
            meanParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
        }
        addVariable(stdevParameter);
        stdevParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
    }

    public LogNormalDistributionModel(Parameter meanParameter, Parameter scaleParameter,
                                      double offset, boolean meanInRealSpace, boolean stdevInRealSpace, boolean usesStDev) {

        super(LOGNORMAL_DISTRIBUTION_MODEL);

        isMeanInRealSpace = meanInRealSpace;
        isStdevInRealSpace = stdevInRealSpace;
        this.usesStDev = usesStDev;

        this.meanParameter = meanParameter;
        this.scaleParameter = scaleParameter;
        this.offset = offset;
        addVariable(meanParameter);
        if (isMeanInRealSpace) {
            meanParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        } else {
            meanParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
        }
        addVariable(this.scaleParameter);
        this.scaleParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
    }


    public final double getS() {
        //System.out.println(isStdevInRealSpace+"\t" + isMeanInRealSpace + "\t" + Math.sqrt(Math.log(1 + scaleParameter.getParameterValue(0)/Math.pow(meanParameter.getParameterValue(0), 2))) + "\t" + scaleParameter.getParameterValue(0));
        if(isStdevInRealSpace) {

            if(isMeanInRealSpace) {
                return Math.sqrt(Math.log(1 + scaleParameter.getParameterValue(0)/Math.pow(meanParameter.getParameterValue(0), 2)));
            }
            else {
                throw new RuntimeException("S can not be computed with M and stdev");
            }
        }
        return scaleParameter.getParameterValue(0);
    }

    public final void setS(double S) {
        scaleParameter.setParameterValue(0, S);
    }

    public final Parameter getSParameter() {
        return scaleParameter;
    }

    /* StDev in this class is actually incorrectly named the S parameter */
    private double getStDev() {
        return usesStDev ? getS() : Math.sqrt(1.0 / getS());
    }

    /**
     * @return the mean (always in log space)
     */
    public final double getM() {
        if (isMeanInRealSpace) {
            double stDev = getStDev();
            return Math.log(meanParameter.getParameterValue(0)) - (0.5 * stDev * stDev);
        } else {
            return meanParameter.getParameterValue(0);

        }
    }

    public final void setM(double M) {
        if (isMeanInRealSpace) {
            double stDev = getStDev();
            meanParameter.setParameterValue(0, Math.exp(M + (0.5 * stDev * stDev)));
        } else {
            meanParameter.setParameterValue(0, M);
        }
    }

    public final Parameter getMeanParameter() {
        return meanParameter;
    }

     public Parameter getPrecisionParameter() {
        if (!usesStDev)
            return scaleParameter;
        return null;
    }

    // *****************************************************************
    // Interface Distribution
    // *****************************************************************

    public double pdf(double x) {
        if (x - offset <= 0.0) return 0.0;
        return NormalDistribution.pdf(Math.log(x - offset), getM(), getStDev()) / (x - offset);
    }


    public double logPdf(double x) {
        if (x - offset <= 0.0) return Double.NEGATIVE_INFINITY;
        return NormalDistribution.logPdf(Math.log(x - offset), getM(), getStDev()) - Math.log(x - offset);
    }

    public double differentiateLogPdf(double x) {
        if (x - offset <= 0.0) return 0.0;
        return NormalDistribution.differentiateLogPdf(Math.log(x - offset), getM(), getStDev()) - Math.log(x - offset);
    }

    public double cdf(double x) {
        if (x - offset <= 0.0) return 0.0;
        return NormalDistribution.cdf(Math.log(x - offset), getM(), getStDev());
    }

    public double quantile(double y) {
        return Math.exp(NormalDistribution.quantile(y, getM(), getStDev())) + offset;
    }

    /**
     * @return the mean of the distribution
     */
    public double mean() {
        return Math.exp(getM() + (getStDev() * getStDev() / 2)) + offset;
    }

    /**
     * @return the variance of the log normal distribution.  Not really the variance of the lognormal but the S^2
     * parameter
     */
    public double variance() {
        if (usesStDev) {
            //double stdev = getStDev();//scaleParameter.getParameterValue(0);
            return getStDev() * getStDev();
        }
        return 1.0 / scaleParameter.getParameterValue(0);
    }

    public final UnivariateFunction getProbabilityDensityFunction() {
        return pdfFunction;
    }

    private final UnivariateFunction pdfFunction = new UnivariateFunction() {
        public final double evaluate(double x) {
            return pdf(Math.log(x));
        }

        public final double getLowerBound() {
            return Double.NEGATIVE_INFINITY;
        }

        public final double getUpperBound() {
            return Double.POSITIVE_INFINITY;
        }
    };

    // *****************************************************************
    // Interface Model
    // *****************************************************************

    public void handleModelChangedEvent(Model model, Object object, int index) {
        // no intermediates need to be recalculated...
    }

    public void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // no intermediates need to be recalculated...
    }

    protected void storeState() {
    } // no additional state needs storing

    protected void restoreState() {
    } // no additional state needs restoring

    protected void acceptState() {
    } // no additional state needs accepting

    // **************************************************************
    // XMLElement IMPLEMENTATION
    // **************************************************************

    public Element createElement(Document document) {
        throw new RuntimeException("Not implemented!");
    }

    // **************************************************************
    // Private instance variables
    // **************************************************************

    private final Parameter meanParameter;
    private final Parameter scaleParameter;
    private final double offset;

    public static final XMLObjectParser<LogNormalDistributionModel> PARSER = new AbstractXMLObjectParser<LogNormalDistributionModel>() {
        public static final String MEAN = "mean";
        public static final String STDEV = "stdev";
        public static final String PRECISION = "precision";
        public static final String OFFSET = "offset";
        public static final String MEAN_IN_REAL_SPACE = "meanInRealSpace";
        public static final String STDEV_IN_REAL_SPACE = "stdevInRealSpace";

        public String getParserName() {
            return LOGNORMAL_DISTRIBUTION_MODEL;
        }

        public LogNormalDistributionModel parseXMLObject(XMLObject xo) throws XMLParseException {
            Parameter meanParam;

            final double offset = xo.getAttribute(OFFSET, 0.0);

            final boolean meanInRealSpace = xo.getAttribute(MEAN_IN_REAL_SPACE, false);
            final boolean stdevInRealSpace = xo.getAttribute(STDEV_IN_REAL_SPACE, false);
            if(!meanInRealSpace && stdevInRealSpace) {
                throw new RuntimeException("Cannot parameterise Lognormal model with M and Stdev");
            }


            {
                final XMLObject cxo = xo.getChild(MEAN);
                if (cxo.getChild(0) instanceof Parameter) {
                    meanParam = (Parameter) cxo.getChild(Parameter.class);
                } else {
                    meanParam = new Parameter.Default(cxo.getDoubleChild(0));
                }
            }

            {
                final XMLObject cxo = xo.getChild(PRECISION);
                if (cxo != null) {
                    Parameter precParam;
                    if (cxo.getChild(0) instanceof Parameter) {
                        precParam = (Parameter) cxo.getChild(Parameter.class);
                    } else {
                        precParam = new Parameter.Default(cxo.getDoubleChild(0));
                    }
                    return new LogNormalDistributionModel(meanParam, precParam, offset, meanInRealSpace,stdevInRealSpace, false);
                }
            }
            {
                final XMLObject cxo = xo.getChild(STDEV);
                Parameter stdevParam;
                if (cxo.getChild(0) instanceof Parameter) {
                    stdevParam = (Parameter) cxo.getChild(Parameter.class);
                } else {
                    stdevParam = new Parameter.Default(cxo.getDoubleChild(0));
                }

                return new LogNormalDistributionModel(meanParam, stdevParam, offset, meanInRealSpace, stdevInRealSpace);
            }
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newBooleanRule(MEAN_IN_REAL_SPACE, true),
                AttributeRule.newBooleanRule(STDEV_IN_REAL_SPACE, true),
                AttributeRule.newDoubleRule(OFFSET, true),
                new ElementRule(MEAN,
                        new XMLSyntaxRule[]{
                                new XORRule(
                                        new ElementRule(Parameter.class),
                                        new ElementRule(Double.class)
                                )}
                ),
                new XORRule(
                        new ElementRule(STDEV,
                                new XMLSyntaxRule[]{
                                        new XORRule(
                                                new ElementRule(Parameter.class),
                                                new ElementRule(Double.class)
                                        )}
                        ),
                        new ElementRule(PRECISION,
                                new XMLSyntaxRule[]{
                                        new XORRule(
                                                new ElementRule(Parameter.class),
                                                new ElementRule(Double.class)
                                        )}

                        ))
        };

        public String getParserDescription() {
            return "Describes a normal distribution with a given mean and standard deviation " +
                    "that can be used in a distributionLikelihood element";
        }

        public Class getReturnType() {
            return LogNormalDistributionModel.class;
        }
    };
}
