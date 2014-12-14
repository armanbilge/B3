/*
 * UniformDistributionModel.java
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
import beast.math.distributions.UniformDistribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

/**
 * A class that acts as a model for uniformly distributed data.
 *
 * @author Alexei Drummond
 *         $Id$
 */

public class UniformDistributionModel extends AbstractModel implements ParametricDistributionModel {

    public static final String UNIFORM_DISTRIBUTION_MODEL = "uniformDistributionModel";

    /*
      * Constructor.
      */
    public UniformDistributionModel(Parameter lowerParameter, Parameter upperParameter) {

        super(UNIFORM_DISTRIBUTION_MODEL);

        this.lowerParameter = lowerParameter;
        addVariable(lowerParameter);
        lowerParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
        this.upperParameter = upperParameter;
        addVariable(upperParameter);
        upperParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1));
    }

    public double getLower() {
        return lowerParameter.getParameterValue(0);
    }

    public double getUpper() {
        return upperParameter.getParameterValue(0);
    }

    // *****************************************************************
    // Interface Distribution
    // *****************************************************************

    public double pdf(double x) {
        return UniformDistribution.pdf(x, getLower(), getUpper());
    }

    public double logPdf(double x) {
        return UniformDistribution.logPdf(x, getLower(), getUpper());
    }

    public double cdf(double x) {
        return UniformDistribution.cdf(x, getLower(), getUpper());
    }

    public double quantile(double y) {
        return UniformDistribution.quantile(y, getLower(), getUpper());
    }

    public double mean() {
        return UniformDistribution.mean(getLower(), getUpper());
    }

    public double variance() {
        return UniformDistribution.variance(getLower(), getUpper());
    }

    public final UnivariateFunction getProbabilityDensityFunction() {
        return pdfFunction;
    }

    private final UnivariateFunction pdfFunction = new UnivariateFunction() {
        public final double evaluate(double x) {
            return 1.0;
        }

        public final double getLowerBound() {
            return getLower();
        }

        public final double getUpperBound() {
            return getUpper();
        }
    };

    // *****************************************************************
    // Interface Model
    // *****************************************************************

    public void handleModelChangedEvent(Model model, Object object, int index) {
        // no intermediates need to be recalculated...
    }

    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // no intermediates need to be recalculated...
    }

    protected void storeState() {
    } // no additional state needs storing

    protected void restoreState() {
    } // no additional state needs restoring

    protected void acceptState() {
    } // no additional state needs accepting

    // **************************************************************
    // Private instance variables
    // **************************************************************

    private final Parameter lowerParameter;
    private final Parameter upperParameter;

    public static final XMLObjectParser<UniformDistributionModel> PARSER = new AbstractXMLObjectParser<UniformDistributionModel>() {
        public static final String LOWER = "lower";
        public static final String UPPER = "upper";

        public String getParserName() {
            return UNIFORM_DISTRIBUTION_MODEL;
        }

        public UniformDistributionModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter upperParam;
            Parameter lowerParam;

            XMLObject cxo = xo.getChild(LOWER);
            if (cxo.getChild(0) instanceof Parameter) {
                lowerParam = (Parameter) cxo.getChild(Parameter.class);
            } else {
                lowerParam = new Parameter.Default(cxo.getDoubleChild(0));
            }

            cxo = xo.getChild(UPPER);
            if (cxo.getChild(0) instanceof Parameter) {
                upperParam = (Parameter) cxo.getChild(Parameter.class);
            } else {
                upperParam = new Parameter.Default(cxo.getDoubleChild(0));
            }

            return new UniformDistributionModel(lowerParam, upperParam);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(LOWER,
                        new XMLSyntaxRule[]{
                                new XORRule(
                                        new ElementRule(Parameter.class),
                                        new ElementRule(Double.class)
                                )}
                ),
                new ElementRule(UPPER,
                        new XMLSyntaxRule[]{
                                new XORRule(
                                        new ElementRule(Parameter.class),
                                        new ElementRule(Double.class)
                                )}
                )
        };

        public String getParserDescription() {
            return "Describes a uniform distribution with a given lower and upper bounds ";
        }

        public Class getReturnType() {
            return UniformDistributionModel.class;
        }
    };

}
