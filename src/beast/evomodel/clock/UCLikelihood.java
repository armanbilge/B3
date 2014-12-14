/*
 * UCLikelihood.java
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

package beast.evomodel.clock;

import beast.evomodel.tree.TreeModel;
import beast.inference.model.Parameter;
import beast.math.MathUtils;
import beast.math.distributions.NormalDistribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * Calculates the likelihood of a set of rate changes in a tree, assuming that rates are lognormally distributed
 * cf Yang and Rannala 2006
 *
 * @author Michael Defoin Platel
 */
public class UCLikelihood extends RateEvolutionLikelihood {

    public UCLikelihood(TreeModel tree, Parameter ratesParameter, Parameter variance, Parameter rootRate, boolean isLogSpace) {

        super((isLogSpace) ? "LogNormally Distributed" : "Normally Distributed", tree, ratesParameter, rootRate, false);

        this.isLogSpace = isLogSpace;
        this.variance = variance;

        addVariable(variance);
    }

    /**
     * @return the log likelihood of the rate.
     */
    double branchRateChangeLogLikelihood(double foo1, double rate, double foo2) {
        double var = variance.getParameterValue(0);
        double meanRate = rootRateParameter.getParameterValue(0);


        if (isLogSpace) {
            final double logmeanRate = Math.log(meanRate);
            final double logRate = Math.log(rate);

            return NormalDistribution.logPdf(logRate, logmeanRate - (var / 2.), Math.sqrt(var)) - logRate;

        } else {
            return NormalDistribution.logPdf(rate, meanRate, Math.sqrt(var));
        }
    }

    double branchRateSample(double foo1, double foo2) {
        double meanRate = rootRateParameter.getParameterValue(0);

        double var = variance.getParameterValue(0);

        if (isLogSpace) {
            final double logMeanRate = Math.log(meanRate);

            return Math.exp(MathUtils.nextGaussian() * Math.sqrt(var) + logMeanRate - (var / 2.));
        } else {
            return MathUtils.nextGaussian() * Math.sqrt(var) + meanRate;
        }
    }

    private final Parameter variance;

    boolean isLogSpace = false;

    public static final XMLObjectParser<RateEvolutionLikelihood> PARSER = new AbstractXMLObjectParser<RateEvolutionLikelihood>() {

        public static final String UC_LIKELIHOOD = "UCLikelihood";

        public static final String VARIANCE = "variance";

        public String getParserName() {
            return UC_LIKELIHOOD;
        }

        public RateEvolutionLikelihood parseXMLObject(XMLObject xo) throws XMLParseException {

            TreeModel tree = (TreeModel) xo.getChild(TreeModel.class);

            Parameter ratesParameter = (Parameter) xo.getElementFirstChild(RateEvolutionLikelihood.RATES);

            Parameter rootRate = (Parameter) xo.getElementFirstChild(RateEvolutionLikelihood.ROOTRATE);

            Parameter variance = (Parameter) xo.getElementFirstChild(VARIANCE);


            boolean isLogSpace = xo.getAttribute(RateEvolutionLikelihood.LOGSPACE, false);

            return new UCLikelihood(tree, ratesParameter, variance, rootRate, isLogSpace);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return
                    "This element returns an object that can calculate the likelihood " +
                            "of rates in a tree under the assumption of " +
                            "(log)normally distributed rates. ";
        }

        public Class getReturnType() {
            return UCLikelihood.class;
        }


        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(TreeModel.class),
                new ElementRule(RateEvolutionLikelihood.RATES, Parameter.class, "The branch rates parameter", false),
                AttributeRule.newBooleanRule(RateEvolutionLikelihood.LOGSPACE, true, "true if model considers the log of the rates."),
                new ElementRule(RateEvolutionLikelihood.ROOTRATE, Parameter.class, "The root rate parameter"),
                new ElementRule(VARIANCE, Parameter.class, "The standard deviation of the (log)normal distribution"),
        };

    };

}
