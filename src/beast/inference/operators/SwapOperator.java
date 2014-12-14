/*
 * SwapOperator.java
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
import beast.math.MathUtils;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A generic operator swapping a number of pairs in a multi-dimensional parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class SwapOperator extends SimpleMCMCOperator {

    public final static String SWAP_OPERATOR = "swapOperator";

    private int size = 1;

    public SwapOperator(Parameter parameter, int size) {
        this.parameter = parameter;
        this.size = size;
        if (parameter.getDimension() < 2 * size) {
            throw new IllegalArgumentException();
        }

        int dimension = parameter.getDimension();
        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < dimension; i++) {
            list.add(i);
        }
        masterList = Collections.unmodifiableList(list);
    }

    /**
     * @return the parameter this operator acts on.
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * swap the values in two random parameter slots.
     */
    public final double doOperation() {

        List<Integer> allIndices = new ArrayList<Integer>(masterList);
        int left, right;

        for (int i = 0; i < size; i++) {
            left = allIndices.remove(MathUtils.nextInt(allIndices.size()));
            right = allIndices.remove(MathUtils.nextInt(allIndices.size()));
            double value1 = parameter.getParameterValue(left);
            double value2 = parameter.getParameterValue(right);
            parameter.setParameterValue(left, value2);
            parameter.setParameterValue(right, value1);
        }

        return 0.0;
    }

    public String getOperatorName() {
        return SWAP_OPERATOR + "(" + parameter.getParameterName() + ")";
    }

    public String getPerformanceSuggestion() {
        return "No suggestions";
    }

    //PRIVATE STUFF

    private Parameter parameter = null;
    private List<Integer> masterList = null;

    public static final XMLObjectParser<SwapOperator> PARSER = new AbstractXMLObjectParser<SwapOperator>() {

        public String getParserName() {
            return SWAP_OPERATOR;
        }

        public SwapOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter parameter = (Parameter) xo.getChild(Parameter.class);
            double weight = xo.getDoubleAttribute("weight");
            int size = xo.getIntegerAttribute("size");

            boolean autoOptimize = xo.getBooleanAttribute("autoOptimize");
            if (autoOptimize) throw new XMLParseException("swapOperator can't be optimized!");

            System.out.println("Creating swap operator for parameter " + parameter.getParameterName() + " (weight=" + weight + ")");

            SwapOperator so = new SwapOperator(parameter, size);
            so.setWeight(weight);

            return so;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents an operator that swaps values in a multi-dimensional parameter.";
        }

        public Class getReturnType() {
            return SwapOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{

                AttributeRule.newDoubleRule("weight"),
                AttributeRule.newIntegerRule("size"),
                AttributeRule.newBooleanRule("autoOptimize"),
                new ElementRule(Parameter.class)
        };
    };
}
