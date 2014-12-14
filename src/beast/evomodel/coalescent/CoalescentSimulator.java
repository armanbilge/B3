/*
 * CoalescentSimulator.java
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

package beast.evomodel.coalescent;

import beast.evolution.tree.MutableTree;
import beast.evolution.tree.NodeRef;
import beast.evolution.tree.SimpleNode;
import beast.evolution.tree.SimpleTree;
import beast.evolution.tree.Tree;
import beast.evolution.util.Taxa;
import beast.evolution.util.TaxonList;
import beast.inference.distribution.ParametricDistributionModel;
import beast.math.UnivariateFunction;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates a set of coalescent intervals given a demographic model.
 *
 * @author Alexei Drummond
 * @version $Id: CoalescentSimulator.java,v 1.43 2005/10/27 10:40:48 rambaut Exp $
 */
public class CoalescentSimulator {

    beast.evolution.coalescent.CoalescentSimulator simulator = new beast.evolution.coalescent.CoalescentSimulator();
    /**
     * Simulates a coalescent tree from a set of subtrees.
     */
    public CoalescentSimulator() {
    }

    /**
     * Simulates a coalescent tree from a set of subtrees.
     *
     * @param subtrees                an array of tree to be used as subtrees
     * @param model                   the demographic model to use
     * @param rootHeight              an optional root height with which to scale the whole tree
     * @param preserveSubtreesHeights true if heights of subtrees should be preserved
     * @return a simulated coalescent tree
     */
    public SimpleTree simulateTree(Tree[] subtrees, DemographicModel model, double rootHeight,
                                   boolean preserveSubtreesHeights) {

        SimpleNode[] roots = new SimpleNode[subtrees.length];
        SimpleTree tree;

        for (int i = 0; i < roots.length; i++) {
            roots[i] = new SimpleNode(subtrees[i], subtrees[i].getRoot());
        }

        // if just one taxonList then finished
        if (roots.length == 1) {
            tree = new SimpleTree(roots[0]);
        } else {
            tree = new SimpleTree(simulator.simulateCoalescent(roots, model.getDemographicFunction()));
        }

        if (!Double.isNaN(rootHeight) && rootHeight > 0.0) {
            if (preserveSubtreesHeights) {
                limitNodes(tree, rootHeight - 1e-12);
                tree.setRootHeight(rootHeight); 
            } else {
                attemptToScaleTree(tree, rootHeight);
            }
        }

        return tree;
    }


    /**
     * Simulates a coalescent tree, given a taxon list.
     *
     * @param taxa  the set of taxa to simulate a coalescent tree between
     * @param model the demographic model to use
     * @return a simulated coalescent tree
     */
    public SimpleTree simulateTree(TaxonList taxa, DemographicModel model) {
        return simulator.simulateTree(taxa, model.getDemographicFunction());
    }

    public void attemptToScaleTree(MutableTree tree, double rootHeight) {
        // avoid empty tree
        if (tree.getRoot() == null) return;

        double scale = rootHeight / tree.getNodeHeight(tree.getRoot());
        for (int i = 0; i < tree.getInternalNodeCount(); i++) {
            NodeRef n = tree.getInternalNode(i);
            tree.setNodeHeight(n, tree.getNodeHeight(n) * scale);
        }
        MutableTree.Utils.correctHeightsForTips(tree);
    }

    public int sizeOfIntersection(TaxonList tl1, TaxonList tl2) {
        int nIn = 0;
        for (int j = 0; j < tl1.getTaxonCount(); ++j) {
            if (tl2.getTaxonIndex(tl1.getTaxon(j)) >= 0) {
                ++nIn;
            }
        }
        return nIn;
    }

    public boolean contained(TaxonList taxons, TaxonList taxons1) {
        return sizeOfIntersection(taxons, taxons1) == taxons.getTaxonCount();
    }

    /**
     * Clip nodes height above limit.
     *
     * @param tree  to clip
     * @param limit height limit
     */
    private void limitNodes(MutableTree tree, double limit) {
        for (int i = 0; i < tree.getInternalNodeCount(); i++) {
            final NodeRef n = tree.getInternalNode(i);
            if (tree.getNodeHeight(n) > limit) {
                tree.setNodeHeight(n, limit);
            }
        }
        MutableTree.Utils.correctHeightsForTips(tree);
    }

    public static class TaxaConstraint {
        public final TaxonList taxons;
        public final double lower;
        public final boolean isMonophyletic;
        public double upper;

        public TaxaConstraint(TaxonList taxons, ParametricDistributionModel p, boolean isMono) {
            this.taxons = taxons;
            this.isMonophyletic = isMono;

            if (p != null) {
                final UnivariateFunction univariateFunction = p.getProbabilityDensityFunction();
                lower = univariateFunction.getLowerBound();
                upper = univariateFunction.getUpperBound();
            } else {
                lower = 0;
                upper = Double.POSITIVE_INFINITY;
            }
        }

        public TaxaConstraint(TaxonList taxons, double low, double high, boolean isMono) {
            this.taxons = taxons;
            this.isMonophyletic = isMono;
            upper = high;
            lower = low;
        }

        public boolean realLimits() {
            return lower != 0 || upper != Double.POSITIVE_INFINITY;
        }
    }

    public static final XMLObjectParser<Tree> PARSER = new AbstractXMLObjectParser<Tree>() {
        public static final String COALESCENT_SIMULATOR = "coalescentSimulator";
        public static final String HEIGHT = "height";

        public String getParserName() {
            return COALESCENT_SIMULATOR;
        }

        public String[] getParserNames() {
            return new String[]{COALESCENT_SIMULATOR, "coalescentTree"};
        }

        public Tree parseXMLObject(XMLObject xo) throws XMLParseException {

            CoalescentSimulator simulator = new CoalescentSimulator();

            DemographicModel demoModel = (DemographicModel) xo.getChild(DemographicModel.class);
            List<TaxonList> taxonLists = new ArrayList<TaxonList>();
            List<Tree> subtrees = new ArrayList<Tree>();

            double height = xo.getAttribute(HEIGHT, Double.NaN);

            // should have one child that is node
            for (int i = 0; i < xo.getChildCount(); i++) {
                final Object child = xo.getChild(i);

                // AER - swapped the order of these round because Trees are TaxonLists...
                if (child instanceof Tree) {
                    subtrees.add((Tree) child);
                } else if (child instanceof TaxonList) {
                    taxonLists.add((TaxonList) child);
                }
            }

            if (taxonLists.size() == 0) {
                if (subtrees.size() == 1) {
                    return subtrees.get(0);
                }
                throw new XMLParseException("Expected at least one taxonList or two subtrees in "
                        + getParserName() + " element.");
            }

            Taxa remainingTaxa = new Taxa();
            for (int i = 0; i < taxonLists.size(); i++) {
                remainingTaxa.addTaxa(taxonLists.get(i));
            }

            for (int i = 0; i < subtrees.size(); i++) {
                remainingTaxa.removeTaxa(subtrees.get(i));
            }

            try {
                Tree[] trees = new Tree[subtrees.size() + remainingTaxa.getTaxonCount()];
                // add the preset trees
                for (int i = 0; i < subtrees.size(); i++) {
                    trees[i] = subtrees.get(i);
                }

                // add all the remaining taxa in as single tip trees...
                for (int i = 0; i < remainingTaxa.getTaxonCount(); i++) {
                    Taxa tip = new Taxa();
                    tip.addTaxon(remainingTaxa.getTaxon(i));
                    trees[i + subtrees.size()] = simulator.simulateTree(tip, demoModel);
                }

                return simulator.simulateTree(trees, demoModel, height, trees.length != 1);

            } catch (IllegalArgumentException iae) {
                throw new XMLParseException(iae.getMessage());
            }
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element returns a simulated tree under the given demographic model. The element can " +
                    "be nested to simulate with monophyletic clades. The tree will be rescaled to the given height.";
        }

        public Class getReturnType() {
            return Tree.class; //Object.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(HEIGHT, true, ""),
                new ElementRule(Tree.class, 0, Integer.MAX_VALUE),
                new ElementRule(TaxonList.class, 0, Integer.MAX_VALUE),
                new ElementRule(DemographicModel.class, 0, Integer.MAX_VALUE),
        };
    };

}
