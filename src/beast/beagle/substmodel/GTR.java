/*
 * GTR.java
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
import beast.inference.model.Variable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * General Time Reversible model of nucleotide evolution
 * This is really just a place-holder because all the implementation
 * already exists in NucleotideModel and GeneralModel, its base classes.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: GTR.java,v 1.19 2005/05/24 20:25:58 rambaut Exp $
 */
public class GTR extends BaseSubstitutionModel {

    private Variable<Double> rateACVariable = null;
    private Variable<Double> rateAGVariable = null;
    private Variable<Double> rateATVariable = null;
    private Variable<Double> rateCGVariable = null;
    private Variable<Double> rateCTVariable = null;
    private Variable<Double> rateGTVariable = null;

    /**
     * @param rateACVariable rate of A<->C substitutions
     * @param rateAGVariable rate of A<->G substitutions
     * @param rateATVariable rate of A<->T substitutions
     * @param rateCGVariable rate of C<->G substitutions
     * @param rateCTVariable rate of C<->T substitutions
     * @param rateGTVariable rate of G<->T substitutions
     * @param freqModel       frequencies
     */
    public GTR(
            Variable<Double> rateACVariable,
            Variable<Double> rateAGVariable,
            Variable<Double> rateATVariable,
            Variable<Double> rateCGVariable,
            Variable<Double> rateCTVariable,
            Variable<Double> rateGTVariable,
            FrequencyModel freqModel) {

        super("GTR", Nucleotides.INSTANCE, freqModel);

        if (rateACVariable != null) {
            addVariable(rateACVariable);
            rateACVariable.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
            this.rateACVariable = rateACVariable;
        }

        if (rateAGVariable != null) {
            addVariable(rateAGVariable);
            rateAGVariable.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
            this.rateAGVariable = rateAGVariable;
        }

        if (rateATVariable != null) {
            addVariable(rateATVariable);
            rateATVariable.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
            this.rateATVariable = rateATVariable;
        }

        if (rateCGVariable != null) {
            addVariable(rateCGVariable);
            rateCGVariable.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
            this.rateCGVariable = rateCGVariable;
        }

        if (rateCTVariable != null) {
            addVariable(rateCTVariable);
            rateCTVariable.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
            this.rateCTVariable = rateCTVariable;
        }

        if (rateGTVariable != null) {
            addVariable(rateGTVariable);
            rateGTVariable.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
            this.rateGTVariable = rateGTVariable;
        }

    }

    public void setAbsoluteRates(double[] rates, int relativeTo) {
        for (int i = 0; i < relativeRates.length; i++) {
            relativeRates[i] = rates[i] / rates[relativeTo];
        }
        updateMatrix = true;
        fireModelChanged();
    }

    public void setRelativeRates(double[] rates) {
        System.arraycopy(rates, 0, relativeRates, 0, relativeRates.length);
        updateMatrix = true;
        fireModelChanged();
    }

    protected void frequenciesChanged() {
        // nothing to do...
    }

    protected void ratesChanged() {
        // nothing to do...
    }

    protected void setupRelativeRates(double[] rates) {
        if (rateACVariable != null) {
            rates[0] = rateACVariable.getValue(0);
        }
        if (rateAGVariable != null) {
            rates[1] = rateAGVariable.getValue(0);
        }
        if (rateATVariable != null) {
            rates[2] = rateATVariable.getValue(0);
        }
        if (rateCGVariable != null) {
            rates[3] = rateCGVariable.getValue(0);
        }
        if (rateCTVariable != null) {
            rates[4] = rateCTVariable.getValue(0);
        }
        if (rateGTVariable != null) {
            rates[5] = rateGTVariable.getValue(0);
        }
    }

    // **************************************************************
    // XHTMLable IMPLEMENTATION
    // **************************************************************

    public String toXHTML() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("<em>GTR Model</em> Instantaneous Rate Matrix = <table><tr><td></td><td>A</td><td>C</td><td>G</td><td>T</td></tr>");
        buffer.append("<tr><td>A</td><td></td><td>");
        buffer.append(relativeRates[0]);
        buffer.append("</td><td>");
        buffer.append(relativeRates[1]);
        buffer.append("</td><td>");
        buffer.append(relativeRates[2]);
        buffer.append("</td></tr>");

        buffer.append("<tr><td>C</td><td></td><td></td><td>");
        buffer.append(relativeRates[3]);
        buffer.append("</td><td>");
        buffer.append(relativeRates[4]);
        buffer.append("</td></tr>");

        buffer.append("<tr><td>G</td><td></td><td></td><td></td><td>");
        buffer.append(relativeRates[5]);
        buffer.append("</td></tr>");

        buffer.append("<tr><td>G</td><td></td><td></td><td></td><td></td></tr></table>");

        return buffer.toString();
    }

    public static final XMLObjectParser<GTR> PARSER = new AbstractXMLObjectParser<GTR>() {
        public static final String GTR_MODEL = "gtrModel";

        public static final String A_TO_C = "rateAC";
        public static final String A_TO_G = "rateAG";
        public static final String A_TO_T = "rateAT";
        public static final String C_TO_G = "rateCG";
        public static final String C_TO_T = "rateCT";
        public static final String G_TO_T = "rateGT";

        public static final String FREQUENCIES = "frequencies";

        public String getParserName() {
            return GTR_MODEL;
        }

        public GTR parseXMLObject(XMLObject xo) throws XMLParseException {

            XMLObject cxo = xo.getChild(FREQUENCIES);
            FrequencyModel freqModel = (FrequencyModel) cxo.getChild(FrequencyModel.class);

            Variable<Double> rateACVariable = null;
            if (xo.hasChildNamed(A_TO_C)) {
                rateACVariable = (Variable<Double>) xo.getElementFirstChild(A_TO_C);
            }
            Variable<Double> rateAGVariable = null;
            if (xo.hasChildNamed(A_TO_G)) {
                rateAGVariable = (Variable<Double>) xo.getElementFirstChild(A_TO_G);
            }
            Variable<Double> rateATVariable = null;
            if (xo.hasChildNamed(A_TO_T)) {
                rateATVariable = (Variable<Double>) xo.getElementFirstChild(A_TO_T);
            }
            Variable<Double> rateCGVariable = null;
            if (xo.hasChildNamed(C_TO_G)) {
                rateCGVariable = (Variable<Double>) xo.getElementFirstChild(C_TO_G);
            }
            Variable<Double> rateCTVariable = null;
            if (xo.hasChildNamed(C_TO_T)) {
                rateCTVariable = (Variable<Double>) xo.getElementFirstChild(C_TO_T);
            }
            Variable<Double> rateGTVariable = null;
            if (xo.hasChildNamed(G_TO_T)) {
                rateGTVariable = (Variable<Double>) xo.getElementFirstChild(G_TO_T);
            }
            int countNull = 0;
            if (rateACVariable == null) countNull++;
            if (rateAGVariable == null) countNull++;
            if (rateATVariable == null) countNull++;
            if (rateCGVariable == null) countNull++;
            if (rateCTVariable == null) countNull++;
            if (rateGTVariable == null) countNull++;

            if (countNull != 1)
                throw new XMLParseException("Only five parameters may be specified in GTR, leave exactly one out, the others will be specifed relative to the one left out.");
            return new GTR(rateACVariable, rateAGVariable, rateATVariable, rateCGVariable, rateCTVariable, rateGTVariable, freqModel);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A general reversible model of nucleotide sequence substitution.";
        }

        public String getExample() {

            return
                    "<!-- A general time reversible model for DNA.                                          -->\n" +
                            "<!-- This element must have parameters for exactly five of the six rates               -->\n" +
                            "<!-- The sixth rate has an implied value of 1.0 and all other rates are relative to it -->\n" +
                            "<!-- This example parameterizes the rate matrix relative to the A<->G transition       -->\n" +
                            "<" + getParserName() + " id=\"gtr1\">\n" +
                            "	<" + FREQUENCIES + "> <frequencyModel idref=\"freqs\"/> </" + FREQUENCIES + ">\n" +
                            "	<" + A_TO_C + "> <parameter id=\"rateAC\" value=\"1.0\"/> </" + A_TO_C + ">\n" +
                            "	<" + A_TO_T + "> <parameter id=\"rateAT\" value=\"1.0\"/> </" + A_TO_T + ">\n" +
                            "	<" + C_TO_G + "> <parameter id=\"rateCG\" value=\"1.0\"/> </" + C_TO_G + ">\n" +
                            "	<" + C_TO_T + "> <parameter id=\"rateCT\" value=\"1.0\"/> </" + C_TO_T + ">\n" +
                            "	<" + G_TO_T + "> <parameter id=\"rateGT\" value=\"1.0\"/> </" + G_TO_T + ">\n" +
                            "</" + getParserName() + ">\n";
        }

        public Class getReturnType() {
            return GTR.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(FREQUENCIES,
                        new XMLSyntaxRule[]{new ElementRule(FrequencyModel.class)}),
                new ElementRule(A_TO_C,
                        new XMLSyntaxRule[]{new ElementRule(Variable.class)}, true),
                new ElementRule(A_TO_G,
                        new XMLSyntaxRule[]{new ElementRule(Variable.class)}, true),
                new ElementRule(A_TO_T,
                        new XMLSyntaxRule[]{new ElementRule(Variable.class)}, true),
                new ElementRule(C_TO_G,
                        new XMLSyntaxRule[]{new ElementRule(Variable.class)}, true),
                new ElementRule(C_TO_T,
                        new XMLSyntaxRule[]{new ElementRule(Variable.class)}, true),
                new ElementRule(G_TO_T,
                        new XMLSyntaxRule[]{new ElementRule(Variable.class)}, true)
        };
    };
}