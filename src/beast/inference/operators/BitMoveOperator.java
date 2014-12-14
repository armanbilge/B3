/*
 * BitMoveOperator.java
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
import java.util.List;

/**
 * A generic operator that moves k 1 bits to k zero locations.
 *
 * @author Alexei Drummond
 * @version $Id$
 */
public class BitMoveOperator extends SimpleMCMCOperator {

    public BitMoveOperator(Parameter bitsParameter, Parameter valuesParameter, int numBitsToMove, double weight) {
        this.bitsParameter = bitsParameter;
        this.valuesParameter = valuesParameter;

        if (valuesParameter != null && bitsParameter.getDimension() != valuesParameter.getDimension()) {
            throw new IllegalArgumentException("bits parameter must be same length as values parameter");
        }

        this.numBitsToMove = numBitsToMove;
        setWeight(weight);
    }

    /**
     * Pick a random k ones in the vector and move them to a random k zero positions.
     */
    public final double doOperation() throws OperatorFailedException {

        final int dim = bitsParameter.getDimension();
        List<Integer> ones = new ArrayList<Integer>();
        List<Integer> zeros = new ArrayList<Integer>();

        for (int i = 0; i < dim; i++) {
            if (bitsParameter.getParameterValue(i) == 1.0) {
                ones.add(i);
            } else {
                zeros.add(i);
            }
        }

        if (ones.size() >= numBitsToMove && zeros.size() >= numBitsToMove) {

            for (int i = 0; i < numBitsToMove; i++) {

                int myOne = ones.remove(MathUtils.nextInt(ones.size()));
                int myZero = zeros.remove(MathUtils.nextInt(zeros.size()));

                bitsParameter.setParameterValue(myOne, 0.0);
                bitsParameter.setParameterValue(myZero, 1.0);

                if (valuesParameter != null) {
                    double value1 = valuesParameter.getParameterValue(myOne);
                    double value2 = valuesParameter.getParameterValue(myZero);
                    valuesParameter.setParameterValue(myOne, value2);
                    valuesParameter.setParameterValue(myZero, value1);
                }

            }
        } else throw new OperatorFailedException("Not enough bits to move!");

        return 0.0;
    }

    // Interface MCMCOperator
    public final String getOperatorName() {
        StringBuilder builder = new StringBuilder();
        builder.append("bitMove(");
        builder.append(bitsParameter.getParameterName());

        if (valuesParameter != null) {
            builder.append(", ").append(valuesParameter.getParameterName());
        }
        builder.append(", ").append(numBitsToMove).append(")");

        return builder.toString();
    }

    public final String getPerformanceSuggestion() {
        return "no performance suggestion";
    }

    public String toString() {
        return getOperatorName();
    }

    // Private instance variables

    private Parameter bitsParameter = null;
    private Parameter valuesParameter = null;
    private int numBitsToMove = 1;

    public static final XMLObjectParser<BitMoveOperator> PARSER = new AbstractXMLObjectParser<BitMoveOperator>() {

        public static final String BIT_MOVE_OPERATOR = "bitMoveOperator";
        public static final String NUM_BITS_TO_MOVE = "numBitsToMove";

        public String getParserName() {
            return BIT_MOVE_OPERATOR;
        }

        public BitMoveOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);

            int numBitsToMove = xo.getIntegerAttribute(NUM_BITS_TO_MOVE);

            Parameter bitsParameter = (Parameter) xo.getElementFirstChild("bits");
            Parameter valuesParameter = null;


            if (xo.hasChildNamed("values")) {
                valuesParameter = (Parameter) xo.getElementFirstChild("values");
            }


            return new BitMoveOperator(bitsParameter, valuesParameter, numBitsToMove, weight);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element returns a bit-move operator on a given parameter.";
        }

        public Class getReturnType() {
            return BitMoveOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                AttributeRule.newIntegerRule(NUM_BITS_TO_MOVE),
                new ElementRule("bits", Parameter.class),
                new ElementRule("values", Parameter.class, "values parameter", true)
        };

    };
}
