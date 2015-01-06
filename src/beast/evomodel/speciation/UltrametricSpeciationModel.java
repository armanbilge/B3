/*
 * UltrametricSpeciationModel.java
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

package beast.evomodel.speciation;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evolution.util.Taxon;
import beast.evolution.util.Units;
import beast.inference.model.Variable;

import java.util.Set;

/**
 * This interface provides methods that describe a speciation model.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public abstract class UltrametricSpeciationModel extends SpeciationModel implements Units {

    public UltrametricSpeciationModel(String modelName, Type units) {
        super(modelName, units);
    }

    /**
     * "fixed" part of likelihood. Guaranteed to be called before any logNodeProbability
     * calls for one evaluation of the tree.
     *
     * @param taxonCount Number of taxa in tree
     * @return density factor which is not node dependent
     */
    public abstract double logTreeProbability(int taxonCount);

    /**
     * Per node part of likelihood.
     *
     * @param tree
     * @param node
     * @return node contribution to density
     */
    public abstract double logNodeProbability(Tree tree, NodeRef node);

    public abstract double differentiateLogNodeProbability(Tree tree, NodeRef node, Variable<Double> var, int index);

    public boolean analyticalMarginalOK() {
       return false;
    }

    public double getMarginal(Tree tree, CalibrationPoints calibration) {
       return calibration.getCorrection(tree, -1);
    }

    /**
     * @return true if calls to logNodeProbability for terminal nodes (tips) are required
     */
    public abstract boolean includeExternalNodesInLikelihoodCalculation();

    /**
     * Generic likelihood calculation
     *
     * @param tree
     * @return log-likelihood of density
     */
    public double calculateTreeLogLikelihood(Tree tree) {
        final int taxonCount = tree.getExternalNodeCount();
        double logL = logTreeProbability(taxonCount);

        for (int j = 0; j < tree.getInternalNodeCount(); j++) {
            logL += logNodeProbability(tree, tree.getInternalNode(j));
        }

        if (includeExternalNodesInLikelihoodCalculation()) {
            for (int j = 0; j < taxonCount; j++) {
                logL += logNodeProbability(tree, tree.getExternalNode(j));
            }
        }

        return logL;
    }

    /**
     * Generic likelihood calculation
     *
     * @param tree
     * @return log-likelihood of density
     */
    public final double differentiateTreeLogLikelihood(Tree tree, Variable<Double> var, int index) {

        double derivative = 0;

        for (int j = 0; j < tree.getInternalNodeCount(); j++) {
            derivative += differentiateLogNodeProbability(tree, tree.getInternalNode(j), var, index);
        }

        final int taxonCount = tree.getTaxonCount();
        if (includeExternalNodesInLikelihoodCalculation()) {
            for (int j = 0; j < taxonCount; j++) {
                derivative += differentiateLogNodeProbability(tree, tree.getExternalNode(j), var, index);
            }
        }

        return derivative;
    }

    /**
     * Alternative likelihood calculation that uses recursion over the tree and allows
     * a list of taxa to exclude
     *
     * @param tree    the tree
     * @param exclude a list of taxa to exclude
     * @return log-likelihood of density
     */
    public double calculateTreeLogLikelihood(Tree tree, Set<Taxon> exclude) {
        final int taxonCount = tree.getExternalNodeCount() - exclude.size();

        double[] lnL = {logTreeProbability(taxonCount)};

        calculateNodeLogLikelihood(tree, tree.getRoot(), exclude, lnL);

        return lnL[0];
    }

    public double differentiateTreeLogLikelihood(Tree tree, Set<Taxon> exclude, Variable<Double> var, int index) {
        double[] derivative = {0};

        differentiateNodeLogLikelihood(tree, tree.getRoot(), exclude, derivative, var, index);

        return derivative[0];
    }

    /**
     * Alternative likelihood calculation that uses recursion over the tree and allows
     * a list of taxa to exclude
     *
     * @param tree    the tree
     * @param node
     * @param exclude a list of taxa to exclude
     * @param lnL     a reference to the lnL sum
     * @return the number of included daughter nodes
     */
    private int calculateNodeLogLikelihood(Tree tree, NodeRef node, Set<Taxon> exclude, double[] lnL) {
        if (tree.isExternal(node)) {
            if (!exclude.contains(tree.getNodeTaxon(node))) {
                if (includeExternalNodesInLikelihoodCalculation()) {
                    lnL[0] += logNodeProbability(tree, node);
                }

                // this tip is included in the subtree...
                return 1;
            }

            // this tip is excluded from the subtree...
            return 0;
        } else {
            int count = 0;
            for (int i = 0; i < tree.getChildCount(node); i++) {
                NodeRef child = tree.getChild(node, i);
                count += calculateNodeLogLikelihood(tree, child, exclude, lnL);
            }

            if (count == 2) {
                // this node is included in the subtree...
                lnL[0] += logNodeProbability(tree, node);
            }

            // if at least one of the children has included tips then return 1 otherwise 0
            return count > 0 ? 1 : 0;
        }
    }

    private int differentiateNodeLogLikelihood(Tree tree, NodeRef node, Set<Taxon> exclude, double[] derivative, Variable<Double> var, int index) {
        if (tree.isExternal(node)) {
            if (!exclude.contains(tree.getNodeTaxon(node))) {
                if (includeExternalNodesInLikelihoodCalculation()) {
                    derivative[0] += differentiateLogNodeProbability(tree, node, var, index);
                }

                // this tip is included in the subtree...
                return 1;
            }

            // this tip is excluded from the subtree...
            return 0;
        } else {
            int count = 0;
            for (int i = 0; i < tree.getChildCount(node); i++) {
                NodeRef child = tree.getChild(node, i);
                count += differentiateNodeLogLikelihood(tree, child, exclude, derivative, var, index);
            }

            if (count == 2) {
                // this node is included in the subtree...
                derivative[0] += logNodeProbability(tree, node);
            }

            // if at least one of the children has included tips then return 1 otherwise 0
            return count > 0 ? 1 : 0;
        }
    }

    @Override
    public double calculateTreeLogLikelihood(Tree tree, CalibrationPoints calibration) {
        double logL = calculateTreeLogLikelihood(tree);
        double mar = getMarginal(tree, calibration);
        logL += mar;
        return logL;
    }

    @Override
    public double differentiateTreeLogLikelihood(Tree tree, CalibrationPoints calibration, Variable<Double> var, int index) {
        throw new UnsupportedOperationException();
    }

}