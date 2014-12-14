/*
 * Sequence.java
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

package beast.evolution.sequence;

import beast.evolution.datatype.AminoAcids;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotides;
import beast.evolution.datatype.TwoStates;
import beast.evolution.util.Taxon;
import beast.util.Attributable;
import beast.util.Identifiable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * Class for storing a molecular sequence.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Sequence.java,v 1.35 2005/05/25 09:35:28 rambaut Exp $
 */
public class Sequence implements Identifiable, Attributable {

    /**
     * Empty constructor.
     */
    public Sequence() {
        sequenceString = new StringBuffer();
    }

    /**
     * Constructor with initial sequence string.
     *
     * @param sequence a string representing the sequence
     */
    public Sequence(String sequence) {
        sequenceString = new StringBuffer();
        setSequenceString(sequence);
    }

    /**
     * Clone constructor
     *
     * @param sequence the sequence to clone
     */
    public Sequence(Sequence sequence) {
        // should clone taxon as well!
        this(sequence.getTaxon(), sequence.getSequenceString());
    }

    /**
     * Constructor with taxon and sequence string.
     *
     * @param taxon    the sequence's taxon
     * @param sequence the sequence's symbol string
     */
    public Sequence(Taxon taxon, String sequence) {
        sequenceString = new StringBuffer();
        setTaxon(taxon);
        setSequenceString(sequence);
    }

    /**
     * @return the DataType of the sequences.
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * @return the length of the sequences.
     */
    public int getLength() {
        return sequenceString.length();
    }

    /**
     * @return a String containing the sequences.
     */
    public String getSequenceString() {
        return sequenceString.toString();
    }

    /**
     * @return a char containing the state at index.
     */
    public char getChar(int index) {
        return sequenceString.charAt(index);
    }

    /**
     * @return the state at site index.
     */
    public int getState(int index) {
        return dataType.getState(sequenceString.charAt(index));
    }

    /**
     */
    public final void setState(int index, int state) {

        sequenceString.setCharAt(index, dataType.getChar(state));
    }

    /**
     * Characters are copied from the sequences into the destination character array dst.
     */
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        sequenceString.getChars(srcBegin, srcEnd, dst, dstBegin);
    }

    /**
     * Set the DataType of the sequences.
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * Set the DataType of the sequences.
     */
    public DataType guessDataType() {
        return DataType.guessDataType(sequenceString.toString());
    }

    /**
     * Set the sequences using a string.
     */
    public void setSequenceString(String sequence) {
        sequenceString.setLength(0);
        sequenceString.append(sequence.toUpperCase());
    }

    /**
     * Append a string to the sequences.
     */
    public void appendSequenceString(String sequence) {
        sequenceString.append(sequence);
    }

    /**
     * Insert a string into the sequences.
     */
    public void insertSequenceString(int offset, String sequence) {
        sequenceString.insert(offset, sequence);
    }

    /**
     * Sets a taxon for this sequences.
     *
     * @param taxon the taxon.
     */
    public void setTaxon(Taxon taxon) {
        this.taxon = taxon;
    }

    /**
     * @return the taxon for this sequences.
     */
    public Taxon getTaxon() {
        return taxon;
    }

    // **************************************************************
    // Attributable IMPLEMENTATION
    // **************************************************************

    private Attributable.AttributeHelper attributes = null;

    /**
     * Sets an named attribute for this object.
     *
     * @param name  the name of the attribute.
     * @param value the new value of the attribute.
     */
    public void setAttribute(String name, Object value) {
        if (attributes == null)
            attributes = new Attributable.AttributeHelper();
        attributes.setAttribute(name, value);
    }

    /**
     * @param name the name of the attribute of interest.
     * @return an object representing the named attributed for this object.
     */
    public Object getAttribute(String name) {
        if (attributes == null)
            return null;
        else
            return attributes.getAttribute(name);
    }

    /**
     * @return an iterator of the attributes that this object has.
     */
    public Iterator<String> getAttributeNames() {
        if (attributes == null)
            return null;
        else
            return attributes.getAttributeNames();
    }

    // **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

    protected String id = null;

    /**
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     */
    public void setId(String id) {
        this.id = id;
    }

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

    protected Taxon taxon = null;
    protected StringBuffer sequenceString = null;
    protected DataType dataType = null;

    public static final XMLObjectParser<Sequence> PARSER = new AbstractXMLObjectParser<Sequence>() {
        public static final String SEQUENCE = "sequence";

        public String getParserName() { return SEQUENCE; }

        /**
         * @return a sequence object based on the XML element it was passed.
         */
        public Sequence parseXMLObject(XMLObject xo) throws XMLParseException {

            Sequence sequence = new Sequence();

            Taxon taxon = (Taxon)xo.getChild(Taxon.class);

            DataType dataType = null;
            if (xo.hasAttribute(DataType.DATA_TYPE)) {
                String dataTypeStr = xo.getStringAttribute(DataType.DATA_TYPE);

                if (dataTypeStr.equals(Nucleotides.DESCRIPTION)) {
                    dataType = Nucleotides.INSTANCE;
                } else if (dataTypeStr.equals(AminoAcids.DESCRIPTION)) {
                    dataType = AminoAcids.INSTANCE;
//                } else if (dataTypeStr.equals(Codons.DESCRIPTION)) {
//                    dataType = Codons.UNIVERSAL;
                } else if (dataTypeStr.equals(TwoStates.DESCRIPTION)) {
                    dataType = TwoStates.INSTANCE;
                }
            }

            StringBuffer seqBuf = new StringBuffer();

            for (int i = 0; i < xo.getChildCount(); i++) {
                Object child = xo.getChild(i);
                if (child instanceof String) {
                    StringTokenizer st = new StringTokenizer((String)child);
                    while (st.hasMoreTokens()) {
                        seqBuf.append(st.nextToken());
                    }
                }
            }

            // We really need to filter the input string to check for illegal characters.
            // Perhaps sequence.setSequenceString could throw an exception if any characters
            // don't fit the dataType.
            String sequenceString = seqBuf.toString();

            if (sequenceString.length() == 0) {
                throw new XMLParseException("Sequence data missing from sequence element!");
            }

            if (dataType != null) {
                sequence.setDataType(dataType);
            }

            sequence.setSequenceString(sequenceString);
            sequence.setTaxon(taxon);

            return sequence;
        }

        public String getParserDescription() {
            return "A biomolecular sequence.";
        }

        public Class getReturnType() { return Sequence.class; }

        public XMLSyntaxRule[] getSyntaxRules() { return rules; }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                new ElementRule(Taxon.class),
                new ElementRule(String.class, "A character string representing the aligned molecular sequence", "ACGACTAGCATCGAGCTTCG--GATAGCATGC")
        };
    };
}


