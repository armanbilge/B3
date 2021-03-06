/*
 * Patterns.java
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

import beast.evolution.datatype.DataType;
import beast.evolution.util.Taxon;
import beast.evolution.util.TaxonList;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLParser;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A concrete implementation of PatternList. Patterns can be added and
 * removed from the list individually or in bulk from an alignment.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: Patterns.java,v 1.10 2005/07/08 11:27:53 rambaut Exp $
 */
public class Patterns implements PatternList {

    public static final int COUNT_INCREMENT = 100;

    /**
     * number of patterns
     */
    protected int patternCount = 0;

    /**
     * length of patterns
     */
    protected int patternLength = 0;

    /**
     * weights of each pattern
     */
    protected double[] weights = new double[COUNT_INCREMENT];

    /**
     * site patterns [pattern][taxon]
     */
    protected int[][] patterns = new int[COUNT_INCREMENT][];

    protected DataType dataType = null;

    protected TaxonList taxonList = null;

    /**
     * Constructor
     */
    public Patterns(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * Constructor
     */
    public Patterns(DataType dataType, TaxonList taxonList) {
        this.dataType = dataType;
        this.taxonList = taxonList;
        patternLength = taxonList.getTaxonCount();
    }

    /**
     * Constructor
     */
    public Patterns(SiteList siteList) {
        addPatterns(siteList, 0, 0, 1);
    }

    /**
     * Constructor
     */
    public Patterns(List<SiteList> siteLists) {
        for (SiteList siteList : siteLists) {
            addPatterns(siteList, 0, 0, 1);
        }
    }

    /**
     * Constructor
     */
    public Patterns(SiteList siteList, int from, int to, int every) {
        addPatterns(siteList, from, to, every);
    }

    /**
     * Constructor
     */
    public Patterns(SiteList siteList, int from, int to, int every, int subSet, int subSetCount) {
        addPatterns(siteList, from, to, every);

        if (subSetCount > 0) {
            // if we are using subSetCount then cut it down to only the subset we want...
            int div = patternCount / subSetCount;
            int rem = patternCount % subSetCount;

            int start = 0;
            for (int i = 0; i < subSet; i++) {
                start += div + (i < rem ? 1 : 0);
            }

            int newPatternCount = div;
            if (subSet < rem) {
                newPatternCount++;
            }

            int[][] newPatterns = new int[newPatternCount][];
            double[] newWeights = new double[newPatternCount];
            for (int i = 0; i < newPatternCount; i++) {
                newPatterns[i] = patterns[start + i];
                newWeights[i] = weights[start + i];
            }
            patterns = newPatterns;
            weights = newWeights;

            patternCount = newPatternCount;
        }
    }

    /**
     * Constructor
     */
    public Patterns(PatternList patternList) {
        addPatterns(patternList);
    }

    /**
     * adds patterns to the list from a SiteList
     */
    public void addPatterns(SiteList siteList, int from, int to, int every) {

        if (siteList == null) {
            return;
        }

        if (taxonList == null) {
            taxonList = siteList;
            patternLength = taxonList.getTaxonCount();
        }

        if (dataType == null) {
            dataType = siteList.getDataType();
        } else if (dataType != siteList.getDataType()) {
            throw new IllegalArgumentException("Patterns' existing DataType does not match that of added SiteList");
        }

        if (from < 0)
            from = 0;

        if (to <= 0)
            to = siteList.getSiteCount() - 1;

        if (every <= 0)
            every = 1;

        for (int i = from; i <= to; i += every) {
            int[] pattern = siteList.getSitePattern(i);

            // don't add patterns that are all gaps or all ambiguous
            if (pattern != null && (!isInvariant(pattern) ||
                    (!isGapped(pattern) &&
                            !isAmbiguous(pattern) &&
                            !isUnknown(pattern)))) {

                addPattern(pattern, 1.0);
            }
        }

    }

    /**
     * adds patterns to the list from a SiteList
     */
    public void addPatterns(PatternList patternList) {

        if (patternList == null) {
            return;
        }

        if (taxonList == null) {
            taxonList = patternList;
            patternLength = taxonList.getTaxonCount();
        }

        if (dataType == null) {
            dataType = patternList.getDataType();
        } else if (dataType != patternList.getDataType()) {
            throw new IllegalArgumentException("Patterns' existing DataType does not match that of added PatternList");
        }

        for (int i = 0; i < patternList.getPatternCount(); i++) {
            int[] pattern = patternList.getPattern(i);

            // don't add patterns that are all gaps or all ambiguous
            if (!isInvariant(pattern) ||
                    (!isGapped(pattern) &&
                            !isAmbiguous(pattern) &&
                            !isUnknown(pattern))) {

                addPattern(pattern, patternList.getPatternWeight(i));
            }
        }

    }

    /**
     * adds a pattern to the pattern list with a default weight of 1
     */
    public void addPattern(int[] pattern) {
        addPattern(pattern, 1.0);
    }

    /**
     * adds a pattern to the pattern list
     */
    public void addPattern(int[] pattern, double weight) {

        if (patternLength == 0) {
            patternLength = pattern.length;
        }

        if (patternLength != 0 && pattern.length != patternLength) {
            throw new IllegalArgumentException("Added pattern's length (" + pattern.length + ") does not match those of existing patterns (" + patternLength + ")");
        }

        for (int i = 0; i < patternCount; i++) {

            if (comparePatterns(patterns[i], pattern)) {

                weights[i] += weight;
                return;
            }
        }

        if (patternCount == patterns.length) {
            int[][] newPatterns = new int[patternCount + COUNT_INCREMENT][];
            double[] newWeights = new double[patternCount + COUNT_INCREMENT];
            for (int i = 0; i < patternCount; i++) {
                newPatterns[i] = patterns[i];
                newWeights[i] = weights[i];
            }
            patterns = newPatterns;
            weights = newWeights;
        }

        patterns[patternCount] = pattern;
        weights[patternCount] = weight;
        patternCount++;
    }

    /**
     * removes a pattern from the pattern list
     */
    public void removePattern(int[] pattern) {

        int index = -1;
        for (int i = 0; i < patternCount; i++) {

            if (comparePatterns(patterns[i], pattern)) {
                index = i;
                break;
            }
        }

        if (index == -1) throw new IllegalArgumentException("Pattern not found");

        weights[index] -= 1;
        if (weights[index] == 0 && patternCount > 1) {
            patterns[index] = patterns[patternCount - 1];
            patterns[patternCount - 1] = null;
            weights[index] = weights[patternCount - 1];
            patternCount--;
        }

    }

    /**
     * removes all patterns from the pattern list
     */
    public void removeAllPatterns() {
        patternCount = 0;
        for (int i = 0; i < patterns.length; i++) patterns[i] = null;
    }

    /**
     * @return true if the pattern has one or more gaps
     */
    protected boolean isGapped(int[] pattern) {
        int len = pattern.length;

        for (int i = 0; i < len; i++) {
            if (getDataType().isGapState(pattern[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the pattern has one or more ambiguous states
     */
    protected boolean isAmbiguous(int[] pattern) {
        int len = pattern.length;

        for (int i = 0; i < len; i++) {
            if (getDataType().isAmbiguousState(pattern[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the pattern is invariant
     */
    protected boolean isUnknown(int[] pattern) {
        int len = pattern.length;

        for (int i = 0; i < len; i++) {
            if (getDataType().isUnknownState(pattern[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the pattern is invariant
     */
    protected static boolean isInvariant(int[] pattern) {
        int len = pattern.length;

        int state = pattern[0];
        for (int i = 1; i < len; i++) {
            if (pattern[i] != state) {
                return false;
            }
        }

        return true;
    }

    /**
     * compares two patterns
     *
     * @return true if they are identical
     */
    protected boolean comparePatterns(int[] pattern1, int[] pattern2) {

        int len = pattern1.length;
        for (int i = 0; i < len; i++) {
            if (pattern1[i] != pattern2[i]) {
                return false;
            }
        }

        return true;
    }

    // **************************************************************
    // PatternList IMPLEMENTATION
    // **************************************************************

    /**
     * @return number of patterns
     */
    public int getPatternCount() {
        return patternCount;
    }

    /**
     * @return number of states for this siteList
     */
    public int getStateCount() {
        return dataType.getStateCount();
    }

    /**
     * Gets the length of the pattern strings which will usually be the
     * same as the number of taxa
     *
     * @return the length of patterns
     */
    public int getPatternLength() {
        return patternLength;
    }

    /**
     * Gets the pattern as an array of state numbers (one per sequences)
     *
     * @return the pattern at patternIndex
     */
    public int[] getPattern(int patternIndex) {
        return patterns[patternIndex];
    }

    /**
     * @return state at (taxonIndex, patternIndex)
     */
    public int getPatternState(int taxonIndex, int patternIndex) {
        return patterns[patternIndex][taxonIndex];
    }

    /**
     * Gets the weight of a site pattern
     */
    public double getPatternWeight(int patternIndex) {
        return weights[patternIndex];
    }

    /**
     * @return the array of pattern weights
     */
    public double[] getPatternWeights() {
        double[] w = new double[weights.length];
        for (int i = 0; i < weights.length; i++) w[i] = weights[i];
        return w;
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

    // **************************************************************
    // TaxonList IMPLEMENTATION
    // **************************************************************

    /**
     * @return a count of the number of taxa in the list.
     */
    public int getTaxonCount() {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonCount();
    }

    /**
     * @return the ith taxon.
     */
    public Taxon getTaxon(int taxonIndex) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxon(taxonIndex);
    }

    /**
     * @return the ID of the ith taxon.
     */
    public String getTaxonId(int taxonIndex) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonId(taxonIndex);
    }

    /**
     * returns the index of the taxon with the given id.
     */
    public int getTaxonIndex(String id) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonIndex(id);
    }

    /**
     * returns the index of the given taxon.
     */
    public int getTaxonIndex(Taxon taxon) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonIndex(taxon);
    }

    public List<Taxon> asList() {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.asList();
    }

    public Iterator<Taxon> iterator() {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.iterator();
    }


    /**
     * @param taxonIndex the index of the taxon whose attribute is being fetched.
     * @param name       the name of the attribute of interest.
     * @return an object representing the named attributed for the given taxon.
     */
    public Object getTaxonAttribute(int taxonIndex, String name) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        return taxonList.getTaxonAttribute(taxonIndex, name);
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

    // ========= Mask =========
    // indexes to mask sth., e.g. taxon index whose state is unknown character in microsatellite
    protected Set<Integer> maskSet = new HashSet<Integer>();

    // no duplication, if duplicate, not add
    public boolean addMask(int index) {
        return maskSet.add(index);
    }

    public boolean isMasked(int index) {
        return maskSet.contains(index);
    }

    public boolean hasMask() {
        return maskSet.size() > 0;
    }

    public void clearMask() {
        maskSet.clear();
    }

    public Set<Integer> getMaskSet() {
        return maskSet;
    }

    /**
     * @return the ith taxon not masked.
     */
    public Taxon getTaxonMasked(int taxonIndex) {
        if (taxonList == null) throw new RuntimeException("Patterns has no TaxonList");
        if (isMasked(taxonIndex)) {
            return null;
        }
        return taxonList.getTaxon(taxonIndex);
    }

    public static final XMLObjectParser<Patterns> SUB_SET_PARSER = new AbstractXMLObjectParser<Patterns>() {
        public static final String PATTERNS_SUB_SET = "patternSubSet";
        public static final String SUB_SET = "subSet";
        public static final String SUB_SET_COUNT = "subSetCount";

        public String getParserName() {
            return PATTERNS_SUB_SET;
        }

        /**
         * Parses a patterns element and returns a patterns object.
         */
        public Patterns parseXMLObject(XMLObject xo) throws XMLParseException {

            SiteList patterns = (SiteList) xo.getChild(SiteList.class);

            int subSet = 0;
            int subSetCount = 0;

            if (xo.hasAttribute(SUB_SET)) {
                subSet = xo.getIntegerAttribute(SUB_SET) - 1;
                if (subSet < 0)
                    throw new XMLParseException("illegal 'subSet' attribute in patterns element");
            }

            if (xo.hasAttribute(SUB_SET_COUNT)) {
                subSetCount = xo.getIntegerAttribute(SUB_SET_COUNT);
                if (subSetCount < 0)
                    throw new XMLParseException("illegal 'subSetCount' attribute in patterns element");
            }

            Patterns subPatterns = new Patterns(patterns, 0, 0, 1, subSet, subSetCount);

            if (xo.hasAttribute(XMLParser.ID)) {
                final Logger logger = Logger.getLogger("beast.evoxml");
                logger.info("Pattern subset '" + xo.getId() + "' created from '" + patterns.getId() +"' ("+(subSet+1)+"/"+subSetCount+")");
                logger.info("  pattern count = " + subPatterns.getPatternCount());
            }

            return subPatterns;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newIntegerRule(SUB_SET, true, "Which subset of patterns to use (out of subSetCount)"),
                AttributeRule.newIntegerRule(SUB_SET_COUNT, true, "The number of subsets"),

                new ElementRule(SiteList.class)
        };

        public String getParserDescription() {
            return "A weighted list of the unique site patterns (unique columns) in an alignment.";
        }

        public Class getReturnType() {
            return Patterns.class;
        }

    };

    public static final XMLObjectParser<Patterns> MERGE_PARSER = new AbstractXMLObjectParser<Patterns>() {

        public static final String MERGE_PATTERNS = "mergePatterns";

        public String getParserName() { return MERGE_PATTERNS; }

        /**
         * Parses a patterns element and returns a patterns object.
         */
        public Patterns parseXMLObject(XMLObject xo) throws XMLParseException {

            PatternList patternList = (PatternList)xo.getChild(0);
            Patterns patterns = new Patterns(patternList);
            for (int i = 1; i < xo.getChildCount(); i++) {
                patterns.addPatterns((PatternList)xo.getChild(i));
            }

            if (xo.hasAttribute(XMLParser.ID)) {
                final Logger logger = Logger.getLogger("beast.evoxml");
                logger.info("Site patterns '" + xo.getId() + "' created by merging " + xo.getChildCount() + " pattern lists");
                logger.info("  pattern count = " + patterns.getPatternCount());
            }

            return patterns;
        }

        public XMLSyntaxRule[] getSyntaxRules() { return rules; }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(PatternList.class, 1, Integer.MAX_VALUE)
        };

        public String getParserDescription() {
            return "A weighted list of the unique site patterns (unique columns) in an alignment.";
        }

        public Class getReturnType() { return Patterns.class; }

    };

    public static final XMLObjectParser<Patterns> ATTRIBUTE_PARSER = new AbstractXMLObjectParser<Patterns>() {

        public static final String ATTRIBUTE = "attribute";

        public static final String PATTERNS = "Patterns";
        public static final String ATTRIBUTE_PATTERNS = ATTRIBUTE + PATTERNS;

        public String getParserName() { return ATTRIBUTE_PATTERNS; }

        public Patterns parseXMLObject(XMLObject xo) throws XMLParseException {

            String attributeName = xo.getStringAttribute(ATTRIBUTE);
            TaxonList taxa = (TaxonList)xo.getChild(TaxonList.class);
            DataType dataType = DataType.parseDataType(xo);

            if (dataType == null) {
                throw new XMLParseException("dataType expected for attributePatterns element");
            }

            Patterns patterns = new Patterns(dataType, taxa);

            int[] pattern = new int[taxa.getTaxonCount()];

            boolean attributeFound = false;

            for (int i = 0; i < taxa.getTaxonCount(); i++) {
                Taxon taxon = taxa.getTaxon(i);

                Object value = taxon.getAttribute(attributeName);

                if (value != null) {
                    int state = dataType.getState(value.toString());
                    if (state < 0) {
                        throw new XMLParseException("State for attribute, " + attributeName + ", in taxon, " + taxon.getId() + ", is unknown: " + value.toString());
                    }
                    pattern[i] = state;
                    attributeFound = true;
                } else {
                    pattern[i] = dataType.getUnknownState();
                }
            }

            if (!attributeFound) {
                throw new XMLParseException("The attribute, " + attributeName + " was missing in all taxa. Check the name of the attribute.");
            }

            patterns.addPattern(pattern);

            if (xo.hasAttribute(XMLParser.ID)) {
                Logger.getLogger("beast.evoxml").info("Read attribute patterns, '" + xo.getId() + "' for attribute, "+ attributeName);
            } else {
                Logger.getLogger("beast.evoxml").info("Read attribute patterns for attribute, "+ attributeName);
            }

            return patterns;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() { return rules; }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                new XORRule(
                        new StringAttributeRule(
                                DataType.DATA_TYPE,
                                "The data type",
                                DataType.getRegisteredDataTypeNames(), false),
                        new ElementRule(DataType.class)
                ),
                AttributeRule.newStringRule(ATTRIBUTE),
                new ElementRule(TaxonList.class, "The taxon set")
        };

        public String getParserDescription() {
            return "A site pattern defined by an attribute in a set of taxa.";
        }

        public Class getReturnType() { return Patterns.class; }

    };

}
