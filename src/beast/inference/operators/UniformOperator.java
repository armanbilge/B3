/*
 * UniformOperator.java
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

package beast.inference.operators;

import beast.inference.model.Bounds;
import beast.inference.model.Parameter;
import beast.math.MathUtils;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * A generic uniform sampler/operator for use with a multi-dimensional parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: UniformOperator.java,v 1.16 2005/06/14 10:40:34 rambaut Exp $
 */
public class UniformOperator extends SimpleMCMCOperator {

    public final static String UNIFORM_OPERATOR = "uniformOperator";

    public UniformOperator(Parameter parameter, double weight) {
        this(parameter, weight, null, null);
    }

    public UniformOperator(Parameter parameter, double weight, Double lowerBound, Double upperBound) {
        this.parameter = parameter;
        setWeight(weight);

        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    /**
     * @return the parameter this operator acts on.
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * change the parameter and return the hastings ratio.
     */
    public final double doOperation() {

        final int index = MathUtils.nextInt(parameter.getDimension());
        final Bounds<Double> bounds = parameter.getBounds();
        final double lower = (lowerBound == null ? bounds.getLowerLimit(index) : Math.max(bounds.getLowerLimit(index), lowerBound));
        final double upper = (upperBound == null ? bounds.getUpperLimit(index) : Math.min(bounds.getUpperLimit(index), upperBound));
        final double newValue = (MathUtils.nextDouble() * (upper - lower)) + lower;

        parameter.setParameterValue(index, newValue);

//		System.out.println(newValue + "[" + lower + "," + upper + "]");
        return 0.0;
    }

    //MCMCOperator INTERFACE
    public final String getOperatorName() {
        return "uniform(" + parameter.getParameterName() + ")";
    }

    public final void optimize(double targetProb) {

        throw new RuntimeException("This operator cannot be optimized!");
    }

    public boolean isOptimizing() {
        return false;
    }

    public void setOptimizing(boolean opt) {
        throw new RuntimeException("This operator cannot be optimized!");
    }

    public String getPerformanceSuggestion() {
        return "";
//        final double acceptance = Utils.getAcceptanceProbability(this);
//        if ( acceptance < getMinimumAcceptanceLevel()) {
//            return "";
//        } else if ( acceptance > getMaximumAcceptanceLevel() ) {
//            return "";
//        } else {
//            return "";
//        }
    }

    public String toString() {
        return UNIFORM_OPERATOR + "(" + parameter.getParameterName() + ")";
    }

    //PRIVATE STUFF

    private Parameter parameter = null;
    private final Double lowerBound;
    private final Double upperBound;

    public static final XMLObjectParser<UniformOperator> PARSER = new AbstractXMLObjectParser<UniformOperator>() {
        public static final String LOWER = "lower";
        public static final String UPPER = "upper";

        public String getParserName() {
            return UNIFORM_OPERATOR;
        }

        public UniformOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);
            Parameter parameter = (Parameter) xo.getChild(Parameter.class);

            if( parameter.getDimension() == 0 ) {
                throw new XMLParseException("parameter with 0 dimension.");
            }

            Double lower = null;
            Double upper = null;

            if (xo.hasAttribute(LOWER)) {
                lower = xo.getDoubleAttribute(LOWER);
            }

            if (xo.hasAttribute(UPPER)) {
                upper = xo.getDoubleAttribute(UPPER);
            }

            return new UniformOperator(parameter, weight, lower, upper);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "An operator that picks new parameter values uniformly at random.";
        }

        public Class getReturnType() {
            return UniformOperator.class;
        }


        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                AttributeRule.newDoubleRule(LOWER, true),
                AttributeRule.newDoubleRule(UPPER, true),
                new ElementRule(Parameter.class)
        };
    };
}
