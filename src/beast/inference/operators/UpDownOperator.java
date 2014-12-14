/*
 * UpDownOperator.java
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

public class UpDownOperator extends AbstractCoercableOperator {

    private Scalable[] upParameter = null;
    private Scalable[] downParameter = null;
    private double scaleFactor;

    public UpDownOperator(Scalable[] upParameter, Scalable[] downParameter,
                          double scale, double weight, CoercionMode mode) {

        super(mode);
        setWeight(weight);

        this.upParameter = upParameter;
        this.downParameter = downParameter;
        this.scaleFactor = scale;
    }

    public final double getScaleFactor() {
        return scaleFactor;
    }

    public final void setScaleFactor(double sf) {
        if( (sf > 0.0) && (sf < 1.0) ) {
            scaleFactor = sf;
        } else {
            throw new IllegalArgumentException("scale must be between 0 and 1");
        }
    }

    /**
     * change the parameter and return the hastings ratio.
     */
    public final double doOperation() throws OperatorFailedException {

        final double scale = (scaleFactor + (MathUtils.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));
        int goingUp = 0, goingDown = 0;

        if( upParameter != null ) {
            for( Scalable up : upParameter ) {
                goingUp += up.scale(scale, -1);
            }
        }

        if( downParameter != null ) {
            for( Scalable dn : downParameter ) {
                goingDown += dn.scale(1.0 / scale, -1);
            }
        }

        return (goingUp - goingDown - 2) * Math.log(scale);
    }

    public final String getPerformanceSuggestion() {

        double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
        double targetProb = getTargetAcceptanceProbability();
        double sf = OperatorUtils.optimizeScaleFactor(scaleFactor, prob, targetProb);
        beast.util.NumberFormatter formatter = new beast.util.NumberFormatter(5);
        if (prob < getMinimumGoodAcceptanceLevel()) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > getMaximumGoodAcceptanceLevel()) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }

    public final String getOperatorName() {
        String name = "";
        if( upParameter != null ) {
            name = "up:";
            for( Scalable up : upParameter ) {
                name = name + up.getName() + " ";
            }
        }

        if( downParameter != null ) {
            name += "down:";
            for( Scalable dn : downParameter ) {
                name = name + dn.getName() + " ";
            }
        }
        return name;
    }

    public double getCoercableParameter() {
        return Math.log(1.0 / scaleFactor - 1.0) / Math.log(10);
    }

    public void setCoercableParameter(double value) {
        scaleFactor = 1.0 / (Math.pow(10.0, value) + 1.0);
    }

    public double getRawParameter() {
        return scaleFactor;
    }

    public double getTargetAcceptanceProbability() {
        return 0.234;
    }

    // Since this operator invariably modifies at least 2 parameters it
    // should allow lower acceptance probabilities
    // as it is known that optimal acceptance levels are inversely
    // proportional to the number of dimensions operated on
    // AD 16/3/2004
    public double getMinimumAcceptanceLevel() {
        return 0.05;
    }

    public double getMaximumAcceptanceLevel() {
        return 0.3;
    }

    public double getMinimumGoodAcceptanceLevel() {
        return 0.10;
    }

    public double getMaximumGoodAcceptanceLevel() {
        return 0.20;
    }

    public static final XMLObjectParser<UpDownOperator> PARSER = new AbstractXMLObjectParser<UpDownOperator>() {

        public static final String UP_DOWN_OPERATOR = "upDownOperator";
        public static final String UP = "up";
        public static final String DOWN = "down";

        public static final String SCALE_FACTOR = "scaleFactor";

        public String getParserName() {
            return UP_DOWN_OPERATOR;
        }

        private Scalable[] getArgs(final XMLObject list) throws XMLParseException {
            Scalable[] args = new Scalable[list.getChildCount()];
            for (int k = 0; k < list.getChildCount(); ++k) {
                final Object child = list.getChild(k);
                if (child instanceof Parameter) {
                    args[k] = new Scalable.Default((Parameter) child);
                } else if (child instanceof Scalable) {
                    args[k] = (Scalable) child;
                } else {
                    XMLObject xo = (XMLObject) child;
                    if (xo.hasAttribute("count")) {
                        final int count = xo.getIntegerAttribute("count");

                        final Scalable s = (Scalable) xo.getChild(Scalable.class);
                        args[k] = new Scalable() {

                            public int scale(double factor, int nDims) throws OperatorFailedException {
                                return s.scale(factor, count);
                            }

                            public String getName() {
                                return s.getName() + "(" + count + ")";
                            }
                        };
                    } else if (xo.hasAttribute("df")) {
                        final int df = xo.getIntegerAttribute("df");

                        final Scalable s = (Scalable) xo.getChild(Scalable.class);
                        args[k] = new Scalable() {

                            public int scale(double factor, int nDims) throws OperatorFailedException {
                                s.scale(factor, -1);
                                return df;
                            }

                            public String getName() {
                                return s.getName() + "[df=" + df + "]";
                            }
                        };
                    }
                }

            }
            return args;
        }

        public UpDownOperator parseXMLObject(XMLObject xo) throws XMLParseException {

            final double scaleFactor = xo.getDoubleAttribute(SCALE_FACTOR);

            final double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);

            final CoercionMode mode = CoercionMode.parseMode(xo);

            final Scalable[] upArgs = getArgs(xo.getChild(UP));
            final Scalable[] dnArgs = getArgs(xo.getChild(DOWN));

            return new UpDownOperator(upArgs, dnArgs, scaleFactor, weight, mode);
        }

        public String getParserDescription() {
            return "This element represents an operator that scales two parameters in different directions. " +
                    "Each operation involves selecting a scale uniformly at random between scaleFactor and 1/scaleFactor. " +
                    "The up parameter is multipled by this scale and the down parameter is divided by this scale.";
        }

        public Class getReturnType() {
            return UpDownOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] ee = {
                new ElementRule(Scalable.class, true),
                new ElementRule(Parameter.class, true),
                new ElementRule("scale", new XMLSyntaxRule[]{
                        AttributeRule.newIntegerRule("count", true),
                        AttributeRule.newIntegerRule("df", true),
                        new ElementRule(Scalable.class),
                }, true),
        };

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(SCALE_FACTOR),
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                AttributeRule.newBooleanRule(CoercableMCMCOperator.AUTO_OPTIMIZE, true),

                // Allow an arbitrary number of Parameters or Scalable in up or down
                new ElementRule(UP, ee, 1, Integer.MAX_VALUE),
                new ElementRule(DOWN, ee, 1, Integer.MAX_VALUE),
        };

    };
}

// The old implementation for reference and until the new one is tested :)

//
//public class UpDownOperator extends AbstractCoercableOperator {
//
//    public static final String UP_DOWN_OPERATOR = "upDownOperator";
//    public static final String UP = "up";
//    public static final String DOWN = "down";
//
//    public static final String SCALE_FACTOR = "scaleFactor";
//
//    public UpDownOperator(Parameter upParameter, Parameter downParameter,
//                          double scale, double weight, CoercionMode mode) {
//
//        super(mode);
//
//        this.upParameter = upParameter;
//        this.downParameter = downParameter;
//        this.scaleFactor = scale;
//        setWeight(weight);
//    }
//
//    public final int getPriorType() {
//        return prior.getPriorType();
//    }
//
//    public final double getScaleFactor() {
//        return scaleFactor;
//    }
//
//    public final void setPriorType(int type) {
//        prior.setPriorType(type);
//    }
//
//    public final void setScaleFactor(double sf) {
//        if ((sf > 0.0) && (sf < 1.0)) scaleFactor = sf;
//        else throw new IllegalArgumentException("minimum scale must be between 0 and 1");
//    }
//
//    /**
//     * change the parameter and return the hastings ratio.
//     */
//    public final double doOperation() throws OperatorFailedException {
//
//        final double scale = (scaleFactor + (MathUtils.nextDouble() * ((1.0 / scaleFactor) - scaleFactor)));
//
//        if( upParameter != null ) {
//            for(int i = 0; i < upParameter.getDimension(); i++) {
//                upParameter.setParameterValue(i, upParameter.getParameterValue(i) * scale);
//            }
//            for(int i = 0; i < upParameter.getDimension(); i++) {
//                if( upParameter.getParameterValue(i) < upParameter.getBounds().getLowerLimit(i) ||
//                        upParameter.getParameterValue(i) > upParameter.getBounds().getUpperLimit(i) ) {
//                    throw new OperatorFailedException("proposed value outside boundaries");
//                }
//            }
//        }
//
//        if( downParameter != null ) {
//            for(int i = 0; i < downParameter.getDimension(); i++) {
//                downParameter.setParameterValue(i, downParameter.getParameterValue(i) / scale);
//            }
//            for(int i = 0; i < downParameter.getDimension(); i++) {
//                if( downParameter.getParameterValue(i) < downParameter.getBounds().getLowerLimit(i) ||
//                        downParameter.getParameterValue(i) > downParameter.getBounds().getUpperLimit(i) ) {
//                    throw new OperatorFailedException("proposed value outside boundaries");
//                }
//            }
//        }
//
//        final int goingUp = (upParameter == null) ? 0 : upParameter.getDimension();
//        final int goingDown = (downParameter == null) ? 0 : downParameter.getDimension();
//
//        final double logq = (goingUp - goingDown - 2) * Math.log(scale);
//
//        return logq;
//    }
//
//    public final String getPerformanceSuggestion() {
//
//        double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
//        double targetProb = getTargetAcceptanceProbability();
//        double sf = OperatorUtils.optimizeScaleFactor(scaleFactor, prob, targetProb);
//        beast.util.NumberFormatter formatter = new beast.util.NumberFormatter(5);
//        if (prob < getMinimumGoodAcceptanceLevel()) {
//            return "Try setting scaleFactor to about " + formatter.format(sf);
//        } else if (prob > getMaximumGoodAcceptanceLevel()) {
//            return "Try setting scaleFactor to about " + formatter.format(sf);
//        } else return "";
//    }
//
//    //MCMCOperator INTERFACE
//    public final String getOperatorName() {
//        return (upParameter != null ? "up:" + upParameter.getParameterName() : "") +
//                (downParameter != null ? " down:" + downParameter.getParameterName() : "");
//    }
//
//    public double getCoercableParameter() {
//        return Math.log(1.0 / scaleFactor - 1.0) / Math.log(10);
//    }
//
//    public void setCoercableParameter(double value) {
//        scaleFactor = 1.0 / (Math.pow(10.0, value) + 1.0);
//    }
//
//    public double getRawParameter() {
//        return scaleFactor;
//    }
//
//    public double getTargetAcceptanceProbability() {
//        return 0.234;
//    }
//
//    // Since this operator invariably modifies at least 2 parameters it
//    // should allow lower acceptance probabilities
//    // as it is known that optimal acceptance levels are inversely
//    // proportional to the number of dimensions operated on
//    // AD 16/3/2004
//    public double getMinimumAcceptanceLevel() {
//        return 0.05;
//    }
//
//    public double getMaximumAcceptanceLevel() {
//        return 0.3;
//    }
//
//    public double getMinimumGoodAcceptanceLevel() {
//        return 0.10;
//    }
//
//    public double getMaximumGoodAcceptanceLevel() {
//        return 0.20;
//    }
//
//    public static beast.xml.XMLObjectParser PARSER = new beast.xml.AbstractXMLObjectParser() {
//
//        public String getParserName() {
//            return UP_DOWN_OPERATOR;
//        }
//
//        public Object parseXMLObject(XMLObject xo) throws XMLParseException {
//
//            final double scaleFactor = xo.getDoubleAttribute(SCALE_FACTOR);
//
//            final double weight = xo.getDoubleAttribute(WEIGHT);
//            CoercionMode mode = CoercionMode.parseMode(xo);
//
//            Parameter param1 = (Parameter) xo.getElementFirstChild(UP);
//            Parameter param2 = (Parameter) xo.getElementFirstChild(DOWN);
//
//            return new UpDownOperator(param1, param2, scaleFactor, weight, mode);
//        }
//
//        public String getParserDescription() {
//            return "This element represents an operator that scales two parameters in different directions. " +
//                    "Each operation involves selecting a scale uniformly at random between scaleFactor and 1/scaleFactor. " +
//                    "The up parameter is multipled by this scale and the down parameter is divided by this scale.";
//        }
//
//        public Class getReturnType() {
//            return UpDownOperator.class;
//        }
//
//        public XMLSyntaxRule[] getSyntaxRules() {
//            return rules;
//        }
//
//        private final XMLSyntaxRule[] rules = {
//                AttributeRule.newDoubleRule(SCALE_FACTOR),
//                AttributeRule.newDoubleRule(WEIGHT),
//                AttributeRule.newBooleanRule(AUTO_OPTIMIZE, true),
//                new ElementRule(UP,
//                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
//                new ElementRule(DOWN,
//                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)})
//        };
//    };
//
//    //PRIVATE STUFF
//
//    private Parameter upParameter = null;
//    private Parameter downParameter = null;
//    private final ContinuousVariablePrior prior = new ContinuousVariablePrior();
//    private double scaleFactor = 0.5;
//}
