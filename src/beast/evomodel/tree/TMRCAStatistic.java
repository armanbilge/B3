/*
 * TMRCAStatistic.java
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

package beast.evomodel.tree;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evolution.util.Taxa;
import beast.evolution.util.TaxonList;
import beast.inference.model.Statistic;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.OrRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.Set;

/**
 * A statistic that tracks the time of MRCA of a set of taxa
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: TMRCAStatistic.java,v 1.21 2005/07/11 14:06:25 rambaut Exp $
 */
public class TMRCAStatistic extends Statistic.Abstract implements TreeStatistic {

    public TMRCAStatistic(String name, Tree tree, TaxonList taxa, boolean isRate, boolean forParent)
            throws Tree.MissingTaxonException {
        super(name);
        this.tree = tree;
        this.leafSet = Tree.Utils.getLeavesForTaxa(tree, taxa);
        this.isRate = isRate;
        this.forParent = forParent;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public Tree getTree() {
        return tree;
    }

    public int getDimension() {
        return 1;
    }

    /**
     * @return the height of the MRCA node.
     */
    public double getStatisticValue(int dim) {

        NodeRef node = Tree.Utils.getCommonAncestorNode(tree, leafSet);
        if (forParent && !tree.isRoot(node))
            node = tree.getParent(node);       
        if (node == null) throw new RuntimeException("No node found that is MRCA of " + leafSet);
        if (isRate) {
            return tree.getNodeRate(node);
        }
        return tree.getNodeHeight(node);
    }

    private Tree tree = null;
    private Set<String> leafSet = null;
    private final boolean isRate;
    private final boolean forParent;

    public static final XMLObjectParser<TMRCAStatistic> PARSER = new AbstractXMLObjectParser<TMRCAStatistic>() {

        public static final String TMRCA_STATISTIC = "tmrcaStatistic";
        public static final String MRCA = "mrca";
        // The tmrcaStatistic will represent that age of the parent node of the MRCA, rather than the MRCA itself
        public static final String PARENT = "forParent";
        public static final String STEM = "includeStem";


        public String getParserName() {
            return TMRCA_STATISTIC;
        }

        public TMRCAStatistic parseXMLObject(XMLObject xo) throws XMLParseException {

            String name = xo.getAttribute(Statistic.NAME, xo.getId());
            Tree tree = (Tree) xo.getChild(Tree.class);
            TaxonList taxa = (TaxonList) xo.getElementFirstChild(MRCA);
            boolean isRate = xo.getAttribute("rate", false);
            boolean includeStem = false;
            if (xo.hasAttribute(PARENT) && xo.hasAttribute(STEM)) {
                throw new XMLParseException("Please use either " + PARENT + " or " + STEM + "!");
            } else if (xo.hasAttribute(PARENT)) {
                includeStem = xo.getBooleanAttribute(PARENT);
            } else if (xo.hasAttribute(STEM)) {
                includeStem = xo.getBooleanAttribute(STEM);
            }

            try {
                return new TMRCAStatistic(name, tree, taxa, isRate, includeStem);
            } catch (Tree.MissingTaxonException mte) {
                throw new XMLParseException(
                        "Taxon, " + mte + ", in " + getParserName() + "was not found in the tree.");
            }
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A statistic that has as its value the height of the most recent common ancestor " +
                    "of a set of taxa in a given tree";
        }

        public Class getReturnType() {
            return TMRCAStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(Tree.class),
                new StringAttributeRule("name",
                        "A name for this statistic primarily for the purposes of logging", true),
                AttributeRule.newBooleanRule("rate", true),
                new ElementRule(MRCA,
                        new XMLSyntaxRule[]{new ElementRule(Taxa.class)}),
                new OrRule(
                        new XMLSyntaxRule[]{
                                AttributeRule.newBooleanRule(PARENT, true),
                                AttributeRule.newBooleanRule(STEM, true)
                        }
                )
        };

    };

}
