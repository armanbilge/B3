/*
 * ExpansionModel.java
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

import beast.evolution.util.Units;
import beast.inference.model.Parameter;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

/**
 * Exponential growth from a constant ancestral population size.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: ExpansionModel.java,v 1.5 2005/05/24 20:25:57 rambaut Exp $
 */
public class ExpansionModel extends DemographicModel {

    //
    // Public stuff
    //

    public static final String EXPANSION_MODEL = "expansion";

    /**
     * Construct demographic model with default settings
     */
    public ExpansionModel(Parameter N0Parameter, Parameter N1Parameter,
                          Parameter growthRateParameter, Type units, boolean usingGrowthRate) {

        this(EXPANSION_MODEL, N0Parameter, N1Parameter, growthRateParameter, units, usingGrowthRate);
    }

    /**
     * Construct demographic model with default settings
     */
    public ExpansionModel(String name, Parameter N0Parameter, Parameter N1Parameter,
                          Parameter growthRateParameter, Type units, boolean usingGrowthRate) {

        super(name);

        this.N0Parameter = N0Parameter;
        addVariable(N0Parameter);
        N0Parameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.N1Parameter = N1Parameter;
        addVariable(N1Parameter);
        N1Parameter.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));

        this.growthRateParameter = growthRateParameter;
        addVariable(growthRateParameter);
        growthRateParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.usingGrowthRate = usingGrowthRate;

        setUnits(units);
    }


    // general functions

    public double getN0() {
        return N0Parameter.getParameterValue(0);
    }

    public void setN0(double N0) {
        N0Parameter.setParameterValue(0, N0);
    }

    /**
     * @return growth rate.
     */
    public final double getGrowthRate() { return growthRateParameter.getParameterValue(0); }

    /**
     * sets growth rate to r.
     * @param r
     */
    public void setGrowthRate(double r) { growthRateParameter.setParameterValue(0, r); }

    public double getN1() {
        return N1Parameter.getParameterValue(0);
    }

    public void setN1(double N1) {
        N1Parameter.setParameterValue(0, N1);
    }

    public void setProportion(double p) {
        setN1(getN0() * p);
    }

    // Implementation of abstract methods

    public double getDemographic(double t) {

        double N0 = getN0();
        double N1 = getN1();
        double r = getGrowthRate();

        if (N1 > N0) throw new IllegalArgumentException("N0 must be greater than N1!");

        return N1 + ((N0 - N1) * Math.exp(-r * t));
    }

    /**
     * Returns value of demographic intensity function at time t
     * (= integral 1/N(x) dx from 0 to t).
     */
    public double getIntensity(double t) {
        double N0 = getN0();
        double N1 = getN1();
        double b = (N0 - N1);
        double r = getGrowthRate();

        return Math.log(b + N1 * Math.exp(r * t)) / (r * N1);
    }

    public double getInverseIntensity(double x) {

        /* AER - I think this is right but until someone checks it...
          double nZero = getN0();
          double nOne = getN1();
          double r = getGrowthRate();

          if (r == 0) {
              return nZero*x;
          } else if (alpha == 0) {
              return Math.log(1.0+nZero*x*r)/r;
          } else {
              return Math.log(-(nOne/nZero) + Math.exp(nOne*x*r))/r;
          }
          */
        throw new RuntimeException("Not implemented!");
    }

    public double getIntegral(double start, double finish) {
        double v1 = getIntensity(finish) - getIntensity(start);
        //double v1 =  getNumericalIntegral(start, finish);

        return v1;
    }

    public int getNumArguments() {
        return 3;
    }

    public String getArgumentName(int n) {
        switch (n) {
            case 0:
                return "N0";
            case 1:
                return "r";
            case 2:
                return "N1";
        }
        throw new IllegalArgumentException("Argument " + n + " does not exist");
    }

    public double getArgument(int n) {
        switch (n) {
            case 0:
                return getN0();
            case 1:
                return getGrowthRate();
            case 2:
                return getN1();
        }
        throw new IllegalArgumentException("Argument " + n + " does not exist");
    }

    public void setArgument(int n, double value) {
        switch (n) {
            case 0:
                setN0(value);
                break;
            case 1:
                setGrowthRate(value);
                break;
            case 2:
                setN1(value);
                break;
            default:
                throw new IllegalArgumentException("Argument " + n + " does not exist");

        }
    }

    public double getLowerBound(int n) {
        return 0.0;
    }

    public double getUpperBound(int n) {
        return Double.POSITIVE_INFINITY;
    }

    //
    // protected stuff
    //

    Parameter N0Parameter = null;
    Parameter N1Parameter = null;
    Parameter growthRateParameter = null;
    boolean usingGrowthRate = true;

    public static final XMLObjectParser<ExpansionModel> PARSER = new AbstractXMLObjectParser<ExpansionModel>() {

        public static final String POPULATION_SIZE = "populationSize";
        public static final String ANCESTRAL_POPULATION_PROPORTION = "ancestralPopulationProportion";

        public static final String GROWTH_RATE = "growthRate";
        public static final String DOUBLING_TIME = "doublingTime";

        public String getParserName() {
            return EXPANSION_MODEL;
        }

        public ExpansionModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Units.Type units = Units.parseUnitsAttribute(xo);

            XMLObject cxo = xo.getChild(POPULATION_SIZE);
            Parameter N0Param = (Parameter) cxo.getChild(Parameter.class);

            cxo = xo.getChild(ANCESTRAL_POPULATION_PROPORTION);
            Parameter N1Param = (Parameter) cxo.getChild(Parameter.class);

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

            return new ExpansionModel(N0Param, N1Param, rParam, units, usingGrowthRate);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A demographic model of constant population size followed by exponential growth.";
        }

        public Class getReturnType() {
            return ExpansionModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                Units.UNITS_RULE,
                new ElementRule(POPULATION_SIZE,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                new ElementRule(ANCESTRAL_POPULATION_PROPORTION,
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
                )
        };
    };
}
