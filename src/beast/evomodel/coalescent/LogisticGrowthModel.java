/*
 * LogisticGrowthModel.java
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

package beast.evomodel.coalescent;

import beast.evolution.coalescent.DemographicFunction;
import beast.evolution.coalescent.LogisticGrowth;
import beast.evolution.util.Units;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

/**
 * Logistic growth.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: LogisticGrowthModel.java,v 1.21 2005/05/24 20:25:57 rambaut Exp $
 */
public class LogisticGrowthModel extends DemographicModel {

    //
    // Public stuff
    //

    public static final String LOGISTIC_GROWTH_MODEL = "logisticGrowth";

    /**
     * Construct demographic model with default settings
     */
    public LogisticGrowthModel(Parameter N0Parameter, Parameter growthRateParameter,
                               Parameter shapeParameter, double alpha, Type units,
                               boolean usingGrowthRate) {

        this(LOGISTIC_GROWTH_MODEL, N0Parameter, growthRateParameter, shapeParameter, alpha, units, usingGrowthRate);
    }

    /**
     * Construct demographic model with default settings
     */
    public LogisticGrowthModel(String name, Parameter N0Parameter, Parameter growthRateParameter, Parameter shapeParameter, double alpha, Type units, boolean usingGrowthRate) {

        super(name);

        logisticGrowth = new LogisticGrowth(units);

        this.N0Parameter = N0Parameter;
        addVariable(N0Parameter);
        N0Parameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.growthRateParameter = growthRateParameter;
        addVariable(growthRateParameter);
        growthRateParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.shapeParameter = shapeParameter;
        addVariable(shapeParameter);
        shapeParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.alpha = alpha;
        this.usingGrowthRate = usingGrowthRate;

        setUnits(units);
    }


    // general functions

    public DemographicFunction getDemographicFunction() {

        logisticGrowth.setN0(N0Parameter.getParameterValue(0));

        if (usingGrowthRate) {
            double r = growthRateParameter.getParameterValue(0);
            logisticGrowth.setGrowthRate(r);
        } else {
            double doublingTime = growthRateParameter.getParameterValue(0);
            logisticGrowth.setDoublingTime(doublingTime);
        }

        logisticGrowth.setTime50(shapeParameter.getParameterValue(0));

        return logisticGrowth;
    }

    public DemographicFunction getDifferentiatedDemographicFunction(Variable<Double> var, int index) {

        logisticGrowth.setN0(N0Parameter.getParameterValue(0));

        if (usingGrowthRate) {
            double r = growthRateParameter.getParameterValue(0);
            logisticGrowth.setGrowthRate(r);
        } else {
            double doublingTime = growthRateParameter.getParameterValue(0);
            logisticGrowth.setDoublingTime(doublingTime);
        }

        logisticGrowth.setTime50(shapeParameter.getParameterValue(0));

        logisticGrowth.setRespectingN0(var == N0Parameter);
        logisticGrowth.setRespectingGrowthRate(var == growthRateParameter);
        logisticGrowth.setRespectingDoublingTime(var == growthRateParameter && !usingGrowthRate);

        return logisticGrowth;
    }

    //
    // protected stuff
    //

    Parameter N0Parameter = null;
    Parameter growthRateParameter = null;
    Parameter shapeParameter = null;
    double alpha = 0.5;
    LogisticGrowth logisticGrowth = null;
    boolean usingGrowthRate = true;

    public static final XMLObjectParser<LogisticGrowthModel> PARSER = new AbstractXMLObjectParser<LogisticGrowthModel>() {

        public static final String POPULATION_SIZE = "populationSize";

        public static final String GROWTH_RATE = "growthRate";
        public static final String DOUBLING_TIME = "doublingTime";
        public static final String TIME_50 = "t50";

        public String getParserName() {
            return LOGISTIC_GROWTH_MODEL;
        }

        public LogisticGrowthModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Units.Type units = Units.parseUnitsAttribute(xo);

            XMLObject cxo = xo.getChild(POPULATION_SIZE);
            Parameter N0Param = (Parameter) cxo.getChild(Parameter.class);

            boolean usingGrowthRate = true;
            Parameter rParam;
            if (xo.getChild(GROWTH_RATE) != null) {
                cxo = xo.getChild(GROWTH_RATE);
                rParam = (Parameter) cxo.getChild(Parameter.class);
            } else {
                cxo = xo.getChild(DOUBLING_TIME);
                rParam = (Parameter) cxo.getChild(Parameter.class);
                usingGrowthRate = false;
            }

            cxo = xo.getChild(TIME_50);
            Parameter cParam = (Parameter) cxo.getChild(Parameter.class);

            return new LogisticGrowthModel(N0Param, rParam, cParam, 0.5, units, usingGrowthRate);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "Logistic growth demographic model.";
        }

        public Class getReturnType() {
            return LogisticGrowthModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                Units.UNITS_RULE,
                new ElementRule(POPULATION_SIZE,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)},
                        "This parameter represents the population size at time 0 (the time of the last tip of the tree)"),
                new XORRule(

                        new ElementRule(GROWTH_RATE,
                                new XMLSyntaxRule[]{new ElementRule(Parameter.class)},
                                "This parameter determines the rate of growth during the exponential phase. See " +
                                        "exponentialGrowth for details."),
                        new ElementRule(DOUBLING_TIME,
                                new XMLSyntaxRule[]{new ElementRule(Parameter.class)},
                                "This parameter determines the doubling time at peak growth rate.")
                ),
                new ElementRule(TIME_50,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)},
                        "This parameter represents the time in the past when the population was half of that which it is" +
                                "at time zero (not half it's carrying capacity). It is therefore a positive number with " +
                                "the same units as divergence times. A scale operator is recommended with a starting value " +
                                "near zero. A lower bound of zero should be employed and an upper bound is recommended.")
        };
    };
}
