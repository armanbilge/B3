/*
 * FrequencyModel.java
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

import beast.evolution.alignment.PatternList;
import beast.evolution.datatype.DataType;
import beast.inference.model.AbstractModel;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.NumberFormat;
import java.util.logging.Logger;

/**
 * A model of equlibrium frequencies
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Marc Suchard
 * @version $Id: FrequencyModel.java,v 1.26 2005/05/24 20:25:58 rambaut Exp $
 */
public class FrequencyModel extends AbstractModel {

    public static final String FREQUENCY_MODEL = "frequencyModel";
    public static final String FREQUENCIES = "frequencies";

    /**
     * A constructor which allows a more programmatic approach with
     * fixed frequencies.
     * @param dataType              DataType
     * @param frequencyParameter    double[]
     */
    public FrequencyModel(DataType dataType, double[] frequencyParameter) {
        this(dataType, new Parameter.Default(frequencyParameter));
    }

    public FrequencyModel(DataType dataType, Parameter frequencyParameter) {

        super(FREQUENCY_MODEL);

        double sum = getSumOfFrequencies(frequencyParameter);

        if (Math.abs(sum - 1.0) > 1e-8) {
            throw new IllegalArgumentException("Frequencies do not sum to 1, they sum to " + sum);
        }

        this.frequencyParameter = frequencyParameter;
        addVariable(frequencyParameter);
        frequencyParameter.addBounds(new Parameter.DefaultBounds(1.0, 0.0, frequencyParameter.getDimension()));
        this.dataType = dataType;
    }

    /**
     * @param frequencies the frequencies
     * @return return the sum of frequencies
     */
    private double getSumOfFrequencies(Parameter frequencies) {
        double total = 0.0;
        for (int i = 0; i < frequencies.getDimension(); i++) {
            total += frequencies.getParameterValue(i);
        }
        return total;
    }

    public void setFrequency(int i, double value) {
        frequencyParameter.setParameterValue(i, value);
    }

    public double getFrequency(int i) {
        return frequencyParameter.getParameterValue(i);
    }

    public int getFrequencyCount() {
        return frequencyParameter.getDimension();
    }

    public Parameter getFrequencyParameter() {
        return frequencyParameter;
    }

    public double[] getFrequencies() {
        double[] frequencies = new double[getFrequencyCount()];
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = getFrequency(i);
        }
        return frequencies;
    }

    public double[] getCumulativeFrequencies() {
        double[] frequencies = getFrequencies();
        for (int i = 1; i < frequencies.length; i++) {
            frequencies[i] += frequencies[i - 1];
        }
        return frequencies;
    }

    public DataType getDataType() {
        return dataType;
    }

    // *****************************************************************
    // Interface Model
    // *****************************************************************

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        // no intermediates need recalculating....
    }

    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // no intermediates need recalculating....
    }

    protected void storeState() {
    } // no state apart from parameters to store

    protected void restoreState() {
    } // no state apart from parameters to restore

    protected void acceptState() {
    } // no state apart from parameters to accept

    public Element createElement(Document doc) {
        throw new RuntimeException("Not implemented!");
    }

    private DataType dataType = null;
    Parameter frequencyParameter = null;

    public static final XMLObjectParser<FrequencyModel> PARSER = new AbstractXMLObjectParser<FrequencyModel>() {
        public static final String NORMALIZE = "normalize";


        public String[] getParserNames() {
            return new String[]{
                    getParserName(), "beast_" + getParserName()
            };
        }

        public String getParserName() {
            return FREQUENCY_MODEL;
        }

        public FrequencyModel parseXMLObject(XMLObject xo) throws XMLParseException {

            DataType dataType = DataType.parseDataType(xo);

            Parameter freqsParam = (Parameter) xo.getElementFirstChild(FREQUENCIES);
            double[] frequencies = null;

            for (int i = 0; i < xo.getChildCount(); i++) {
                Object obj = xo.getChild(i);
                if (obj instanceof PatternList) {
                    frequencies = ((PatternList) obj).getStateFrequencies();
                    break;
                }
            }

            StringBuilder sb = new StringBuilder("Creating state frequencies model '" + freqsParam.getParameterName() + "': ");
            if (frequencies != null) {
                if (freqsParam.getDimension() != frequencies.length) {
                    throw new XMLParseException("dimension of frequency parameter and number of sequence states don't match!");
                }
                for (int j = 0; j < frequencies.length; j++) {
                    freqsParam.setParameterValue(j, frequencies[j]);
                }
                sb.append("Using empirical frequencies from data ");
            } else {
                sb.append("Initial frequencies ");
            }
            sb.append("= {");

            double sum = 0;
            for (int j = 0; j < freqsParam.getDimension(); j++) {
                sum += freqsParam.getParameterValue(j);
            }

            if (xo.getAttribute(NORMALIZE, false)) {
                for (int j = 0; j < freqsParam.getDimension(); j++) {
                    if (sum != 0)
                        freqsParam.setParameterValue(j, freqsParam.getParameterValue(j) / sum);
                    else
                        freqsParam.setParameterValue(j, 1.0 / freqsParam.getDimension());
                }
                sum = 1.0;
            }

            if (Math.abs(sum - 1.0) > 1e-8) {
                throw new XMLParseException("Frequencies do not sum to 1 (they sum to " + sum + ")");
            }


            NumberFormat format = NumberFormat.getNumberInstance();
            format.setMaximumFractionDigits(5);

            sb.append(format.format(freqsParam.getParameterValue(0)));
            for (int j = 1; j < freqsParam.getDimension(); j++) {
                sb.append(", ");
                sb.append(format.format(freqsParam.getParameterValue(j)));
            }
            sb.append("}");
            Logger.getLogger("beast.evomodel").info(sb.toString());

            return new FrequencyModel(dataType, freqsParam);
        }

        public String getParserDescription() {
            return "A model of equilibrium base frequencies.";
        }

        public Class getReturnType() {
            return FrequencyModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newBooleanRule(NORMALIZE, true),

                new ElementRule(PatternList.class, "Initial value", 0, 1),

                new XORRule(
                        new StringAttributeRule(DataType.DATA_TYPE, "The type of sequence data",
                                DataType.getRegisteredDataTypeNames(), false),
                        new ElementRule(DataType.class)
                ),

                new ElementRule(FREQUENCIES,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),

        };

    };

}
