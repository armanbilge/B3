/*
 * ConstantPopulationModel.java
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

import beast.evolution.coalescent.ConstantPopulation;
import beast.evolution.coalescent.DemographicFunction;
import beast.evolution.util.Units;
import beast.inference.model.Parameter;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * A wrapper for ConstantPopulation.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: ConstantPopulationModel.java,v 1.10 2005/05/24 20:25:57 rambaut Exp $
 */
public class ConstantPopulationModel extends DemographicModel {

    public static final String CONSTANT_POPULATION_MODEL = "constantSize";

    //
    // Public stuff
    //
    /**
     * Construct demographic model with default settings
     */
    public ConstantPopulationModel(Parameter N0Parameter, Type units) {

        this(CONSTANT_POPULATION_MODEL, N0Parameter, units);
    }

    /**
     * Construct demographic model with default settings
     */
    public ConstantPopulationModel(String name, Parameter N0Parameter, Type units) {

        super(name);

        constantPopulation = new ConstantPopulation(units);

        this.N0Parameter = N0Parameter;
        addVariable(N0Parameter);
        N0Parameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        setUnits(units);
    }

    // general functions

    public DemographicFunction getDemographicFunction() {
        constantPopulation.setN0(N0Parameter.getParameterValue(0));
        return constantPopulation;
    }

    //
    // protected stuff
    //

    private Parameter N0Parameter;
    private ConstantPopulation constantPopulation = null;

    public static final XMLObjectParser<ConstantPopulationModel> PARSER = new AbstractXMLObjectParser<ConstantPopulationModel>() {

        public static final String POPULATION_SIZE = "populationSize";

        public String getParserName() {
            return CONSTANT_POPULATION_MODEL;
        }

        public ConstantPopulationModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Units.Type units = Units.parseUnitsAttribute(xo);

            XMLObject cxo = xo.getChild(POPULATION_SIZE);
            Parameter N0Param = (Parameter) cxo.getChild(Parameter.class);

            return new ConstantPopulationModel(N0Param, units);
        }


        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A demographic model representing a constant population size through time.";
        }

        public Class getReturnType() {
            return ConstantPopulationModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                Units.UNITS_RULE,
                new ElementRule(POPULATION_SIZE,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)})
        };
    };
}
