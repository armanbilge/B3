/*
 * AbstractLikelihoodCore.java
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

import java.util.Arrays;

/**
 * AbstractLikelihoodCore - An abstract base class for LikelihoodCores
 *
 * @author Andrew Rambaut
 * @version $Id: AbstractLikelihoodCore.java,v 1.11 2006/08/30 16:02:42 rambaut Exp $
 */

public abstract class AbstractLikelihoodCore implements LikelihoodCore {

    protected int stateCount;
    protected int nodeCount;
    protected int patternCount;
    protected int partialsSize;
    protected int matrixSize;
    protected int matrixCount;

    protected boolean integrateCategories;

    protected double[][][] partials;
    protected double[][][] conditionals;
    protected double[][][] upperPartials;

    protected int[][] states;

    protected double[][][] matrices;

    protected int[] currentMatricesIndices;
    protected int[] storedMatricesIndices;
    protected int[] currentPartialsIndices;
    protected int[] storedPartialsIndices;

    protected boolean useScaling = false;

    protected double[][][] scalingFactors;

    private double scalingThreshold = 1.0E-100;

    /**
     * Constructor
     *
     * @param stateCount number of states
     */
    public AbstractLikelihoodCore(int stateCount) {
        this.stateCount = stateCount;
    }

    /**
     * initializes partial likelihood arrays.
     *
     * @param nodeCount           the number of nodes in the tree
     * @param patternCount        the number of patterns
     * @param matrixCount         the number of matrices (i.e., number of categories)
     * @param integrateCategories whether sites are being integrated over all matrices
     */
    public void initialize(int nodeCount, int patternCount, int matrixCount, boolean integrateCategories) {

        this.nodeCount = nodeCount;
        this.patternCount = patternCount;
        this.matrixCount = matrixCount;

        this.integrateCategories = integrateCategories;

        if (integrateCategories) {
            partialsSize = patternCount * stateCount * matrixCount;
        } else {
            partialsSize = patternCount * stateCount;
        }

        partials = new double[2][nodeCount][];
        conditionals = new double[2][nodeCount][];
        upperPartials = new double[2][nodeCount][];

        currentMatricesIndices = new int[nodeCount];
        storedMatricesIndices = new int[nodeCount];

        currentPartialsIndices = new int[nodeCount];
        storedPartialsIndices = new int[nodeCount];

        states = new int[nodeCount][];

        for (int i = 0; i < nodeCount; i++) {
            partials[0][i] = null;
            partials[1][i] = null;

            states[i] = null;
        }

        matrixSize = stateCount * stateCount;

        matrices = new double[2][nodeCount][matrixCount * matrixSize];
    }

    /**
     * cleans up and deallocates arrays.
     */
    public void finalize() throws java.lang.Throwable {
        super.finalize();

        nodeCount = 0;
        patternCount = 0;
        matrixCount = 0;

        partials = null;
        currentPartialsIndices = null;
        storedPartialsIndices = null;
        states = null;
        matrices = null;
        currentMatricesIndices = null;
        storedMatricesIndices = null;

        scalingFactors = null;
    }

    public void setUseScaling(boolean useScaling) {
        this.useScaling = useScaling;

        if (useScaling) {
            scalingFactors = new double[2][nodeCount][patternCount];
        }
    }

    /**
     * Allocates partials for a node
     */
    public void createNodePartials(int nodeIndex) {

        this.partials[0][nodeIndex] = new double[partialsSize];
        this.partials[1][nodeIndex] = new double[partialsSize];
        this.conditionals[0][nodeIndex] = new double[partialsSize];
        this.conditionals[1][nodeIndex] = new double[partialsSize];
        this.upperPartials[0][nodeIndex] = new double[partialsSize];
        this.upperPartials[1][nodeIndex] = new double[partialsSize];
    }

    /**
     * Sets partials for a node
     */
    public void setNodePartials(int nodeIndex, double[] partials) {

        if (this.partials[0][nodeIndex] == null) {
            createNodePartials(nodeIndex);
        }
        if (partials.length < partialsSize) {
            int k = 0;
            for (int i = 0; i < matrixCount; i++) {
                System.arraycopy(partials, 0, this.partials[0][nodeIndex], k, partials.length);
                k += partials.length;
            }
        } else {
            System.arraycopy(partials, 0, this.partials[0][nodeIndex], 0, partials.length);
        }
    }

    public void setNodeUpperPartials(int nodeIndex, double[] upperPartials) {
        if (upperPartials.length < partialsSize) {
            int k = 0;
            for (int i = 0; i < matrixCount; i++) {
                System.arraycopy(upperPartials, 0, this.upperPartials[0][nodeIndex], k, upperPartials.length);
                k += upperPartials.length;
            }
        } else {
            System.arraycopy(upperPartials, 0, this.upperPartials[0][nodeIndex], 0, upperPartials.length);
        }
    }

    /**
     * Allocates states for a node
     */
    public void createNodeStates(int nodeIndex) {

        this.states[nodeIndex] = new int[patternCount];
    }

    /**
     * Sets states for a node
     */
    public void setNodeStates(int nodeIndex, int[] states) {

        if (this.states[nodeIndex] == null) {
            createNodeStates(nodeIndex);
        }
        System.arraycopy(states, 0, this.states[nodeIndex], 0, patternCount);
    }

    /**
     * Gets states for a node
     */
    public void getNodeStates(int nodeIndex, int[] states) {
        System.arraycopy(this.states[nodeIndex], 0, states, 0, patternCount);
    }

    public void setNodeMatrixForUpdate(int nodeIndex) {
        currentMatricesIndices[nodeIndex] = 1 - currentMatricesIndices[nodeIndex];

    }


    /**
     * Sets probability matrix for a node
     */
    public void setNodeMatrix(int nodeIndex, int matrixIndex, double[] matrix) {
        System.arraycopy(matrix, 0, matrices[currentMatricesIndices[nodeIndex]][nodeIndex],
                matrixIndex * matrixSize, matrixSize);
    }

    /**
     * Gets probability matrix for a node
     */
    public void getNodeMatrix(int nodeIndex, int matrixIndex, double[] matrix) {
        System.arraycopy(matrices[currentMatricesIndices[nodeIndex]][nodeIndex],
                matrixIndex * matrixSize, matrix, 0, matrixSize);
    }

    public void setNodePartialsForUpdate(int nodeIndex) {
        currentPartialsIndices[nodeIndex] = 1 - currentPartialsIndices[nodeIndex];
    }

    /**
     * Sets the currently updating node partials for node nodeIndex. This may
     * need to repeatedly copy the partials for the different category partitions
     */
    public void setCurrentNodePartials(int nodeIndex, double[] partials) {
        if (partials.length < partialsSize) {
            int k = 0;
            for (int i = 0; i < matrixCount; i++) {
                System.arraycopy(partials, 0, this.partials[currentPartialsIndices[nodeIndex]][nodeIndex], k, partials.length);
                k += partials.length;
            }
        } else {
            System.arraycopy(partials, 0, this.partials[currentPartialsIndices[nodeIndex]][nodeIndex], 0, partials.length);
        }
    }

    /**
     * Calculates partial likelihoods at a node.
     *
     * @param nodeIndex1 the 'child 1' node
     * @param nodeIndex2 the 'child 2' node
     * @param nodeIndex3 the 'parent' node
     */
    public void calculatePartials(int nodeIndex1, int nodeIndex2, int nodeIndex3) {
        if (states[nodeIndex1] != null) {
            if (states[nodeIndex2] != null) {
                calculateStatesStatesPruning(
                        states[nodeIndex1], matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1],
                        states[nodeIndex2], matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2],
                        partials[currentPartialsIndices[nodeIndex3]][nodeIndex3]);
            } else {
                calculateStatesPartialsPruning(states[nodeIndex1], matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1],
                        partials[currentPartialsIndices[nodeIndex2]][nodeIndex2], matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2],
                        partials[currentPartialsIndices[nodeIndex3]][nodeIndex3]);
            }
        } else {
            if (states[nodeIndex2] != null) {
                calculateStatesPartialsPruning(states[nodeIndex2], matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2],
                        partials[currentPartialsIndices[nodeIndex1]][nodeIndex1], matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1],
                        partials[currentPartialsIndices[nodeIndex3]][nodeIndex3]);
            } else {
                calculatePartialsPartialsPruning(partials[currentPartialsIndices[nodeIndex1]][nodeIndex1], conditionals[currentPartialsIndices[nodeIndex1]][nodeIndex1], matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1],
                        partials[currentPartialsIndices[nodeIndex2]][nodeIndex2], conditionals[currentPartialsIndices[nodeIndex2]][nodeIndex2], matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2],
                        partials[currentPartialsIndices[nodeIndex3]][nodeIndex3]);
            }
        }

        if (useScaling) {
            scalePartials(nodeIndex3);
        }

//
//        int k =0;
//        for (int i = 0; i < patternCount; i++) {
//            double f = 0.0;
//
//            for (int j = 0; j < stateCount; j++) {
//                f += partials[currentPartialsIndices[nodeIndex3]][nodeIndex3][k];
//                k++;
//            }
//            if (f == 0.0) {
//                Logger.getLogger("error").severe("A partial likelihood (node index = " + nodeIndex3 + ", pattern = "+ i +") is zero for all states.");
//            }
//        }
    }

    public void calculateUpperPartials(int node, int sibling, int parent) {
        if (states[node] != null) {
            throw new UnsupportedOperationException();
        } else {
            if (states[sibling] != null) {
                throw new UnsupportedOperationException();
            } else {
                calculateUpperPartials(upperPartials[currentPartialsIndices[parent]][parent], conditionals[currentPartialsIndices[sibling]][sibling],
                        matrices[currentMatricesIndices[node]][node], upperPartials[currentPartialsIndices[node]][node]);
            }
        }

        if (useScaling) {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * Calculates partial likelihoods at a node when both children have states.
     */
    protected abstract void calculateStatesStatesPruning(int[] states1, double[] matrices1,
                                                         int[] states2, double[] matrices2,
                                                         double[] partials3);

    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
    protected abstract void calculateStatesPartialsPruning(int[] states1, double[] matrices1,
                                                           double[] partials2, double[] matrices2,
                                                           double[] partials3);

    /**
     * Calculates partial likelihoods at a node when both children have partials.
     */
    protected abstract void calculatePartialsPartialsPruning(double[] partials1, double[] conditionals1, double[] matrices1,
                                                             double[] partials2, double[] conditionals2, double[] matrices2,
                                                             double[] partials3);

    protected void calculateUpperPartials(double[] upperPartials1, double[] conditionals, double[] matrices, double[] upperPartials2) {
        throw new UnsupportedOperationException();
    };

    /**
     * Calculates partial likelihoods at a node.
     *
     * @param nodeIndex1 the 'child 1' node
     * @param nodeIndex2 the 'child 2' node
     * @param nodeIndex3 the 'parent' node
     * @param matrixMap  a map of which matrix to use for each pattern (can be null if integrating over categories)
     */
    public void calculatePartials(int nodeIndex1, int nodeIndex2, int nodeIndex3, int[] matrixMap) {
        if (states[nodeIndex1] != null) {
            if (states[nodeIndex2] != null) {
                calculateStatesStatesPruning(
                        states[nodeIndex1], matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1],
                        states[nodeIndex2], matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2],
                        partials[currentPartialsIndices[nodeIndex3]][nodeIndex3], matrixMap);
            } else {
                calculateStatesPartialsPruning(
                        states[nodeIndex1], matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1],
                        partials[currentPartialsIndices[nodeIndex2]][nodeIndex2], matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2],
                        partials[currentPartialsIndices[nodeIndex3]][nodeIndex3], matrixMap);
            }
        } else {
            if (states[nodeIndex2] != null) {
                calculateStatesPartialsPruning(
                        states[nodeIndex2], matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2],
                        partials[currentPartialsIndices[nodeIndex1]][nodeIndex1], matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1],
                        partials[currentPartialsIndices[nodeIndex3]][nodeIndex3], matrixMap);
            } else {
                calculatePartialsPartialsPruning(
                        partials[currentPartialsIndices[nodeIndex1]][nodeIndex1], partials[currentPartialsIndices[nodeIndex1]][nodeIndex1], matrices[currentMatricesIndices[nodeIndex1]][nodeIndex1],
                        partials[currentPartialsIndices[nodeIndex2]][nodeIndex2], partials[currentPartialsIndices[nodeIndex2]][nodeIndex2], matrices[currentMatricesIndices[nodeIndex2]][nodeIndex2],
                        partials[currentPartialsIndices[nodeIndex3]][nodeIndex3], matrixMap);
            }
        }

        if (useScaling) {
            scalePartials(nodeIndex3);
        }
    }

    /**
     * Calculates partial likelihoods at a node when both children have states.
     */
    protected abstract void calculateStatesStatesPruning(int[] states1, double[] matrices1,
                                                         int[] states2, double[] matrices2,
                                                         double[] partials3, int[] matrixMap);

    /**
     * Calculates partial likelihoods at a node when one child has states and one has partials.
     */
    protected abstract void calculateStatesPartialsPruning(int[] states1, double[] matrices1,
                                                           double[] partials2, double[] matrices2,
                                                           double[] partials3, int[] matrixMap);

    /**
     * Calculates partial likelihoods at a node when both children have partials.
     */
    protected abstract void calculatePartialsPartialsPruning(double[] partials1, double[] conditionals1, double[] matrices1,
                                                             double[] partials2, double[] conditionals2, double[] matrices2,
                                                             double[] partials3, int[] matrixMap);


    public void integratePartials(int nodeIndex, double[] proportions, double[] outPartials) {
        calculateIntegratePartials(partials[currentPartialsIndices[nodeIndex]][nodeIndex], proportions, outPartials);
    }

    /**
     * Integrates partials across categories.
     *
     * @param inPartials  the partials at the node to be integrated
     * @param proportions the proportions of sites in each category
     * @param outPartials an array into which the integrated partials will go
     */
    protected abstract void calculateIntegratePartials(double[] inPartials, double[] proportions, double[] outPartials);

    public void calculateDifferentiatedLogLikelihoods(int parent, int sibling, double[] matrix, int node, double[] outDifferentiatedLogLikelihoods) {
        calculateDifferentiatedLogLikelihoods(upperPartials[currentPartialsIndices[parent]][parent],
                conditionals[currentPartialsIndices[sibling]][sibling],
                matrix,
                partials[currentPartialsIndices[node]][node],
                outDifferentiatedLogLikelihoods);
    }

    protected void calculateDifferentiatedLogLikelihoods(double[] upperPartials, double[] conditionals, double[] matrix, double[] partials, double[] outDifferentiatedLogLikelihoods) {
        throw new UnsupportedOperationException();
    }

    /**
     * Scale the partials at a given node. This uses a scaling suggested by Ziheng Yang in
     * Yang (2000) J. Mol. Evol. 51: 423-432
     * <p/>
     * This function looks over the partial likelihoods for each state at each pattern
     * and finds the largest. If this is less than the scalingThreshold (currently set
     * to 1E-40) then it rescales the partials for that pattern by dividing by this number
     * (i.e., normalizing to between 0, 1). It then stores the log of this scaling.
     * This is called for every internal node after the partials are calculated so provides
     * most of the performance hit. Ziheng suggests only doing this on a proportion of nodes
     * but this sounded like a headache to organize (and he doesn't use the threshold idea
     * which improves the performance quite a bit).
     *
     * @param nodeIndex
     */
    protected void scalePartials(int nodeIndex) {
        int u = 0;

        for (int i = 0; i < patternCount; i++) {

            double scaleFactor = 0.0;
            int v = u;
            for (int k = 0; k < matrixCount; k++) {
                for (int j = 0; j < stateCount; j++) {
                    if (partials[currentPartialsIndices[nodeIndex]][nodeIndex][v] > scaleFactor) {
                        scaleFactor = partials[currentPartialsIndices[nodeIndex]][nodeIndex][v];
                    }
                    v++;
                }
                v += (patternCount - 1) * stateCount;
            }

            if (scaleFactor < scalingThreshold) {

                v = u;
                for (int k = 0; k < matrixCount; k++) {
                    for (int j = 0; j < stateCount; j++) {
                        partials[currentPartialsIndices[nodeIndex]][nodeIndex][v] /= scaleFactor;
                        v++;
                    }
                    v += (patternCount - 1) * stateCount;
                }
                scalingFactors[currentPartialsIndices[nodeIndex]][nodeIndex][i] = Math.log(scaleFactor);

            } else {
                scalingFactors[currentPartialsIndices[nodeIndex]][nodeIndex][i] = 0.0;
            }
            u += stateCount;


        }
    }

    /**
     * This function returns the scaling factor for that pattern by summing over
     * the log scalings used at each node. If scaling is off then this just returns
     * a 0.
     *
     * @return the log scaling factor
     */
    public double getLogScalingFactor(int pattern) {
        double logScalingFactor = 0.0;
        if (useScaling) {
            for (int i = 0; i < nodeCount; i++) {
                logScalingFactor += scalingFactors[currentPartialsIndices[i]][i][pattern];
            }
        }
        return logScalingFactor;
    }

    public void getLogScalingFactors(int nodeIndex, double[] buffer) {
        if (useScaling) {
            System.arraycopy(scalingFactors[currentPartialsIndices[nodeIndex]][nodeIndex], 0,
                    buffer, 0, patternCount);
        } else {
            Arrays.fill(buffer, 0.0);
        }
    }

    public boolean arePartialsRescaled() {
        return useScaling;
    }

    /**
     * Gets the partials for a particular node.
     *
     * @param nodeIndex   the node
     * @param outPartials an array into which the partials will go
     */
    public void getPartials(int nodeIndex, double[] outPartials) {
        double[] partials1 = partials[currentPartialsIndices[nodeIndex]][nodeIndex];

        System.arraycopy(partials1, 0, outPartials, 0, partialsSize);
    }

    /**
     * Store current state
     */
    public void storeState() {

        System.arraycopy(currentMatricesIndices, 0, storedMatricesIndices, 0, nodeCount);
        System.arraycopy(currentPartialsIndices, 0, storedPartialsIndices, 0, nodeCount);
    }

    /**
     * Restore the stored state
     */
    public void restoreState() {
        // Rather than copying the stored stuff back, just swap the pointers...
        int[] tmp1 = currentMatricesIndices;
        currentMatricesIndices = storedMatricesIndices;
        storedMatricesIndices = tmp1;

        int[] tmp2 = currentPartialsIndices;
        currentPartialsIndices = storedPartialsIndices;
        storedPartialsIndices = tmp2;
    }
}
