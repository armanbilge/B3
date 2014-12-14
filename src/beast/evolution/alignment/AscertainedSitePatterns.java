/*
 * AscertainedSitePatterns.java
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

import beast.evolution.util.TaxonList;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ContentRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.logging.Logger;

/**
 * Package: AscertainedSitePatterns
 * Description:
 * <p/>
 * <p/>
 * Created by
 * Alexander V. Alekseyenko (alexander.alekseyenko@gmail.com)
 * Date: Mar 10, 2008
 * Time: 12:50:36 PM
 */
public class AscertainedSitePatterns extends SitePatterns {

    protected int[] includePatterns;
    protected int[] excludePatterns;
    protected int ascertainmentIncludeCount;
    protected int ascertainmentExcludeCount;

    /**
     * Constructor
     */
    public AscertainedSitePatterns(Alignment alignment) {
        super(alignment);
    }

    /**
     * Constructor
     */
    public AscertainedSitePatterns(Alignment alignment, TaxonList taxa) {
        super(alignment, taxa);
    }

    /**
     * Constructor
     */
    public AscertainedSitePatterns(Alignment alignment, int from, int to, int every) {
        super(alignment, from, to, every);
    }

    /**
     * Constructor
     */
    public AscertainedSitePatterns(Alignment alignment, TaxonList taxa, int from, int to, int every) {
        super(alignment, taxa, from, to, every);
    }

    /**
     * Constructor
     */
    public AscertainedSitePatterns(SiteList siteList) {
        super(siteList);
    }

    /**
     * Constructor
     */
    public AscertainedSitePatterns(SiteList siteList, int from, int to, int every) {
        super(siteList, from, to, every);
    }

    public AscertainedSitePatterns(Alignment alignment, TaxonList taxa, int from, int to, int every,
                                   int includeFrom, int includeTo,
                                   int excludeFrom, int excludeTo) {
        super(alignment, taxa, from, to, every);
        int[][] newPatterns = new int[patterns.length + (includeTo - includeFrom) + (excludeTo - excludeFrom)][];
        double[] newWeights = new double[patterns.length + (includeTo - includeFrom) + (excludeTo - excludeFrom)];
        for (int i = 0; i < patterns.length; ++i) {
            newPatterns[i] = patterns[i];
            newWeights[i] = weights[i];
        }
        patterns = newPatterns;
        weights = newWeights;

        if (includeTo - includeFrom >= 1)
            includePatterns(includeFrom, includeTo, every);
        if (excludeTo - excludeFrom >= 1)
            excludePatterns(excludeFrom, excludeTo, every);

    }

    public int getIncludePatternCount() {
        return ascertainmentIncludeCount;
    }

    public int[] getIncludePatternIndices() {
        return includePatterns;
    }

    protected void includePatterns(int includeFrom, int includeTo, int every) {
        if (includePatterns == null) {
            includePatterns = new int[includeTo - includeFrom];
        }
        for (int i = includeFrom; i < includeTo; i += every) {
            int[] pattern = siteList.getPattern(i);
            int index = addAscertainmentPattern(pattern);
            includePatterns[ascertainmentIncludeCount] = index;
            ascertainmentIncludeCount += 1;
        }
    }

    public int getExcludePatternCount() {
        return ascertainmentExcludeCount;
    }

    public int[] getExcludePatternIndices() {
        return excludePatterns;
    }

    protected void excludePatterns(int excludeFrom, int excludeTo, int every) {
        if (excludePatterns == null)
            excludePatterns = new int[excludeTo - excludeFrom];

        for (int i = excludeFrom; i < excludeTo; i += every) {
            int[] pattern = siteList.getPattern(i);
            int index = addAscertainmentPattern(pattern);
            weights[index] = 0.0; // Site is excluded, so set weight = 0
            excludePatterns[ascertainmentExcludeCount] = index;
            ascertainmentExcludeCount += 1;
        }

    }

    public double getAscertainmentCorrection(double[] patternLogProbs) {
        double excludeProb = 0, includeProb = 0, returnProb = 1.0;

        int[] includeIndices = getIncludePatternIndices();
        int[] excludeIndices = getExcludePatternIndices();
        for (int i = 0; i < getIncludePatternCount(); i++) {
            int index = includeIndices[i];
            includeProb += Math.exp(patternLogProbs[index]);
        }
        for (int j = 0; j < getExcludePatternCount(); j++) {
            int index = excludeIndices[j];
            excludeProb += Math.exp(patternLogProbs[index]);
        }
        if (includeProb == 0.0) {
            returnProb -= excludeProb;
        } else if (excludeProb == 0.0) {
            returnProb = includeProb;
        } else {
            returnProb = includeProb - excludeProb;
        }
        return Math.log(returnProb);
    }

    private int addAscertainmentPattern(int[] pattern) {
        for (int i = 0; i < patternCount; i++) {
            if (comparePatterns(patterns[i], pattern)) {
                return i;
            }
        }
        int index = patternCount;
        patterns[index] = pattern;
        weights[index] = 0.0;  /* do not affect weight */
        patternCount++;

        return index;
    }

    public static final XMLObjectParser<AscertainedSitePatterns> PARSER = new AbstractXMLObjectParser<AscertainedSitePatterns>() {

        public static final String APATTERNS = "ascertainedPatterns";
        public static final String FROM = "from";
        public static final String TO = "to";
        public static final String EVERY = "every";
        public static final String TAXON_LIST = "taxonList";
        public static final String INCLUDE = "includePatterns";
        public static final String EXCLUDE = "excludePatterns";

        public String getParserName() {
            return APATTERNS;
        }

        public AscertainedSitePatterns parseXMLObject(XMLObject xo) throws XMLParseException {
            Alignment alignment = (Alignment) xo.getChild(Alignment.class);
            XMLObject xoc;
            TaxonList taxa = null;

            int from = -1;
            int to = -1;
            int every = xo.getAttribute(EVERY, 1);
            if (every <= 0) throw new XMLParseException("illegal 'every' attribute in patterns element");

            int startInclude = -1;
            int stopInclude = -1;
            int startExclude = -1;
            int stopExclude = -1;

            if (xo.hasAttribute(FROM)) {
                from = xo.getIntegerAttribute(FROM) - 1;
                if (from < 0)
                    throw new XMLParseException("illegal 'from' attribute in patterns element");
            }

            if (xo.hasAttribute(TO)) {
                to = xo.getIntegerAttribute(TO) - 1;
                if (to < 0 || to < from)
                    throw new XMLParseException("illegal 'to' attribute in patterns element");
            }


            if (xo.hasChildNamed(TAXON_LIST)) {
                taxa = (TaxonList) xo.getElementFirstChild(TAXON_LIST);
            }

            if (from > alignment.getSiteCount())
                throw new XMLParseException("illegal 'from' attribute in patterns element");

            if (to > alignment.getSiteCount())
                throw new XMLParseException("illegal 'to' attribute in patterns element");

            if (from < 0) from = 0;
            if (to < 0) to = alignment.getSiteCount() - 1;

//        if (xo.hasAttribute(XMLParser.ID)) {
            Logger.getLogger("dr.evoxml").info("Creating ascertained site patterns '" + xo.getId() + "' from positions " +
                    Integer.toString(from + 1) + "-" + Integer.toString(to + 1) +
                    " of alignment '" + alignment.getId() + "'");
            if (every > 1) {
                Logger.getLogger("dr.evoxml").info("  only using every " + every + " site");
            }
//        }

            if (xo.hasChildNamed(INCLUDE)) {
                xoc = xo.getChild(INCLUDE);
                if (xoc.hasAttribute(FROM) && xoc.hasAttribute(TO)) {
                    startInclude = xoc.getIntegerAttribute(FROM) - 1;
                    stopInclude = xoc.getIntegerAttribute(TO);
                } else {
                    throw new XMLParseException("both from and to attributes are required for includePatterns");
                }

                if (startInclude < 0 || stopInclude < startInclude) {
                    throw new XMLParseException("invalid 'from' and 'to' attributes in includePatterns");
                }
                Logger.getLogger("dr.evoxml").info("\tAscertainment: Patterns in columns " + (startInclude + 1) + " to " + (stopInclude) + " are only possible. ");
            }

            if (xo.hasChildNamed(EXCLUDE)) {
                xoc = xo.getChild(EXCLUDE);
                if (xoc.hasAttribute(FROM) && xoc.hasAttribute(TO)) {
                    startExclude = xoc.getIntegerAttribute(FROM) - 1;
                    stopExclude = xoc.getIntegerAttribute(TO);
                } else {
                    throw new XMLParseException("both from and to attributes are required for excludePatterns");
                }

                if (startExclude < 0 || stopExclude < startExclude) {
                    throw new XMLParseException("invalid 'from' and 'to' attributes in includePatterns");
                }
                Logger.getLogger("dr.evoxml").info("\tAscertainment: Patterns in columns " + (startExclude + 1) + " to " + (stopExclude) + " are not possible. ");
            }

            AscertainedSitePatterns patterns = new AscertainedSitePatterns(alignment, taxa,
                    from, to, every,
                    startInclude, stopInclude,
                    startExclude, stopExclude);

            Logger.getLogger("dr.evoxml").info("\tThere are " + patterns.getPatternCount() + " patterns in total.");

//            Logger.getLogger("dr.evoxml").info("\tPlease cite:\n" + Citable.Utils.getCitationString(patterns));

            return patterns;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                AttributeRule.newIntegerRule(FROM, true, "The site position to start at, default is 1 (the first position)"),
                AttributeRule.newIntegerRule(TO, true, "The site position to finish at, must be greater than <b>" + FROM + "</b>, default is length of given alignment"),
                AttributeRule.newIntegerRule(EVERY, true, "Determines how many sites are selected. A value of 3 will select every third site starting from <b>" + FROM + "</b>, default is 1 (every site)"),
                new ElementRule(TAXON_LIST,
                        new XMLSyntaxRule[]{new ElementRule(TaxonList.class)}, true),
                new ElementRule(Alignment.class),
                new ContentRule("<includePatterns from=\"Z\" to=\"X\"/>"),
                new ContentRule("<excludePatterns from=\"Z\" to=\"X\"/>")
        };

        public String getParserDescription() {
            return "A weighted list of the unique site patterns (unique columns) in an alignment.";
        }

        public Class getReturnType() {
            return PatternList.class;
        }

    };
}
