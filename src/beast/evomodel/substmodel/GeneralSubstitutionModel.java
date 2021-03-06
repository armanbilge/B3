/*
 * GeneralSubstitutionModel.java
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

package beast.evomodel.substmodel;

import beast.evolution.datatype.DataType;
import beast.evolution.datatype.TwoStates;
import beast.inference.model.Parameter;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.logging.Logger;

/**
 * <b>A general model of sequence substitution</b>. A general reversible class for any
 * data type.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: GeneralSubstitutionModel.java,v 1.37 2006/05/05 03:05:10 alexei Exp $
 */
public class GeneralSubstitutionModel extends AbstractSubstitutionModel implements beast.util.XHTMLable {

    public static final String GENERAL_SUBSTITUTION_MODEL = "generalSubstitutionModel";
    public static final String FREQUENCIES = "frequencies";

    /**
     * the rate which the others are set relative to
     */
    protected int ratesRelativeTo;

    /**
     * constructor
     *
     * @param dataType   the data type
     * @param freqModel  the equilibrium frequency model - this must match the data type
     * @param parameter  the rates parameter, minus the rate that they are specified relative to
     * @param relativeTo the index of the rate that all other are specified relative to
     */
    public GeneralSubstitutionModel(
            DataType dataType,
            FrequencyModel freqModel,
            Parameter parameter,
            int relativeTo) {

        super(GENERAL_SUBSTITUTION_MODEL, dataType, freqModel);

        ratesParameter = parameter;
        if (ratesParameter != null) {
            addVariable(ratesParameter);
            ratesParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, ratesParameter.getDimension()));
        }
        setRatesRelativeTo(relativeTo);
    }

    /**
     * constructor
     *
     * @param name       a name to give the substitution model
     * @param dataType   the data type
     * @param freqModel  the equilibrium frequency model - this must match the data type
     * @param relativeTo the index of the rate that all other are specified relative to
     */
    protected GeneralSubstitutionModel(
            String name,
            DataType dataType,
            FrequencyModel freqModel,
            int relativeTo) {

        super(name, dataType, freqModel);

        setRatesRelativeTo(relativeTo);
    }

    protected void frequenciesChanged() {
        // Nothing to precalculate
    }

    protected void ratesChanged() {
        // Nothing to precalculate
    }

    protected void setupRelativeRates() {

        for (int i = 0; i < relativeRates.length; i++) {
            if (i == ratesRelativeTo) {
                relativeRates[i] = 1.0;
            } else if (i < ratesRelativeTo) {
                relativeRates[i] = ratesParameter.getParameterValue(i);
            } else {
                relativeRates[i] = ratesParameter.getParameterValue(i - 1);
            }
        }
    }

    /**
     * set which rate the others are relative to
     *
     * @param ratesRelativeTo the index of the rate in the matrix that all other
     *                        rates are parameterized relative to.
     */
    public void setRatesRelativeTo(int ratesRelativeTo) {
        this.ratesRelativeTo = ratesRelativeTo;
    }

    // *****************************************************************
    // Interface Model
    // *****************************************************************


    protected void storeState() {
    } // nothing to do

    /**
     * Restore the additional stored state
     */
    protected void restoreState() {
        updateMatrix = true;
    }

    protected void acceptState() {
    } // nothing to do

    // **************************************************************
    // XHTMLable IMPLEMENTATION
    // **************************************************************

    public String toXHTML() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("<em>General Model</em>");

        return buffer.toString();
    }

    protected Parameter ratesParameter = null;

    public static final XMLObjectParser<GeneralSubstitutionModel> PARSER = new AbstractXMLObjectParser<GeneralSubstitutionModel>() {

        public static final String DATA_TYPE = "dataType";
        public static final String RATES = "rates";
        public static final String RELATIVE_TO = "relativeTo";
        public static final String INDICATOR = "rateIndicator";

        public static final String SVS_GENERAL_SUBSTITUTION_MODEL = "svsGeneralSubstitutionModel";
        public static final String SVS_COMPLEX_SUBSTITUTION_MODEL = "svsComplexSubstitutionModel";

        public String getParserName() {
            return GENERAL_SUBSTITUTION_MODEL;
        }

//        public String[] getParserNames() {
//            return new String[]{getParserName(), SVS_GENERAL_SUBSTITUTION_MODEL, SVS_COMPLEX_SUBSTITUTION_MODEL};
//        }

        public GeneralSubstitutionModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter ratesParameter = null;
            FrequencyModel freqModel = null;

            if (xo.hasChildNamed(FREQUENCIES)) {
                XMLObject cxo = xo.getChild(FREQUENCIES);
                freqModel = (FrequencyModel) cxo.getChild(FrequencyModel.class);
            }

            DataType dataType = DataType.parseDataType(xo);

            if (dataType == null) dataType = (DataType) xo.getChild(DataType.class);

//        if (xo.hasAttribute(DataType.DATA_TYPE)) {
//            String dataTypeStr = xo.getStringAttribute(DataType.DATA_TYPE);
//            if (dataTypeStr.equals(Nucleotides.DESCRIPTION)) {
//                dataType = Nucleotides.INSTANCE;
//            } else if (dataTypeStr.equals(AminoAcids.DESCRIPTION)) {
//                dataType = AminoAcids.INSTANCE;
//            } else if (dataTypeStr.equals(Codons.DESCRIPTION)) {
//                dataType = Codons.UNIVERSAL;
//            } else if (dataTypeStr.equals(TwoStates.DESCRIPTION)) {
//                dataType = TwoStates.INSTANCE;
//            }
//        }

            if (dataType == null) dataType = freqModel.getDataType();

            if (dataType != freqModel.getDataType()) {
                throw new XMLParseException("Data type of " + getParserName() + " element does not match that of its frequencyModel.");
            }

            XMLObject cxo = xo.getChild(RATES);
            ratesParameter = (Parameter) cxo.getChild(Parameter.class);

            int states = dataType.getStateCount();
            Logger.getLogger("beast.evomodel").info("  General Substitution Model (stateCount=" + states + ")");

            boolean hasRelativeRates = cxo.hasChildNamed(RELATIVE_TO) || (cxo.hasAttribute(RELATIVE_TO) && cxo.getIntegerAttribute(RELATIVE_TO) > 0);

            int nonReversibleRateCount = ((dataType.getStateCount() - 1) * dataType.getStateCount());
            int reversibleRateCount = (nonReversibleRateCount / 2);

            boolean isNonReversible = ratesParameter.getDimension() == nonReversibleRateCount;
            boolean hasIndicator = xo.hasChildNamed(INDICATOR);

            if (!hasRelativeRates) {
                throw new XMLParseException();
//                Parameter indicatorParameter = null;
//
//                if (ratesParameter.getDimension() != reversibleRateCount && ratesParameter.getDimension() != nonReversibleRateCount) {
//                    throw new XMLParseException("Rates parameter in " + getParserName() + " element should have " + (reversibleRateCount)
//                            + " dimensions for reversible model or " + nonReversibleRateCount + " dimensions for non-reversible. " +
//                            "However parameter dimension is " + ratesParameter.getDimension());
//                }
//
//                if (hasIndicator) { // this is using BSSVS
//                    cxo = xo.getChild(INDICATOR);
//                    indicatorParameter = (Parameter) cxo.getChild(Parameter.class);
//
//                    if (indicatorParameter.getDimension() != ratesParameter.getDimension()) {
//                        throw new XMLParseException("Rates and indicator parameters in " + getParserName() + " element must be the same dimension.");
//                    }
//
//                    boolean randomize = xo.getAttribute(ComplexSubstitutionModelParser.RANDOMIZE, false);
//                    if (randomize) {
//                        BayesianStochasticSearchVariableSelection.Utils.randomize(indicatorParameter,
//                                dataType.getStateCount(), !isNonReversible);
//                    }
//                }
//
//                if (isNonReversible) {
//                    Logger.getLogger("beast.evomodel").info("  Using BSSVS Complex Substitution Model");
//                    return new SVSComplexSubstitutionModel(getParserName(), dataType, freqModel, ratesParameter, indicatorParameter);
//                } else {
//                    Logger.getLogger("beast.evomodel").info("  Using BSSVS General Substitution Model");
//                    return new SVSGeneralSubstitutionModel(dataType, freqModel, ratesParameter, indicatorParameter);
//                }
//

            } else {
                // if we have relativeTo attribute then we use the old GeneralSubstitutionModel

                if (ratesParameter.getDimension() != reversibleRateCount - 1) {
                    throw new XMLParseException("Rates parameter in " + getParserName() + " element should have " + (reversibleRateCount - 1)
                            + " dimensions. However parameter dimension is " + ratesParameter.getDimension());
                }

                int relativeTo = 0;
                if (hasRelativeRates) {
                    relativeTo = cxo.getIntegerAttribute(RELATIVE_TO) - 1;
                }

                if (relativeTo < 0 || relativeTo >= reversibleRateCount) {
                    throw new XMLParseException(RELATIVE_TO + " must be 1 or greater");
                } else {
                    int t = relativeTo;
                    int s = states - 1;
                    int row = 0;
                    while (t >= s) {
                        t -= s;
                        s -= 1;
                        row += 1;
                    }
                    int col = t + row + 1;

                    Logger.getLogger("beast.evomodel").info("  Rates relative to "
                            + dataType.getCode(row) + "<->" + dataType.getCode(col));
                }

                if (ratesParameter == null) {
                    if (reversibleRateCount == 1) {
                        // simplest model for binary traits...
                    } else {
                        throw new XMLParseException("No rates parameter found in " + getParserName());
                    }
                }

                return new GeneralSubstitutionModel(dataType, freqModel, ratesParameter, relativeTo);
            }
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A general reversible model of sequence substitution for any data type.";
        }

        public Class getReturnType() {
            return GeneralSubstitutionModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new XORRule(
                        new StringAttributeRule(DataType.DATA_TYPE, "The type of sequence data",
                                DataType.getRegisteredDataTypeNames(), false),
                        new ElementRule(DataType.class)
                        , true),
                new ElementRule(FREQUENCIES, FrequencyModel.class),
                new ElementRule(RATES,
                        new XMLSyntaxRule[]{
                                new ElementRule(Parameter.class)}
                ),
                new ElementRule(INDICATOR,
                        new XMLSyntaxRule[]{
                                new ElementRule(Parameter.class),
                        }, true),
//                AttributeRule.newBooleanRule(ComplexSubstitutionModelParser.RANDOMIZE, true),
                AttributeRule.newIntegerRule(RELATIVE_TO)
        };
    };

    public static final XMLObjectParser<GeneralSubstitutionModel> BINARY_PARSER = new AbstractXMLObjectParser<GeneralSubstitutionModel>() {

        public static final String BINARY_SUBSTITUTION_MODEL = "binarySubstitutionModel";

        public String getParserName() {
            return BINARY_SUBSTITUTION_MODEL;
        }

        public GeneralSubstitutionModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter ratesParameter;

            XMLObject cxo = xo.getChild(FREQUENCIES);
            FrequencyModel freqModel = (FrequencyModel) cxo.getChild(FrequencyModel.class);

            DataType dataType = freqModel.getDataType();

            if (dataType != TwoStates.INSTANCE)
                throw new XMLParseException("Frequency model must have binary (two state) data type.");

            int relativeTo = 0;

            ratesParameter = new Parameter.Default(0);

            return new GeneralSubstitutionModel(dataType, freqModel, ratesParameter, relativeTo);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A general reversible model of sequence substitution for binary data type.";
        }

        public Class getReturnType() {
            return GeneralSubstitutionModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new ElementRule(FREQUENCIES, FrequencyModel.class),
        };
    };

}
