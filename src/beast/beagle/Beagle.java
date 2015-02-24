/*
 * Beagle.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
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

package beast.beagle;

import beagle.BeagleException;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.ptr.DoubleByReference;

import java.util.Arrays;
import java.util.List;

/**
 * This file documents the API as well as header for the
 * Broad-platform Evolutionary Analysis General Likelihood Evaluator
 *
 * KEY CONCEPTS
 *
 * The key to BEAGLE performance lies in delivering fine-scale
 * parallelization while minimizing data transfer and memory copy overhead.
 * To accomplish this, the library lacks the concept of data structure for
 * a tree, in spite of the intended use for phylogenetic analysis. Instead,
 * BEAGLE acts directly on flexibly indexed data storage (called buffers)
 * for observed character states and partial likelihoods. The client
 * program can set the input buffers to reflect the data and can calculate
 * the likelihood of a particular phylogeny by invoking likelihood
 * calculations on the appropriate input and output buffers in the correct
 * order. Because of this design simplicity, the library can support many
 * different tree inference algorithms and likelihood calculation on a
 * variety of models. Arbitrary numbers of states can be used, as can
 * nonreversible substitution matrices via complex eigen decompositions,
 * and mixture models with multiple rate categories and/or multiple eigen
 * decompositions. Finally, BEAGLE application programming interface (API)
 * calls can be asynchronous, allowing the calling program to implement
 * other coarse-scale parallelization schemes such as evaluating
 * independent genes or running concurrent Markov chains.
 *
 * USAGE
 *
 * To use the library, a client program first creates an instance of BEAGLE
 * by calling beagleCreateInstance; multiple instances per client are
 * possible and encouraged. All additional functions are called with a
 * reference to this instance. The client program can optionally request
 * that an instance run on certain hardware (e.g., a GPU) or have
 * particular features (e.g., double-precision math). Next, the client
 * program must specify the data dimensions and specify key aspects of the
 * phylogenetic model. Character state data are then loaded and can be in
 * the form of discrete observed states or partial likelihoods for
 * ambiguous characters. The observed data are usually unchanging and
 * loaded only once at the start to minimize memory copy overhead. The
 * character data can be compressed into unique “site patterns” and
 * associated weights for each. The parameters of the substitution process
 * can then be specified, including the equilibrium state frequencies, the
 * rates for one or more substitution rate categories and their weights,
 * and finally, the eigen decomposition for the substitution process.
 *
 * In order to calculate the likelihood of a particular tree, the client
 * program then specifies a series of integration operations that
 * correspond to steps in Felsenstein’s algorithm. Finite-time transition
 * probabilities for each edge are loaded directly if considering a
 * nondiagonalizable model or calculated in parallel from the eigen
 * decomposition and edge lengths specified. This is performed within
 * BEAGLE’s memory space to minimize data transfers. A single function call
 * will then request one or more integration operations to calculate
 * partial likelihoods over some or all nodes. The operations are performed
 * in the order they are provided, typically dictated by a postorder
 * traversal of the tree topology. The client needs only specify nodes for
 * which the partial likelihoods need updating, but it is up to the calling
 * software to keep track of these dependencies. The final step in
 * evaluating the phylogenetic model is done using an API call that yields
 * a single log likelihood for the model given the data.
 *
 * Aspects of the BEAGLE API design support both maximum likelihood (ML)
 * and Bayesian phylogenetic tree inference. For ML inference, API calls
 * can calculate first and second derivatives of the likelihood with
 * respect to the lengths of edges (branches). In both cases, BEAGLE
 * provides the ability to cache and reuse previously computed partial
 * likelihood results, which can yield a tremendous speedup over
 * recomputing the entire likelihood every time a new phylogenetic model is
 * evaluated.
 *
 * @author Likelihood API Working Group
 *
 * @author Daniel Ayres
 * @author Peter Beerli
 * @author Michael Cummings
 * @author Aaron Darling
 * @author Mark Holder
 * @author John Huelsenbeck
 * @author Paul Lewis
 * @author Michael Ott
 * @author Andrew Rambaut
 * @author Fredrik Ronquist
 * @author Marc Suchard
 * @author David Swofford
 * @author Derrick Zwickl
 *
 * @author Arman Bilge
 */
public final class Beagle {

    static {
        Native.register("libhmsbeagle");
    }

    /**
     * Information about a specific instance
     */
    private static class BeagleInstanceDetails extends Structure {

        private static class ByReference extends BeagleInstanceDetails implements Structure.ByReference {}

        /** Resource upon which instance is running */
        private int resourceNumber;
        /** Name of resource on which this instance is running as a NULL-terminated character string */
        private String resourceName;
        /** Name of implementation on which this instance is running as a NULL-terminated character string */
        private String implName;
        /** Description of implementation with details such as how auto-scaling is performed */
        private String implDescription;
        /* Bit-flags that characterize the activate capabilities of the resource and implementation for this instance */
        private long flags;

        @Override
        protected List getFieldOrder() {
            return null;
        }
    }

    /**
     * Description of a hardware resource
     */
    private static class BeagleResource extends Structure {

        /** Name of resource as a NULL-terminated character string */
        private String name;
        /** Description of resource as a NULL-terminated character string */
        private String description;
        /** Bit-flags of supported capabilities on resource */
        private long supportFlags;
        /** Bit-flags that identify resource type */
        private long requiredFlags;

        @Override
        protected List getFieldOrder() {
            return null;
        }
    }

    /**
     * List of hardware resources
     */
    private static class BeagleResourceList extends Structure {

        private static class ByReference extends BeagleResourceList implements Structure.ByReference {}

        /** Pointer list of resources */
        private BeagleResource[] list;
        /** Length of list */
        private int length;

        public BeagleResource[] getList() {
            return list;
        }

        @Override
        protected List getFieldOrder() {
            return null;
        }
    }

    /**
     * Get version
     *
     * This function returns a pointer to a string with the library version number.
     *
     * @return A string with the version number
     */
    private static native String beagleGetVersion();

    /**
     * Get citation
     *
     * This function returns a pointer to a string describing the version of the
     * library and how to cite it.
     *
     * @return A string describing the version of the library and how to cite it
     */
    private static native String beagleGetCitation();

    private static native BeagleResourceList.ByReference beagleGetResourceList();

    private static native int beagleCreateInstance(int tipCount,
                                                   int partialsBufferCount,
                                                   int compactBufferCount,
                                                   int stateCount,
                                                   int patternCount,
                                                   int eigenBufferCount,
                                                   int matrixBufferCount,
                                                   int categoryCount,
                                                   int scaleBufferCount,
                                                   int[] resourceList,
                                                   int resourceCount,
                                                   long preferenceFlags,
                                                   long requirementFlags,
                                                   BeagleInstanceDetails.ByReference returnInfo);

    private static native int beagleFinalizeInstance(int instance);

    private static native int beagleFinalize();

    private static native int beagleSetTipStates(int instance, int tipIndex, int[] inStates);

    private static native int beagleSetTipPartials(int instance, int tipIndex, double[] inPartials);

    private static native int beagleSetPartials(int instance, int bufferIndex, double[] outPartials);

    private static native int beagleGetPartials(int instance, int bufferIndex, int scaleIndex, double[] outPartials);

    private static native int beagleSetEigenDecomposition(int instance,
                                                          int eigenIndex,
                                                          double[] inEigenVectors,
                                                          double[] inInverseEigenVectors,
                                                          double[] inEigenValues);

    private static native int beagleSetStateFrequencies(int instance, int stateFrequenciesIndex, double[] inStateFrequencies);

    private static native int beagleSetCategoryWeights(int instance, int categoryWeightsIndex, double[] inCategoryWeights);

    private static native int beagleSetCategoryRates(int instance, double[] inCategoryRates);

    private static native int beagleSetPatternWeights(int instance, double[] inPatternWeights);

    private static native int beagleConvolveTransitionMatrices(int instance,
                                                               int[] firstIndices,
                                                               int[] secondIndices,
                                                               int[] resultIndices,
                                                               int matrixCount);

    private static native int beagleUpdateTransitionMatrices(int instance,
                                                             int eigenIndex,
                                                             int[] probabilityIndices,
                                                             int[] firstDerivativeIndices,
                                                             int[] secondDerivativeIndices,
                                                             double[] edgeLengths,
                                                             int count);

    private static native int beagleSetTransitionMatrix(int instance,
                                                        int matrixIndex,
                                                        double[] inMatrix,
                                                        double paddedValue);

    private static native int beagleGetTransitionMatrix(int instance, int matrixIndex, double[] outMatrix);

    private static native int beagleSetTransitionMatrices(int instance,
                                                          int[] matrixIndices,
                                                          double inMatrices,
                                                          double[] paddedValues,
                                                          int count);

    /**
     * A list of integer indices which specify a partial likelihoods operation.
     */
    public static class BeagleOperation extends Structure {

        private static class ByReference extends BeagleOperation implements Structure.ByReference {}

        /** index of first child partials buffer */
        private int destinationPartials;
        /** index of transition matrix of first partials child buffer */
        private int destinationScaleWrite;
        /** index of second child partials buffer */
        private int destinationScaleRead;
        /** index of transition matrix of second partials child buffer */
        private int child1Partials;
        /** index of destination, or parent, partials buffer */
        private int child1TransitionMatrix;
        /** index of scaling buffer to read from (if set to BEAGLE_OP_NONE then use of existing scale factors is disabled) */
        private int child2Partials;
        /** index of scaling buffer to write to (if set to BEAGLE_OP_NONE then calculation of new scalers is disabled) */
        private int child2TransitionMatrix;

        @Override
        protected List getFieldOrder() {
            return null;
        }
    }

    private static native int beagleUpdatePartials(int instance,
                                                   BeagleOperation[] operations,
                                                   int operationCount,
                                                   int cumulativeScaleIndex);

    private static native int beagleWaitForPartials(int instance, int[] destinationPartials, int destinationPartialsCount);

    private static native int beagleAccumulateScaleFactors(int instance, int[] scaleIndices, int count, int cumulativeScaleIndex);

    private static native int beagleRemoveScaleFactors(int instance, int[] scaleIndices, int count, int cumulativeScaleIndex);

    private static native int beagleResetScaleFactor(int instance, int cumulativeScaleIndex);

    private static native int beagleCopyScaleFactors(int instance, int destScalingIndex, int srcScalingIndex);

    private static native int beagleGetScaleFactors(int instance, int srcScalingIndex, double[] outScaleFactors);

    private static native int beagleCalculateRootLogLikelihoods(int instance,
                                                                int[] bufferIndices,
                                                                int[] categoryWeightsIndices,
                                                                int[] stateFrequenciesIndices,
                                                                int[] cumulativeScaleIndices,
                                                                int count,
                                                                DoubleByReference outSumLogLikelihood);

    private static native int beagleCalculateEdgeLogLikelihoods(int instance,
                                                                int[] parentBufferIndices,
                                                                int[] childBufferIndices,
                                                                int[] probabilityIndices,
                                                                int[] firstDerivativeIndices,
                                                                int[] secondDerivativeIndices,
                                                                int[] categoryWeightsIndices,
                                                                int[] stateFrequenciesIndices,
                                                                int[] cumulativeScaleIndices,
                                                                int count,
                                                                double[] outSumLogLikelihood,
                                                                double[] outSumFirstDerivative,
                                                                double[] outSumSecondDerivative);

    private static native int beagleGetSiteLogLikelihoods(int instance, double[] outLogLikelihoods);

    private static native int beagleGetSiteDerivatives(int instance, double[] outFirstDerivatives, double[] outSecondDerivatives);

    /**
     * Get version
     *
     * This function returns a pointer to a string with the library version number.
     *
     * @return A string with the version number
     */
    public static String getVersion() {
        return beagleGetVersion();
    }

    /**
     * Get citation
     *
     * This function returns a pointer to a string describing the version of the
     * library and how to cite it.
     *
     * @return A string describing the version of the library and how to cite it
     */
    public static String getCitation() {
        return beagleGetCitation();
    }

    /**
     * Get list of hardware resources
     *
     * This function returns a pointer to a BeagleResourceList struct, which includes
     * a BeagleResource array describing the available hardware resources.
     *
     * @return A list of hardware resources available to the library as a BeagleResourceList
     */
    public static List<BeagleResource> getResourceList() {
        return Arrays.asList(beagleGetResourceList().getList());
    }

    private final int instance;

    /**
     * Create a single instance
     *
     * This function creates a single instance of the BEAGLE library and can be called
     * multiple times to create multiple data partition instances each returning a unique
     * identifier.
     *
     * @param tipCount              Number of tip data elements (input)
     * @param partialsBufferCount   Number of partials buffers to create (input)
     * @param compactBufferCount    Number of compact state representation buffers to create (input)
     * @param stateCount            Number of states in the continuous-time Markov chain (input)
     * @param patternCount          Number of site patterns to be handled by the instance (input)
     * @param eigenBufferCount      Number of rate matrix eigen-decomposition, category weight, and
     *                               state frequency buffers to allocate (input)
     * @param matrixBufferCount     Number of transition probability matrix buffers (input)
     * @param categoryCount         Number of rate categories (input)
     * @param scaleBufferCount		Number of scale buffers to create, ignored for auto scale or always scale (input)
     * @param resourceList          List of potential resources on which this instance is allowed
     *                               (input, NULL implies no restriction)
     * @param resourceCount         Length of resourceList list (input)
     * @param preferenceFlags       Bit-flags indicating preferred implementation characteristics,
     *                               see BeagleFlags (input)
     * @param requirementFlags      Bit-flags indicating required implementation characteristics,
     *                               see BeagleFlags (input)
     * @param returnInfo            Pointer to return implementation and resource details
     *
     * @throws BeagleException
     *
     */
    public Beagle(int tipCount,
                  int partialsBufferCount,
                  int compactBufferCount,
                  int stateCount,
                  int patternCount,
                  int eigenBufferCount,
                  int matrixBufferCount,
                  int categoryCount,
                  int scaleBufferCount,
                  int[] resourceList,
                  int resourceCount,
                  long preferenceFlags,
                  long requirementFlags,
                  BeagleInstanceDetails.ByReference returnInfo) {
        instance = beagleCreateInstance(tipCount,
                partialsBufferCount,
                compactBufferCount,
                stateCount,
                patternCount,
                eigenBufferCount,
                matrixBufferCount,
                categoryCount,
                scaleBufferCount,
                resourceList,
                resourceCount,
                preferenceFlags,
                requirementFlags,
                returnInfo);
        if (instance < 0) throw new BeagleException("constructor", instance);
    }


    /**
     * Finalize this instance
     *
     * This function finalizes the instance by releasing allocated memory
     *
     * @throws BeagleException
     */
    public void finalizeInstance() {
        int errCode = beagleFinalizeInstance(instance);
        if (errCode != 0) throw new BeagleException("finalizeInstance", errCode);
    }

    /**
     * Finalize the library
     *
     * This function finalizes the library and releases all allocated memory.
     * This function is automatically called under GNU C via __attribute__ ((destructor)).
     *
     * @throws BeagleException
     */
    public static void finalizeLibrary() {
        int errCode = beagleFinalize();
        if (errCode != 0) throw new BeagleException("finalize", errCode);
    }

    /**
     * Set the compact state representation for tip node
     *
     * This function copies a compact state representation into an instance buffer.
     * Compact state representation is an array of states: 0 to stateCount - 1 (missing = stateCount).
     * The inStates array should be patternCount in length (replication across categoryCount is not
     * required).
     *
     * @param tipIndex  Index of destination compactBuffer (input)
     * @param inStates  Pointer to compact states (input)
     *
     * @throws BeagleException
     */
    public void setTipStates(int tipIndex, int[] inStates) {
        int errCode = beagleSetTipStates(instance, tipIndex, inStates);
        if (errCode != 0) throw new BeagleException("setTipStates", errCode);
    }

    /**
     * Set an instance partials buffer for tip node
     *
     * This function copies an array of partials into an instance buffer. The inPartials array should
     * be stateCount * patternCount in length. For most applications this will be used
     * to set the partial likelihoods for the observed states. Internally, the partials will be copied
     * categoryCount times.
     *
     * @param tipIndex      Index of destination partialsBuffer (input)
     * @param inPartials    Pointer to partials values to set (input)
     *
     * @throws BeagleException
     */
    public void setTipPartials(int tipIndex, double[] inPartials) {
        int errCode = beagleSetTipPartials(instance, tipIndex, inPartials);
        if (errCode != 0) throw new BeagleException("setTipPartials", errCode);
    }

    /**
     * Set an instance partials buffer
     *
     * This function copies an array of partials into an instance buffer. The inPartials array should
     * be stateCount * patternCount * categoryCount in length.
     *
     * @param bufferIndex   Index of destination partialsBuffer (input)
     * @param inPartials    Pointer to partials values to set (input)
     *
     * @throws BeagleException
     */
    public void setPartials(int bufferIndex, double[] inPartials) {
        int errCode = beagleSetPartials(instance, bufferIndex, inPartials);
        if (errCode != 0) throw new BeagleException("setPartials", errCode);
    }

    /**
     * Get partials from an instance buffer
     *
     * This function copies an instance buffer into the array outPartials. The outPartials array should
     * be stateCount * patternCount * categoryCount in length.
     *
     * @param bufferIndex   Index of source partialsBuffer (input)
     * @param scaleIndex  	Index of scaleBuffer to apply to partialsBuffer (input)
     * @param outPartials   Pointer to which to receive partialsBuffer (output)
     *
     * @throws BeagleException
     */
    public void getPartials(int bufferIndex, int scaleIndex, double[] outPartials) {
        int errCode = beagleGetPartials(instance, bufferIndex, scaleIndex, outPartials);
        if (errCode != 0) throw new BeagleException("getPartials", errCode);
    }

    /**
     * Set an eigen-decomposition buffer
     *
     * This function copies an eigen-decomposition into an instance buffer.
     *
     * @param eigenIndex            Index of eigen-decomposition buffer (input)
     * @param inEigenVectors        Flattened matrix (stateCount x stateCount) of eigen-vectors (input)
     * @param inInverseEigenVectors Flattened matrix (stateCount x stateCount) of inverse-eigen- vectors
     *                               (input)
     * @param inEigenValues         Vector of eigenvalues
     *
     * @throws BeagleException
     */
    public void setEigenDecomposition(int eigenIndex,
                                      double[] inEigenVectors,
                                      double[] inInverseEigenVectors,
                                      double[] inEigenValues) {
        int errCode = beagleSetEigenDecomposition(instance, eigenIndex, inEigenVectors, inInverseEigenVectors, inEigenValues);
        if (errCode != 0) throw new BeagleException("setEigenDecomposition", errCode);
    }

    /**
     * Set a state frequency buffer
     *
     * This function copies a state frequency array into an instance buffer.
     *
     * @param stateFrequenciesIndex Index of state frequencies buffer (input)
     * @param inStateFrequencies    State frequencies array (stateCount) (input)
     *
     * @throws BeagleException
     */
    public void setStateFrequencies(int stateFrequenciesIndex, double[] inStateFrequencies) {
        int errCode = beagleSetStateFrequencies(instance, stateFrequenciesIndex, inStateFrequencies);
        if (errCode != 0) throw new BeagleException("setStateFrequencies", errCode);
    }

    /**
     * Set a category weights buffer
     *
     * This function copies a category weights array into an instance buffer.
     *
     * @param categoryWeightsIndex  Index of category weights buffer (input)
     * @param inCategoryWeights     Category weights array (categoryCount) (input)
     *
     * @throws BeagleException
     */
    public void setCategoryWeights(int categoryWeightsIndex, double[] inCategoryWeights) {
        int errCode = beagleSetCategoryWeights(instance, categoryWeightsIndex, inCategoryWeights);
        if (errCode != 0) throw new BeagleException("setCategoryWeights", errCode);
    }

    /**
     * Set category rates
     *
     * This function sets the vector of category rates for an instance.
     *
     * @param inCategoryRates       Array containing categoryCount rate scalers (input)
     *
     * @throws BeagleException
     */
    public void setCategoryRates(double[] inCategoryRates) {
        int errCode = beagleSetCategoryRates(instance, inCategoryRates);
        if (errCode != 0) throw new BeagleException("setCategoryRates", errCode);
    }

    /**
     * Set pattern weights
     *
     * This function sets the vector of pattern weights for an instance.
     *
     * @param inPatternWeights      Array containing patternCount weights (input)
     *
     * @throws BeagleException
     */
    public void setPatternWeights(double[] inPatternWeights) {
        int errCode = beagleSetPatternWeights(instance, inPatternWeights);
        if (errCode != 0) throw new BeagleException("setPatternWeights", errCode);
    }

    /**
     * Convolve lists of transition probability matrices
     *
     * This function convolves two lists of transition probability matrices.
     *
     * @param firstIndices              List of indices of the first transition probability matrices
     *                                   to convolve (input)
     * @param secondIndices             List of indices of the second transition probability matrices
     *                                   to convolve (input)
     * @param resultIndices             List of indices of resulting transition probability matrices
     *                                   (input)
     */
    public void convolveTransitionMatrices(int[] firstIndices, int[] secondIndices, int[] resultIndices) {
        int errCode = beagleConvolveTransitionMatrices(instance, firstIndices, secondIndices, resultIndices, firstIndices.length);
        if (errCode != 0) throw new BeagleException("convolveTransitionMatrices", errCode);
    }

    /**
     * Calculate a list of transition probability matrices
     *
     * This function calculates a list of transition probabilities matrices and their first and
     * second derivatives (if requested).
     *
     * @param eigenIndex                Index of eigen-decomposition buffer (input)
     * @param probabilityIndices        List of indices of transition probability matrices to update
     *                                   (input)
     * @param firstDerivativeIndices    List of indices of first derivative matrices to update
     *                                   (input, NULL implies no calculation)
     * @param secondDerivativeIndices    List of indices of second derivative matrices to update
     *                                   (input, NULL implies no calculation)
     * @param edgeLengths               List of edge lengths with which to perform calculations (input)
     *
     * @throws BeagleException
     */
    public void updateTransitionMatrices(int eigenIndex,
                                         int[] probabilityIndices,
                                         int[] firstDerivativeIndices,
                                         int[] secondDerivativeIndices,
                                         double[] edgeLengths) {
        int errCode = beagleUpdateTransitionMatrices(instance,
                eigenIndex,
                probabilityIndices,
                firstDerivativeIndices,
                secondDerivativeIndices,
                edgeLengths,
                probabilityIndices.length);
        if (errCode != 0) throw new BeagleException("updateTransitionMatrices", errCode);
    }

    /**
     * Set a finite-time transition probability matrix
     *
     * This function copies a finite-time transition probability matrix into a matrix buffer. This function
     * is used when the application wishes to explicitly set the transition probability matrix rather than
     * using the beagleSetEigenDecomposition and beagleUpdateTransitionMatrices functions. The inMatrix array should be
     * of size stateCount * stateCount * categoryCount and will contain one matrix for each rate category.
     *
     * @param matrixIndex   Index of matrix buffer (input)
     * @param inMatrix      Pointer to source transition probability matrix (input)
     * @param paddedValue   Value to be used for padding for ambiguous states (e.g. 1 for probability matrices, 0 for derivative matrices) (input)
     *
     * @throws BeagleException
     */
    public void setTransitionMatrix(int matrixIndex, double[] inMatrix, double paddedValue) {
        int errCode = beagleSetTransitionMatrix(instance, matrixIndex, inMatrix, paddedValue);
        if (errCode != 0) throw new BeagleException("setTransitionMatrix", errCode);
    }

    /**
     * Get a finite-time transition probability matrix
     *
     * This function copies a finite-time transition matrix buffer into the array outMatrix. The
     * outMatrix array should be of size stateCount * stateCount * categoryCount and will be filled
     * with one matrix for each rate category.
     *
     * @param matrixIndex  Index of matrix buffer (input)
     * @param outMatrix    Pointer to destination transition probability matrix (output)
     *
     * @throws BeagleException
     */
    public void getTransitionMatrix(int matrixIndex, double[] outMatrix) {
        int errCode = beagleGetTransitionMatrix(instance, matrixIndex, outMatrix);
        if (errCode != 0) throw new BeagleException("getTransitionMatrix", errCode);
    }

    /**
     * Set multiple transition matrices
     *
     * This function copies multiple transition matrices into matrix buffers. This function
     * is used when the application wishes to explicitly set the transition matrices rather than
     * using the beagleSetEigenDecomposition and beagleUpdateTransitionMatrices functions. The inMatrices array should be
     * of size stateCount * stateCount * categoryCount * count.
     *
     * @param matrixIndices Indices of matrix buffers (input)
     * @param inMatrices    Pointer to source transition matrices (input)
     * @param paddedValues  Values to be used for padding for ambiguous states (e.g. 1 for probability matrices, 0 for derivative matrices) (input)
     *
     * @throws BeagleException
     */
    public void setTransitionMatrices(int[] matrixIndices, double inMatrices, double[] paddedValues) {
        int errCode = beagleSetTransitionMatrices(instance, matrixIndices, inMatrices, paddedValues, matrixIndices.length);
        if (errCode != 0) throw new BeagleException("setTransitionMatrices", errCode);
    }

    /**
     * Calculate or queue for calculation partials using a list of operations
     *
     * This function either calculates or queues for calculation a list partials. Implementations
     * supporting ASYNCH may queue these calculations while other implementations perform these
     * operations immediately and in order.
     *
     * @param operations                BeagleOperation list specifying operations (input)
     * @param cumulativeScaleIndex   	Index number of scaleBuffer to store accumulated factors (input)
     *
     * @throws BeagleException
     */
    public void updatePartials(List<BeagleOperation> operations, int cumulativeScaleIndex) {
        int count = operations.size();
        int errCode = beagleUpdatePartials(instance, operations.toArray(new BeagleOperation[count]), count, cumulativeScaleIndex);
        if (errCode != 0) throw new BeagleException("updatePartials", errCode);
    }

    /**
     * Block until all calculations that write to the specified partials have completed.
     *
     * This function is optional and only has to be called by clients that "recycle" partials.
     *
     * If used, this function must be called after an beagleUpdatePartials call and must refer to
     * indices of "destinationPartials" that were used in a previous beagleUpdatePartials
     * call.  The library will block until those partials have been calculated.
     *
     * @param destinationPartials       List of the indices of destinationPartials that must be
     *                                   calculated before the function returns
     *
     * @throws BeagleException
     */
    public void waitForPartials(int[] destinationPartials) {
        int errCode = beagleWaitForPartials(instance, destinationPartials, destinationPartials.length);
        if (errCode != 0) throw new BeagleException("waitForPartials", errCode);
    }

    /**
     * Accumulate scale factors
     *
     * This function adds (log) scale factors from a list of scaleBuffers to a cumulative scale
     * buffer. It is used to calculate the marginal scaling at a specific node for each site.
     *
     * @param scaleIndices            	List of scaleBuffers to add (input)
     * @param cumulativeScaleIndex      Index number of scaleBuffer to accumulate factors into (input)
     */
    public void accumulateScaleFactors(int[] scaleIndices, int cumulativeScaleIndex) {
        int errCode = beagleAccumulateScaleFactors(instance, scaleIndices, scaleIndices.length, cumulativeScaleIndex);
        if (errCode != 0) throw new BeagleException("accumulateScaleFactors", errCode);
    }

    /**
     * Remove scale factors
     *
     * This function removes (log) scale factors from a cumulative scale buffer. The
     * scale factors to be removed are indicated in a list of scaleBuffers.
     *
     * @param scaleIndices            	List of scaleBuffers to remove (input)
     * @param cumulativeScaleIndex    	Index number of scaleBuffer containing accumulated factors (input)
     */
    public void removeScaleFactors(int[] scaleIndices, int cumulativeScaleIndex) {
        int errCode = beagleRemoveScaleFactors(instance, scaleIndices, scaleIndices.length, cumulativeScaleIndex);
        if (errCode != 0) throw new BeagleException("removeScaleFactors", errCode);
    }

    /**
     * Reset scalefactors
     *
     * This function resets a cumulative scale buffer.
     *
     * @param cumulativeScaleIndex    	Index number of cumulative scaleBuffer (input)
     */
    public void resetScaleFactor(int cumulativeScaleIndex) {
        int errCode = beagleResetScaleFactor(instance, cumulativeScaleIndex);
        if (errCode != 0) throw new BeagleException("resetScaleFactor", errCode);
    }

    /**
     * Copy scale factors
     *
     * This function copies scale factors from one buffer to another.
     *
     * @param destScalingIndex          Destination scaleBuffer (input)
     * @param srcScalingIndex           Source scaleBuffer (input)
     */
    public void copyScaleFactors(int destScalingIndex, int srcScalingIndex) {
        int errCode = beagleCopyScaleFactors(instance, destScalingIndex, srcScalingIndex);
        if (errCode != 0) throw new BeagleException("copyScaleFactors", errCode);
    }

    /**
     * Get scale factors
     *
     * This function retrieves a buffer of scale factors.
     *
     * @param srcScalingIndex           Source scaleBuffer (input)
     * @param outScaleFactors           Pointer to which to receive scaleFactors (output)
     */
    public void getScaleFactors(int srcScalingIndex, double[] outScaleFactors) {
        int errCode = beagleGetScaleFactors(instance, srcScalingIndex, outScaleFactors);
        if (errCode != 0) throw new BeagleException("getScaleFactors", errCode);
    }

    /**
     * Calculate site log likelihoods at a root node
     *
     * This function integrates a list of partials at a node with respect to a set of partials-weights
     * and state frequencies to return the log likelihood sum
     *
     * @param bufferIndices            List of partialsBuffer indices to integrate (input)
     * @param categoryWeightsIndices   List of weights to apply to each partialsBuffer (input). There
     *                                  should be one categoryCount sized set for each of
     *                                  parentBufferIndices
     * @param stateFrequenciesIndices  List of state frequencies for each partialsBuffer (input). There
     *                                  should be one set for each of parentBufferIndices
     * @param cumulativeScaleIndices   List of scaleBuffers containing accumulated factors to apply to
     *                                  each partialsBuffer (input). There should be one index for each
     *                                  of parentBufferIndices
     *
     * @return Resulting log likelihood
     *
     * @throws BeagleException
     */
    public double calculateRootLogLikelihoods(int[] bufferIndices,
                                            int[] categoryWeightsIndices,
                                            int[] stateFrequenciesIndices,
                                            int[] cumulativeScaleIndices) {
        DoubleByReference outSumLogLikelihood = new DoubleByReference();
        int errCode = beagleCalculateRootLogLikelihoods(instance,
                bufferIndices,
                categoryWeightsIndices,
                stateFrequenciesIndices,
                cumulativeScaleIndices,
                bufferIndices.length,
                outSumLogLikelihood);
        if (errCode != 0) throw new BeagleException("calculateRootLogLikelihoods", errCode);
        return outSumLogLikelihood.getValue();
    }

    /**
     * Calculate site log likelihoods and derivatives along an edge
     *
     * This function integrates a list of partials at a parent and child node with respect
     * to a set of partials-weights and state frequencies to return the log likelihood
     * and first and second derivative sums
     *
     * @param parentBufferIndices       List of indices of parent partialsBuffers (input)
     * @param childBufferIndices        List of indices of child partialsBuffers (input)
     * @param probabilityIndices        List indices of transition probability matrices for this edge
     *                                   (input)
     * @param firstDerivativeIndices    List indices of first derivative matrices (input)
     * @param secondDerivativeIndices   List indices of second derivative matrices (input)
     * @param categoryWeightsIndices    List of weights to apply to each partialsBuffer (input)
     * @param stateFrequenciesIndices   List of state frequencies for each partialsBuffer (input). There
     *                                   should be one set for each of parentBufferIndices
     * @param cumulativeScaleIndices    List of scaleBuffers containing accumulated factors to apply to
     *                                   each partialsBuffer (input). There should be one index for each
     *                                   of parentBufferIndices
     * @param outSumLogLikelihood       Pointer to destination for resulting log likelihood (output)
     * @param outSumFirstDerivative     Pointer to destination for resulting first derivative (output)
     * @param outSumSecondDerivative    Pointer to destination for resulting second derivative (output)
     *
     * @throws BeagleException
     */
    public void calculateEdgeLogLikelihoods(int[] parentBufferIndices,
                                            int[] childBufferIndices,
                                            int[] probabilityIndices,
                                            int[] firstDerivativeIndices,
                                            int[] secondDerivativeIndices,
                                            int[] categoryWeightsIndices,
                                            int[] stateFrequenciesIndices,
                                            int[] cumulativeScaleIndices,
                                            double[] outSumLogLikelihood,
                                            double[] outSumFirstDerivative,
                                            double[] outSumSecondDerivative) {

        int errCode = beagleCalculateEdgeLogLikelihoods(instance,
                parentBufferIndices,
                childBufferIndices,
                probabilityIndices,
                firstDerivativeIndices,
                secondDerivativeIndices,
                categoryWeightsIndices,
                stateFrequenciesIndices,
                cumulativeScaleIndices,
                parentBufferIndices.length,
                outSumLogLikelihood,
                outSumFirstDerivative,
                outSumSecondDerivative);
        if (errCode != 0) throw new BeagleException("calculateEdgeLogLikelihoods", errCode);
    }

    /**
     * Get site log likelihoods for last beagleCalculateRootLogLikelihoods or
     *         beagleCalculateEdgeLogLikelihoods call
     *
     * This function returns the log likelihoods for each site
     *
     * @param outLogLikelihoods      Pointer to destination for resulting log likelihoods (output)
     *
     * @throws BeagleException
     */
    public void getSiteLogLikelihoods(double[] outLogLikelihoods) {
        int errCode = beagleGetSiteLogLikelihoods(instance, outLogLikelihoods);
        if (errCode != 0) throw new BeagleException("getSiteLogLikelihoods", errCode);
    }

    /**
     * Get site derivatives for last beagleCalculateEdgeLogLikelihoods call
     *
     * This function returns the derivatives for each site
     *
     * @param outFirstDerivatives    Pointer to destination for resulting first derivatives (output)
     * @param outSecondDerivatives   Pointer to destination for resulting second derivatives (output)
     *
     * @throws BeagleException
     */
    public void getSiteDerivatives(double[] outFirstDerivatives, double[] outSecondDerivatives) {
        int errCode = beagleGetSiteDerivatives(instance, outFirstDerivatives, outSecondDerivatives);
        if (errCode != 0) throw new BeagleException("getSiteDerivatives", errCode);
    }

}
