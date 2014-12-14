/*
 * AminoAcidModelType.java
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

import beast.util.Citation;

/**
 * @author Alexei Drummond
 */
public enum AminoAcidModelType {

    BLOSUM_62("Blosum62", "blosum62", Blosum62.INSTANCE),
    DAYHOFF("Dayhoff", "dayhoff", Dayhoff.INSTANCE),
    JTT("JTT", beast.evomodel.substmodel.JTT.INSTANCE),
    MT_REV_24("mtREV", MTREV.INSTANCE),
    CP_REV_45("cpREV", CPREV.INSTANCE),
    WAG("WAG", beast.evomodel.substmodel.WAG.INSTANCE),
    LG("LG", beast.evomodel.substmodel.LG.INSTANCE),
    FLU("FLU", beast.evomodel.substmodel.FLU.INSTANCE);

    AminoAcidModelType(String displayName, EmpiricalRateMatrix matrix) {
        this(displayName, displayName, matrix);
    }

    AminoAcidModelType(String displayName, String xmlName, EmpiricalRateMatrix matrix) {
        this.displayName = displayName;
        this.xmlName = xmlName;
        this.matrix = matrix;
    }

    public String toString() {
        return displayName;
    }


    public String getXMLName() {
        return xmlName;
    }

    public EmpiricalRateMatrix getRateMatrixInstance() {
        return matrix;
    }

    public Citation getCitation() {
        return matrix.getCitations().get(0);
    }

    public static String[] xmlNames() {

        AminoAcidModelType[] values = values();

        String[] xmlNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            xmlNames[i] = values[i].getXMLName();
        }
        return xmlNames;
    }

    private final String displayName, xmlName;
    private final EmpiricalRateMatrix matrix;
}
