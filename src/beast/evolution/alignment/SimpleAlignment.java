/*
 * SimpleAlignment.java
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

package beast.evolution.alignment;

//import beast.app.bss.XMLExporter;
//import beast.app.tools.NexusExporter;

import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotides;
import beast.evolution.sequence.Sequence;
import beast.evolution.sequence.Sequences;
import beast.evolution.util.Taxon;
import beast.evolution.util.TaxonList;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLParser;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.Collections;
import java.util.List;

/**
 * A simple alignment class that implements gaps by characters in the sequences.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: SimpleAlignment.java,v 1.46 2005/06/21 16:25:15 beth Exp $
 */
@SuppressWarnings("serial")
public class SimpleAlignment extends Sequences implements Alignment, beast.util.XHTMLable {

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

//    private OutputType outputType = OutputType.FASTA;
    private DataType dataType = null;
    private int siteCount = 0;
    private boolean siteCountKnown = false;
//    private boolean countStatistics = !(dataType instanceof Codons) && !(dataType instanceof GeneralDataType);

    // **************************************************************
    // SimpleAlignment METHODS
    // **************************************************************

    /**
     * parameterless constructor.
     */
    public SimpleAlignment() {
    }

    /**
     * Constructs a sub alignment based on the provided taxa.
     *
     * @param a
     * @param taxa
     */
    public SimpleAlignment(Alignment a, TaxonList taxa) {

        for (int i = 0; i < taxa.getTaxonCount(); i++) {
            Taxon taxon = taxa.getTaxon(i);

            Sequence sequence = a.getSequence(a.getTaxonIndex(taxon));
            addSequence(sequence);
        }
    }

//    public void setOutputType(OutputType out) {
//        outputType = out;
//    }

    public List<Sequence> getSequences() {
        return Collections.unmodifiableList(sequences);
    }

    /**
     * Calculates the siteCount by finding the longest sequence.
     */
    public void updateSiteCount() {
        siteCount = 0;
        int i, len, n = getSequenceCount();

        for (i = 0; i < n; i++) {
            len = getSequence(i).getLength();
            if (len > siteCount)
                siteCount = len;
        }

        siteCountKnown = true;
    }

    // **************************************************************
    // Alignment IMPLEMENTATION
    // **************************************************************

    /**
     * Sets the dataType of this alignment. This should be the same as
     * the sequences.
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * @return number of sites
     */
    public int getSiteCount(DataType dataType) {
        return getSiteCount();
    }

    /**
     * sequence character at (sequence, site)
     */
    public char getChar(int sequenceIndex, int siteIndex) {
        return getSequence(sequenceIndex).getChar(siteIndex);
    }

    /**
     * Returns string representation of single sequence in
     * alignment with gap characters included.
     */
    public String getAlignedSequenceString(int sequenceIndex) {
        return getSequence(sequenceIndex).getSequenceString();
    }


    /**
     * Returns string representation of single sequence in
     * alignment with gap characters excluded.
     */
    public String getUnalignedSequenceString(int sequenceIndex) {

        StringBuffer unaligned = new StringBuffer();
        for (int i = 0, n = getSiteCount(); i < n; i++) {

            int state = getState(sequenceIndex, i);
            if (!dataType.isGapState(state)) {
                unaligned.append(dataType.getChar(state));
            }
        }

        return unaligned.toString();
    }

    // **************************************************************
    // Sequences METHODS
    // **************************************************************

    /**
     * Add a sequence to the sequence list
     */
    public void addSequence(Sequence sequence) {
        if (dataType == null) {
            if (sequence.getDataType() == null) {
                dataType = sequence.guessDataType();
                sequence.setDataType(dataType);
            } else {
                setDataType(sequence.getDataType());
            }
        } else if (sequence.getDataType() == null) {
            sequence.setDataType(dataType);
        } else if (dataType != sequence.getDataType()) {
            throw new IllegalArgumentException("Sequence's dataType does not match the alignment's");
        }

        int invalidCharAt = getInvalidChar(sequence.getSequenceString(), dataType);
        if (invalidCharAt >= 0)
            throw new IllegalArgumentException("Sequence of " + sequence.getTaxon().getId()
                    + " contains invalid char \'" + sequence.getChar(invalidCharAt) + "\' at index " + invalidCharAt);

        super.addSequence(sequence);
        updateSiteCount();
    }

    /**
     * Insert a sequence to the sequence list at position
     */
    public void insertSequence(int position, Sequence sequence) {
        if (dataType == null) {
            if (sequence.getDataType() == null) {
                dataType = sequence.guessDataType();
                sequence.setDataType(dataType);
            } else {
                setDataType(sequence.getDataType());
            }
        } else if (sequence.getDataType() == null) {
            sequence.setDataType(dataType);
        } else if (dataType != sequence.getDataType()) {
            throw new IllegalArgumentException("Sequence's dataType does not match the alignment's");
        }

        int invalidCharAt = getInvalidChar(sequence.getSequenceString(), dataType);
        if (invalidCharAt >= 0)
            throw new IllegalArgumentException("Sequence of " + sequence.getTaxon().getId()
                    + " contains invalid char \'" + sequence.getChar(invalidCharAt) + "\' at index " + invalidCharAt);

        super.insertSequence(position, sequence);
    }

    /**
     * search invalid character in the sequence by given data type, and return its index
     */
    protected int getInvalidChar(String sequence, DataType dataType) {
        final char[] validChars = dataType.getValidChars();
        if (validChars != null) {
            String validString = new String(validChars);

            for (int i = 0; i < sequence.length(); i++) {
                char c = sequence.charAt(i);

                if (validString.indexOf(c) < 0) return i;
            }
        }
        return -1;
    }

    // **************************************************************
    // SiteList IMPLEMENTATION
    // **************************************************************

    /**
     * @return number of sites
     */
    public int getSiteCount() {
        if (!siteCountKnown)
            updateSiteCount();
        return siteCount;
    }

    /**
     * Gets the pattern of site as an array of state numbers (one per sequence)
     *
     * @return the site pattern at siteIndex
     */
    public int[] getSitePattern(int siteIndex) {
        Sequence seq;
        int i, n = getSequenceCount();

        int[] pattern = new int[n];

        for (i = 0; i < n; i++) {
            seq = getSequence(i);

            if (siteIndex >= seq.getLength())
                pattern[i] = dataType.getGapState();
            else
                pattern[i] = seq.getState(siteIndex);
        }

        return pattern;
    }

    /**
     * Gets the pattern index at a particular site
     *
     * @return the patternIndex
     */
    public int getPatternIndex(int siteIndex) {
        return siteIndex;
    }

    /**
     * @return the sequence state at (taxon, site)
     */
    public int getState(int taxonIndex, int siteIndex) {

        Sequence seq = getSequence(taxonIndex);

        if (siteIndex >= seq.getLength()) {
            return dataType.getGapState();
        }

        return seq.getState(siteIndex);
    }

    /**
     */
    public void setState(int taxonIndex, int siteIndex, int state) {

        Sequence seq = getSequence(taxonIndex);

        if (siteIndex >= seq.getLength()) {
            throw new IllegalArgumentException();
        }

        seq.setState(siteIndex, state);
    }

    // **************************************************************
    // PatternList IMPLEMENTATION
    // **************************************************************

    /**
     * @return number of patterns
     */
    public int getPatternCount() {
        return getSiteCount();
    }

    /**
     * @return number of invariant sites
     */
    public int getInvariantCount() {
        int invariantSites = 0;
        for (int i = 0; i < getSiteCount(); i++) {
            int[] pattern = getSitePattern(i);
            if (Patterns.isInvariant(pattern)) {
                invariantSites++;
            }
        }
        return invariantSites;
    }

    public int getUniquePatternCount() {
        Patterns patterns = new Patterns(this);
        return patterns.getPatternCount();
    }

    public int getInformativeCount() {
        Patterns patterns = new Patterns(this);
        int informativeCount = 0;

        for (int i = 0; i < patterns.getPatternCount(); i++) {
            int[] pattern = patterns.getPattern(i);

            if (isInformative(pattern)) {
                informativeCount += patterns.getPatternWeight(i);
            }
        }

        return informativeCount;
    }

    public int getSingletonCount() {
        Patterns patterns = new Patterns(this);
        int singletonCount = 0;

        for (int i = 0; i < patterns.getPatternCount(); i++) {
            int[] pattern = patterns.getPattern(i);

            if (!Patterns.isInvariant(pattern) && !isInformative(pattern)) {
                singletonCount += patterns.getPatternWeight(i);
            }
        }

        return singletonCount;
    }

    private boolean isInformative(int[] pattern) {
        int[] stateCounts = new int[getStateCount()];
        for (int j = 0; j < pattern.length; j++) {
            stateCounts[pattern[j]]++;
        }

        boolean oneStateGreaterThanOne = false;
        boolean secondStateGreaterThanOne = false;
        for (int j = 0; j < stateCounts.length; j++) {
            if (stateCounts[j] > 1) {
                if (!oneStateGreaterThanOne) {
                    oneStateGreaterThanOne = true;
                } else {
                    secondStateGreaterThanOne = true;
                }

            }
        }

        return secondStateGreaterThanOne;
    }

    /**
     * @return number of states for this siteList
     */
    public int getStateCount() {
        return getDataType().getStateCount();
    }

    /**
     * Gets the length of the pattern strings which will usually be the
     * same as the number of taxa
     *
     * @return the length of patterns
     */
    public int getPatternLength() {
        return getSequenceCount();
    }

    /**
     * Gets the pattern as an array of state numbers (one per sequence)
     *
     * @return the pattern at patternIndex
     */
    public int[] getPattern(int patternIndex) {
        return getSitePattern(patternIndex);
    }

    /**
     * @return state at (taxonIndex, patternIndex)
     */
    public int getPatternState(int taxonIndex, int patternIndex) {
        return getState(taxonIndex, patternIndex);
    }

    /**
     * Gets the weight of a site pattern (always 1.0)
     */
    public double getPatternWeight(int patternIndex) {
        return 1.0;
    }

    /**
     * @return the array of pattern weights
     */
    public double[] getPatternWeights() {
        double[] weights = new double[siteCount];
        for (int i = 0; i < siteCount; i++)
            weights[i] = 1.0;
        return weights;
    }

    /**
     * @return the DataType of this siteList
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * @return the frequency of each state
     */
    public double[] getStateFrequencies() {
        return PatternList.Utils.empiricalStateFrequencies(this);
    }

//    public void setReportCountStatistics(boolean report) {
//        countStatistics = report;
//    }

//    public String toString() {
//        return outputType.makeOutputString(this);   // generic delegation to ease extensibility
//    }// END: toString

    public String toXHTML() {
        String xhtml = "<p><em>Alignment</em> data type = ";
        xhtml += getDataType().getDescription();
        xhtml += ", no. taxa = ";
        xhtml += getTaxonCount();
        xhtml += ", no. sites = ";
        xhtml += getSiteCount();
        xhtml += "</p>";

        xhtml += "<pre>";

        int length, maxLength = 0;
        for (int i = 0; i < getTaxonCount(); i++) {
            length = getTaxonId(i).length();
            if (length > maxLength)
                maxLength = length;
        }

        for (int i = 0; i < getTaxonCount(); i++) {
            length = getTaxonId(i).length();
            xhtml += getTaxonId(i);
            for (int j = length; j <= maxLength; j++)
                xhtml += " ";
            xhtml += getAlignedSequenceString(i) + "\n";
        }
        xhtml += "</pre>";
        return xhtml;
    }

//    public enum OutputType {
//        FASTA("fasta", "fsa") {
//            @Override
//            public String makeOutputString(SimpleAlignment alignment) {
//
//                NumberFormatter formatter = new NumberFormatter(6);
//                StringBuffer buffer = new StringBuffer();
//
//                if (alignment.countStatistics) {
//                    buffer.append("Site count = ").append(alignment.getSiteCount()).append("\n");
//                    buffer.append("Invariant sites = ").append(alignment.getInvariantCount()).append("\n");
//                    buffer.append("Singleton sites = ").append(alignment.getSingletonCount()).append("\n");
//                    buffer.append("Parsimony informative sites = ").append(alignment.getInformativeCount()).append("\n");
//                    buffer.append("Unique site patterns = ").append(alignment.getUniquePatternCount()).append("\n\n");
//                }
//
//                for (int i = 0; i < alignment.getSequenceCount(); i++) {
//                    String name = formatter.formatToFieldWidth(alignment.getTaxonId(i), 10);
//                    buffer.append(">" + name + "\n");
//                    buffer.append(alignment.getAlignedSequenceString(i) + "\n");
//                }
//
//                return buffer.toString();
//            }
//        },
//
//        NEXUS("nexus", "nxs") {
//            @Override
//            public String makeOutputString(SimpleAlignment alignment) {
//
//                StringBuffer buffer = new StringBuffer();
//
//                try {
//
//                    File tmp = File.createTempFile("tempfile", ".tmp");
//                    PrintStream ps = new PrintStream(tmp);
//
//                    NexusExporter nexusExporter = new NexusExporter(ps);
//                    buffer.append(nexusExporter.exportAlignment(alignment));
//
//                } catch (IllegalArgumentException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                return buffer.toString();
//            }// END: makeOutputString
//        },
//        XML("xml", "xml") {
//            @Override
//            public String makeOutputString(SimpleAlignment alignment) {
//
//                StringBuffer buffer = new StringBuffer();
//
//                try {
//
//                    XMLExporter xmlExporter = new XMLExporter();
//                    buffer.append(xmlExporter.exportAlignment(alignment));
//
//                } catch (IllegalArgumentException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//					e.printStackTrace();
//				}
//
//                return buffer.toString();
//            }// END: makeOutputString
//        };
//
//        private final String text;
//        private final String extension;
//
//        private OutputType(String text, String extension) {
//            this.text = text;
//            this.extension = extension;
//        }
//
//        public String getText() {
//            return text;
//        }
//
//        public String getExtension() {
//            return extension;
//        }
//
//        public abstract String makeOutputString(SimpleAlignment alignment);
//
//        public static OutputType parseFromString(String text) {
//            for (OutputType type : OutputType.values()) {
//                if (type.getText().compareToIgnoreCase(text) == 0) {
//                    return type;
//                }
//            }
//            return null;
//        }
//
//        public static OutputType parseFromExtension(String extension) {
//            for (OutputType type : OutputType.values()) {
//                if (type.getExtension().compareToIgnoreCase(extension) == 0) {
//                    return type;
//                }
//            }
//            return null;
//        }
//    }

    public static final XMLObjectParser<SimpleAlignment> PARSER = new AbstractXMLObjectParser<SimpleAlignment>() {
        public static final String ALIGNMENT = "alignment";

        public String getParserName() {
            return ALIGNMENT;
        }

        public SimpleAlignment parseXMLObject(XMLObject xo) throws XMLParseException {

            final SimpleAlignment alignment = new SimpleAlignment();

            final DataType dataType = DataType.parseDataType(xo);

            if (dataType == null) {
                throw new XMLParseException("dataType attribute expected for alignment element");
            }

            alignment.setDataType(dataType);

            for (int i = 0; i < xo.getChildCount(); i++) {

                final Object child = xo.getChild(i);
                if (child instanceof Sequence) {
                    alignment.addSequence((Sequence) child);
                } else if (child instanceof DataType) {
                    // already dealt with
                } else {
                    throw new XMLParseException("Unknown child element found in alignment");
                }
            }

            final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("beast.evoxml");
            logger.info("Read alignment" + (xo.hasAttribute(XMLParser.ID) ? ": " + xo.getId() : "") +
                    "\n  Sequences = " + alignment.getSequenceCount() +
                    "\n      Sites = " + alignment.getSiteCount() +
                    "\n   Datatype = " + alignment.getDataType().getDescription());

            return alignment;
        }

        public String getParserDescription() {
            return "This element represents an alignment of molecular sequences.";
        }

        public Class getReturnType() {
            return Alignment.class;
        }

        public String getExample() {

            return
                    "<!-- An alignment of three short DNA sequences -->\n" +
                            "<alignment missing=\"-?\" dataType=\"" + Nucleotides.DESCRIPTION + "\">\n" +
                            "  <sequence>\n" +
                            "    <taxon idref=\"taxon1\"/>\n" +
                            "    ACGACTAGCATCGAGCTTCG--GATAGCAGGC\n" +
                            "  </sequence>\n" +
                            "  <sequence>\n" +
                            "    <taxon idref=\"taxon2\"/>\n" +
                            "    ACGACTAGCATCGAGCTTCGG-GATAGCATGC\n" +
                            "  </sequence>\n" +
                            "  <sequence>\n" +
                            "    <taxon idref=\"taxon3\"/>\n" +
                            "    ACG?CTAGAATCGAGCTTCGAGGATAGCATGC\n" +
                            "  </sequence>\n" +
                            "</alignment>\n";
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new XORRule(
                        new StringAttributeRule(
                                DataType.DATA_TYPE,
                                "The data type",
                                DataType.getRegisteredDataTypeNames(), false),
                        new ElementRule(DataType.class)
                ),
                new ElementRule(Sequence.class, 1, Integer.MAX_VALUE)
        };
    };
}// END: class
