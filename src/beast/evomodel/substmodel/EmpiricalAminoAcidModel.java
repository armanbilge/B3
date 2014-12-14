/*
 * EmpiricalAminoAcidModel.java
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

import beast.evolution.datatype.AminoAcids;
import beast.inference.model.Parameter;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * <b>A general model of sequence substitution</b>. A general reversible class for any
 * data type.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: EmpiricalAminoAcidModel.java,v 1.15 2005/05/24 20:25:58 rambaut Exp $
 */
public class EmpiricalAminoAcidModel extends AbstractAminoAcidModel {
    /**
     * constructor
     *
     * @param rateMatrix      EmpiricalRateMatrix
     * @param freqModel       the frequency model
     */
    public EmpiricalAminoAcidModel(EmpiricalRateMatrix rateMatrix, FrequencyModel freqModel) {

        super(rateMatrix.getName(), freqModel);

        if (freqModel == null) {
            areFrequenciesConstant = true;

            double[] freqs = rateMatrix.getEmpiricalFrequencies();
            this.freqModel = new FrequencyModel(AminoAcids.INSTANCE, new Parameter.Default(freqs));
        }

        this.rateMatrix = rateMatrix;
    }

    protected void frequenciesChanged() {
        // Nothing to precalculate
    }

    protected void ratesChanged() {
        // Nothing to precalculate
    }

    protected void setupRelativeRates() {
        double[] rates = rateMatrix.getEmpiricalRates();
        System.arraycopy(rates, 0, relativeRates, 0, relativeRates.length);
    }

    // *****************************************************************
    // Interface Model
    // *****************************************************************


    protected void storeState() {
    } // nothing to do

    protected void restoreState() {
        updateMatrix = !areFrequenciesConstant;
    }

    protected void acceptState() {
    } // nothing to do

    private EmpiricalRateMatrix rateMatrix;
    private boolean areFrequenciesConstant = false;

    public static final XMLObjectParser<EmpiricalAminoAcidModel> PARSER = new AbstractXMLObjectParser<EmpiricalAminoAcidModel>() {

        public static final String EMPIRICAL_AMINO_ACID_MODEL = "aminoAcidModel";
        public static final String FREQUENCIES = "frequencies";
        public static final String TYPE = "type";


        public String getParserName() {
            return EMPIRICAL_AMINO_ACID_MODEL;
        }

        public EmpiricalAminoAcidModel parseXMLObject(XMLObject xo) throws XMLParseException {

            FrequencyModel freqModel = null;

            if (xo.hasAttribute(FREQUENCIES)) {
                XMLObject cxo = xo.getChild(FREQUENCIES);
                freqModel = (FrequencyModel) cxo.getChild(FrequencyModel.class);
            }

            EmpiricalRateMatrix rateMatrix = null;

            String type = xo.getStringAttribute(TYPE);

            if (type.equals(AminoAcidModelType.BLOSUM_62.getXMLName())) {
                rateMatrix = Blosum62.INSTANCE;
            } else if (type.equals(AminoAcidModelType.DAYHOFF.getXMLName())) {
                rateMatrix = Dayhoff.INSTANCE;
            } else if (type.equals(AminoAcidModelType.JTT.getXMLName())) {
                rateMatrix = JTT.INSTANCE;
            } else if (type.equals(AminoAcidModelType.MT_REV_24.getXMLName())) {
                rateMatrix = MTREV.INSTANCE;
            } else if (type.equals(AminoAcidModelType.CP_REV_45.getXMLName())) {
                rateMatrix = CPREV.INSTANCE;
            } else if (type.equals(AminoAcidModelType.WAG.getXMLName())) {
                rateMatrix = WAG.INSTANCE;
            } else if (type.equals(AminoAcidModelType.FLU.getXMLName())) {
                rateMatrix = FLU.INSTANCE;
            } else if (type.equals(AminoAcidModelType.LG.getXMLName())) {
                rateMatrix = LG.INSTANCE;
            }

            return new EmpiricalAminoAcidModel(rateMatrix, freqModel);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                new StringAttributeRule(TYPE, "The type of empirical amino-acid rate matrix", AminoAcidModelType.xmlNames(), false),
                new ElementRule(FREQUENCIES, FrequencyModel.class, "If the frequencies are omitted than the empirical frequencies associated with the selected model are used.", true)
        };

        public String getParserDescription() {
            return "An empirical amino acid substitution model.";
        }

        public Class getReturnType() {
            return EmpiricalAminoAcidModel.class;
        }
    };

}
