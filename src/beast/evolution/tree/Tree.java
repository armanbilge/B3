/*
 * Tree.java
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

import beast.evolution.util.Taxon;
import beast.evolution.util.TaxonList;
import beast.evolution.util.Units;
import beast.util.Attributable;
import beast.util.Identifiable;
import jebl.evolution.graphs.Node;
import jebl.evolution.trees.SimpleRootedTree;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Interface for a phylogenetic or genealogical tree.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: Tree.java,v 1.59 2006/09/08 17:34:23 rambaut Exp $
 */
public interface Tree extends TaxonList, Units, Identifiable, Attributable {

    public enum BranchLengthType {
        NO_BRANCH_LENGTHS, LENGTHS_AS_TIME, LENGTHS_AS_SUBSTITUTIONS
    }

    /**
     * @return root node of this tree.
     */
    NodeRef getRoot();

    /**
     * @return a count of the number of nodes (internal + external) in this
     *         tree, currently connected from the root node.
     */
    int getNodeCount();

    /**
     * @param i node index, terminal nodes are first
     * @return the ith node.
     */
    NodeRef getNode(int i);

    /**
     * @param i index of an internal node
     * @return the ith internal node.
     */
    NodeRef getInternalNode(int i);

    /**
     * @param i the index of an external node
     * @return the ith external node.
     */
    NodeRef getExternalNode(int i);

    /**
     * @return a count of the number of external nodes (tips) in this
     *         tree, currently connected from the root node.
     */
    int getExternalNodeCount();

    /**
     * @return a count of the number of internal nodes in this
     *         tree, currently connected from the root node.
     */
    int getInternalNodeCount();

    /**
     * @param node the node to retrieve the taxon of
     * @return the taxon of this node.
     */
    Taxon getNodeTaxon(NodeRef node);

    /**
     * @return whether this tree has known node heights.
     */
    boolean hasNodeHeights();

    /**
     * @param node the node to retrieve height of
     * @return the height of node in the tree.
     */
    double getNodeHeight(NodeRef node);

    /**
     * @return whether this tree has known branch lengths.
     */
    boolean hasBranchLengths();

    /**
     * @param node the node to retrieve the length of branch to parent
     * @return the length of the branch from node to its parent.
     */
    double getBranchLength(NodeRef node);

    /**
     * @param node the node to retrieve the rate of
     * @return the rate of node in the tree.
     */
    double getNodeRate(NodeRef node);

    /**
     * @param node the node whose attribute is being fetched.
     * @param name the name of the attribute of interest.
     * @return an object representing the named attributed for the given node.
     */
    Object getNodeAttribute(NodeRef node, String name);

    /**
     * @param node the node whose attribute is being fetched.
     * @return an interator of attribute names available for this node.
     */
    Iterator getNodeAttributeNames(NodeRef node);

    /**
     * @param node the node to test if external
     * @return whether the node is external.
     */
    boolean isExternal(NodeRef node);

    /**
     * @param node the node to test if root
     * @return whether the node is the root.
     */
    boolean isRoot(NodeRef node);

    /**
     * @param node the node to get child count of
     * @return the number of children of node.
     */
    int getChildCount(NodeRef node);

    /**
     * @param node the node to get jth child of
     * @param j    the index of child to retrieve
     * @return the jth child of node
     */
    NodeRef getChild(NodeRef node, int j);

    NodeRef getParent(NodeRef node);

    /**
     * @return a clone of this tree
     */
    public Tree getCopy();

    //
    // Utility functions
    //

    /**
     * Count number of leaves in subtree whose root is node.
     *
     * @param node the node to get leaf count below
     * @return the number of leaves under this node.
     */
    default int getLeafCount(NodeRef node) {

        int childCount = getChildCount(node);
        if (childCount == 0) return 1;

        int leafCount = 0;
        for (int i = 0; i < childCount; i++) {
            leafCount += getLeafCount(getChild(node, i));
        }
        return leafCount;
    }

    default double getTreeLength(NodeRef node) {

        int childCount = getChildCount(node);
        if (childCount == 0) return getBranchLength(node);

        double length = 0;
        for (int i = 0; i < childCount; i++) {
            length += getTreeLength(getChild(node, i));
        }
        if (node != getRoot())
            length += getBranchLength(node);
        return length;

    }

    default double getMinNodeHeight(NodeRef node) {

        int childCount = getChildCount(node);
        if (childCount == 0) return getNodeHeight(node);

        double minNodeHeight = Double.MAX_VALUE;
        for (int i = 0; i < childCount; i++) {
            double height = getMinNodeHeight(getChild(node, i));
            if (height < minNodeHeight) {
                minNodeHeight = height;
            }
        }
        return minNodeHeight;
    }

    /**
     * @return true only if all tips have height 0.0
     */
    default boolean isUltrametric() {
        for (int i = 0; i < getExternalNodeCount(); i++) {
            if (getNodeHeight(getExternalNode(i)) != 0.0)
                return false;
        }
        return true;
    }

    /**
     * @return true only if internal nodes have 2 children
     */
    default boolean isBinary() {
        for (int i = 0; i < getInternalNodeCount(); i++) {
            if (getChildCount(getInternalNode(i)) > 2)
                return false;
        }
        return true;
    }

    /**
     * @return a set of strings which are the taxa of the tree.
     */
    default Set<String> getLeafSet() {

        HashSet<String> leafSet = new HashSet<String>();
        int m = getTaxonCount();

        for (int i = 0; i < m; i++) {

            Taxon taxon = getTaxon(i);
            leafSet.add(taxon.getId());
        }

        return leafSet;
    }

    /**
     * @param taxa the taxa
     * @return Set of taxon names (id's) associated with the taxa in taxa.
     * @throws beast.evolution.tree.Tree.MissingTaxonException
     *          if a taxon in taxa is not contained in the tree
     */
    default Set<String> getLeavesForTaxa(TaxonList taxa) throws MissingTaxonException {

        HashSet<String> leafNodes = new HashSet<String>();
        int m = taxa.getTaxonCount();
        int n = getExternalNodeCount();

        for (int i = 0; i < m; i++) {

            Taxon taxon = taxa.getTaxon(i);
            boolean found = false;
            for (int j = 0; j < n; j++) {

                NodeRef node = getExternalNode(j);
                if (getNodeTaxon(node).getId().equals(taxon.getId())) {

                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new MissingTaxonException(taxon);
            }

            leafNodes.add(taxon.getId());
        }

        return leafNodes;
    }

    /**
     * @param node the node to get names of leaves below
     * @return a set of taxa names (as strings) of the leaf nodes descended from the given node.
     */
    default Set<String> getDescendantLeaves(NodeRef node) {

        HashSet<String> set = new HashSet<String>();
        getDescendantLeaves(node, set);
        return set;
    }

    /**
     * @param node the node to get name of leaves below
     * @param set  will be populated with taxa names (as strings) of the leaf nodes descended from the given node.
     */
    default void getDescendantLeaves(NodeRef node, Set<String> set) {

        if (isExternal(node)) {
            set.add(getTaxonId(node.getNumber()));
        } else {

            for (int i = 0; i < getChildCount(node); i++) {

                NodeRef node1 = getChild(node, i);

                getDescendantLeaves(node1, set);
            }
        }
    }


    /**
     * @param node the node to get external nodes below
     * @return a set of noderefs of the leaf nodes descended from the given node.
     */
    default Set<NodeRef> getExternalNodes(NodeRef node) {

        HashSet<NodeRef> set = new HashSet<NodeRef>();
        getExternalNodes(node, set);
        return set;
    }

    /**
     * @param node the node to get external nodes below
     * @param set  is populated with noderefs of the leaf nodes descended from the given node.
     */
    default void getExternalNodes(NodeRef node, Set<NodeRef> set) {

        if (isExternal(node)) {
            set.add(node);
        } else {

            for (int i = 0; i < getChildCount(node); i++) {

                NodeRef node1 = getChild(node, i);

                getExternalNodes(node1, set);
            }
        }
    }

    /**
     * Gets the most recent common ancestor (MRCA) node of a set of leaf nodes.
     *
     * @param leafNodes a set of names
     * @return the NodeRef of the MRCA
     */
     default NodeRef getCommonAncestorNode(Set<String> leafNodes) {

        int cardinality = leafNodes.size();

        if (cardinality == 0) {
            throw new IllegalArgumentException("No leaf nodes selected");
        }

        NodeRef[] mrca = {null};
        getCommonAncestorNode(getRoot(), leafNodes, cardinality, mrca);

        return mrca[0];
    }

    /*
     * Private recursive function used by getCommonAncestorNode.
     */
    default int getCommonAncestorNode(NodeRef node,
                                             Set<String> leafNodes, int cardinality,
                                             NodeRef[] mrca) {

        if (isExternal(node)) {

            if (leafNodes.contains(getTaxonId(node.getNumber()))) {
                if (cardinality == 1) {
                    mrca[0] = node;
                }
                return 1;
            } else {
                return 0;
            }
        }

        int matches = 0;

        for (int i = 0; i < getChildCount(node); i++) {

            NodeRef node1 = getChild(node, i);

            matches += getCommonAncestorNode(node1, leafNodes, cardinality, mrca);

            if (mrca[0] != null) {
                break;
            }
        }

        if (mrca[0] == null) {
            // If we haven't already found the MRCA, test this node
            if (matches == cardinality) {
                mrca[0] = node;
            }
        }

        return matches;
    }

    /**
     * @param taxa the taxa
     * @return A bitset with the node numbers set.
     * @throws beast.evolution.tree.Tree.MissingTaxonException
     *          if a taxon in taxa is not contained in the tree
     */
    default BitSet getTipsBitSetForTaxa(TaxonList taxa) throws Tree.MissingTaxonException {

        BitSet tips = new BitSet();
        for (int n: getTipsForTaxa(taxa)) {
            tips.set(n);
        }
        return tips;
    }

    /**
     * @param taxa the taxa
     * @return A HashSet of node numbers.
     * @throws beast.evolution.tree.Tree.MissingTaxonException
     *          if a taxon in taxa is not contained in the tree
     */
    default Set<Integer> getTipsForTaxa(TaxonList taxa) throws Tree.MissingTaxonException {

        Set<Integer> tips = new LinkedHashSet<Integer>();

        for (int i = 0; i < taxa.getTaxonCount(); i++) {

            Taxon taxon = taxa.getTaxon(i);
            boolean found = false;
            for (int j = 0; j < getExternalNodeCount(); j++) {

                NodeRef node = getExternalNode(j);
                if (getNodeTaxon(node).getId().equals(taxon.getId())) {
                    tips.add(node.getNumber());
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new Tree.MissingTaxonException(taxon);
            }
        }

        return tips;
    }

    public class MissingTaxonException extends Exception {
        /**
         *
         */
        private static final long serialVersionUID = 8468656622238269963L;

        public MissingTaxonException(Taxon taxon) {
            super(taxon.getId());
        }
    }

    default boolean isMonophyletic(Set<String> leafNodes) {
        return isMonophyletic(leafNodes, Collections.<String>emptySet());
    }

    /**
     * Performs a monophyly test on a set of leaf nodes. The nodes are monophyletic
     * if there is a node in the tree which subtends all the taxa in the set (and
     * only those taxa).
     *
     * @param leafNodes a set of leaf node ids
     * @param ignore    a set of ids to ignore in monophyly assessment
     * @return boolean is monophyletic?
     */
    default boolean isMonophyletic(Set<String> leafNodes, Set<String> ignore) {

        int cardinality = leafNodes.size();

        if (cardinality == 1) {
            // A single selected leaf is always monophyletic
            return true;
        }

        if (cardinality == getExternalNodeCount()) {
            // All leaf nodes are selected
            return true;
        }

        if (cardinality == 0) {
            throw new IllegalArgumentException("No leaf nodes selected");
        }

        int[] matchCount = {0};
        int[] leafCount = {0};
        boolean[] isMono = {false};
        isMonophyletic(getRoot(), leafNodes, ignore, cardinality, matchCount, leafCount, isMono);

        return isMono[0];
    }

    /**
     * Private recursive function used by isMonophyletic.
     *
     * @param node        the node that is currently being assessed in recursive procedure
     * @param leafNodes   a set of leaf node ids
     * @param ignore      a set of ids to ignore in monophyly assessment
     * @param cardinality the size of leafNodes set
     * @return boolean is monophyletic?
     */
    default boolean isMonophyletic(NodeRef node,
                                          Set<String> leafNodes, Set<String> ignore,
                                          int cardinality,
                                          int[] matchCount, int[] leafCount,
                                          boolean[] isMono) {

        if (isExternal(node)) {

            String id = getNodeTaxon(node).getId();
            if (leafNodes.contains(id)) {
                matchCount[0] = 1;
            } else {
                matchCount[0] = 0;
            }
            if (!ignore.contains(id)) {
                leafCount[0] = 1;
            } else {
                leafCount[0] = 0;
            }
            return false;
        }

        int mc = 0;
        int lc = 0;

        for (int i = 0; i < getChildCount(node); i++) {

            NodeRef node1 = getChild(node, i);

            boolean done = isMonophyletic(node1, leafNodes, ignore, cardinality, matchCount, leafCount, isMono);
            mc += matchCount[0];
            lc += leafCount[0];

            if (done) {
                return true;
            }
        }

        matchCount[0] = mc;
        leafCount[0] = lc;

        // If we haven't already found the MRCA, test this node
        if (mc == lc && lc == cardinality) {
            isMono[0] = true;
            return true;
        }

        return false;
    }

    default NodeRef getCommonAncestor(NodeRef n1, NodeRef n2) {
        while( n1 != n2 ) {
            if( getNodeHeight(n1) < getNodeHeight(n2) ) {
                n1 = getParent(n1);
            } else {
                n2 = getParent(n2);
            }
        }
        return n1;
    }

    // A lightweight version for finding the most recent common ancestor of a group of taxa.
    // return the node-ref of the MRCA.

    // would be nice to use nodeRef's, but they are not preserved :(
    default NodeRef getCommonAncestor(int[] nodes) {
        NodeRef cur = getNode(nodes[0]);

        for(int k = 1; k < nodes.length; ++k) {
            cur = getCommonAncestor(cur, getNode(nodes[k]));
        }
        return cur;
    }

    /**
     * @param range
     * @return the size of the largest clade with tips in the given range of times.
     */
    default int largestClade(double range) {

        return largestClade(getRoot(), range, new double[]{0.0, 0.0});

    }

    /**
     * @return the size of the largest clade with tips in the given range of times.
     */
    default int largestClade(NodeRef node, double range, double[] currentBounds) {

        if (isExternal(node)) {
            currentBounds[0] = getNodeHeight(node);
            currentBounds[1] = getNodeHeight(node);
            return 1;
        } else {
            // get the bounds and max clade size of the left clade
            int cladeSize1 = largestClade(getChild(node, 0), range, currentBounds);
            double min = currentBounds[0];
            double max = currentBounds[1];

            // get the bounds and max clade size of the right clade
            int cladeSize2 = largestClade(getChild(node, 1), range, currentBounds);
            min = Math.min(min, currentBounds[0]);
            max = Math.max(max, currentBounds[1]);

            // update the joint bounds
            currentBounds[0] = min;
            currentBounds[1] = max;

            // if the joint clade is valid return the joint size
            if (max - min < range) {
                return cladeSize1 + cladeSize2;
            }
            // if the joint clade is not valid return the max of the two
            return Math.max(cladeSize1, cladeSize2);
        }
    }

    /**
     * Calculates the minimum number of steps for the parsimony reconstruction of a
     * binary character defined by leafStates.
     *
     * @param leafStates a set of booleans, one for each leaf node
     * @return number of parsimony steps
     */
    default int getParsimonySteps(Set leafStates) {

        int[] score = {0};
        getParsimonySteps(getRoot(), leafStates, score);
        return score[0];
    }

    default int getParsimonySteps(NodeRef node, Set leafStates, int[] score) {

        if (isExternal(node)) {
            return (leafStates.contains(getTaxonId(node.getNumber())) ? 1 : 2);

        } else {

            int uState = getParsimonySteps(getChild(node, 0), leafStates, score);
            int iState = uState;

            for (int i = 1; i < getChildCount(node); i++) {

                int state = getParsimonySteps(getChild(node, i), leafStates, score);
                uState = state | uState;

                iState = state & iState;

            }

            if (iState == 0) {
                score[0] += 1;
            }

            return uState;
        }

    }

    /**
     * Calculates the parsimony reconstruction of a binary character defined
     * by leafStates at a given node.
     *
     * @param node       a NodeRef object from tree
     * @param leafStates a set of booleans, one for each leaf node
     * @return number of parsimony steps
     */
    default double getParsimonyState(NodeRef node, Set leafStates) {

        int state = getParsimonyStateAtNode(node, leafStates);
        switch (state) {
            case 1:
                return 0.0;
            case 2:
                return 1.0;
            default:
                return 0.5;
        }
    }

    default int getParsimonyStateAtNode(NodeRef node, Set leafStates) {

        if (isExternal(node)) {
            return (leafStates.contains(getTaxonId(node.getNumber())) ? 1 : 2);

        } else {

            int uState = getParsimonyStateAtNode(getChild(node, 0), leafStates);
            int iState = uState;

            for (int i = 1; i < getChildCount(node); i++) {

                int state = getParsimonyStateAtNode(getChild(node, i), leafStates);
                uState = state | uState;

                iState = state & iState;

            }

            return uState;
        }

    }

    /**
     * determine preorder successor of this node
     *
     * @return next node
     */
    default NodeRef preorderSuccessor(NodeRef node) {

        NodeRef next = null;

        if (isExternal(node)) {
            NodeRef cn = node, ln = null; // Current and last node

            // Go up
            do {
                if (isRoot(cn)) {
                    next = cn;
                    break;
                }
                ln = cn;
                cn = getParent(cn);
            }
            while (getChild(cn, getChildCount(cn) - 1) == ln);

            // Determine next node
            if (next == null) {
                // Go down one node
                for (int i = 0; i < getChildCount(cn) - 1; i++) {

                    if (getChild(cn, i) == ln) {
                        next = getChild(cn, i + 1);
                        break;
                    }
                }
            }
        } else {
            next = getChild(node, 0);
        }

        return next;
    }

    /**
     * determine a postorder traversal list of nodes in a tree
     *
     */
    default void postOrderTraversalList(int[] postOrderList) {

        final int nodeCount = getNodeCount();
        if (postOrderList.length != nodeCount) {
            throw new IllegalArgumentException("Illegal list length");
        }

        int idx = nodeCount - 1;
        int cidx = nodeCount - 1;

        postOrderList[idx] = getRoot().getNumber();

        while (cidx > 0) {
            NodeRef cNode = getNode(postOrderList[idx]);
            for(int i = 0; i < getChildCount(cNode); ++i) {
                cidx -= 1;
                postOrderList[cidx] = getChild(cNode, i).getNumber();
            }
            idx -= 1;
        }
    }

    // populate  postOrderList with node numbers in posorder: parent before children.
    //  postOrderList is pre-allocated with the right size
    default void preOrderTraversalList(int[] postOrderList) {

        final int nodeCount = getNodeCount();
        if (postOrderList.length != nodeCount) {
            throw new IllegalArgumentException("Illegal list length");
        }
        postOrderList[0] = getRoot().getNumber();
        preOrderTraversalList(0, postOrderList);
    }

    default int preOrderTraversalList(int idx, int[] postOrderList) {
        final NodeRef node = getNode(postOrderList[idx]);
        for(int i = 0; i < getChildCount(node); ++i) {
            final NodeRef child = getChild(node, i);
            idx += 1;
            postOrderList[idx] = child.getNumber();
            if( ! isExternal(child) ) {
                idx = preOrderTraversalList(idx, postOrderList);
            }
        }
        return idx;
    }


    /**
     * determine postorder successor of a node
     *
     * @return next node
     */
    default NodeRef postorderSuccessor(NodeRef node) {

        NodeRef cn = null;
        NodeRef parent = getParent(node);

        if (getRoot() == node) {
            cn = node;
        } else {

            // Go up one node
            if (getChild(parent, getChildCount(parent) - 1) == node) {
                return parent;
            }

            // Go down one node
            for (int i = 0; i < getChildCount(parent) - 1; i++) {
                if (getChild(parent, i) == node) {
                    cn = getChild(parent, i + 1);
                    break;
                }
            }
        }

        // Go down until leaf
        while (getChildCount(cn) > 0) {
            cn = getChild(cn, 0);
        }

        return cn;
    }

    /**
     * Gets finds the most ancestral node with attribute set.
     */
    default NodeRef findNodeWithAttribute(String attribute) {

        NodeRef root = getRoot();
        NodeRef node = root;

        do {

            if (getNodeAttribute(node, attribute) != null) {
                return node;
            }

            node = preorderSuccessor(node);

        } while (node != root);

        return null;
    }

    /**
     * Gets finds the most recent date amongst the external nodes.
     */
    default beast.evolution.util.Date findMostRecentDate() {

        beast.evolution.util.Date mostRecent = null;

        for (int i = 0; i < getExternalNodeCount(); i++) {
            Taxon taxon = getNodeTaxon(getExternalNode(i));

            beast.evolution.util.Date date = (beast.evolution.util.Date) taxon.getAttribute(beast.evolution.util.Date.DATE);
            if ((date != null) && (mostRecent == null || date.after(mostRecent))) {
                mostRecent = date;
            }
        }

        return mostRecent;
    }

    /**
     * Recursive function for constructing a newick tree representation in the given buffer.
     */
    default String newick() {
        StringBuffer buffer = new StringBuffer();
        newick(getRoot(), true, BranchLengthType.LENGTHS_AS_TIME, null, null, null, null, buffer);
        buffer.append(";");
        return buffer.toString();
    }

    /**
     * @param dp   the decimal places for branch lengths
     * @return a string representation of the tree in newick format with branch lengths expressed with the given
     *         number of decimal places
     */
    default String newick(int dp) {
        StringBuffer buffer = new StringBuffer();

        // use the English locale to ensure there are no commas in the number!
        NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(dp);

        newick(getRoot(), true, BranchLengthType.LENGTHS_AS_TIME, format, null, null, null, buffer);
        buffer.append(";");
        return buffer.toString();
    }

    default String newick(BranchRates branchRates) {
        StringBuffer buffer = new StringBuffer();
        newick(getRoot(), true, BranchLengthType.LENGTHS_AS_SUBSTITUTIONS, null, branchRates, null, null, buffer);
        buffer.append(";");
        return buffer.toString();
    }

    default String newick(TreeTraitProvider[] treeTraitProviders) {
        StringBuffer buffer = new StringBuffer();
        newick(getRoot(), true, BranchLengthType.LENGTHS_AS_TIME, null, null, treeTraitProviders, null, buffer);
        buffer.append(";");
        return buffer.toString();
    }

    /**
     * Recursive function for constructing a newick tree representation in the given buffer.
     */
    default String newickNoLengths() {
        StringBuffer buffer = new StringBuffer();
        newick(getRoot(), true, BranchLengthType.NO_BRANCH_LENGTHS, null, null, null, null, buffer);
        buffer.append(";");
        return buffer.toString();
    }

    /**
     * Recursive function for constructing a newick tree representation in the given buffer.
     *
     * @param node                     The node [tree.getRoot()]
     * @param labels                   whether labels or numbers should be used
     * @param lengths                  What type of branch lengths: NO_BRANCH_LENGTHS, LENGTHS_AS_TIME, LENGTHS_AS_SUBSTITUTIONS
     * @param branchRates              An optional BranchRates (or null) used to scale branch times into substitutions
     * @param treeTraitProviders       An array of TreeTraitProvider
     * @param format                   formatter for branch lengths
     * @param idMap                    A map if id names to integers that is used to overide node labels when present
     * @param buffer                   The StringBuffer
     */
    default void newick(NodeRef node, boolean labels, BranchLengthType lengths, NumberFormat format,
                              BranchRates branchRates,
                              TreeTraitProvider[] treeTraitProviders,
                              Map<String, Integer> idMap, StringBuffer buffer) {

        NodeRef parent = getParent(node);

        if (isExternal(node)) {
            if (!labels) {
                int k = node.getNumber();
                if (idMap != null) {
                    buffer.append(idMap.get(getTaxonId(k)));
                } else {
                    buffer.append((k + 1));
                }
            } else {
                String label = getTaxonId(node.getNumber());
                if (label.contains(" ") || label.contains(":") || label.contains(";") || label.contains(",")) {
                    buffer.append("\"");
                    buffer.append(label);
                    buffer.append("\"");
                } else {
                    buffer.append(label);
                }
            }
        } else {
            buffer.append("(");
            newick(getChild(node, 0), labels, lengths, format,
                    branchRates,
                    treeTraitProviders, idMap,
                    buffer);
            for (int i = 1; i < getChildCount(node); i++) {
                buffer.append(",");
                newick(getChild(node, i), labels, lengths, format,
                        branchRates,
                        treeTraitProviders, idMap,
                        buffer);
            }
            buffer.append(")");
        }

        writeTreeTraits(buffer, node, treeTraitProviders, TreeTrait.Intent.NODE);

        if (parent != null && lengths != BranchLengthType.NO_BRANCH_LENGTHS) {
            buffer.append(":");
            writeTreeTraits(buffer, node, treeTraitProviders, TreeTrait.Intent.BRANCH);

            if (lengths != BranchLengthType.NO_BRANCH_LENGTHS) {
                double length = getNodeHeight(parent) - getNodeHeight(node);
                if (lengths == BranchLengthType.LENGTHS_AS_SUBSTITUTIONS) {
                    if (branchRates == null) {
                        throw new IllegalArgumentException("No BranchRates provided");
                    }
                    length *= branchRates.getBranchRate(this, node);
                }
                String lengthString;
                if (format != null) {
                    lengthString = format.format(length);
                } else {
                    lengthString = String.valueOf(length);
                }

                buffer.append(lengthString);
            }
        }
    }

    default void writeTreeTraits(StringBuffer buffer, NodeRef node, TreeTraitProvider[] treeTraitProviders, TreeTrait.Intent intent) {
        if (treeTraitProviders != null) {
            boolean hasAttribute = false;
            for (TreeTraitProvider ttp : treeTraitProviders) {
                TreeTrait[] tts = ttp.getTreeTraits();
                for (TreeTrait treeTrait: tts) {
                    if (treeTrait.getLoggable() && treeTrait.getIntent() == intent) {
                        String value = treeTrait.getTraitString(this, node);

                        if (value != null) {
                            if (!hasAttribute) {
                                buffer.append("[&");
                                hasAttribute = true;
                            } else {
                                buffer.append(",");
                            }
                            buffer.append(treeTrait.getTraitName());
                            buffer.append("=");
                            buffer.append(value);

//                                if (values.length > 1) {
//                                    buffer.append("{");
//                                    buffer.append(values[0]);
//                                    for (int i = 1; i < values.length; i++) {
//                                        buffer.append(",");
//                                        buffer.append(values[i]);
//                                    }
//                                    buffer.append("}");
//                                } else {
//                                    buffer.append(values[0]);
//                                }
                        }
                    }

                }
            }
            if (hasAttribute) {
                buffer.append("]");
            }
        }
    }
    /**
     * Recursive function for constructing a newick tree representation in the given buffer.
     */
    default String uniqueNewick(NodeRef node) {
        if (isExternal(node)) {
            //buffer.append(tree.getNodeTaxon(node).getId());
            return getNodeTaxon(node).getId();
        } else {
            StringBuffer buffer = new StringBuffer("(");

            ArrayList<String> subtrees = new ArrayList<String>();
            for (int i = 0; i < getChildCount(node); i++) {
                NodeRef child = getChild(node, i);
                subtrees.add(uniqueNewick(child));
            }
            Collections.sort(subtrees);
            for (int i = 0; i < subtrees.size(); i++) {
                buffer.append(subtrees.get(i));
                if (i < subtrees.size() - 1) {
                    buffer.append(",");
                }
            }
            buffer.append(")");

            return buffer.toString();
        }
    }

    /**
     * Recursive function for constructing a newick tree representation in the given buffer.
     */
    default Tree rotateByName() {

        return new SimpleTree(rotateNodeByName(getRoot()));
    }

    /**
     * Recursive function for constructing a newick tree representation in the given buffer.
     */
    default SimpleNode rotateNodeByName(NodeRef node) {

        if (isExternal(node)) {
            return new SimpleNode(this, node);
        } else {

            SimpleNode parent = new SimpleNode(this, node);

            NodeRef child1 = getChild(node, 0);
            NodeRef child2 = getChild(node, 1);

            String subtree1 = uniqueNewick(child1);
            String subtree2 = uniqueNewick(child2);

            if (subtree1.compareTo(subtree2) > 0) {
                parent.addChild(rotateNodeByName(child2));
                parent.addChild(rotateNodeByName(child1));
            } else {
                parent.addChild(rotateNodeByName(child1));
                parent.addChild(rotateNodeByName(child2));
            }
            return parent;
        }
    }

    default MutableTree rotateTreeByComparator(Comparator<NodeRef> comparator) {

        return new SimpleTree(rotateTreeByComparator(getRoot(), comparator));
    }

    /**
     * Recursive function for constructing a newick tree representation in the given buffer.
     */
    default SimpleNode rotateTreeByComparator(NodeRef node, Comparator<NodeRef> comparator) {

        SimpleNode newNode = new SimpleNode();
        newNode.setHeight(getNodeHeight(node));
        newNode.setRate(getNodeRate(node));
        newNode.setId(getTaxonId(node.getNumber()));
        newNode.setNumber(node.getNumber());
        newNode.setTaxon(getNodeTaxon(node));

        if (!isExternal(node)) {

            NodeRef child1 = getChild(node, 0);
            NodeRef child2 = getChild(node, 1);

            if (comparator.compare(child1, child2) > 0) {
                newNode.addChild(rotateTreeByComparator(child2, comparator));
                newNode.addChild(rotateTreeByComparator(child1, comparator));
            } else {
                newNode.addChild(rotateTreeByComparator(child1, comparator));
                newNode.addChild(rotateTreeByComparator(child2, comparator));
            }
        }

        return newNode;
    }

    default Comparator<NodeRef> createNodeDensityComparator() {

        return new Comparator<NodeRef>() {

            public int compare(NodeRef node1, NodeRef node2) {
                return getLeafCount(node2) - getLeafCount(node1);
            }

            public boolean equals(NodeRef node1, NodeRef node2) {
                return getLeafCount(node1) == getLeafCount(node2);
            }
        };
    }

    default Comparator<NodeRef> createNodeDensityMinNodeHeightComparator() {

        return new Comparator<NodeRef>() {

            public int compare(NodeRef node1, NodeRef node2) {
                int larger = getLeafCount(node1) - getLeafCount(node2);

                if (larger != 0) return larger;

                double tipRecent = getMinNodeHeight(node2) - getMinNodeHeight(node1);
                if (tipRecent > 0.0) return 1;
                if (tipRecent < 0.0) return -1;
                return 0;
            }
        };
    }

    static boolean allDisjoint(SimpleNode[] nodes) {

        // check with java 1.6
        Set<String>[] ids = new Set[nodes.length];
        for (int k = 0; k < nodes.length; ++k) {
            ids[k] = new SimpleTree(nodes[k]).getLeafSet();
            for (int j = 0; j < k; ++j) {
                Set<String> intersection = new HashSet<String>(ids[j]);
                intersection.retainAll(ids[k]);
                if (intersection.size() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Compares 2 trees and returns true if they have the same topology (same taxon
     * order is assumed).
     */
    default boolean equals(Tree tree2) {

        return uniqueNewick(getRoot()).equals(tree2.uniqueNewick(tree2.getRoot()));
    }

    default Node convertToJebl(NodeRef node, SimpleRootedTree jtree) {
        if (isExternal(node)) {
            String taxonId = getTaxonId(node.getNumber());
            Node externalNode = jtree.createExternalNode(jebl.evolution.taxa.Taxon.getTaxon(taxonId));
            jtree.setHeight(externalNode, getNodeHeight(node));
            return externalNode;
        }
        List<Node> jchildren = new ArrayList<Node>();
        for (int nc = 0; nc < getChildCount(node); ++nc) {
            NodeRef child = getChild(node, nc);
            Node node1 = convertToJebl(child, jtree);
            jtree.setHeight(node1, getNodeHeight(child));
            jchildren.add(node1);
        }

        return jtree.createInternalNode(jchildren);
    }

    /**
     * Convert from beast tree to JEBL tree.
     * Note that currently only topology and branch lengths are preserved.
     * Can add attributes later if needed.
     *
     * @return jebl tree
     */
    default SimpleRootedTree asJeblTree() {
        SimpleRootedTree jtree = new SimpleRootedTree();

        convertToJebl(getRoot(), jtree);
        jtree.setHeight(jtree.getRootNode(), getNodeHeight(getRoot()));
        return jtree;
    }

    /**
     * Gets the set of clades in a tree
     *
     * @return the set of clades
     */
    default Set<Set<String>> getClades() {
        Set<Set<String>> clades = new HashSet<Set<String>>();
        getClades(getRoot(), null, clades);

        return clades;
    }

    default void getClades(NodeRef node, Set<String> leaves, Set<Set<String>> clades) {

        if (isExternal(node)) {
            leaves.add(getTaxonId(node.getNumber()));
        } else {

            Set<String> ls = new HashSet<String>();

            for (int i = 0; i < getChildCount(node); i++) {

                NodeRef node1 = getChild(node, i);

                getClades(node1, ls, clades);
            }

            if (leaves != null) {
                // except for the root clade...
                leaves.addAll(ls);
                clades.add(ls);
            }

        }
    }

    /**
     * Tests whether the given tree is compatible with a set of clades
     *
     * @param clades the set of clades
     * @return
     */
    default boolean isCompatible(Set<Set<String>> clades) {
        return isCompatible(getRoot(), null, clades);
    }

    default boolean isCompatible(NodeRef node, Set<String> leaves, Set<Set<String>> clades) {
        if (isExternal(node)) {
            leaves.add(getTaxonId(node.getNumber()));
            return true;
        } else {

            Set<String> ls = new HashSet<String>();

            for (int i = 0; i < getChildCount(node); i++) {

                NodeRef node1 = getChild(node, i);

                if (!isCompatible(node1, ls, clades)) {
                    // as soon as we have an incompatibility break out...
                    return false;
                }
            }

            if (leaves != null) {
                // except for the root clade...
                for (Set<String> clade : clades) {
                    Set<String> intersection = new HashSet<String>(clade);
                    intersection.retainAll(ls);

                    if (intersection.size() != 0 &&
                            intersection.size() != ls.size() &&
                            intersection.size() != clade.size()) {
                        return false;
                    }
                }

                leaves.addAll(ls);
            }
        }
        return true;
    }

    /**
     * Static utility functions for trees.
     */
    @Deprecated
    public class Utils {

        /**
         * Count number of leaves in subtree whose root is node.
         *
         * @param tree the tree
         * @param node the node to get leaf count below
         * @return the number of leaves under this node.
         */
        @Deprecated
        public static int getLeafCount(Tree tree, NodeRef node) {
            return tree.getLeafCount(node);
        }

        @Deprecated
        public static double getTreeLength(Tree tree, NodeRef node) {
            return tree.getTreeLength(node);
        }

        @Deprecated
        public static double getMinNodeHeight(Tree tree, NodeRef node) {

            return tree.getMinNodeHeight(node);
        }

        /**
         * @param tree the tree to test fo ultrametricity
         * @return true only if all tips have height 0.0
         */
        @Deprecated
        public static boolean isUltrametric(Tree tree) {
            return tree.isUltrametric();
        }

        /**
         * @param tree the tree to test if binary
         * @return true only if internal nodes have 2 children
         */
        @Deprecated
        public static boolean isBinary(Tree tree) {
            return tree.isBinary();
        }

        /**
         * @param tree the tree to retrieve leaf set of
         * @return a set of strings which are the taxa of the tree.
         */
        @Deprecated
        public static Set<String> getLeafSet(Tree tree) {

            return tree.getLeafSet();
        }

        /**
         * @param tree the tree
         * @param taxa the taxa
         * @return Set of taxon names (id's) associated with the taxa in taxa.
         * @throws beast.evolution.tree.Tree.MissingTaxonException
         *          if a taxon in taxa is not contained in the tree
         */
        @Deprecated
        public static Set<String> getLeavesForTaxa(Tree tree, TaxonList taxa) throws MissingTaxonException {

            return tree.getLeavesForTaxa(taxa);
        }

        /**
         * @param tree the tree
         * @param node the node to get names of leaves below
         * @return a set of taxa names (as strings) of the leaf nodes descended from the given node.
         */
        @Deprecated
        public static Set<String> getDescendantLeaves(Tree tree, NodeRef node) {
            return tree.getDescendantLeaves(node);
        }

        /**
         * @param tree the tree
         * @param node the node to get external nodes below
         * @return a set of noderefs of the leaf nodes descended from the given node.
         */
        @Deprecated
        public static Set<NodeRef> getExternalNodes(Tree tree, NodeRef node) {
            return tree.getExternalNodes(node);
        }

        /**
         * Gets the most recent common ancestor (MRCA) node of a set of leaf nodes.
         *
         * @param tree      the Tree
         * @param leafNodes a set of names
         * @return the NodeRef of the MRCA
         */
        @Deprecated
        public static NodeRef getCommonAncestorNode(Tree tree, Set<String> leafNodes) {
            return tree.getCommonAncestorNode(leafNodes);
        }

        /**
         * @param tree the tree
         * @param taxa the taxa
         * @return A bitset with the node numbers set.
         * @throws beast.evolution.tree.Tree.MissingTaxonException
         *          if a taxon in taxa is not contained in the tree
         */
        @Deprecated
        public static BitSet getTipsBitSetForTaxa(Tree tree, TaxonList taxa) throws Tree.MissingTaxonException {
            return tree.getTipsBitSetForTaxa(taxa);
        }

        /**
         * @param tree the tree
         * @param taxa the taxa
         * @return A HashSet of node numbers.
         * @throws beast.evolution.tree.Tree.MissingTaxonException
         *          if a taxon in taxa is not contained in the tree
         */
        @Deprecated
        public static Set<Integer> getTipsForTaxa(Tree tree, TaxonList taxa) throws Tree.MissingTaxonException {
            return tree.getTipsForTaxa(taxa);
        }

        @Deprecated
        public static boolean isMonophyletic(Tree tree, Set<String> leafNodes) {
            return tree.isMonophyletic(leafNodes, Collections.<String>emptySet());
        }

        /**
         * Performs a monophyly test on a set of leaf nodes. The nodes are monophyletic
         * if there is a node in the tree which subtends all the taxa in the set (and
         * only those taxa).
         *
         * @param tree      a tree object to perform test on
         * @param leafNodes a set of leaf node ids
         * @param ignore    a set of ids to ignore in monophyly assessment
         * @return boolean is monophyletic?
         */
        @Deprecated
        public static boolean isMonophyletic(Tree tree, Set<String> leafNodes, Set<String> ignore) {
            return tree.isMonophyletic(leafNodes, ignore);
        }

        @Deprecated
        public static NodeRef getCommonAncestor(Tree tree, NodeRef n1, NodeRef n2) {
            return tree.getCommonAncestor(n1, n2);
        }

        // A lightweight version for finding the most recent common ancestor of a group of taxa.
        // return the node-ref of the MRCA.

        // would be nice to use nodeRef's, but they are not preserved :(
        @Deprecated
        public static NodeRef getCommonAncestor(Tree tree, int[] nodes) {
            return tree.getCommonAncestor(nodes);
        }


        /**
         * @param tree
         * @param range
         * @return the size of the largest clade with tips in the given range of times.
         */
        @Deprecated
        public static int largestClade(Tree tree, double range) {
            return tree.largestClade(range);
        }

        /**
         * @return the size of the largest clade with tips in the given range of times.
         */
        @Deprecated
        private static int largestClade(Tree tree, NodeRef node, double range, double[] currentBounds) {
            return tree.largestClade(node, range, currentBounds);
        }

        /**
         * Calculates the minimum number of steps for the parsimony reconstruction of a
         * binary character defined by leafStates.
         *
         * @param tree       a tree object to perform test on
         * @param leafStates a set of booleans, one for each leaf node
         * @return number of parsimony steps
         */
        @Deprecated
        public static int getParsimonySteps(Tree tree, Set leafStates) {
            return tree.getParsimonySteps(leafStates);
        }

        /**
         * Calculates the parsimony reconstruction of a binary character defined
         * by leafStates at a given node.
         *
         * @param tree       a tree object to perform test on
         * @param node       a NodeRef object from tree
         * @param leafStates a set of booleans, one for each leaf node
         * @return number of parsimony steps
         */
        @Deprecated
        public static double getParsimonyState(Tree tree, NodeRef node, Set leafStates) {
            return tree.getParsimonyState(node, leafStates);
        }

        /**
         * determine preorder successor of this node
         *
         * @return next node
         */
        @Deprecated
        public static NodeRef preorderSuccessor(Tree tree, NodeRef node) {
            return tree.preorderSuccessor(node);
        }

        /**
         * determine a postorder traversal list of nodes in a tree
         *
         */
        @Deprecated
        public static void postOrderTraversalList(Tree tree, int[] postOrderList) {
            tree.postOrderTraversalList(postOrderList);
        }

        // populate  postOrderList with node numbers in posorder: parent before children.
        //  postOrderList is pre-allocated with the right size
        @Deprecated
        public static void preOrderTraversalList(Tree tree, int[] postOrderList) {
            tree.preOrderTraversalList(postOrderList);
        }

        @Deprecated
        static int preOrderTraversalList(Tree tree, int idx, int[] postOrderList) {
            return tree.preOrderTraversalList(idx, postOrderList);
        }


        /**
         * determine postorder successor of a node
         *
         * @return next node
         */
        @Deprecated
        public static NodeRef postorderSuccessor(Tree tree, NodeRef node) {
            return tree.postorderSuccessor(node);
        }

        /**
         * Gets finds the most ancestral node with attribute set.
         */
        @Deprecated
        public static NodeRef findNodeWithAttribute(Tree tree, String attribute) {
            return tree.findNodeWithAttribute(attribute);
        }

        /**
         * Gets finds the most recent date amongst the external nodes.
         */
        @Deprecated
        public static beast.evolution.util.Date findMostRecentDate(Tree tree) {
            return tree.findMostRecentDate();
        }

        /**
         * Recursive function for constructing a newick tree representation in the given buffer.
         */
        @Deprecated
        public static String newick(Tree tree) {
            return tree.newick();
        }

        /**
         * @param tree tree to return in newick format
         * @param dp   the decimal places for branch lengths
         * @return a string representation of the tree in newick format with branch lengths expressed with the given
         *         number of decimal places
         */
        @Deprecated
        public static String newick(Tree tree, int dp) {
            return tree.newick(dp);
        }

        @Deprecated
        public static String newick(Tree tree, BranchRates branchRates) {
            return tree.newick(branchRates);
        }

        @Deprecated
        public static String newick(Tree tree,
                                    TreeTraitProvider[] treeTraitProviders
        ) {
            return tree.newick(treeTraitProviders);
        }

        /**
         * Recursive function for constructing a newick tree representation in the given buffer.
         */
        @Deprecated
        public static String newickNoLengths(Tree tree) {
            return tree.newickNoLengths();
        }

        /**
         * Recursive function for constructing a newick tree representation in the given buffer.
         *
         * @param tree                     The tree
         * @param node                     The node [tree.getRoot()]
         * @param labels                   whether labels or numbers should be used
         * @param lengths                  What type of branch lengths: NO_BRANCH_LENGTHS, LENGTHS_AS_TIME, LENGTHS_AS_SUBSTITUTIONS
         * @param branchRates              An optional BranchRates (or null) used to scale branch times into substitutions
         * @param treeTraitProviders       An array of TreeTraitProvider
         * @param format                   formatter for branch lengths
         * @param idMap                    A map if id names to integers that is used to overide node labels when present
         * @param buffer                   The StringBuffer
         */
        @Deprecated
        public static void newick(Tree tree, NodeRef node, boolean labels, BranchLengthType lengths, NumberFormat format,
                                  BranchRates branchRates,
                                  TreeTraitProvider[] treeTraitProviders,
                                  Map<String, Integer> idMap, StringBuffer buffer) {
            tree.newick(node, labels, lengths, format, branchRates, treeTraitProviders, idMap, buffer);
        }

        /**
         * Recursive function for constructing a newick tree representation in the given buffer.
         */
        @Deprecated
        public static String uniqueNewick(Tree tree, NodeRef node) {
            return tree.uniqueNewick(node);
        }

        /**
         * Recursive function for constructing a newick tree representation in the given buffer.
         */
        @Deprecated
        public static Tree rotateByName(Tree tree) {
            return tree.rotateByName();
        }

        @Deprecated
        public static MutableTree rotateTreeByComparator(Tree tree, Comparator<NodeRef> comparator) {
            return tree.rotateTreeByComparator(comparator);
        }

        @Deprecated
        public static Comparator<NodeRef> createNodeDensityComparator(final Tree tree) {
            return tree.createNodeDensityComparator();
        }

        @Deprecated
        public static Comparator<NodeRef> createNodeDensityMinNodeHeightComparator(final Tree tree) {
            return tree.createNodeDensityMinNodeHeightComparator();
        }

        @Deprecated
        public static boolean allDisjoint(SimpleNode[] nodes) {
            return Tree.allDisjoint(nodes);
        }

        /**
         * Compares 2 trees and returns true if they have the same topology (same taxon
         * order is assumed).
         */
        @Deprecated
        public static boolean equal(Tree tree1, Tree tree2) {
            return tree1.equals(tree2);
        }

        /**
         * Convert from beast tree to JEBL tree.
         * Note that currently only topology and branch lengths are preserved.
         * Can add attributes later if needed.
         *
         * @param tree beast
         * @return jebl tree
         */
        @Deprecated
        static public SimpleRootedTree asJeblTree(Tree tree) {
            return tree.asJeblTree();
        }

        /**
         * Gets the set of clades in a tree
         *
         * @param tree the tree
         * @return the set of clades
         */
        @Deprecated
        public static Set<Set<String>> getClades(Tree tree) {
            return tree.getClades();
        }

        /**
         * Tests whether the given tree is compatible with a set of clades
         *
         * @param tree   the test tree
         * @param clades the set of clades
         * @return
         */
        @Deprecated
        public static boolean isCompatible(Tree tree, Set<Set<String>> clades) {
            return tree.isCompatible(clades);
        }

    }

}
