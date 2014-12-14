/*
 * UniformIntegerOperator.java
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

import beast.inference.model.Parameter;
import beast.inference.model.Variable;
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
public class UniformIntegerOperator extends SimpleMCMCOperator {

    public final static String UNIFORM_INTEGER_OPERATOR = "uniformIntegerOperator";

    private final int howMany;

    public UniformIntegerOperator(Parameter parameter, int lower, int upper, double weight, int howMany) {
        this(parameter, weight, howMany);
        this.lower = lower;
        this.upper = upper;
    }

    public UniformIntegerOperator(Parameter parameter, int lower, int upper, double weight) {
        this(parameter, lower, upper, weight, 1);
    }

    public UniformIntegerOperator(Variable parameter, double weight, int howMany) { // Bounds.Staircase
        this.parameter = parameter;
        this.howMany = howMany;
        setWeight(weight);
    }

    /**
     * @return the parameter this operator acts on.
     */
    public Parameter getParameter() {
        return (Parameter) parameter;
    }

    /**
     * @return the Variable this operator acts on.
     */
    public Variable getVariable() {
        return parameter;
    }

    /**
     * change the parameter and return the hastings ratio.
     */
    public final double doOperation() {

        for (int n = 0; n < howMany; ++n) {
            // do not worry about duplication, does not matter
            int index = MathUtils.nextInt(parameter.getSize());

            if (parameter instanceof Parameter) {
                int newValue = MathUtils.nextInt(upper - lower + 1) + lower; // from 0 to n-1, n must > 0,
                ((Parameter) parameter).setParameterValue(index, newValue);
            } else { // Variable<Integer>, Bounds.Staircase

                int upper = ((Variable<Integer>) parameter).getBounds().getUpperLimit(index);
                int lower = ((Variable<Integer>) parameter).getBounds().getLowerLimit(index);
                int newValue = MathUtils.nextInt(upper - lower + 1) + lower; // from 0 to n-1, n must > 0,
                ((Variable<Integer>) parameter).setValue(index, newValue);

            }

        }

        return 0.0;
    }

    //MCMCOperator INTERFACE
    public final String getOperatorName() {
        return "uniformInteger(" + parameter.getId() + ")";
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

    public double getMinimumAcceptanceLevel() {
        return 0.1;
    }

    public double getMaximumAcceptanceLevel() {
        return 0.4;
    }

    public double getMinimumGoodAcceptanceLevel() {
        return 0.20;
    }

    public double getMaximumGoodAcceptanceLevel() {
        return 0.30;
    }

    public String getPerformanceSuggestion() {
        if (Utils.getAcceptanceProbability(this) < getMinimumAcceptanceLevel()) {
            return "";
        } else if (Utils.getAcceptanceProbability(this) > getMaximumAcceptanceLevel()) {
            return "";
        } else {
            return "";
        }
    }

    public String toString() {
        return UNIFORM_INTEGER_OPERATOR + "(" + parameter.getId() + ")";
    }

    //PRIVATE STUFF

    private Variable parameter = null;
    private int upper;
    private int lower;

    public static final XMLObjectParser<UniformIntegerOperator> PARSER = new AbstractXMLObjectParser<UniformIntegerOperator>() {


        public String getParserName() {
            return UNIFORM_INTEGER_OPERATOR;
        }

        public UniformIntegerOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);

            Variable parameter = (Variable) xo.getChild(Variable.class);

            int count = 1;
            if (xo.hasAttribute("count")) count = xo.getIntegerAttribute("count");

            if (parameter instanceof Parameter) {
                int lower = (int) (double) ((Parameter) parameter).getBounds().getLowerLimit(0);
                if (xo.hasAttribute("lower")) lower = xo.getIntegerAttribute("lower");

                int upper = (int) (double) ((Parameter) parameter).getBounds().getUpperLimit(0);
                if (xo.hasAttribute("upper")) upper = xo.getIntegerAttribute("upper");

                if (upper == lower || lower == (int) Double.NEGATIVE_INFINITY || upper == (int) Double.POSITIVE_INFINITY) {
                    throw new XMLParseException(this.getParserName() + " boundaries not found in parameter "
                            + parameter.getId() + " Use operator lower and upper !");
                }

                return new UniformIntegerOperator((Parameter) parameter, lower, upper, weight, count);
            } else { // Variable<Integer>, Bounds.Staircase
                return new UniformIntegerOperator(parameter, weight, count);
            }
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "An operator that picks new parameter values uniformly at random.";
        }

        public Class getReturnType() {
            return UniformIntegerOperator.class;
        }


        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                AttributeRule.newDoubleRule("upper", true),
                AttributeRule.newDoubleRule("lower", true),
                AttributeRule.newDoubleRule("count", true),
                new ElementRule(Variable.class)
        };
    };
}
