/*
 * AbstractTreeLikelihood.java
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

package beast.beagle.treelikelihood;

import beast.evolution.tree.NodeRef;
import beast.evomodel.tree.TreeModel;
import beast.inference.model.AbstractModelLikelihood;
import beast.inference.model.CompoundLikelihood;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.xml.Reportable;

/**
 * AbstractTreeLikelihood - a base class for likelihood calculators of sites on a tree.
 *
 * @author Andrew Rambaut
 * @author Marc Suchard
 * @version $Id: AbstractTreeLikelihood.java,v 1.16 2005/06/07 16:27:39 alexei Exp $
 */

public abstract class AbstractTreeLikelihood extends AbstractModelLikelihood implements Reportable {

    protected static final boolean COUNT_TOTAL_OPERATIONS = false;

    public AbstractTreeLikelihood(String name, TreeModel treeModel) {

        super(name);

        this.treeModel = treeModel;
        addModel(treeModel);

        nodeCount = treeModel.getNodeCount();

        updateNode = new boolean[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            updateNode[i] = true;
        }

        likelihoodKnown = false;

    }


    /**
     * Set update flag for a node and its children
     */
    protected void updateNode(NodeRef node) {

        updateNode[node.getNumber()] = true;
        likelihoodKnown = false;
    }

    /**
     * Set update flag for a node and its direct children
     */
    protected void updateNodeAndChildren(NodeRef node) {
        updateNode[node.getNumber()] = true;

        for (int i = 0; i < treeModel.getChildCount(node); i++) {
            NodeRef child = treeModel.getChild(node, i);
            updateNode[child.getNumber()] = true;
        }
        likelihoodKnown = false;
    }

    /**
     * Set update flag for a node and all its descendents
     */
    protected void updateNodeAndDescendents(NodeRef node) {
        updateNode[node.getNumber()] = true;

        for (int i = 0; i < treeModel.getChildCount(node); i++) {
            NodeRef child = treeModel.getChild(node, i);
            updateNodeAndDescendents(child);
        }

        likelihoodKnown = false;
    }

    /**
     * Set update flag for all nodes
     */
    protected void updateAllNodes() {
        for (int i = 0; i < nodeCount; i++) {
            updateNode[i] = true;
        }
        likelihoodKnown = false;
    }

    // **************************************************************
    // VariableListener IMPLEMENTATION
    // **************************************************************

    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // do nothing
    }

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        if (COUNT_TOTAL_OPERATIONS)
            totalModelChangedCount++;
        likelihoodKnown = false;
    }

    /**
     * Stores the additional state other than model components
     */
    protected void storeState() {

        storedLikelihoodKnown = likelihoodKnown;
        storedLogLikelihood = logLikelihood;
    }

    /**
     * Restore the additional stored state
     */
    protected void restoreState() {

        likelihoodKnown = storedLikelihoodKnown;
        logLikelihood = storedLogLikelihood;
    }

    protected void acceptState() {
    } // nothing to do

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    public final Model getModel() {
        return this;
    }

    public final double getLogLikelihood() {
        if (COUNT_TOTAL_OPERATIONS)
            totalGetLogLikelihoodCount++;
        if (CompoundLikelihood.DEBUG_PARALLEL_EVALUATION) {
            System.err.println((likelihoodKnown ? "lazy" : "evaluate"));
        }
        if (!likelihoodKnown) {
            if (COUNT_TOTAL_OPERATIONS)
                totalCalculateLikelihoodCount++;
            logLikelihood = calculateLogLikelihood();
            likelihoodKnown = true;
        }
        return logLikelihood;
    }

    /**
     * Forces a complete recalculation of the likelihood next time getLikelihood is called
     */
    public void makeDirty() {
        if (COUNT_TOTAL_OPERATIONS)
            totalMakeDirtyCount++;
        likelihoodKnown = false;
        updateAllNodes();
    }
    
    public boolean isLikelihoodKnown() {
    	return likelihoodKnown;
    }

    protected abstract double calculateLogLikelihood();

    public String getReport() {
        if (hasInitialized) {
            String rtnValue =  getClass().getName() + "(" + getLogLikelihood() + ")";
            if (COUNT_TOTAL_OPERATIONS)
             rtnValue += " total operations = " + totalOperationCount +
                         " matrix updates = " + totalMatrixUpdateCount + " model changes = " + totalModelChangedCount +
                         " make dirties = " + totalMakeDirtyCount +
                         " calculate likelihoods = " + totalCalculateLikelihoodCount +
                         " get likelihoods = " + totalGetLogLikelihoodCount +
                         " all rate updates = " + totalRateUpdateAllCount +
                         " partial rate updates = " + totalRateUpdateSingleCount;
            return rtnValue;
        } else {
            return getClass().getName() + "(uninitialized)";
        }
    }

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

    /**
     * the tree
     */
    protected TreeModel treeModel = null;

    /**
     * the number of nodes in the tree
     */
    protected int nodeCount;

    /**
     * Flags to specify which nodes are to be updated
     */
    protected boolean[] updateNode;

    private double logLikelihood;
    private double storedLogLikelihood;
    protected boolean likelihoodKnown = false;
    private boolean storedLikelihoodKnown = false;

    protected boolean hasInitialized = false;

    protected int totalOperationCount = 0;
    protected int totalMatrixUpdateCount = 0;
    protected int totalGetLogLikelihoodCount = 0;
    protected int totalModelChangedCount = 0;
    protected int totalMakeDirtyCount = 0;
    protected int totalCalculateLikelihoodCount = 0;
    protected int totalRateUpdateAllCount = 0;
    protected int totalRateUpdateSingleCount = 0;

}