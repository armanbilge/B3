/*
 * TreeLogger.java
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

package beast.evolution.tree;

import beast.inference.loggers.LogFormatter;
import beast.inference.loggers.Loggable;
import beast.inference.loggers.MCLogger;
import beast.inference.loggers.TabDelimitedFormatter;
import beast.inference.model.Likelihood;
import beast.inference.model.Model;
import beast.util.FileHelpers;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLParser;
import beast.xml.XMLSyntaxRule;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A logger that logs tree and clade frequencies.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: TreeLogger.java,v 1.25 2006/09/05 13:29:34 rambaut Exp $
 */
public class TreeLogger extends MCLogger {

    private Tree tree;
	private BranchRates branchRates = null;

    private TreeAttributeProvider[] treeAttributeProviders;
    private TreeTraitProvider[] treeTraitProviders;

    private boolean nexusFormat = false;
    public boolean usingRates = false;
    public boolean substitutions = false;
    private final Map<String, Integer> idMap = new HashMap<String, Integer>();
    private final List<String> taxaIds = new ArrayList<String>();
    private boolean mapNames = true;

    /*private double normaliseMeanRateTo = Double.NaN;
    boolean normaliseMeanRate = false;*/

    private NumberFormat format;

    public TreeLogger(Tree tree, LogFormatter formatter, int logEvery, boolean nexusFormat,
                      boolean sortTranslationTable, boolean mapNames) {

        this(tree, null, null, null, formatter, logEvery, nexusFormat, sortTranslationTable, mapNames, null, null/*, Double.NaN*/);
    }

    public TreeLogger(Tree tree, LogFormatter formatter, int logEvery, boolean nexusFormat,
                      boolean sortTranslationTable, boolean mapNames, NumberFormat format) {

        this(tree, null, null, null, formatter, logEvery, nexusFormat, sortTranslationTable, mapNames, format, null/*, Double.NaN*/);
    }

    public TreeLogger(Tree tree, BranchRates branchRates,
                      TreeAttributeProvider[] treeAttributeProviders,
                      TreeTraitProvider[] treeTraitProviders,
                      LogFormatter formatter, int logEvery, boolean nexusFormat,
                      boolean sortTranslationTable, boolean mapNames, NumberFormat format,
                      TreeLogger.LogUpon condition) {

        super(formatter, logEvery, false);

        addLogCondition(condition);

        /*this.normaliseMeanRateTo = normaliseMeanRateTo;
        if(!Double.isNaN(normaliseMeanRateTo)) {
            normaliseMeanRate = true;
        }*/

        this.nexusFormat = nexusFormat;
        // if not NEXUS, can't map names
        this.mapNames = mapNames && nexusFormat;

        this.branchRates = branchRates;

        this.treeAttributeProviders = treeAttributeProviders;
        this.treeTraitProviders = treeTraitProviders;

        if (this.branchRates != null) {
            this.substitutions = true;
        }
        this.tree = tree;

        for (int i = 0; i < tree.getTaxonCount(); i++) {
            taxaIds.add(tree.getTaxon(i).getId());
        }
        if (sortTranslationTable) {
            Collections.sort(taxaIds);
        }

        int k = 1;
        for (String taxaId : taxaIds) {
            idMap.put(taxaId, k);
            k += 1;
        }

        this.format = format;
    }

    public void startLogging() {

        if (nexusFormat) {
            int taxonCount = tree.getTaxonCount();
            logLine("#NEXUS");
            logLine("");
            logLine("Begin taxa;");
            logLine("\tDimensions ntax=" + taxonCount + ";");
            logLine("\tTaxlabels");

            for (String taxaId : taxaIds) {
                logLine("\t\t" + cleanTaxonName(taxaId));
                }

            logLine("\t\t;");
            logLine("End;");
            logLine("");
            logLine("Begin trees;");

            if (mapNames) {
                // This is needed if the trees use numerical taxon labels
                logLine("\tTranslate");
                int k = 1;
                for (String taxaId : taxaIds) {
                    if (k < taxonCount) {
                        logLine("\t\t" + k + " " + cleanTaxonName(taxaId) + ",");
                    } else {
                        logLine("\t\t" + k + " " + cleanTaxonName(taxaId));
                    }
                    k += 1;
                }
                logLine("\t\t;");
            }
        }
    }

    private String cleanTaxonName(String taxaId) {
        if (taxaId.matches(".*[\\s\\.;,\"\'].*")) { // NexusExporter.SPECIAL_CHARACTERS_REGEX
            if (taxaId.contains("\'")) {
                if (taxaId.contains("\"")) {
                    throw new RuntimeException("Illegal taxon name - contains both single and double quotes");
                }

                return "\"" + taxaId + "\"";
            }

            return "\'" + taxaId + "\'";
        }
        return taxaId;
    }

    public void logState(long state) {

        StringBuffer buffer = new StringBuffer(TREE_STATE_);
        buffer.append(state);
        if (treeAttributeProviders != null) {
            boolean hasAttribute = false;
            for (TreeAttributeProvider tap : treeAttributeProviders) {
                String[] attributeLabel = tap.getTreeAttributeLabel();
                String[] attributeValue = tap.getAttributeForTree(tree);
                for (int i = 0; i < attributeLabel.length; i++) {
                    if (!hasAttribute) {
                        buffer.append(" [&");
                        hasAttribute = true;
                    } else {
                        buffer.append(",");
                    }
                    buffer.append(attributeLabel[i]);
                    buffer.append("=");
                    buffer.append(attributeValue[i]);
                }
            }
            if (hasAttribute) {
                buffer.append("]");
            }
        }

        buffer.append(" = [&R] ");

        if (substitutions) {
            Tree.Utils.newick(tree, tree.getRoot(), false, Tree.BranchLengthType.LENGTHS_AS_SUBSTITUTIONS,
                    format, branchRates, treeTraitProviders, idMap, buffer);
        } else {
            Tree.Utils.newick(tree, tree.getRoot(), !mapNames, Tree.BranchLengthType.LENGTHS_AS_TIME,
                    format, null, treeTraitProviders, idMap, buffer);
        }

        buffer.append(";");
        logLine(buffer.toString());

    }

    public void stopLogging() {
        logLine(END);
        super.stopLogging();
    }

    public Tree getTree() {
		return tree;
	}

	public void setTree(Tree tree) {
		this.tree = tree;
	}

    protected long getLastLoggedState(final File file) {
        String line;
        try {
            line = FileHelpers.readLastLine(file);
            if (line.contains(END)) {
                FileHelpers.deleteLastLine(file);
                line = FileHelpers.readLastLine(file);
            }
        } catch (final IOException ex) {
            throw new RuntimeException("Problem resuming logger!", ex);
        }
        return Long.parseLong(line.trim().substring(TREE_STATE_.length()).split("\\s+")[0]);
    }

    private static final String END = "End;";
    private static final String TREE_STATE_ = "tree TREE_STATE_";

    public static final XMLObjectParser<TreeLogger> PARSER = new AbstractXMLObjectParser<TreeLogger>() {

        public static final String LOG_TREE = "logTree";
        public static final String NEXUS_FORMAT = "nexusFormat";
        //    public static final String USING_RATES = "usingRates";
        public static final String BRANCH_LENGTHS = "branchLengths";
        public static final String TIME = "time";
        public static final String SUBSTITUTIONS = "substitutions";
        public static final String SORT_TRANSLATION_TABLE = "sortTranslationTable";
        public static final String MAP_NAMES = "mapNamesToNumbers";
        //    public static final String NORMALISE_MEAN_RATE_TO = "normaliseMeanRateTo";

        public static final String FILTER_TRAITS = "traitFilter";
        public static final String TREE_TRAIT = "trait";
        public static final String NAME = "name";
        public static final String TAG = "tag";

        public static final String LOG = "log";
        public static final String ECHO = "echo";
        public static final String ECHO_EVERY = "echoEvery";
        public static final String TITLE = "title";
        public static final String HEADER = "header";
        public static final String FILE_NAME = FileHelpers.FILE_NAME;
        public static final String FORMAT = "format";
        public static final String TAB = "tab";
        public static final String HTML = "html";
        public static final String PRETTY = "pretty";
        public static final String LOG_EVERY = "logEvery";
        public static final String ALLOW_OVERWRITE_LOG = "overwrite";

        public static final String COLUMNS = "columns";
        public static final String COLUMN = "column";
        public static final String LABEL = "label";
        public static final String SIGNIFICANT_FIGURES = "sf";
        public static final String DECIMAL_PLACES = "dp";
        public static final String WIDTH = "width";


        public String getParserName() {
            return LOG_TREE;
        }

        protected void parseXMLParameters(XMLObject xo) throws XMLParseException
        {
            // reset this every time...
            branchRates = null;

            tree = (Tree) xo.getChild(Tree.class);

            title = xo.getAttribute(TITLE, "");

            nexusFormat = xo.getAttribute(NEXUS_FORMAT, false);

            sortTranslationTable = xo.getAttribute(SORT_TRANSLATION_TABLE, true);

            boolean substitutions = xo.getAttribute(BRANCH_LENGTHS, "").equals(SUBSTITUTIONS);

            List<TreeAttributeProvider> taps = new ArrayList<TreeAttributeProvider>();
            List<TreeTraitProvider> ttps = new ArrayList<TreeTraitProvider>();

            // ttps2 are for TTPs that are not specified within a Trait element. These are only
            // included if not already added through a trait element to avoid duplication of
            // (in particular) the BranchRates which is required for substitution trees.
            List<TreeTraitProvider> ttps2 = new ArrayList<TreeTraitProvider>();

            for (int i = 0; i < xo.getChildCount(); i++) {
                Object cxo = xo.getChild(i);

                // This needs to be refactored into using a TreeTrait if Colouring is resurrected...
//            if (cxo instanceof TreeColouringProvider) {
//                final TreeColouringProvider colouringProvider = (TreeColouringProvider) cxo;
//                baps.add(new BranchAttributeProvider() {
//
//                    public String getBranchAttributeLabel() {
//                        return "deme";
//                    }
//
//                    public String getAttributeForBranch(Tree tree, NodeRef node) {
//                        TreeColouring colouring = colouringProvider.getTreeColouring(tree);
//                        BranchColouring bcol = colouring.getBranchColouring(node);
//                        StringBuilder buffer = new StringBuilder();
//                        if (bcol != null) {
//                            buffer.append("{");
//                            buffer.append(bcol.getChildColour());
//                            for (int i = 1; i <= bcol.getNumEvents(); i++) {
//                                buffer.append(",");
//                                buffer.append(bcol.getBackwardTime(i));
//                                buffer.append(",");
//                                buffer.append(bcol.getBackwardColourAbove(i));
//                            }
//                            buffer.append("}");
//                        }
//                        return buffer.toString();
//                    }
//                });
//
//            } else

                if (cxo instanceof Likelihood) {
                    final Likelihood likelihood = (Likelihood) cxo;
                    taps.add(new LikelihoodTreeAttributeProvider(likelihood));
                }

                if (cxo instanceof TreeAttributeProvider) {
                    taps.add((TreeAttributeProvider) cxo);
                }
                if (cxo instanceof TreeTraitProvider) {
                    if (xo.hasAttribute(FILTER_TRAITS)) {
                        String[] matches = ((String) xo.getAttribute(FILTER_TRAITS)).split("[\\s,]+");
                        TreeTraitProvider ttp = (TreeTraitProvider) cxo;
                        TreeTrait[] traits = ttp.getTreeTraits();
                        List<TreeTrait> filteredTraits = new ArrayList<TreeTrait>();
                        for (String match : matches) {
                            for (TreeTrait trait : traits) {
                                if (trait.getTraitName().startsWith(match)) {
                                    filteredTraits.add(trait);
                                }
                            }
                        }
                        if (filteredTraits.size() > 0) {
                            ttps2.add(new TreeTraitProvider.Helper(filteredTraits));
                        }

                    } else {
                        // Add all of them
                        ttps2.add((TreeTraitProvider) cxo);
                    }
                }
                if (cxo instanceof XMLObject) {
                    XMLObject xco = (XMLObject)cxo;
                    if (xco.getName().equals(TREE_TRAIT)) {

                        TreeTraitProvider ttp = (TreeTraitProvider)xco.getChild(TreeTraitProvider.class);

                        if (xco.hasAttribute(NAME)) {
                            // a specific named trait is required (optionally with a tag to name it in the tree file)

                            String name = xco.getStringAttribute(NAME);
                            final TreeTrait trait = ttp.getTreeTrait(name);

                            if (trait == null) {
                                String childName = "TreeTraitProvider";

                                if (ttp instanceof Likelihood) {
                                    childName = ((Likelihood)ttp).prettyName();
                                } else  if (ttp instanceof Model) {
                                    childName = ((Model)ttp).getModelName();
                                }

                                throw new XMLParseException("Trait named, " + name + ", not found for " + childName);
                            }

                            final String tag;
                            if (xco.hasAttribute(TAG)) {
                                tag = xco.getStringAttribute(TAG);
                            } else {
                                tag = name;
                            }

                            ttps.add(new TreeTraitProvider.Helper(tag, new WrappedTreeTrait(tag, trait)));
                        } else if (xo.hasAttribute(FILTER_TRAITS)) {
                            // else a filter attribute is given to ask for all traits that starts with a specific
                            // string

                            String[] matches = ((String) xo.getAttribute(FILTER_TRAITS)).split("[\\s,]+");
                            TreeTrait[] traits = ttp.getTreeTraits();
                            List<TreeTrait> filteredTraits = new ArrayList<TreeTrait>();
                            for (String match : matches) {
                                for (TreeTrait trait : traits) {
                                    if (trait.getTraitName().startsWith(match)) {
                                        filteredTraits.add(trait);
                                    }
                                }
                            }
                            if (filteredTraits.size() > 0) {
                                ttps.add(new TreeTraitProvider.Helper(filteredTraits));
                            }

                        } else {
                            // neither named or filtered traits so just add them all
                            ttps.add(ttp);
                        }
                    }
                }
                // Without this next block, branch rates get ignored :-(
                // BranchRateModels are now TreeTraitProviders so this is not needed.
//            if (cxo instanceof TreeTrait) {
//                final TreeTrait trait = (TreeTrait)cxo;
//                TreeTraitProvider ttp = new TreeTraitProvider() {
//                    public TreeTrait[] getTreeTraits() {
//                        return new TreeTrait[]  { trait };
//                    }
//
//                    public TreeTrait getTreeTrait(String key) {
//                        if (key.equals(trait.getTraitName())) {
//                            return trait;
//                        }
//                        return null;
//                    }
//                };
//                ttps.add(ttp);
//            }
                //}

                // be able to put arbitrary statistics in as tree attributes
                if (cxo instanceof Loggable) {
                    final Loggable loggable = (Loggable) cxo;
                    taps.add(new LoggableTreeAttributeProvider(loggable));
                }

            }

            // if we don't have any of the newer trait elements but we do have some tree trait providers
            // included directly then assume the user wanted to log these as tree traits (it may be an older
            // form XML).
//        if (ttps.size() == 0 && ttps2.size() > 0) {
//            ttps.addAll(ttps2);
//        }

            // The above code destroyed the logging of complete histories - which need to be logged by direct
            // inclusion of the codon partitioned robust counting TTP...
            if (ttps2.size() > 0) {
                ttps.addAll(ttps2);
            }

            if (substitutions) {
                branchRates = (BranchRates) xo.getChild(BranchRates.class);
            }
            if (substitutions && branchRates == null) {
                throw new XMLParseException("To log trees in units of substitutions a BranchRateModel must be provided");
            }

            // logEvery of zero only displays at the end
            logEvery = xo.getAttribute(LOG_EVERY, 0);

//        double normaliseMeanRateTo = xo.getAttribute(NORMALISE_MEAN_RATE_TO, Double.NaN);

            // decimal places
            final int dp = xo.getAttribute(DECIMAL_PLACES, -1);
            if (dp != -1) {
                format = NumberFormat.getNumberInstance(Locale.ENGLISH);
                format.setMaximumFractionDigits(dp);
            }

            final PrintWriter pw = XMLParser.getFilePrintWriter(xo, getParserName());

            formatter = new TabDelimitedFormatter(pw);

            treeAttributeProviders = new TreeAttributeProvider[taps.size()];
            taps.toArray(treeAttributeProviders);
            treeTraitProviders = new TreeTraitProvider[ttps.size()];
            ttps.toArray(treeTraitProviders);

            // I think the default should be to have names rather than numbers, thus the false default - AJD
            // I think the default should be numbers - using names results in larger files and end user never
            // sees the numbers anyway as any software loading the nexus files does the translation - JH
            mapNames = xo.getAttribute(MAP_NAMES, true);

            condition = logEvery == 0 ? (TreeLogger.LogUpon) xo.getChild(TreeLogger.LogUpon.class) : null;
        }

        /**
         * @return an object based on the XML element it was passed.
         */
        public TreeLogger parseXMLObject(XMLObject xo) throws XMLParseException {
            parseXMLParameters(xo);

            TreeLogger logger = new TreeLogger(tree, branchRates,
                    treeAttributeProviders, treeTraitProviders,
                    formatter, logEvery, nexusFormat, sortTranslationTable, mapNames, format, condition/*,
                normaliseMeanRateTo*/);

            if (title != null) {
                logger.setTitle(title);
            }

            logger.addFile(XMLParser.getLogFile(xo, FILE_NAME));

            return logger;
        }

        protected Tree tree;
        protected String title;
        protected boolean nexusFormat;
        protected boolean sortTranslationTable;
        protected BranchRates branchRates;
        protected NumberFormat format = null;
        protected TreeLogger.LogUpon condition;
        protected boolean mapNames;
        protected LogFormatter formatter;
        protected TreeAttributeProvider[] treeAttributeProviders;
        protected TreeTraitProvider[] treeTraitProviders;
        protected int logEvery;

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************
        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newIntegerRule(LOG_EVERY, true),
                AttributeRule.newBooleanRule(ALLOW_OVERWRITE_LOG, true),
                new StringAttributeRule(FILE_NAME,
                        "The name of the file to send log output to. " +
                                "If no file name is specified then log is sent to standard output", true),
                new StringAttributeRule(TITLE, "The title of the log", true),
                AttributeRule.newBooleanRule(NEXUS_FORMAT, true,
                        "Whether to use the NEXUS format for the tree log"),
                AttributeRule.newBooleanRule(SORT_TRANSLATION_TABLE, true,
                        "Whether the translation table is sorted."),
            /*AttributeRule.newDoubleRule(NORMALISE_MEAN_RATE_TO, true,
                    "Value to normalise the mean rate to."),*/
                new StringAttributeRule(BRANCH_LENGTHS, "What units should the branch lengths be in",
                        new String[]{TIME, SUBSTITUTIONS}, true),
                AttributeRule.newStringRule(FILTER_TRAITS, true),
                AttributeRule.newBooleanRule(MAP_NAMES, true),
                AttributeRule.newIntegerRule(DECIMAL_PLACES, true),

                new ElementRule(Tree.class, "The tree which is to be logged"),
//            new ElementRule(BranchRates.class, true),
//            new ElementRule(TreeColouringProvider.class, true),
                new ElementRule(TREE_TRAIT,
                        new XMLSyntaxRule[] {
                                AttributeRule.newStringRule(NAME, false, "The name of the trait"),
                                AttributeRule.newStringRule(TAG, true, "The label of the trait to be used in the tree"),
                                new ElementRule(TreeAttributeProvider.class, "The trait provider")
                        }, 0, Integer.MAX_VALUE),
                new ElementRule(Likelihood.class, true),
                new ElementRule(Loggable.class, 0, Integer.MAX_VALUE),
                new ElementRule(TreeAttributeProvider.class, 0, Integer.MAX_VALUE),
                new ElementRule(TreeTraitProvider.class, 0, Integer.MAX_VALUE),
                new ElementRule(TreeLogger.LogUpon.class, true)
        };

        public String getParserDescription() {
            return "Logs a tree to a file";
        }

        public String getExample() {
            final String name = getParserName();
            return
                    "<!-- The " + name + " element takes a treeModel to be logged -->\n" +
                            "<" + name + " " + LOG_EVERY + "=\"100\" " + FILE_NAME + "=\"log.trees\" "
                            + NEXUS_FORMAT + "=\"true\">\n" +
                            "	<treeModel idref=\"treeModel1\"/>\n" +
                            "</" + name + ">\n";
        }

        public Class getReturnType() {
            return TreeLogger.class;
        }
    };

    private static class WrappedTreeTrait implements TreeTrait {

        private final String tag;
        private final TreeTrait trait;

        public WrappedTreeTrait(final String tag, final TreeTrait trait) {
            this.tag = tag;
            this.trait = trait;
        }

        public String getTraitName() {
            return tag;
        }

        public Intent getIntent() {
            return trait.getIntent();
        }

        public Class getTraitClass() {
            return trait.getTraitClass();
        }

        public Object getTrait(Tree tree, NodeRef node) {
            return trait.getTrait(tree, node);
        }

        public String getTraitString(Tree tree, NodeRef node) {
            return trait.getTraitString(tree, node);
        }

        public boolean getLoggable() {
            return trait.getLoggable();
        }

    }

    private static class LikelihoodTreeAttributeProvider implements TreeAttributeProvider {

        private final Likelihood likelihood;

        public LikelihoodTreeAttributeProvider(final Likelihood likelihood) {
            this.likelihood = likelihood;
        }

        public String[] getTreeAttributeLabel() {
            return new String[] {"lnP"};
        }

        public String[] getAttributeForTree(Tree tree) {
            return new String[] {Double.toString(likelihood.getLogLikelihood())};
        }

    }

    private static class LoggableTreeAttributeProvider implements TreeAttributeProvider {

        private final Loggable loggable;

        public LoggableTreeAttributeProvider(final Loggable loggable) {
            this.loggable = loggable;
        }

        public String[] getTreeAttributeLabel() {
            String[] labels = new String[loggable.getColumns().length];
            for (int i = 0; i < loggable.getColumns().length; i++) {
                labels[i] = loggable.getColumns()[i].getLabel();
            }
            return labels;
        }

        public String[] getAttributeForTree(Tree tree) {
            String[] values = new String[loggable.getColumns().length];
            for (int i = 0; i < loggable.getColumns().length; i++) {
                values[i] = loggable.getColumns()[i].getFormatted();
            }
            return values;
        }

    }

}