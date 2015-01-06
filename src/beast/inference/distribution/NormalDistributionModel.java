/*
 * NormalDistributionModel.java
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
import beast.math.MathUtils;
import beast.math.UnivariateFunction;
import beast.math.distributions.NormalDistribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class that acts as a model for normally distributed data.
 *
 * @author Alexei Drummond
 * @version $Id: NormalDistributionModel.java,v 1.6 2005/05/24 20:25:59 rambaut Exp $
 */

public class NormalDistributionModel extends AbstractModel implements ParametricDistributionModel/*, RandomGenerator*/ {

    public static final String NORMAL_DISTRIBUTION_MODEL = "normalDistributionModel";

    /**
     * Constructor.
     */
    public NormalDistributionModel(Variable<Double> mean, Variable<Double> stdev) {

        super(NORMAL_DISTRIBUTION_MODEL);

        this.mean = mean;
        this.stdev = stdev;
        addVariable(mean);
        mean.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
        addVariable(stdev);
        stdev.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
    }

    public NormalDistributionModel(Parameter meanParameter, Parameter scale, boolean isPrecision) {
        super(NORMAL_DISTRIBUTION_MODEL);
        this.hasPrecision = isPrecision;
        this.mean = meanParameter;
        addVariable(meanParameter);
        meanParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
        if (isPrecision) {
            this.precision = scale;
            this.stdev = null;  // todo why not keep the name scale to avoid confusion??
        } else {
            this.stdev = scale;
        }
        addVariable(scale);
        scale.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
    }

    public double getStdev() {
        if (hasPrecision)
            return 1.0 / Math.sqrt(precision.getValue(0));
        return stdev.getValue(0);
    }

    public Variable<Double> getMean() {
        return mean;
    }

    public Variable<Double> getPrecision() {
        if (hasPrecision)
            return precision;
        return null;
    }

    // *****************************************************************
    // Interface Distribution
    // *****************************************************************

    public double pdf(double x) {
        return NormalDistribution.pdf(x, mean(), getStdev());
    }

    public double logPdf(double x) {
        return NormalDistribution.logPdf(x, mean(), getStdev());
    }

    public double differentiateLogPdf(double x) {
        return NormalDistribution.differentiateLogPdf(x, mean(), getStdev());
    }


    public double cdf(double x) {
        return NormalDistribution.cdf(x, mean(), getStdev());
    }

    public double quantile(double y) {
        return NormalDistribution.quantile(y, mean(), getStdev());
    }

    public double mean() {
        return mean.getValue(0);
    }

    public double variance() {
        if (hasPrecision)
            return 1.0 / precision.getValue(0);
        double sd = stdev.getValue(0);
        return sd * sd;
    }

    public final UnivariateFunction getProbabilityDensityFunction() {
        return pdfFunction;
    }

    private final UnivariateFunction pdfFunction = new UnivariateFunction() {
        public final double evaluate(double x) {
            return pdf(x);
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

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // no intermediates need to be recalculated...
    }

    protected void storeState() {
    } // no additional state needs storing

    protected void restoreState() {
    } // no additional state needs restoring

    protected void acceptState() {
    } // no additional state needs accepting

    public Element createElement(Document document) {
        throw new RuntimeException("Not implemented!");
    }

    // **************************************************************
    // Private instance variables
    // **************************************************************

    private final Variable<Double> mean;
    private final Variable<Double> stdev;
    private Variable<Double> precision;
    private boolean hasPrecision = false;

    public Object nextRandom() {
        double eps = MathUtils.nextGaussian();
        eps *= getStdev();
        eps += mean();
        return eps;
    }

    public double logPdf(Object x) {
        double v = (Double) x;
        return logPdf(v);
    }

    public static final XMLObjectParser<NormalDistributionModel> PARSER = new AbstractXMLObjectParser<NormalDistributionModel>() {
        public static final String MEAN = "mean";
        public static final String STDEV = "stdev";
        public static final String PREC = "precision";

        public String getParserName() {
            return NORMAL_DISTRIBUTION_MODEL;
        }

        public NormalDistributionModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter meanParam;
            Parameter stdevParam;
            Parameter precParam;

            XMLObject cxo = xo.getChild(MEAN);
            if (cxo.getChild(0) instanceof Parameter) {
                meanParam = (Parameter) cxo.getChild(Parameter.class);
            } else {
                meanParam = new Parameter.Default(cxo.getDoubleChild(0));
            }

            if (xo.getChild(STDEV) != null) {

                cxo = xo.getChild(STDEV);
                if (cxo.getChild(0) instanceof Parameter) {
                    stdevParam = (Parameter) cxo.getChild(Parameter.class);
                } else {
                    stdevParam = new Parameter.Default(cxo.getDoubleChild(0));
                }

                return new NormalDistributionModel(meanParam, stdevParam);
            }

            cxo = xo.getChild(PREC);
            if (cxo.getChild(0) instanceof Parameter) {
                precParam = (Parameter) cxo.getChild(Parameter.class);
            } else {
                precParam = new Parameter.Default(cxo.getDoubleChild(0));
            }
            return new NormalDistributionModel(meanParam, precParam, true);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
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
                        new ElementRule(PREC,
                                new XMLSyntaxRule[]{
                                        new XORRule(
                                                new ElementRule(Parameter.class),
                                                new ElementRule(Double.class)
                                        )}
                        )
                )
        };

        public String getParserDescription() {
            return "Describes a normal distribution with a given mean and standard deviation " +
                    "that can be used in a distributionLikelihood element";
        }

        public Class getReturnType() {
            return NormalDistributionModel.class;
        }

    };

}
