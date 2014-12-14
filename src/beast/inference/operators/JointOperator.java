/*
 * JointOperator.java
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

import java.util.ArrayList;

/**
 * @author Marc A. Suchard
 */
public class JointOperator extends SimpleMCMCOperator implements CoercableMCMCOperator {

    private final ArrayList<SimpleMCMCOperator> operatorList;
    private final ArrayList<Integer> operatorToOptimizeList;

    private int currentOptimizedOperator;
    private final double targetProbability;

    public JointOperator(double weight, double targetProb) {

        operatorList = new ArrayList<SimpleMCMCOperator>();
        operatorToOptimizeList = new ArrayList<Integer>();
        targetProbability = targetProb;

        setWeight(weight);
    }

    public void addOperator(SimpleMCMCOperator operation) {

        operatorList.add(operation);
        if (operation instanceof CoercableMCMCOperator) {

            if (((CoercableMCMCOperator) operation).getMode() == CoercionMode.COERCION_ON)

                operatorToOptimizeList.add(operatorList.size() - 1);

        }
    }

    public final double doOperation() throws OperatorFailedException {

        double logP = 0;

        boolean failed = false;
        OperatorFailedException failure = null;

        for (SimpleMCMCOperator operation : operatorList) {

            try {
                logP += operation.doOperation();
            } catch (OperatorFailedException ofe) {
                failed = true;
                failure = ofe;
            }
            // todo After a failure, should not have to complete remaining operations, need to fake their operate();
        }
        if (failed)
            throw failure;

        return logP;
    }

//    private double old;

    public double getCoercableParameter() {
        if (operatorToOptimizeList.size() > 0) {
            currentOptimizedOperator = operatorToOptimizeList.get(MathUtils.nextInt(operatorToOptimizeList.size()));
            return ((CoercableMCMCOperator) operatorList.get(currentOptimizedOperator)).getCoercableParameter();
        }
        throw new IllegalArgumentException();
    }

    public void setCoercableParameter(double value) {
        if (operatorToOptimizeList.size() > 0) {
            ((CoercableMCMCOperator) operatorList.get(currentOptimizedOperator)).setCoercableParameter(value);
            return;
        }
        throw new IllegalArgumentException();
    }


    public int getNumberOfSubOperators() {
        return operatorList.size();
    }

    public double getRawParamter(int i) {
        if (i < 0 || i >= operatorList.size())
            throw new IllegalArgumentException();
        return ((CoercableMCMCOperator) operatorList.get(i)).getRawParameter();
    }


    public double getRawParameter() {

        throw new RuntimeException("More than one raw parameter for a joint operator");
    }

    public CoercionMode getMode() {
        if (operatorToOptimizeList.size() > 0)
            return CoercionMode.COERCION_ON;
        return CoercionMode.COERCION_OFF;
    }

    public MCMCOperator getSubOperator(int i) {
        return operatorList.get(i);
    }

    public CoercionMode getSubOperatorMode(int i) {
        if (i < 0 || i >= operatorList.size())
            throw new IllegalArgumentException();
        if (operatorList.get(i) instanceof CoercableMCMCOperator)
            return ((CoercableMCMCOperator) operatorList.get(i)).getMode();
        return CoercionMode.COERCION_OFF;
    }

    public String getSubOperatorName(int i) {
        if (i < 0 || i >= operatorList.size())
            throw new IllegalArgumentException();
        return "Joint." + operatorList.get(i).getOperatorName();
    }

    public String getOperatorName() {
//        StringBuffer sb = new StringBuffer("Joint(\n");
//        for(SimpleMCMCOperator operation : operatorList)
//            sb.append("\t"+operation.getOperatorName()+"\n");
//        sb.append(") opt = "+optimizedOperator.getOperatorName());
//        return sb.toString();
        return "JointOperator";
    }

    public Element createOperatorElement(Document d) {
        throw new RuntimeException("not implemented");
    }

    public double getTargetAcceptanceProbability() {
        return targetProbability;
    }

    public double getMinimumAcceptanceLevel() {
        double min = targetProbability - 0.2;
        if (min < 0)
            min = 0.01;
        return min;
    }

    public double getMaximumAcceptanceLevel() {
        double max = targetProbability + 0.2;
        if (max > 1)
            max = 0.9;
        return max;
    }

    public double getMinimumGoodAcceptanceLevel() {
        double min = targetProbability - 0.1;
        if (min < 0)
            min = 0.01;
        return min;
    }

    public double getMaximumGoodAcceptanceLevel() {
        double max = targetProbability + 0.2;
        if (max > 1)
            max = 0.9;
        return max;
    }

    public final String getPerformanceSuggestion() {

//		double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
//		double targetProb = getTargetAcceptanceProbability();
//		beast.util.NumberFormatter formatter = new beast.util.NumberFormatter(5);
//		double sf = OperatorUtils.optimizeScaleFactor(scaleFactor, prob, targetProb);
//		if (prob < getMinimumGoodAcceptanceLevel()) {
//			return "Try setting scaleFactor to about " + formatter.format(sf);
//		} else if (prob > getMaximumGoodAcceptanceLevel()) {
//			return "Try setting scaleFactor to about " + formatter.format(sf);
//		} else return "";
        return "";
    }

    public static final XMLObjectParser<JointOperator> PARSER = new AbstractXMLObjectParser<JointOperator>() {

        public static final String JOINT_OPERATOR = "jointOperator";
        public static final String WEIGHT = "weight";
        public static final String TARGET_ACCEPTANCE = "targetAcceptance";

        public String getParserName() {
            return JOINT_OPERATOR;
        }

        public JointOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            final double weight = xo.getDoubleAttribute(WEIGHT);

            final double targetProb = xo.getAttribute(TARGET_ACCEPTANCE, 0.2);

            if (targetProb <= 0.0 || targetProb >= 1.0)
                throw new RuntimeException("Target acceptance probability must be between 0.0 and 1.0");

            JointOperator operator = new JointOperator(weight, targetProb);

            for (int i = 0; i < xo.getChildCount(); i++) {
                operator.addOperator((SimpleMCMCOperator) xo.getChild(i));
            }

            return operator;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents an arbitrary list of operators; only the first is optimizable";
        }

        public Class getReturnType() {
            return JointOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(SimpleMCMCOperator.class, 1, Integer.MAX_VALUE),
                AttributeRule.newDoubleRule(WEIGHT),
                AttributeRule.newDoubleRule(TARGET_ACCEPTANCE, true)
        };
    };

}

