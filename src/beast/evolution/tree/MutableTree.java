/*
 * MutableTree.java
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

import beast.evolution.util.MutableTaxonList;
import beast.math.MathUtils;

/**
 * Interface for a phylogenetic or genealogical tree.
 *
 * @version $Id: MutableTree.java,v 1.22 2006/07/28 11:27:32 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public interface MutableTree extends Tree, MutableTaxonList {

    public class InvalidTreeException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = 1955744780140327882L;

		public InvalidTreeException(String message) { super(message); }
	}

    // return true if tree already in edit mode
    boolean beginTreeEdit();

    void endTreeEdit();

	/**
	 * Add child to the children of parent.
	 * @throws IllegalArgumentException If child is already a child of parent
	 */
	void addChild(NodeRef parent, NodeRef child);

	/**
	 * Removes child from the children of parent.
	 * @throws IllegalArgumentException If child is not a child of parent
	 */
	void removeChild(NodeRef parent, NodeRef child);

    /**
     *  Replace child with another
     * @param node
     * @param child  of node to replace
     * @param newChild replacment child
     */
    void replaceChild(NodeRef node, NodeRef child, NodeRef newChild);

    /**
	 * Will throw an exception if any nodes have this node as their children.
	 */
	void setRoot(NodeRef root);


	/**
	 * set the height of the ith node in the tree (where the first n are internal).
	 */
	void setNodeHeight(NodeRef node, double height);

	/**
	 * set the rate of the ith node in the tree (where the first n are internal).
	 */
	void setNodeRate(NodeRef node, double height);

	/**
	 * set the length of the branch above the ith node in the tree (where the first n are internal).
	 */
	void setBranchLength(NodeRef node, double length);

	/**
	 * Sets an named attribute for a given node.
	 * @param node the node whose attribute is being set.
	 * @param name the name of the attribute.
	 * @param value the new value of the attribute.
	 */
	public void setNodeAttribute(NodeRef node, String name, Object value);

	/**
	 * Adds a listener to this tree.
	 */
	void addMutableTreeListener(MutableTreeListener listener);

	default int order(NodeRef node) {

		if (isExternal(node)) {
			return node.getNumber();
		} else {
			NodeRef child1 = getChild(node, 0);
			NodeRef child2 = getChild(node, 1);

			int num1 = order(child1);
			int num2 = order(child2);

			if (num1 > num2) {
				// swap child order
				removeChild(node, child1);
				removeChild(node, child2);
				addChild(node, child2);
				addChild(node, child1);
			}
			return Math.min(num1, num2);
		}
	}

	/**
	 * Multiples all node heights by the given scale.
	 */
	default void scaleNodeHeights(double scale) {
		for (int i = 0; i < getExternalNodeCount(); i++) {
			NodeRef node = getExternalNode(i);
			setNodeHeight(node, getNodeHeight(node)*scale);
		}

		for (int i = 0; i < getInternalNodeCount(); i++) {
			NodeRef node = getInternalNode(i);
			setNodeHeight(node, getNodeHeight(node)*scale);
		}
	}

	/**
	 * This method makes sure all internal nodes are at least as tall as their children.
	 * Following this method call there will be no negative branches, but some node heights may
	 * have changed
	 */
	default void correctHeightsForTips() {
		correctHeightsForTips(getRoot());
	}

	/**
	 * This method makes sure all internal nodes from the given node down are at least as tall
	 * as their children.
	 * Following this method call there will be no negative branches, but some node heights may
	 * have changed
	 */
	default void correctHeightsForTips(NodeRef node) {

		if( !isExternal(node) ) {
			// pre-order recursion
			for(int i = 0; i < getChildCount(node); i++) {
				correctHeightsForTips(getChild(node, i));
			}
		}

		if( !isRoot(node) ) {
			final double parentHeight = getNodeHeight(getParent(node));

			if( parentHeight <= getNodeHeight(node) ) {
				// set the parent height to be slightly above this node's height
				// picks
				double height = getNodeHeight(node);
				height += getNodeHeight(getRoot()) * (MathUtils.nextDouble() * 0.001);
				setNodeHeight(getParent(node), height);
			}
		}
	}

	@Deprecated
	public class Utils {

		@Deprecated
		public static int order(MutableTree tree, NodeRef node) {
			return tree.order(node);
		}

		/**
		 * Multiples all node heights by the given scale.
		 */
		@Deprecated
		public static void scaleNodeHeights(MutableTree tree, double scale) {
			tree.scaleNodeHeights(scale);
		}

		/**
		 * This method makes sure all internal nodes are at least as tall as their children.
		 * Following this method call there will be no negative branches, but some node heights may
		 * have changed
		 */
		@Deprecated
		public static void correctHeightsForTips(MutableTree tree) {
			tree.correctHeightsForTips();
		}

	}
}
