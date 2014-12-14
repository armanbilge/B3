/*
 * CenteredScaleOperator.java
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

/**
 * A generic operator for use with a sum-constrained vector parameter.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: CenteredScaleOperator.java,v 1.20 2005/06/14 10:40:34 rambaut Exp $
 */
public class CenteredScaleOperator extends AbstractCoercableOperator {

    public CenteredScaleOperator(Parameter parameter) {
        super(CoercionMode.DEFAULT);
        this.parameter = parameter;
    }

    public CenteredScaleOperator(Parameter parameter, double scale, int weight, CoercionMode mode) {
        super(mode);
        this.parameter = parameter;
        this.scaleFactor = scale;
        setWeight(weight);
    }

    /**
     * @return the parameter this operator acts on.
     */
    public Parameter getParameter() {
        return parameter;
    }

    /**
     * Change the parameter and return the hastings ratio.
     * Performs a centered scale operation on the vector
     * and returns the hastings ratio.
     * This operator changes the variance but maintains the order
     * of the scalars.
     */
    public final double doOperation() throws OperatorFailedException {

        double total = 0.0;

        for (int i = 0; i < parameter.getDimension(); i++) {
            total += parameter.getParameterValue(i);
        }
        double mean = total / parameter.getDimension();
        double scaleFactor = getRandomScaleFactor();
        double logq = parameter.getDimension() * Math.log(1.0 / scaleFactor);

        for (int i = 0; i < parameter.getDimension(); i++) {

            double newScalar = (parameter.getParameterValue(i) - mean) * scaleFactor + mean;
            if (newScalar < parameter.getBounds().getLowerLimit(i) || newScalar > parameter.getBounds().getUpperLimit(i)) {
                throw new OperatorFailedException("Proposed value out of bounds");
            }
            parameter.setParameterValue(i, newScalar);
        }

        // non-symmetrical move
        return logq;
    }

    public final double getRandomScaleFactor() {
        return scaleFactor + (MathUtils.nextDouble() * ((1 / scaleFactor) - scaleFactor));
    }

    // Interface MCMCOperator
    public final String getOperatorName() {
        return parameter.getParameterName();
    }

    public double getCoercableParameter() {
        return Math.log(1.0 / scaleFactor - 1.0);
    }

    public void setCoercableParameter(double value) {
        scaleFactor = 1.0 / (Math.exp(value) + 1.0);
    }

    public double getRawParameter() {
        return scaleFactor;
    }

    public double getTargetAcceptanceProbability() {
        return 0.234;
    }

    public final String getPerformanceSuggestion() {

        double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
        double targetProb = getTargetAcceptanceProbability();
        beast.util.NumberFormatter formatter = new beast.util.NumberFormatter(5);
        double sf = OperatorUtils.optimizeScaleFactor(scaleFactor, prob, targetProb);
        if (prob < getMinimumGoodAcceptanceLevel()) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > getMaximumGoodAcceptanceLevel()) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }

    public String toString() {
        return getOperatorName() + "(scaleFactor=" + scaleFactor + ")";
    }

    // Private instance variables

    private Parameter parameter = null;
    public double scaleFactor = 0.5;

    public static final XMLObjectParser<CenteredScaleOperator> PARSER = new AbstractXMLObjectParser<CenteredScaleOperator>() {
        public static final String CENTERED_SCALE = "centeredScale";
        public static final String SCALE_FACTOR = "scaleFactor";

        public String getParserName() {
            return CENTERED_SCALE;
        }

        public CenteredScaleOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter parameter = (Parameter) xo.getChild(Parameter.class);

            double scale = xo.getDoubleAttribute(SCALE_FACTOR);
            double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);
            CenteredScaleOperator op = new CenteredScaleOperator(parameter);
            op.setWeight(weight);
            op.scaleFactor = scale;
            op.mode = CoercionMode.parseMode(xo);
            return op;
        }

        public String getParserDescription() {
            return "A centered-scale operator. This operator scales the the values of a multi-dimensional parameter so as to perserve the mean. It does this by expanding or conrtacting the parameter values around the mean.";
        }

        public Class getReturnType() {
            return CenteredScaleOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                AttributeRule.newDoubleRule(SCALE_FACTOR),
                AttributeRule.newBooleanRule(CoercableMCMCOperator.AUTO_OPTIMIZE, true),
                new ElementRule(Parameter.class)
        };
    };
}
