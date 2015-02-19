/*
 * HKY.java
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
import beast.inference.model.Parameter;
import beast.inference.model.Statistic;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.logging.Logger;


/**
 * Hasegawa-Kishino-Yano model of nucleotide evolution
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Marc A. Suchard
 */
public class HKY extends BaseSubstitutionModel {

    private Parameter kappaParameter = null;

    /**
     * A constructor which allows a more programmatic approach with
     * fixed kappa.
     * @param kappa
     * @param freqModel
     */
    public HKY(double kappa, FrequencyModel freqModel) {
        this(new Parameter.Default(kappa), freqModel);
    }
    /**
     * Constructor
     * @param kappaParameter
     * @param freqModel
     */
    public HKY(Parameter kappaParameter, FrequencyModel freqModel) {

        super("HKY", Nucleotides.INSTANCE, freqModel);

        this.kappaParameter = kappaParameter;
        addVariable(kappaParameter);
        kappaParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        addStatistic(tsTvStatistic);
    }

    /**
     * set kappa
     * @param kappa
     */
    public void setKappa(double kappa) {
        kappaParameter.setParameterValue(0, kappa);
        updateMatrix = true;
    }

    /**
     * @return kappa
     */
    public final double getKappa() {
        return kappaParameter.getParameterValue(0);
    }

    /**
     * set ts/tv
     * @param tsTv
     */
    public void setTsTv(double tsTv) {
        double freqA = freqModel.getFrequency(0);
        double freqC = freqModel.getFrequency(1);
        double freqG = freqModel.getFrequency(2);
        double freqT = freqModel.getFrequency(3);
        double freqR = freqA + freqG;
        double freqY = freqC + freqT;
        setKappa((tsTv * freqR * freqY) / (freqA * freqG + freqC * freqT));
    }

    /**
     * @return tsTv
     */
    public double getTsTv() {
        double freqA = freqModel.getFrequency(0);
        double freqC = freqModel.getFrequency(1);
        double freqG = freqModel.getFrequency(2);
        double freqT = freqModel.getFrequency(3);
        double freqR = freqA + freqG;
        double freqY = freqC + freqT;
        double tsTv = (getKappa() * (freqA * freqG + freqC * freqT)) / (freqR * freqY);

        return tsTv;
    }

    protected void frequenciesChanged() {
    }

    protected void ratesChanged() {
    }

    protected void setupRelativeRates(double[] rates) {
        double kappa =  kappaParameter.getParameterValue(0);
        rates[0] = 1.0;
        rates[1] = kappa;
        rates[2] = 1.0;
        rates[3] = 1.0;
        rates[4] = kappa;
        rates[5] = 1.0;
    }

    protected void decompose() {

        if (eigenDecomposition == null) {
            double[] evec = new double[stateCount * stateCount];
            double[] ivec = new double[stateCount * stateCount];
            double[] eval = new double[stateCount];
            eigenDecomposition = new EigenDecomposition(evec, ivec, eval);

            ivec[2 * stateCount + 1] =  1; // left eigenvectors 3 = (0,1,0,-1); 4 = (1,0,-1,0)
            ivec[2 * stateCount + 3] = -1;

            ivec[3 * stateCount + 0] =  1;
            ivec[3 * stateCount + 2] = -1;

            evec[0 * stateCount + 0] =  1; // right eigenvector 1 = (1,1,1,1)'
            evec[1 * stateCount + 0] =  1;
            evec[2 * stateCount + 0] =  1;
            evec[3 * stateCount + 0] =  1;

        }

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
        ivec[1 * stateCount + 0] =  pi[0]*piY;
        ivec[1 * stateCount + 1] = -pi[1]*piR;
        ivec[1 * stateCount + 2] =  pi[2]*piY;
        ivec[1 * stateCount + 3] = -pi[3]*piR;

        // right eigenvector #2
        evec[0 * stateCount + 1] =  1.0/piR;
        evec[1 * stateCount + 1] = -1.0/piY;
        evec[2 * stateCount + 1] =  1.0/piR;
        evec[3 * stateCount + 1] = -1.0/piY;

        // right eigenvector #3
        evec[1 * stateCount + 2] =  pi[3]/piY;
        evec[3 * stateCount + 2] = -pi[1]/piY;

        // right eigenvector #4
        evec[0 * stateCount + 3] =  pi[2]/piR;
        evec[2 * stateCount + 3] = -pi[0]/piR;

        // eigenvectors
        double[] eval = eigenDecomposition.getEigenValues();
        final double kappa = getKappa();

        final double beta = -1.0 / (2.0 * (piR * piY + kappa * (pi[0] * pi[2] + pi[1] * pi[3])));
        final double A_R  =  1.0 + piR * (kappa - 1);
        final double A_Y  =  1.0 + piY * (kappa - 1);

        eval[1] = beta;
        eval[2] = beta*A_Y;
        eval[3] = beta*A_R;

        updateMatrix = false;

    }

    //
    // Private stuff
    //

    private Statistic tsTvStatistic = new Statistic.Abstract() {

        public String getStatisticName() {
            return "tsTv";
        }

        public int getDimension() {
            return 1;
        }

        public double getStatisticValue(int dim) {
            return getTsTv();
        }

    };

//    public static void main(String[] args) {
////        double kappa = 2.0;
////        double[] pi = new double[]{0.15,0.30,0.20,0.35};
////        double time = 0.1;
//
//        double kappa = 1.0;
//        double[] pi = new double[]{0.25,0.25,0.25,0.25};
//        double time = 0.1;
//
//        FrequencyModel freqModel= new FrequencyModel(Nucleotides.INSTANCE, pi);
//        HKY hky = new HKY(kappa,freqModel);
//
//        EigenDecomposition decomp = hky.getEigenDecomposition();
////        Matrix evec = new Matrix(decomp.getEigenVectors());
////        Matrix ivec = new Matrix(decomp.getInverseEigenVectors());
////        System.out.println("Evec =\n"+evec);
////        System.out.println("Ivec =\n"+ivec);
//
//        Vector eval = new Vector(decomp.getEigenValues());
//        System.out.println("Eval = "+eval);
//
//        double[] probs = new double[16];
//        hky.getTransitionProbabilities(time,probs);
//        System.out.println("new probs = "+new Vector(probs));
//
//        // check against old implementation
//        beast.evomodel.substmodel.FrequencyModel oldFreq = new beast.evomodel.substmodel.FrequencyModel(Nucleotides.INSTANCE,pi);
//        beast.evomodel.substmodel.HKY oldHKY = new beast.evomodel.substmodel.HKY(kappa,oldFreq);
//        oldHKY.setKappa(kappa);
//
//        oldHKY.getTransitionProbabilities(time,probs);
//        System.out.println("old probs = "+new Vector(probs));
//
//    }

    public static final XMLObjectParser<HKY> PARSER = new AbstractXMLObjectParser<HKY>() {

        public static final String HKY_MODEL = "hkyModel";
        public static final String KAPPA = "kappa";
        public static final String FREQUENCIES = "frequencies";

        public String getParserName() {
            return HKY_MODEL;
        }

        public HKY parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter kappaParam = (Parameter) xo.getElementFirstChild(KAPPA);
            FrequencyModel freqModel = (FrequencyModel) xo.getElementFirstChild(FrequencyModel.FREQUENCIES);

            Logger.getLogger("dr.evomodel").info("Creating HKY substitution model. Initial kappa = " +
                    kappaParam.getParameterValue(0));

            return new HKY(kappaParam, freqModel);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents an instance of the HKY85 " +
                    "(Hasegawa, Kishino & Yano, 1985) model of nucleotide evolution.";
        }

        public Class getReturnType() {
            return HKY.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(FrequencyModel.FREQUENCIES,
                        new XMLSyntaxRule[]{new ElementRule(FrequencyModel.class)}),
                new ElementRule(KAPPA,
                        new XMLSyntaxRule[]{new ElementRule(Parameter.class)})
        };

    };
}