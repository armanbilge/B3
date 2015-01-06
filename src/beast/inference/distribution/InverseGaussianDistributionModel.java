/*
 * InverseGaussianDistributionModel.java
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
import beast.math.distributions.InverseGaussianDistribution;
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
 * @author Wai Lok Sibon Li
 * @version $Id: InverseGaussianDistributionModel.java,v 1.8 2009/03/30 20:25:59 rambaut Exp $
 */

public class InverseGaussianDistributionModel extends AbstractModel implements ParametricDistributionModel {

    public static final String INVERSEGAUSSIAN_DISTRIBUTION_MODEL = "inverseGaussianDistributionModel";

    /**
     * @param meanParameter  the mean, mu
     * @param igParameter   either the standard deviation parameter, sigma or the shape parameter, lamba
     * @param offset         offset of the distribution
     * @param useShape         whether shape or stdev is used
     */
    public InverseGaussianDistributionModel(Parameter meanParameter, Parameter igParameter, double offset, boolean useShape) {

        super(INVERSEGAUSSIAN_DISTRIBUTION_MODEL);

        if(useShape) {
            this.shapeParameter = igParameter;
            this.stdevParameter = null;
            addVariable(shapeParameter);
            this.shapeParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        }
        else {
            this.stdevParameter = igParameter;
            this.shapeParameter = null;
            addVariable(stdevParameter);
            this.stdevParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        }

        this.meanParameter = meanParameter;
        addVariable(meanParameter);
        this.offset = offset;
        this.meanParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

    }

    public final double getS() {
        if(stdevParameter==null) {
            return Math.sqrt(InverseGaussianDistribution.variance(getM(), getShape()));
        }
        return stdevParameter.getParameterValue(0);
    }

    public final void setS(double S) {
        if(stdevParameter==null) {
            throw new RuntimeException("Standard deviation parameter is not being used");
        }
        else {
            stdevParameter.setParameterValue(0, S);
        }
    }

    public final Parameter getSParameter() {
        if(stdevParameter==null) {
            throw new RuntimeException("Standard deviation parameter is not being used");
        }
        return stdevParameter;
    }

    public final double getShape() {
        if(shapeParameter == null) {
            double shape = (getM() * getM() * getM()) / (getS() * getS());
            return shape;
        }
        return shapeParameter.getParameterValue(0);
    }

    public final void setShape(double shape) {
        if(shapeParameter==null) {
            throw new RuntimeException("Shape parameter is not being used");

        }
        else {
            shapeParameter.setParameterValue(0, shape);
        }
    }

    public final Parameter getShapeParameter() {
        if(shapeParameter==null) {
            throw new RuntimeException("Shape parameter is not being used");
        }
        return shapeParameter;
    }

    /* Unused method */
    //private double getStDev() {
    //return Math.sqrt(InverseGaussianDistribution.variance(getM(), getShape()));//Math.sqrt((getM()*getM()*getM())/getShape());
    //}

    /**
     * @return the mean
     */
    public final double getM() {
        return meanParameter.getParameterValue(0);
    }

    public final void setM(double M) {
        meanParameter.setParameterValue(0, M);
        //double shape = (getM() * getM() * getM()) / (getS() * getS());
        //setShape(shape);
    }

    public final Parameter getMParameter() {
        return meanParameter;
    }

    // *****************************************************************
    // Interface Distribution
    // *****************************************************************

    public double pdf(double x) {
        if (x - offset <= 0.0) return 0.0;
        return InverseGaussianDistribution.pdf(x - offset, getM(), getShape());
    }

    public double logPdf(double x) {
        if (x - offset <= 0.0) return Double.NEGATIVE_INFINITY;
        return InverseGaussianDistribution.logPdf(x - offset, getM(), getShape());
    }

    public double differentiateLogPdf(double x) {
        if (x - offset <= 0.0) return 0.0;
        return InverseGaussianDistribution.differentiateLogPdf(x - offset, getM(), getShape());
    }

    public double cdf(double x) {
        if (x - offset <= 0.0) return 0.0;
        return InverseGaussianDistribution.cdf(x - offset, getM(), getShape());
    }

    public double quantile(double y) {
        return InverseGaussianDistribution.quantile(y, getM(), getShape()) + offset;
    }

    /**
     * @return the mean of the distribution
     */
    public double mean() {
        //return InverseGaussianDistribution.mean(getM(), getShape()) + offset;
        return getM() + offset;
    }

    /**
     * @return the variance of the distribution.
     */
    public double variance() {
        //return InverseGaussianDistribution.variance(getM(), getShape());
        return getS() * getS();
    }

    public final UnivariateFunction getProbabilityDensityFunction() {
        return pdfFunction;
    }

    private final UnivariateFunction pdfFunction = new UnivariateFunction() {
        public final double evaluate(double x) {
            System.out.println("just checking if this ever gets used anyways... probably have to change the getLowerBound in LogNormalDistributionModel if it does");
            return pdf(x);
        }

        public final double getLowerBound() {
            return 0.0;
            //return Double.NEGATIVE_INFINITY;
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
    private final Parameter stdevParameter;
    private final Parameter shapeParameter;
    private final double offset;

    public static final XMLObjectParser<InverseGaussianDistributionModel> PARSER = new AbstractXMLObjectParser<InverseGaussianDistributionModel>() {
        public static final String MEAN = "mean";
        public static final String STDEV = "stdev";
        public static final String SHAPE = "shape";
        public static final String OFFSET = "offset";

        public String getParserName() {
            return INVERSEGAUSSIAN_DISTRIBUTION_MODEL;
        }

        public InverseGaussianDistributionModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter meanParam;

            double offset = xo.getAttribute(OFFSET, 0.0);

            XMLObject cxo = xo.getChild(MEAN);
            if (cxo.getChild(0) instanceof Parameter) {
                meanParam = (Parameter) cxo.getChild(Parameter.class);
            } else {
                meanParam = new Parameter.Default(cxo.getDoubleChild(0));
            }

            if(xo.hasChildNamed(STDEV) && xo.hasChildNamed(SHAPE)) {
                throw new RuntimeException("XML has both standard deviation and shape for Inverse Gaussian distribution");
            }
            else if(xo.hasChildNamed(STDEV)) {
                Parameter stdevParam;
                cxo = xo.getChild(STDEV);
                if (cxo.getChild(0) instanceof Parameter) {
                    stdevParam = (Parameter) cxo.getChild(Parameter.class);
                } else {
                    stdevParam = new Parameter.Default(cxo.getDoubleChild(0));
                }
                return new InverseGaussianDistributionModel(meanParam, stdevParam, offset, false);
            }
            else if(xo.hasChildNamed(SHAPE)) {
                Parameter shapeParam;
                cxo = xo.getChild(SHAPE);
                if (cxo.getChild(0) instanceof Parameter) {
                    shapeParam = (Parameter) cxo.getChild(Parameter.class);
                } else {
                    shapeParam = new Parameter.Default(cxo.getDoubleChild(0));
                }
                return new InverseGaussianDistributionModel(meanParam, shapeParam, offset, true);
            }
            else {
                throw new RuntimeException("XML has neither standard deviation nor shape for Inverse Gaussian distribution");
            }
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(OFFSET, true),
                new ElementRule(MEAN,
                        new XMLSyntaxRule[]{
                                new XORRule(
                                        new ElementRule(Parameter.class),
                                        new ElementRule(Double.class)
                                )}
                        , false),
                new ElementRule(STDEV,
                        new XMLSyntaxRule[]{
                                new XORRule(
                                        new ElementRule(Parameter.class),
                                        new ElementRule(Double.class)
                                )}
                        , true),

                new ElementRule(SHAPE,
                        new XMLSyntaxRule[]{
                                new XORRule(
                                        new ElementRule(Parameter.class),
                                        new ElementRule(Double.class)
                                )}
                        , true)
        };

        public String getParserDescription() {
            return "Describes a inverse gaussian distribution with a given mean and shape (or standard deviation) " +
                    "that can be used in a distributionLikelihood element";
        }

        public Class getReturnType() {
            return InverseGaussianDistributionModel.class;
        }
    };

}