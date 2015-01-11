/*
 * ExponentialGrowthModel.java
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
import beast.evolution.coalescent.ExponentialGrowth;
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
 * This class models an exponentially growing (or shrinking) population
 * (Parameters: N0=present-day population size; r=growth rate).
 * This model is nested with the constant-population size model (r=0).
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: ExponentialGrowthModel.java,v 1.14 2005/05/24 20:25:57 rambaut Exp $
 */
public class ExponentialGrowthModel extends DemographicModel {

    public static final String EXPONENTIAL_GROWTH_MODEL = "exponentialGrowth";

    //
    // Public stuff
    //
    /**
     * Construct demographic model with default settings
     */
    public ExponentialGrowthModel(Parameter N0Parameter, Parameter growthRateParameter,
                                  Type units, boolean usingGrowthRate) {

        this(EXPONENTIAL_GROWTH_MODEL, N0Parameter, growthRateParameter, units, usingGrowthRate);
    }

    /**
     * Construct demographic model with default settings
     */
    public ExponentialGrowthModel(String name, Parameter N0Parameter, Parameter growthRateParameter,
                                  Type units, boolean usingGrowthRate) {

        super(name);

        exponentialGrowth = new ExponentialGrowth(units);

        this.N0Parameter = N0Parameter;
        addVariable(N0Parameter);
        N0Parameter.addBounds(new Parameter.DefaultBounds(Double.MAX_VALUE, 0.0, 1));

        this.growthRateParameter = growthRateParameter;
        addVariable(growthRateParameter);
        growthRateParameter.addBounds(new Parameter.DefaultBounds(Double.MAX_VALUE, -Double.MAX_VALUE, 1));

        this.usingGrowthRate = usingGrowthRate;

        setUnits(units);
    }


    // general functions

    public DemographicFunction getDemographicFunction() {
        exponentialGrowth.setN0(N0Parameter.getParameterValue(0));

        if (usingGrowthRate) {
            double r = growthRateParameter.getParameterValue(0);
            exponentialGrowth.setGrowthRate(r);
        } else {
            double doublingTime = growthRateParameter.getParameterValue(0);
            exponentialGrowth.setDoublingTime(doublingTime);
        }

        return exponentialGrowth;
    }

    public DemographicFunction getDifferentiatedDemographicFunction(Variable<Double> var, int index) {
        exponentialGrowth.setN0(N0Parameter.getParameterValue(0));

        if (usingGrowthRate) {
            double r = growthRateParameter.getParameterValue(0);
            exponentialGrowth.setGrowthRate(r);
        } else {
            double doublingTime = growthRateParameter.getParameterValue(0);
            exponentialGrowth.setDoublingTime(doublingTime);
        }

        exponentialGrowth.setRespectingN0(var == N0Parameter);
        exponentialGrowth.setRespectingGrowthRate(var == growthRateParameter);
        exponentialGrowth.setRespectingDoublingTime(var == growthRateParameter && !usingGrowthRate);

        return exponentialGrowth;
    }

    //
    // protected stuff
    //

    Parameter N0Parameter = null;
    Parameter growthRateParameter = null;
    ExponentialGrowth exponentialGrowth = null;
    boolean usingGrowthRate = true;

    public static final XMLObjectParser<ExponentialGrowthModel> PARSER = new AbstractXMLObjectParser<ExponentialGrowthModel>() {

        public static final String POPULATION_SIZE = "populationSize";

        public static final String GROWTH_RATE = "growthRate";
        public static final String DOUBLING_TIME = "doublingTime";


        public String getParserName() {
            return EXPONENTIAL_GROWTH_MODEL;
        }

        public ExponentialGrowthModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Units.Type units = Units.parseUnitsAttribute(xo);

            XMLObject cxo = xo.getChild(POPULATION_SIZE);
            Parameter N0Param = (Parameter) cxo.getChild(Parameter.class);
            Parameter rParam;
            boolean usingGrowthRate = true;

            if (xo.getChild(GROWTH_RATE) != null) {
                cxo = xo.getChild(GROWTH_RATE);
                rParam = (Parameter) cxo.getChild(Parameter.class);
            } else {
                cxo = xo.getChild(DOUBLING_TIME);
                rParam = (Parameter) cxo.getChild(Parameter.class);
                usingGrowthRate = false;
            }

            return new ExponentialGrowthModel(N0Param, rParam, units, usingGrowthRate);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A demographic model of exponential growth.";
        }

        public Class getReturnType() {
            return ExponentialGrowthModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(POPULATION_SIZE,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new XORRule(

                        new ElementRule(GROWTH_RATE,
                                new XMLSyntaxRule[]{new ElementRule(Parameter.class)},
                                "A value of zero represents a constant population size, negative values represent decline towards the present, " +
                                        "positive numbers represents exponential growth towards the present. " +
                                        "A random walk operator is recommended for this parameter with a starting value of 0.0 and no upper or lower limits."),
                        new ElementRule(DOUBLING_TIME,
                                new XMLSyntaxRule[]{new ElementRule(Parameter.class)},
                                "This parameter determines the doubling time.")
                ),
                Units.UNITS_RULE
        };
    };
}
