/*
 * RateEvolutionLikelihood.java
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

package beast.evomodel.clock;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evomodel.branchratemodel.AbstractBranchRateModel;
import beast.evomodel.tree.TreeModel;
import beast.evomodel.tree.TreeParameterModel;
import beast.inference.model.CompoundParameter;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;

import java.util.logging.Logger;

/**
 * Abstract superclass of likelihoods of rate evolution through time.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Michael Defoin Platel
 */
public abstract class RateEvolutionLikelihood extends AbstractBranchRateModel {

    public static final String RATES = "rates";
    public static final String EPISODIC = "episodic";
    public static final String LOGSPACE = "logspace";

    public static final String ROOTRATE = "rootRate";

    public RateEvolutionLikelihood(String name, TreeModel treeModel, Parameter ratesParameter, Parameter rootRateParameter, boolean isEpisodic) {

        super(name);

        this.treeModel = treeModel;
        addModel(treeModel);

        this.ratesParameter = new TreeParameterModel(treeModel, ratesParameter, false);
        Parameter.DefaultBounds bound = new Parameter.DefaultBounds(Double.MAX_VALUE, 0, ratesParameter.getDimension());
        ratesParameter.addBounds(bound);

        addModel(this.ratesParameter);

        this.rootRateParameter = rootRateParameter;
        rootRateParameter.addBounds(new Parameter.DefaultBounds(Double.MAX_VALUE, 0, 1));
        addVariable(rootRateParameter);

        if (rootRateParameter.getDimension() != 1) {
            throw new IllegalArgumentException("The root rate parameter must be of dimension 1");
        }

        this.isEpisodic = isEpisodic;

        Logger.getLogger("beast.evomodel").info("AutoCorrelated Relaxed Clock: " + name + (isEpisodic ? " (episodic)." : "."));

    }

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

    public final void handleModelChangedEvent(Model model, Object object, int index) {
        likelihoodKnown = false;
        if (model == ratesParameter) {
            fireModelChanged(this, index);
        }
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        likelihoodKnown = false;
    }

    protected void storeState() {
    }

    protected void restoreState() {
        likelihoodKnown = false;
    }

    protected void acceptState() {
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Get the model.
     *
     * @return the model.
     */
    public Model getModel() {
        return this;
    }

    public final double getLogLikelihood() {
        if (!getLikelihoodKnown()) {
            logLikelihood = calculateLogLikelihood();
            likelihoodKnown = true;
        }
        return logLikelihood;
    }

    public void makeDirty() {
        likelihoodKnown = false;
    }

    /**
     * Called to decide if the likelihood must be calculated. Can be overridden
     * (for example, to always return false).
     *
     * @return the likelihood.
     */
    protected boolean getLikelihoodKnown() {
        return likelihoodKnown;
    }

    /**
     * Get the log likelihood of the rate changes in this tree.
     *
     * @return the log likelihood.
     */
    private double calculateLogLikelihood() {
        NodeRef root = treeModel.getRoot();
        NodeRef node1 = treeModel.getChild(root, 0);
        NodeRef node2 = treeModel.getChild(root, 1);

        return calculateLogLikelihood(root, node1) + calculateLogLikelihood(root, node2);
    }

    public double differentiate(Variable<Double> var, int index) {
        NodeRef root = treeModel.getRoot();
        NodeRef node1 = treeModel.getChild(root, 0);
        NodeRef node2 = treeModel.getChild(root, 1);

        return differentiateLogLikelihood(root, node1, var, index) + differentiateLogLikelihood(root, node2, var, index);
    }

    /**
     * Recursively calculate the log likelihood of the rate changes in the given tree.
     *
     * @return the partial log likelihood of the rate changes below the given node plus the
     *         branch directly above.
     */
    private double calculateLogLikelihood(NodeRef parent, NodeRef node) {

        double logL, length;
        length = treeModel.getBranchLength(node);

        logL = branchRateChangeLogLikelihood(getBranchRate(treeModel, parent), getBranchRate(treeModel, node),
                length);

        //System.out.print(parent.getNumber() + " " + getBranchRate(treeModel, parent)+ " " + node.getNumber() + " " + getBranchRate(treeModel, node) + " " + treeModel.getBranchLength(node) + " " + logL + ", ");

        for (int i = 0; i < treeModel.getChildCount(node); i++) {
            logL += calculateLogLikelihood(node, treeModel.getChild(node, i));
        }
        return logL;
    }

    private double differentiateLogLikelihood(NodeRef parent, NodeRef node, Variable<Double> var, int index) {

        double deriv = 0;
        double length = treeModel.getBranchLength(node);

        if (var instanceof CompoundParameter)
            var = ((CompoundParameter) var).getMaskedParameter(index);

        if (var == rootRateParameter) {

            if (parent == treeModel.getRoot())
                deriv += differentiateBranchRateChangeLogLikelihood(getBranchRate(treeModel, parent), getBranchRate(treeModel, node), length, true);
            else if (node == treeModel.getRoot())
                deriv += differentiateBranchRateChangeLogLikelihood(getBranchRate(treeModel, parent), getBranchRate(treeModel, node), length, false);

        } else if (ratesParameter.hasVariable(var)) {

            if (index == ratesParameter.getParameterIndexFromNodeNumber(parent.getNumber()))
                deriv += differentiateBranchRateChangeLogLikelihood(getBranchRate(treeModel, parent), getBranchRate(treeModel, node), length, true);
            else if (index == ratesParameter.getParameterIndexFromNodeNumber(node.getNumber()))
                deriv += differentiateBranchRateChangeLogLikelihood(getBranchRate(treeModel, parent), getBranchRate(treeModel, node), length, false);

        } else if (var instanceof Parameter) {
            boolean respectNode = treeModel.isHeightParameterForNode(node, (Parameter) var);
            boolean respectParent = treeModel.isHeightParameterForNode(parent, (Parameter) var);
            if (respectNode || respectParent)
                deriv += (respectParent ? 1 : -1) * differentiateBranchRateChangeLogLikelihood(getBranchRate(treeModel, parent), getBranchRate(treeModel, node), length);
        } else {
            deriv = differentiateBranchRateChangeLogLikelihood(getBranchRate(treeModel, parent), getBranchRate(treeModel, node),
                    length, var, index);
        }


        //System.out.print(parent.getNumber() + " " + getBranchRate(treeModel, parent)+ " " + node.getNumber() + " " + getBranchRate(treeModel, node) + " " + treeModel.getBranchLength(node) + " " + logL + ", ");

        for (int i = 0; i < treeModel.getChildCount(node); i++) {
            deriv += differentiateLogLikelihood(node, treeModel.getChild(node, i), var, index);
        }
        return deriv;
    }

    public String toString() {
        return Double.toString(getLogLikelihood());
    }

    abstract double branchRateSample(double parentRate, double time);

    public void sampleRate(NodeRef node) {

        final NodeRef parent = treeModel.getParent(node);
        final double length = treeModel.getBranchLength(node);
        final double rate = branchRateSample(getBranchRate(treeModel, parent), length);

        treeModel.setNodeRate(node, rate);

    }

    public double getBranchRate(Tree tree, NodeRef node) {

        if (tree.isRoot(node)) return rootRateParameter.getParameterValue(0);
        return ratesParameter.getNodeValue(tree, node);
    }

    public boolean isVariableForNode(Tree tree, NodeRef node, Variable<Double> var, int index) {
        if (tree.isRoot(node))
            return var == rootRateParameter;
        else if (var instanceof Parameter)
            return ratesParameter.isParameterForNode(tree, node, (Parameter) var, index);
        else
            return false;
    }

    public boolean isEpisodic() {
        return isEpisodic;
    }

    /**
     * @return the log likelihood of the rate change from the parent to the given node.
     */
    abstract double branchRateChangeLogLikelihood(double parentRate, double childRate, double time);

    abstract double differentiateBranchRateChangeLogLikelihood(double parentRate, double childRate, double time);

    abstract double differentiateBranchRateChangeLogLikelihood(double parentRate, double childRate, double time, boolean respectParent);

    abstract double differentiateBranchRateChangeLogLikelihood(double parentRate, double childRate, double time, Variable<Double> var, int index);

    // **************************************************************
    // Private members
    // **************************************************************

    private double logLikelihood;
    private boolean likelihoodKnown = false;

    private final TreeModel treeModel;
    private final TreeParameterModel ratesParameter;
    protected final Parameter rootRateParameter;
    private final boolean isEpisodic;

}
