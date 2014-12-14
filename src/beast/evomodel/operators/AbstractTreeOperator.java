/*
 * AbstractTreeOperator.java
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

package beast.evomodel.operators;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evomodel.tree.TreeModel;
import beast.inference.operators.OperatorFailedException;
import beast.inference.operators.SimpleMCMCOperator;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public abstract class AbstractTreeOperator extends SimpleMCMCOperator {

	private int transitions = 0;

	/**
     * @return the number of transitions since last call to reset().
     */
    public int getTransitions() {
    	return transitions;
    }

    /**
     * Set the number of transitions since last call to reset(). This is used
     * to restore the state of the operator
     *
     * @param transitions number of transition
     */
    public void setTransitions(int transitions) {
    	this.transitions = transitions;
    }

    public double getTransistionProbability() {
        final int accepted = getAcceptCount();
        final int rejected = getRejectCount();
        final int transition = getTransitions();
        return (double) transition / (double) (accepted + rejected);
    }

	/* exchange sub-trees whose root are i and j */
	protected void exchangeNodes(TreeModel tree, NodeRef i, NodeRef j,
	                             NodeRef iP, NodeRef jP) throws OperatorFailedException {

	    tree.beginTreeEdit();
	    tree.removeChild(iP, i);
	    tree.removeChild(jP, j);
	    tree.addChild(jP, i);
	    tree.addChild(iP, j);

        tree.endTreeEdit();
	}

	public void reset() {
        super.reset();
        transitions = 0;
    }

	/**
	 * @param tree   the tree
	 * @param parent the parent
	 * @param child  the child that you want the sister of
	 * @return the other child of the given parent.
	 */
    protected NodeRef getOtherChild(Tree tree, NodeRef parent, NodeRef child) {
        if( tree.getChild(parent, 0) == child ) {
            return tree.getChild(parent, 1);
        } else {
            return tree.getChild(parent, 0);
        }
    }
}
