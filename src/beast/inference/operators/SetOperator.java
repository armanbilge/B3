/*
 * SetOperator.java
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A generic operator for selecting uniformly from a discrete set of values.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: SetOperator.java,v 1.12 2005/05/24 20:26:00 rambaut Exp $
 */
public class SetOperator extends SimpleMCMCOperator {

    public SetOperator(Parameter parameter, double[] values) {
        this.parameter = parameter;
        this.values = values;
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
    public final double doOperation() throws OperatorFailedException {

        int index = MathUtils.nextInt(values.length);
        double newValue = values[index];

        if (newValue < parameter.getBounds().getLowerLimit(index) || newValue > parameter.getBounds().getUpperLimit(index)) {
            throw new OperatorFailedException("proposed value outside boundaries");
        }

        parameter.setParameterValue(index, newValue);

        return 0.0;
    }

    public Element createOperatorElement(Document document) {
        throw new RuntimeException("Not implememented!");
    }

    public String getOperatorName() {
        return "setOperator(" + parameter.getParameterName() + ")";
    }

    public String getPerformanceSuggestion() {
        return "No suggestions";
    }

    //PRIVATE STUFF

    private Parameter parameter = null;
    private double[] values;

    public static final XMLObjectParser<SetOperator> PARSER = new AbstractXMLObjectParser<SetOperator>() {

        public static final String SET_OPERATOR = "setOperator";
        public static final String SET = "set";

        public String getParserName() {
            return SET_OPERATOR;
        }

        public SetOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            double[] values = xo.getDoubleArrayAttribute(SET);
            double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);

            Parameter parameter = (Parameter) xo.getChild(Parameter.class);

            System.out.println("Creating set operator for parameter " + parameter.getParameterName());
            System.out.print("  set = {" + values[0]);
            for (int i = 1; i < values.length; i++) {
                System.out.print(", " + values[i]);
            }
            System.out.println("}");

            SetOperator operator = new SetOperator(parameter, values);
            operator.setWeight(weight);

            return operator;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents an operator on a set.";
        }

        public Class getReturnType() {
            return SetOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newDoubleArrayRule(SET),
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                new ElementRule(Parameter.class)
        };
    };

}
