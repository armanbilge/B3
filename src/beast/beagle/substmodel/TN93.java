/*
 * TN93.java
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

package beast.beagle.substmodel;

import beast.evolution.datatype.Nucleotides;
import beast.evomodel.substmodel.NucleotideModelType;
import beast.inference.model.Parameter;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.logging.Logger;

/**
 * Tamura-Nei model of nucleotide evolution
 *
 * @author Marc A. Suchard
 */
public class TN93 extends BaseSubstitutionModel {

    private Parameter kappaParameter1 = null;
    private Parameter kappaParameter2 = null;

    public TN93(Parameter kappaParameter1, Parameter kappaParameter2, FrequencyModel freqModel) {

        super("TN93", Nucleotides.INSTANCE, freqModel);

        this.kappaParameter1 = kappaParameter1;
        addVariable(kappaParameter1);
        kappaParameter1.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.kappaParameter2 = kappaParameter2;
        addVariable(kappaParameter2);
        kappaParameter2.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
    }

    public final double getKappa1() {
        return kappaParameter1.getParameterValue(0);
    }

    public final double getKappa2() {
        return kappaParameter2.getParameterValue(0);
    }

    protected void frequenciesChanged() {
    }

    protected void ratesChanged() {
    }

    protected void setupRelativeRates(double[] rates) {
        double kappa1 = getKappa1();
        double kappa2 = getKappa2();
        rates[0] = 1.0;
        rates[1] = kappa1;
        rates[2] = 1.0;
        rates[3] = 1.0;
        rates[4] = kappa2;
        rates[5] = 1.0;
    }

    public EigenDecomposition getEigenDecomposition() {

        if (eigenDecomposition == null) {
            double[] evec = new double[stateCount * stateCount];
            double[] ivec = new double[stateCount * stateCount];
            double[] eval = new double[stateCount];
            eigenDecomposition = new EigenDecomposition(evec, ivec, eval);

            ivec[2 * stateCount + 1] = 1; // left eigenvectors 3 = (0,1,0,-1); 4 = (1,0,-1,0)
            ivec[2 * stateCount + 3] = -1;

            ivec[3 * stateCount + 0] = 1;
            ivec[3 * stateCount + 2] = -1;

            evec[0 * stateCount + 0] = 1; // right eigenvector 1 = (1,1,1,1)'
            evec[1 * stateCount + 0] = 1;
            evec[2 * stateCount + 0] = 1;
            evec[3 * stateCount + 0] = 1;

        }

        if (updateMatrix) {

            double[] evec = eigenDecomposition.getEigenVectors();
            double[] ivec = eigenDecomposition.getInverseEigenVectors();
            double[] pi = freqModel.getFrequencies();
            double piR = pi[0] + pi[2];
            double piY = pi[1] + pi[3];

            // left eigenvector #1
            ivec[0 * stateCount + 0] = pi[0]; // or, evec[0] = pi;
            ivec[0 * stateCount + 1] = pi[1];
            ivec[0 * stateCount + 2] = pi[2];
            ivec[0 * stateCount + 3] = pi[3];

            // left eigenvector #2
            ivec[1 * stateCount + 0] = pi[0] * piY;
            ivec[1 * stateCount + 1] = -pi[1] * piR;
            ivec[1 * stateCount + 2] = pi[2] * piY;
            ivec[1 * stateCount + 3] = -pi[3] * piR;

            // right eigenvector #2
            evec[0 * stateCount + 1] = 1.0 / piR;
            evec[1 * stateCount + 1] = -1.0 / piY;
            evec[2 * stateCount + 1] = 1.0 / piR;
            evec[3 * stateCount + 1] = -1.0 / piY;

            // right eigenvector #3
            evec[1 * stateCount + 2] = pi[3] / piY;
            evec[3 * stateCount + 2] = -pi[1] / piY;

            // right eigenvector #4
            evec[0 * stateCount + 3] = pi[2] / piR;
            evec[2 * stateCount + 3] = -pi[0] / piR;

            // eigenvectors
            double[] eval = eigenDecomposition.getEigenValues();

            final double kappa1 = getKappa1();
            final double kappa2 = getKappa2();
            final double beta = -1.0 / (2.0 * (piR * piY + kappa1 * pi[0] * pi[2] + kappa2 * pi[1] * pi[3]));
            final double A_R = 1.0 + piR * (kappa1 - 1);
            final double A_Y = 1.0 + piY * (kappa2 - 1);

            eval[1] = beta;
            eval[2] = beta * A_Y;
            eval[3] = beta * A_R;

            updateMatrix = false;
        }

        return eigenDecomposition;
    }

    public static final XMLObjectParser<TN93> PARSER = new AbstractXMLObjectParser<TN93>() {
        public final String TN93_MODEL = NucleotideModelType.TN93.getXMLName();
        public static final String KAPPA1 = "kappa1";
        public static final String KAPPA2 = "kappa2";
        public static final String FREQUENCIES = "frequencies";

        public String getParserName() {
            return TN93_MODEL;
        }

        public TN93 parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter kappa1Param = (Parameter) xo.getElementFirstChild(KAPPA1);
            Parameter kappa2Param = (Parameter) xo.getElementFirstChild(KAPPA2);
            FrequencyModel freqModel = (FrequencyModel) xo.getElementFirstChild(FREQUENCIES);

            Logger.getLogger("dr.evomodel").info("Creating TN93 substitution model. Initial kappa = "
                    + kappa1Param.getValue(0) + "," + kappa2Param.getValue(0));

            return new TN93(kappa1Param, kappa2Param, freqModel);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents an instance of the TN93 (Tamura and Nei 1993) model of nucleotide evolution.";
        }

        public Class getReturnType() {
            return TN93.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules;{
            rules = new XMLSyntaxRule[]{
                    new ElementRule(FREQUENCIES,
                            new XMLSyntaxRule[]{new ElementRule(FrequencyModel.class)}),
                    new ElementRule(KAPPA1,
                            new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
                    new ElementRule(KAPPA2,
                            new XMLSyntaxRule[]{new ElementRule(Parameter.class)})
            };
        }
    };
}