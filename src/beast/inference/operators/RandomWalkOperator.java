/*
 * RandomWalkOperator.java
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
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic random walk operator for use with a multi-dimensional parameters.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: RandomWalkOperator.java,v 1.16 2005/06/14 10:40:34 rambaut Exp $
 */
public class RandomWalkOperator extends AbstractCoercableOperator {

    public static final String RANDOM_WALK_OPERATOR = "randomWalkOperator";

    public enum BoundaryCondition {
        reflecting,
        absorbing
    }

    public RandomWalkOperator(Parameter parameter, double windowSize, BoundaryCondition bc, double weight, CoercionMode mode) {
        this(parameter, null, windowSize, bc, weight, mode);
    }

    public RandomWalkOperator(Parameter parameter, Parameter updateIndex, double windowSize, BoundaryCondition bc,
                              double weight, CoercionMode mode) {
        this(parameter, updateIndex, windowSize, bc, weight, mode, null, null);
    }

    public RandomWalkOperator(Parameter parameter, Parameter updateIndex, double windowSize, BoundaryCondition bc,
                              double weight, CoercionMode mode, Double lowerOperatorBound, Double upperOperatorBound) {
        super(mode);
        this.parameter = parameter;
        this.windowSize = windowSize;
        this.condition = bc;

        setWeight(weight);
        if (updateIndex != null) {
            updateMap = new ArrayList<Integer>();
            for (int i = 0; i < updateIndex.getDimension(); i++) {
                if (updateIndex.getParameterValue(i) == 1.0)
                    updateMap.add(i);
            }
            updateMapSize=updateMap.size();
        }

        this.lowerOperatorBound = lowerOperatorBound;
        this.upperOperatorBound = upperOperatorBound;
    }

    /**
     * @return the parameter this operator acts on.
     */
    public Parameter getParameter() {
        return parameter;
    }

    public final double getWindowSize() {
        return windowSize;
    }

    /**
     * change the parameter and return the hastings ratio.
     */
    public final double doOperation() throws OperatorFailedException {

        // a random dimension to perturb
        int index;
        if (updateMap == null) {
            index = MathUtils.nextInt(parameter.getDimension());
        } else {
            index = updateMap.get(MathUtils.nextInt(updateMapSize));
        }

        // a random point around old value within windowSize * 2
        double draw = (2.0 * MathUtils.nextDouble() - 1.0) * windowSize;
        double newValue = parameter.getParameterValue(index) + draw;

        final Bounds<Double> bounds = parameter.getBounds();
        final double lower = (lowerOperatorBound == null ? bounds.getLowerLimit(index) : Math.max(bounds.getLowerLimit(index), lowerOperatorBound));
        final double upper = (upperOperatorBound == null ? bounds.getUpperLimit(index) : Math.min(bounds.getUpperLimit(index), upperOperatorBound));

        if (condition == BoundaryCondition.reflecting) {
            newValue = reflectValue(newValue, lower, upper);
        } else if (newValue < lower || newValue > upper) {
            throw new OperatorFailedException("proposed value outside boundaries");
        }

        parameter.setParameterValue(index, newValue);

        return 0.0;
    }

    public double reflectValue(double value, double lower, double upper) {

        double newValue = value;

        if (value < lower) {
            if (Double.isInfinite(upper)) {
                // we are only going to reflect once as the upper bound is at infinity...
                newValue = lower + (lower - value);
            } else {
                double remainder = lower - value;

                double widths = Math.floor(remainder / (upper - lower));
                remainder -= (upper - lower) * widths;

                // even reflections
                if (widths % 2 == 0) {
                    newValue = lower + remainder;
                    // odd reflections
                } else {
                    newValue = upper - remainder;
                }
            }
        } else if (value > upper) {
            if (Double.isInfinite(lower)) {
                // we are only going to reflect once as the lower bound is at -infinity...
                newValue = upper - (newValue - upper);
            } else {

                double remainder = value - upper;

                double widths = Math.floor(remainder / (upper - lower));
                remainder -= (upper - lower) * widths;

                // even reflections
                if (widths % 2 == 0) {
                    newValue = upper - remainder;
                    // odd reflections
                } else {
                    newValue = lower + remainder;
                }
            }
        }

        return newValue;
    }

    public double reflectValueLoop(double value, double lower, double upper) {
        double newValue = value;

        while (newValue < lower || newValue > upper) {
            if (newValue < lower) {
                newValue = lower + (lower - newValue);
            }
            if (newValue > upper) {
                newValue = upper - (newValue - upper);

            }
        }

        return newValue;
    }

    //MCMCOperator INTERFACE
    public final String getOperatorName() {
        return parameter.getParameterName();
    }

    public double getCoercableParameter() {
        return Math.log(windowSize);
    }

    public void setCoercableParameter(double value) {
        windowSize = Math.exp(value);
    }

    public double getRawParameter() {
        return windowSize;
    }

    public double getTargetAcceptanceProbability() {
        return 0.234;
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

    public final String getPerformanceSuggestion() {

        double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
        double targetProb = getTargetAcceptanceProbability();

        double ws = OperatorUtils.optimizeWindowSize(windowSize, parameter.getParameterValue(0) * 2.0, prob, targetProb);

        if (prob < getMinimumGoodAcceptanceLevel()) {
            return "Try decreasing windowSize to about " + ws;
        } else if (prob > getMaximumGoodAcceptanceLevel()) {
            return "Try increasing windowSize to about " + ws;
        } else return "";
    }

    public String toString() {
        return RANDOM_WALK_OPERATOR + "(" + parameter.getParameterName() + ", " + windowSize + ", " + getWeight() + ")";
    }

    //PRIVATE STUFF

    private Parameter parameter = null;
    private double windowSize = 0.01;
    private List<Integer> updateMap = null;
    private int updateMapSize;
    private final BoundaryCondition condition;

    private final Double lowerOperatorBound;
    private final Double upperOperatorBound;

    public static final XMLObjectParser<RandomWalkOperator> PARSER = new AbstractXMLObjectParser<RandomWalkOperator>() {

        public static final String WINDOW_SIZE = "windowSize";
        public static final String UPDATE_INDEX = "updateIndex";
        public static final String UPPER = "upper";
        public static final String LOWER = "lower";

        public static final String BOUNDARY_CONDITION = "boundaryCondition";

        public String getParserName() {
            return RANDOM_WALK_OPERATOR;
        }

        public RandomWalkOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            CoercionMode mode = CoercionMode.parseMode(xo);

            double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);
            double windowSize = xo.getDoubleAttribute(WINDOW_SIZE);
            Parameter parameter = (Parameter) xo.getChild(Parameter.class);

            Double lower = null;
            Double upper = null;

            if (xo.hasAttribute(LOWER)) {
                lower = xo.getDoubleAttribute(LOWER);
            }

            if (xo.hasAttribute(UPPER)) {
                upper = xo.getDoubleAttribute(UPPER);
            }

            RandomWalkOperator.BoundaryCondition condition = RandomWalkOperator.BoundaryCondition.valueOf(
                    xo.getAttribute(BOUNDARY_CONDITION, RandomWalkOperator.BoundaryCondition.reflecting.name()));

            if (xo.hasChildNamed(UPDATE_INDEX)) {
                XMLObject cxo = xo.getChild(UPDATE_INDEX);
                Parameter updateIndex = (Parameter) cxo.getChild(Parameter.class);
                if (updateIndex.getDimension() != parameter.getDimension())
                    throw new RuntimeException("Parameter to update and missing indices must have the same dimension");
                return new RandomWalkOperator(parameter, updateIndex, windowSize, condition,
                        weight, mode, lower, upper);
            }

            return new RandomWalkOperator(parameter, null, windowSize, condition, weight, mode, lower, upper);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element returns a random walk operator on a given parameter.";
        }

        public Class getReturnType() {
            return MCMCOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(WINDOW_SIZE),
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                AttributeRule.newDoubleRule(LOWER, true),
                AttributeRule.newDoubleRule(UPPER, true),
                AttributeRule.newBooleanRule(CoercableMCMCOperator.AUTO_OPTIMIZE, true),
                new ElementRule(UPDATE_INDEX,
                        new XMLSyntaxRule[] {
                                new ElementRule(Parameter.class),
                        },true),
                new StringAttributeRule(BOUNDARY_CONDITION, null, RandomWalkOperator.BoundaryCondition.values(), true),
                new ElementRule(Parameter.class)
        };
    };
}
