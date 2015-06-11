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

package beast.evomodel.treelikelihood;

import beast.evolution.alignment.PatternList;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.NodeRef;
import beast.evomodel.tree.TreeModel;
import beast.inference.model.AbstractModelLikelihood;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.xml.Reportable;

import java.util.Arrays;

/**
 * AbstractTreeLikelihood - a base class for likelihood calculators of sites on a tree.
 *
 * @author Andrew Rambaut
 * @version $Id: AbstractTreeLikelihood.java,v 1.16 2005/06/07 16:27:39 alexei Exp $
 */

public abstract class AbstractTreeLikelihood extends AbstractModelLikelihood implements Reportable {

    protected static final boolean COUNT_TOTAL_OPERATIONS = true;

    public AbstractTreeLikelihood(String name, PatternList patternList,
                                  TreeModel treeModel) {

        super(name);

        this.patternList = patternList;
        this.dataType = patternList.getDataType();
        patternCount = patternList.getPatternCount();
        stateCount = dataType.getStateCount();

        patternWeights = patternList.getPatternWeights();

        this.treeModel = treeModel;
        addModel(treeModel);

        nodeCount = treeModel.getNodeCount();

        derivatives = new double[nodeCount];
        storedDerivatives = new double[nodeCount];

        updateNode = new boolean[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            updateNode[i] = true;
        }

        likelihoodKnown = false;
        derivativesKnown = false;

    }

    /**
     * Sets the partials from a sequence in an alignment.
     */
    protected final void setStates(LikelihoodCore likelihoodCore, PatternList patternList,
                                   int sequenceIndex, int nodeIndex) {
        int i;

        int[] states = new int[patternCount];

        for (i = 0; i < patternCount; i++) {

            states[i] = patternList.getPatternState(sequenceIndex, i);
        }

        likelihoodCore.setNodeStates(nodeIndex, states);
    }

    public TreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Sets the partials from a sequence in an alignment.
     */
    protected final void setPartials(LikelihoodCore likelihoodCore, PatternList patternList,
                                     int categoryCount,
                                     int sequenceIndex, int nodeIndex) {
        double[] partials = new double[patternCount * stateCount];

        boolean[] stateSet;

        int v = 0;
        for (int i = 0; i < patternCount; i++) {

            int state = patternList.getPatternState(sequenceIndex, i);
            stateSet = dataType.getStateSet(state);

            for (int j = 0; j < stateCount; j++) {
                if (stateSet[j]) {
                    partials[v] = 1.0;
                } else {
                    partials[v] = 0.0;
                }
                v++;
            }
        }

        likelihoodCore.setNodePartials(nodeIndex, partials);
    }

    /**
     * Sets the partials from a sequence in an alignment.
     */
    protected final void setMissingStates(LikelihoodCore likelihoodCore, int nodeIndex) {
        int[] states = new int[patternCount];

        for (int i = 0; i < patternCount; i++) {
            states[i] = dataType.getGapState();
        }

        likelihoodCore.setNodeStates(nodeIndex, states);
    }

    /**
     * Sets the partials from a sequence in an alignment.
     */
    protected final void setMissingPartials(LikelihoodCore likelihoodCore, int nodeIndex) {
        double[] partials = new double[patternCount * stateCount];

        int v = 0;
        for (int i = 0; i < patternCount; i++) {
            for (int j = 0; j < stateCount; j++) {
                partials[v] = 1.0;
                v++;
            }
        }

        likelihoodCore.setNodePartials(nodeIndex, partials);
    }

    /**
     * Set update flag for a node and its children
     */
    protected void updateNode(NodeRef node) {

        updateNode[node.getNumber()] = true;
        likelihoodKnown = false;
        derivativesKnown = false;
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
        derivativesKnown = false;
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
        derivativesKnown = false;
    }

    /**
     * Set update flag for all nodes
     */
    protected void updateAllNodes() {
        for (int i = 0; i < nodeCount; i++) {
            updateNode[i] = true;
        }
        likelihoodKnown = false;
        derivativesKnown = false;
    }

    /**
     * Set update flag for a pattern
     */
    protected void updatePattern(int i) {
        if (updatePattern != null) {
            updatePattern[i] = true;
        }
        likelihoodKnown = false;
        derivativesKnown = false;
    }

    /**
     * Set update flag for all patterns
     */
    protected void updateAllPatterns() {
        if (updatePattern != null) {
            for (int i = 0; i < patternCount; i++) {
                updatePattern[i] = true;
            }
        }
        likelihoodKnown = false;
        derivativesKnown = false;
    }

    public final double[] getPatternWeights() {
        return patternWeights;
    }

    public final int getPatternCount() {
        return patternCount;
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
        likelihoodKnown = false;
    }

    /**
     * Stores the additional state other than model components
     */
    protected void storeState() {

        storedLikelihoodKnown = likelihoodKnown;
        storedLogLikelihood = logLikelihood;

        storedDerivativesKnown = derivativesKnown;
        if (derivativesKnown)
            System.arraycopy(derivatives, 0, storedDerivatives, 0, nodeCount);
    }

    /**
     * Restore the additional stored state
     */
    protected void restoreState() {

        likelihoodKnown = storedLikelihoodKnown;
        logLikelihood = storedLogLikelihood;

        derivativesKnown = storedDerivativesKnown;
        if (derivativesKnown) {
            final double tmp[] = derivatives;
            derivatives = storedDerivatives;
            storedDerivatives = tmp;
        }
    }

    protected void acceptState() {
    } // nothing to do

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    public final Model getModel() {
        return this;
    }

    public final PatternList getPatternList() {
        return patternList;
    }

    public final double getLogLikelihood() {
        if (!likelihoodKnown) {
            logLikelihood = calculateLogLikelihood();
            likelihoodKnown = true;
        }
        return logLikelihood;
    }

    protected void differentiateBranchSubstitutions() {
        if (!derivativesKnown) {
            Arrays.setAll(derivatives, i -> differentiateRespectingBranchSubstitutions(treeModel.getNode(i)));
            derivativesKnown = true;
        }
    }

    protected double getDerivativeRespectingBranchSubstitutions(final NodeRef node) {
        differentiateBranchSubstitutions();
        return derivatives[node.getNumber()];
    }

    protected double differentiateRespectingNode(final NodeRef node) {
        double deriv = - differentiateRespectingBranch(node);
        if (!treeModel.isExternal(node)) {
            deriv += differentiateRespectingBranch(treeModel.getChild(node, 0));
            deriv += differentiateRespectingBranch(treeModel.getChild(node, 1));
        }
        return deriv;
    }

    protected abstract double differentiateRespectingBranchSubstitutions(NodeRef node);

    protected abstract double differentiateRespectingBranch(NodeRef node);

    protected abstract double differentiateRespectingRate(NodeRef node);

    /**
     * Forces a complete recalculation of the likelihood next time getLikelihood is called
     */
    public void makeDirty() {
        likelihoodKnown = false;
        derivativesKnown = false;
        updateAllNodes();
        updateAllPatterns();
    }

    protected abstract double calculateLogLikelihood();

    public String getReport() {
        getLogLikelihood();
        return getClass().getName() + "(" + logLikelihood + ") total operations = " + totalOperationCount;

    }

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

    /**
     * the tree
     */
    protected TreeModel treeModel = null;

    /**
     * the patternList
     */
    protected PatternList patternList = null;

    protected DataType dataType = null;

    /**
     * the pattern weights
     */
    protected double[] patternWeights;

    /**
     * the number of patterns
     */
    protected int patternCount;

    /**
     * the number of states in the data
     */
    protected int stateCount;

    /**
     * the number of nodes in the tree
     */
    protected int nodeCount;

    /**
     * Flags to specify which patterns are to be updated
     */
    protected boolean[] updatePattern = null;

    /**
     * Flags to specify which nodes are to be updated
     */
    protected boolean[] updateNode;

    private double logLikelihood;
    private double storedLogLikelihood;
    protected boolean likelihoodKnown = false;
    private boolean storedLikelihoodKnown = false;

    protected double[] derivatives;
    protected double[] storedDerivatives;
    protected boolean derivativesKnown;
    protected boolean storedDerivativesKnown;

    protected int totalOperationCount = 0;
}