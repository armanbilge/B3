/*
 * TreeLikelihood.java
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

import beast.evolution.alignment.AscertainedSitePatterns;
import beast.evolution.alignment.PatternList;
import beast.evolution.alignment.SitePatterns;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evolution.util.Taxon;
import beast.evolution.util.TaxonList;
import beast.evomodel.branchratemodel.BranchRateModel;
import beast.evomodel.branchratemodel.DefaultBranchRateModel;
import beast.evomodel.sitemodel.SiteModel;
import beast.evomodel.substmodel.FrequencyModel;
import beast.evomodel.tree.TipStatesModel;
import beast.evomodel.tree.TreeModel;
import beast.inference.model.CompoundParameter;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Statistic;
import beast.inference.model.Variable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.logging.Logger;

/**
 * TreeLikelihoodModel - implements a Likelihood Function for sequences on a tree.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: TreeLikelihood.java,v 1.31 2006/08/30 16:02:42 rambaut Exp $
 */

public class TreeLikelihood extends AbstractTreeLikelihood {

    public static final String TREE_LIKELIHOOD = "treeLikelihood";
    private static final boolean DEBUG = false;

    /**
     * Constructor.
     */
    public TreeLikelihood(PatternList patternList,
                          TreeModel treeModel,
                          SiteModel siteModel,
                          BranchRateModel branchRateModel,
                          TipStatesModel tipStatesModel,
                          boolean useAmbiguities,
                          boolean allowMissingTaxa,
                          boolean storePartials,
                          boolean forceJavaCore,
                          boolean forceRescaling) {

        super(TREE_LIKELIHOOD, patternList, treeModel);

        this.useAmbiguities = useAmbiguities;
        this.storePartials = storePartials;

        try {
            this.siteModel = siteModel;
            addModel(siteModel);

            this.frequencyModel = siteModel.getFrequencyModel();
            addModel(frequencyModel);

            this.tipStatesModel = tipStatesModel;

            integrateAcrossCategories = siteModel.integrateAcrossCategories();

            this.categoryCount = siteModel.getCategoryCount();

            final Logger logger = Logger.getLogger("beast.evomodel");
            String coreName = "Java general";
            if (integrateAcrossCategories) {

                final DataType dataType = patternList.getDataType();

                if (dataType instanceof beast.evolution.datatype.Nucleotides) {

                    if (!forceJavaCore && NativeNucleotideLikelihoodCore.isAvailable()) {
                        coreName = "native nucleotide";
                        likelihoodCore = new NativeNucleotideLikelihoodCore();
                    } else {
                        coreName = "Java nucleotide";
                        likelihoodCore = new NucleotideLikelihoodCore();
                    }

                } else if (dataType instanceof beast.evolution.datatype.AminoAcids) {
                    if (!forceJavaCore && NativeAminoAcidLikelihoodCore.isAvailable()) {
                        coreName = "native amino acid";
                        likelihoodCore = new NativeAminoAcidLikelihoodCore();
                    } else {
                        coreName = "Java amino acid";
                        likelihoodCore = new AminoAcidLikelihoodCore();
                    }

                    // The codon core was out of date and did nothing more than the general core...
//                } else if (dataType instanceof beast.evolution.datatype.Codons) {
//                    if (!forceJavaCore && NativeGeneralLikelihoodCore.isAvailable()) {
//                        coreName = "native general";
//                        likelihoodCore = new NativeGeneralLikelihoodCore(patternList.getStateCount());
//                    } else {
//                        coreName = "Java general";
//                        likelihoodCore = new GeneralLikelihoodCore(patternList.getStateCount());
//                    }
//                    useAmbiguities = true;
                } else {
                    if (!forceJavaCore && NativeGeneralLikelihoodCore.isAvailable()) {
                        coreName = "native general";
                        likelihoodCore = new NativeGeneralLikelihoodCore(patternList.getStateCount());
                    } else {
                        coreName = "Java general";
                        likelihoodCore = new GeneralLikelihoodCore(patternList.getStateCount());
                    }
                }
            } else {
                likelihoodCore = new GeneralLikelihoodCore(patternList.getStateCount());
            }
            {
              final String id = getId();
              logger.info("TreeLikelihood(" + ((id != null) ? id : treeModel.getId()) + ") using " + coreName + " likelihood core");

              logger.info("  " + (useAmbiguities ? "Using" : "Ignoring") + " ambiguities in tree likelihood.");
              logger.info("  With " + patternList.getPatternCount() + " unique site patterns.");
            }

            if (branchRateModel != null) {
                this.branchRateModel = branchRateModel;
                logger.info("Branch rate model used: " + branchRateModel.getModelName());
            } else {
                this.branchRateModel = new DefaultBranchRateModel();
            }
            addModel(this.branchRateModel);

            probabilities = new double[stateCount * stateCount];

            storedMatrices = new double[categoryCount][stateCount * stateCount];

            likelihoodCore.initialize(nodeCount, patternCount, categoryCount, integrateAcrossCategories);

            int extNodeCount = treeModel.getExternalNodeCount();
            int intNodeCount = treeModel.getInternalNodeCount();

            if (tipStatesModel != null) {
                tipStatesModel.setTree(treeModel);

                tipPartials = new double[patternCount * stateCount];

                for (int i = 0; i < extNodeCount; i++) {
                    // Find the id of tip i in the patternList
                    String id = treeModel.getTaxonId(i);
                    int index = patternList.getTaxonIndex(id);

                    if (index == -1) {
                        throw new TaxonList.MissingTaxonException("Taxon, " + id + ", in tree, " + treeModel.getId() +
                                ", is not found in patternList, " + patternList.getId());
                    }

                    tipStatesModel.setStates(patternList, index, i, id);
                    likelihoodCore.createNodePartials(i);
                }

                addModel(tipStatesModel);
            } else {
                for (int i = 0; i < extNodeCount; i++) {
                    // Find the id of tip i in the patternList
                    String id = treeModel.getTaxonId(i);
                    int index = patternList.getTaxonIndex(id);

                    if (index == -1) {
                        if (!allowMissingTaxa) {
                            throw new TaxonList.MissingTaxonException("Taxon, " + id + ", in tree, " + treeModel.getId() +
                                    ", is not found in patternList, " + patternList.getId());
                        }
                        if (useAmbiguities) {
                            setMissingPartials(likelihoodCore, i);
                        } else {
                            setMissingStates(likelihoodCore, i);
                        }
                    } else {
                        if (useAmbiguities) {
                            setPartials(likelihoodCore, patternList, categoryCount, index, i);
                        } else {
                            setStates(likelihoodCore, patternList, index, i);
                        }
                    }
                }
            }
            for (int i = 0; i < intNodeCount; i++) {
                likelihoodCore.createNodePartials(extNodeCount + i);
            }

            if (forceRescaling) {
                likelihoodCore.setUseScaling(true);
                logger.info("  Forcing use of partials rescaling.");
            }

        } catch (TaxonList.MissingTaxonException mte) {
            throw new RuntimeException(mte.toString());
        }

        addStatistic(new SiteLikelihoodsStatistic());
    }

    public final LikelihoodCore getLikelihoodCore() {
        return likelihoodCore;
    }

    // **************************************************************
    // ModelListener IMPLEMENTATION
    // **************************************************************

    /**
     * Handles model changed events from the submodels.
     */
    protected void handleModelChangedEvent(Model model, Object object, int index) {

        if (model == treeModel) {
            if (object instanceof TreeModel.TreeChangedEvent) {

                if (((TreeModel.TreeChangedEvent) object).isNodeChanged()) {
                    // If a node event occurs the node and its two child nodes
                    // are flagged for updating (this will result in everything
                    // above being updated as well. Node events occur when a node
                    // is added to a branch, removed from a branch or its height or
                    // rate changes.
                    updateNodeAndChildren(((TreeModel.TreeChangedEvent) object).getNode());

                } else if (((TreeModel.TreeChangedEvent) object).isTreeChanged()) {
                    // Full tree events result in a complete updating of the tree likelihood
                    updateAllNodes();
                } else {
                    // Other event types are ignored (probably trait changes).
                    //System.err.println("Another tree event has occured (possibly a trait change).");
                }
            }

        } else if (model == branchRateModel) {
            if (index == -1) {
                updateAllNodes();
            } else {
                if (DEBUG) {
                if (index >= treeModel.getNodeCount()) {
                    throw new IllegalArgumentException("Node index out of bounds");
                }
                }
                updateNode(treeModel.getNode(index));
            }

        } else if (model == frequencyModel) {

            updateAllNodes();

        } else if (model == tipStatesModel) {
        	if(object instanceof Taxon)
        	{
        		for(int i=0; i<treeModel.getNodeCount(); i++)
        			if(treeModel.getNodeTaxon(treeModel.getNode(i))!=null && treeModel.getNodeTaxon(treeModel.getNode(i)).getId().equalsIgnoreCase(((Taxon)object).getId()))
        				updateNode(treeModel.getNode(i));
        	}else
        		updateAllNodes();

        } else if (model instanceof SiteModel) {

            updateAllNodes();

        } else {

            throw new RuntimeException("Unknown componentChangedEvent");
        }

        super.handleModelChangedEvent(model, object, index);
    }

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

    /**
     * Stores the additional state other than model components
     */
    protected void storeState() {

        if (storePartials) {
            likelihoodCore.storeState();
        }
        super.storeState();

    }

    /**
     * Restore the additional stored state
     */
    protected void restoreState() {

        if (storePartials) {
            likelihoodCore.restoreState();
        } else {
            updateAllNodes();
        }

        super.restoreState();

    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    protected double calculateLogLikelihood() {

        if (patternLogLikelihoods == null) {
            patternLogLikelihoods = new double[patternCount];
        }

        if (!integrateAcrossCategories) {
            if (siteCategories == null) {
                siteCategories = new int[patternCount];
            }
            for (int i = 0; i < patternCount; i++) {
                siteCategories[i] = siteModel.getCategoryOfSite(i);
            }
        }

        if (tipStatesModel != null) {
            int extNodeCount = treeModel.getExternalNodeCount();
            for (int index = 0; index < extNodeCount; index++) {
                if (updateNode[index]) {
                    likelihoodCore.setNodePartialsForUpdate(index);
                    tipStatesModel.getTipPartials(index, tipPartials);
                    likelihoodCore.setCurrentNodePartials(index, tipPartials);
                }
            }
        }


        final NodeRef root = treeModel.getRoot();
        traverse(treeModel, root);

        double logL = 0.0;
        double ascertainmentCorrection = getAscertainmentCorrection(patternLogLikelihoods);
        for (int i = 0; i < patternCount; i++) {
            logL += (patternLogLikelihoods[i] - ascertainmentCorrection) * patternWeights[i];
        }

        if (logL == Double.NEGATIVE_INFINITY) {
            Logger.getLogger("beast.evomodel").info("TreeLikelihood, " + this.getId() + ", turning on partial likelihood scaling to avoid precision loss");

            // We probably had an underflow... turn on scaling
            likelihoodCore.setUseScaling(true);

            // and try again...
            updateAllNodes();
            updateAllPatterns();
            traverse(treeModel, root);

            logL = 0.0;
            ascertainmentCorrection = getAscertainmentCorrection(patternLogLikelihoods);
            for (int i = 0; i < patternCount; i++) {
                logL += (patternLogLikelihoods[i] - ascertainmentCorrection) * patternWeights[i];
            }
        }

        //********************************************************************
        // after traverse all nodes and patterns have been updated --
        //so change flags to reflect this.
        for (int i = 0; i < nodeCount; i++) {
            updateNode[i] = false;
        }
        //********************************************************************

        return logL;
    }

    public double differentiate(Variable<Double> var, int index) {

        if (!useAmbiguities)
            throw new UnsupportedOperationException("Differentiation unsupported when not using ambiguities!");
        if (categoryCount > 1)
            throw new UnsupportedOperationException("Differentiation unsupported when categoryCount > 1!");

        if (var instanceof CompoundParameter)
            var = ((CompoundParameter) var).getMaskedParameter(index);
        if (var instanceof Parameter) {
            final Parameter param = (Parameter) var;
            final NodeRef node = treeModel.getNodeOfParameter(param);
            if (node != null && treeModel.isHeightParameterForNode(node, param)) {
                getLogLikelihood();
                if (treeModel.isExternal(node)) {
                    if (!externalDerivativesKnown)
                        differentiateExternalNodes();
                } else {
                    if (!internalDerivativesKnown)
                        differentiateInternalNodes();
                }
                return derivatives[node.getNumber()];
            }
        }

        return super.differentiate(var, index);
    }

    protected double differentiateRespectingNode(final NodeRef node) {

        double deriv = 0.0;

        final double[][] rateMatrix = siteModel.getSubstitutionModel().getRateMatrix();
        if (!treeModel.isRoot(node)) {
            final int nodeNum = node.getNumber();
            final double rate = branchRateModel.getBranchRate(treeModel, node);
            for (int i = 0; i < categoryCount; i++) {
                likelihoodCore.getNodeMatrix(nodeNum, i, storedMatrices[i]);
                multiply(rate, rateMatrix, storedMatrices[i]);
                likelihoodCore.setNodeMatrix(nodeNum, i, probabilities);
            }
            updateNode[nodeNum] = true;
            deriv -= calculateDifferentiatedLogLikelihood();
            for (int i = 0; i < categoryCount; i++)
                likelihoodCore.setNodeMatrix(nodeNum, i, storedMatrices[i]);
        }

        if (!treeModel.isExternal(node)) {

            {
                final NodeRef child = treeModel.getChild(node, 0);
                final int nodeNum = child.getNumber();
                final double rate = branchRateModel.getBranchRate(treeModel, child);
                for (int i = 0; i < categoryCount; i++) {
                    likelihoodCore.getNodeMatrix(nodeNum, i, storedMatrices[i]);
                    multiply(rate, rateMatrix, storedMatrices[i]);
                    likelihoodCore.setNodeMatrix(nodeNum, i, probabilities);
                }
                updateNode[nodeNum] = true;
                deriv += calculateDifferentiatedLogLikelihood();
                for (int i = 0; i < categoryCount; i++)
                    likelihoodCore.setNodeMatrix(nodeNum, i, storedMatrices[i]);
            }

            {
                final NodeRef child = treeModel.getChild(node, 1);
                final int nodeNum = child.getNumber();
                final double rate = branchRateModel.getBranchRate(treeModel, child);
                for (int i = 0; i < categoryCount; i++) {
                    likelihoodCore.getNodeMatrix(nodeNum, i, storedMatrices[i]);
                    multiply(rate, rateMatrix, storedMatrices[i]);
                    likelihoodCore.setNodeMatrix(nodeNum, i, probabilities);
                }
                updateNode[nodeNum] = true;
                deriv += calculateDifferentiatedLogLikelihood();
                for (int i = 0; i < categoryCount; i++)
                    likelihoodCore.setNodeMatrix(nodeNum, i, storedMatrices[i]);
            }

        }

        updateNode[node.getNumber()] = !treeModel.isRoot(node);
        if (!treeModel.isExternal(node)) {
            updateNode[treeModel.getChild(node, 0).getNumber()] = true;
            updateNode[treeModel.getChild(node, 1).getNumber()] = true;
        }

        return deriv;
    }

    protected double calculateDifferentiatedLogLikelihood() {

        if (differentiatedPatternLogLikelihoods == null)
            differentiatedPatternLogLikelihoods = new double[patternCount];

        final NodeRef root = treeModel.getRoot();
        traverseDifferentiate(treeModel, root);

        double deriv = 0.0;
        for (int i = 0; i < patternCount; i++) {
            deriv += differentiatedPatternLogLikelihoods[i] / Math.exp(patternLogLikelihoods[i]) * patternWeights[i];
        }

//        if (logL == Double.NEGATIVE_INFINITY) {
//            Logger.getLogger("beast.evomodel").info("TreeLikelihood, " + this.getId() + ", turning on partial likelihood scaling to avoid precision loss");
//
//            // We probably had an underflow... turn on scaling
//            likelihoodCore.setUseScaling(true);
//
//            // and try again...
//            updateAllNodes();
//            updateAllPatterns();
//            traverseDifferentiate(treeModel, root);
//
//            logL = 0.0;
//            ascertainmentCorrection = getAscertainmentCorrection(patternLogLikelihoods);
//            for (int i = 0; i < patternCount; i++) {
//                logL += (patternLogLikelihoods[i] - ascertainmentCorrection) * patternWeights[i];
//            }
//        }

        //********************************************************************
        // after traverse all nodes and patterns have been updated --
        //so change flags to reflect this.
        for (int i = 0; i < nodeCount; i++) {
            updateNode[i] = false;
        }
        //********************************************************************

        return deriv;
    }

    protected void multiply(final double c, final double[][] a, final double[] b) {
        for (int i = 0; i < stateCount; ++i) {
            for (int j = 0; j < stateCount; ++j) {
                final int n = i * stateCount + j;
                probabilities[n] = 0;
                for (int k = 0; k < stateCount; ++k) {
                    probabilities[n] += a[i][k] * b[k * stateCount + j];
                }
                probabilities[n] *= c;
            }
        }
    }

    public double[] getPatternLogLikelihoods() {
        getLogLikelihood(); // Ensure likelihood is up-to-date
        double ascertainmentCorrection = getAscertainmentCorrection(patternLogLikelihoods);
        double[] out = new double[patternCount];
        for (int i = 0; i < patternCount; i++) {
            if (patternWeights[i] > 0) {
                out[i] = (patternLogLikelihoods[i] - ascertainmentCorrection) * patternWeights[i];
            } else {
                out[i] = Double.NEGATIVE_INFINITY;
            }
        }
        return out;
    }

    /* Calculate ascertainment correction if working off of AscertainedSitePatterns
    @param patternLogProbs log pattern probabilities
    @return the log total probability for a pattern.
    */
    protected double getAscertainmentCorrection(double[] patternLogProbs) {
        if (patternList instanceof AscertainedSitePatterns) {
            return ((AscertainedSitePatterns) patternList).getAscertainmentCorrection(patternLogProbs);
        } else {
            return 0.0;
        }
    }

    /**
     * Check whether the scaling is still required. If the sum of all the logScalingFactors
     * is zero then we simply turn off the useScaling flag. This will speed up the likelihood
     * calculations when scaling is not required.
     */
    public void checkScaling() {
//	    if (useScaling) {
//	        if (scalingCheckCount % 1000 == 0) {
//	            double totalScalingFactor = 0.0;
//	            for (int i = 0; i < nodeCount; i++) {
//	                for (int j = 0; j < patternCount; j++) {
//	                    totalScalingFactor += scalingFactors[currentPartialsIndices[i]][i][j];
//	                }
//	            }
//	            useScaling = totalScalingFactor < 0.0;
//	            Logger.getLogger("beast.evomodel").info("LikelihoodCore total log scaling factor: " + totalScalingFactor);
//	            if (!useScaling) {
//	                Logger.getLogger("beast.evomodel").info("LikelihoodCore scaling turned off.");
//	            }
//	        }
//	        scalingCheckCount++;
//	    }
    }



    /**
     * Traverse the tree calculating partial likelihoods.
     *
     * @return whether the partials for this node were recalculated.
     */
    protected boolean traverse(Tree tree, NodeRef node) {

        boolean update = false;

        int nodeNum = node.getNumber();

        NodeRef parent = tree.getParent(node);

        // First update the transition probability matrix(ices) for this branch
        if (parent != null && updateNode[nodeNum]) {

            final double branchRate = branchRateModel.getBranchRate(tree, node);

            // Get the operational time of the branch
            final double branchTime = branchRate * (tree.getNodeHeight(parent) - tree.getNodeHeight(node));

            if (branchTime < 0.0) {
                throw new RuntimeException("Negative branch length: " + branchTime);
            }

            likelihoodCore.setNodeMatrixForUpdate(nodeNum);

            for (int i = 0; i < categoryCount; i++) {

                double branchLength = siteModel.getRateForCategory(i) * branchTime;
                siteModel.getSubstitutionModel().getTransitionProbabilities(branchLength, probabilities);
                likelihoodCore.setNodeMatrix(nodeNum, i, probabilities);
            }

            update = true;
        }

        // If the node is internal, update the partial likelihoods.
        if (!tree.isExternal(node)) {

            // Traverse down the two child nodes
            NodeRef child1 = tree.getChild(node, 0);
            final boolean update1 = traverse(tree, child1);

            NodeRef child2 = tree.getChild(node, 1);
            final boolean update2 = traverse(tree, child2);

            // If either child node was updated then update this node too
            if (update1 || update2) {

                final int childNum1 = child1.getNumber();
                final int childNum2 = child2.getNumber();

                likelihoodCore.setNodePartialsForUpdate(nodeNum);

                if (integrateAcrossCategories) {
                    likelihoodCore.calculatePartials(childNum1, childNum2, nodeNum);
                } else {
                    likelihoodCore.calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
                }

                if (COUNT_TOTAL_OPERATIONS) {
                    totalOperationCount ++;
                }

                if (parent == null) {
                    // No parent this is the root of the tree -
                    // calculate the pattern likelihoods
                    double[] frequencies = frequencyModel.getFrequencies();

                    double[] partials = getRootPartials();

                    likelihoodCore.calculateLogLikelihoods(partials, frequencies, patternLogLikelihoods);
                }

                update = true;
            }
        }

        return update;

    }

    /**
     * Traverse the tree calculating partial likelihoods.
     *
     * @return whether the partials for this node were recalculated.
     */
    protected boolean traverseDifferentiate(Tree tree, NodeRef node) {

        int nodeNum = node.getNumber();

        NodeRef parent = tree.getParent(node);

        boolean update = parent != null && updateNode[nodeNum];

        // If the node is internal, update the partial likelihoods.
        if (!tree.isExternal(node)) {

            // Traverse down the two child nodes
            NodeRef child1 = tree.getChild(node, 0);
            final boolean update1 = traverseDifferentiate(tree, child1);

            NodeRef child2 = tree.getChild(node, 1);
            final boolean update2 = traverseDifferentiate(tree, child2);

            // If either child node was updated then update this node too
            if (update1 || update2) {

                final int childNum1 = child1.getNumber();
                final int childNum2 = child2.getNumber();

//                likelihoodCore.setNodePartialsForUpdate(nodeNum);

                if (integrateAcrossCategories) {
                    likelihoodCore.calculatePartials(childNum1, childNum2, nodeNum);
                } else {
                    likelihoodCore.calculatePartials(childNum1, childNum2, nodeNum, siteCategories);
                }

                if (COUNT_TOTAL_OPERATIONS) {
                    totalOperationCount++;
                }

                if (parent == null) {
                    // No parent this is the root of the tree -
                    // calculate the pattern likelihoods
                    double[] frequencies = frequencyModel.getFrequencies();

                    double[] partials = getRootPartials();

                    likelihoodCore.calculateDifferentiatedLogLikelihoods(partials, frequencies, differentiatedPatternLogLikelihoods);
                }

                update = true;
            }
        }

        return update;

    }

    public final double[] getRootPartials() {
        if (rootPartials == null) {
            rootPartials = new double[patternCount * stateCount];
        }

        int nodeNum = treeModel.getRoot().getNumber();
        if (integrateAcrossCategories) {

            // moved this call to here, because non-integrating siteModels don't need to support it - AD
            double[] proportions = siteModel.getCategoryProportions();
            likelihoodCore.integratePartials(nodeNum, proportions, rootPartials);
        } else {
            likelihoodCore.getPartials(nodeNum, rootPartials);
        }

        return rootPartials;
    }

    /**
     * the root partial likelihoods (a temporary array that is used
     * to fetch the partials - it should not be examined directly -
     * use getRootPartials() instead).
     */
    private double[] rootPartials = null;

    public class SiteLikelihoodsStatistic extends Statistic.Abstract {

        public SiteLikelihoodsStatistic() {
            super("siteLikelihoods");
        }

        public int getDimension() {
            if (patternList instanceof SitePatterns) {
                return ((SitePatterns)patternList).getSiteCount();
            } else {
                return patternList.getPatternCount();
            }
        }

        public String getDimensionName(int dim) {
            return getTreeModel().getId() + "site-" + dim;
        }

        public double getStatisticValue(int i) {

            if (patternList instanceof SitePatterns) {
                int index = ((SitePatterns)patternList).getPatternIndex(i);
                if( index >= 0 ) {
                    return patternLogLikelihoods[index] / patternWeights[index];
                } else {
                    return 0.0;
                }
            } else {
                return patternList.getPatternCount();
            }
        }

    }

    // **************************************************************
    // INSTANCE VARIABLES
    // **************************************************************

    /**
     * the frequency model for these sites
     */
    protected final FrequencyModel frequencyModel;

    /**
     * the site model for these sites
     */
    protected final SiteModel siteModel;

    /**
     * the branch rate model
     */
    protected final BranchRateModel branchRateModel;

    /**
     * the tip partials model
     */
    private final TipStatesModel tipStatesModel;

    private final boolean useAmbiguities;

    private final boolean storePartials;

    protected final boolean integrateAcrossCategories;

    /**
     * the categories for each site
     */
    protected int[] siteCategories = null;


    /**
     * the pattern likelihoods
     */
    protected double[] patternLogLikelihoods = null;

    protected double[] differentiatedPatternLogLikelihoods;

    /**
     * the number of rate categories
     */
    protected int categoryCount;

    /**
     * an array used to transfer transition probabilities
     */
    protected double[] probabilities;

    protected double[][] storedMatrices;

    /**
     * an array used to transfer tip partials
     */
    protected double[] tipPartials;

    /**
     * the LikelihoodCore
     */
    protected LikelihoodCore likelihoodCore;

    public static final XMLObjectParser<TreeLikelihood> PARSER = new AbstractXMLObjectParser<TreeLikelihood>() {
        public static final String USE_AMBIGUITIES = "useAmbiguities";
        public static final String ALLOW_MISSING_TAXA = "allowMissingTaxa";
        public static final String STORE_PARTIALS = "storePartials";
        public static final String FORCE_JAVA_CORE = "forceJavaCore";
        public static final String FORCE_RESCALING = "forceRescaling";


        public String getParserName() {
            return TREE_LIKELIHOOD;
        }

        public TreeLikelihood parseXMLObject(XMLObject xo) throws XMLParseException {

            boolean useAmbiguities = xo.getAttribute(USE_AMBIGUITIES, false);
            boolean allowMissingTaxa = xo.getAttribute(ALLOW_MISSING_TAXA, false);
            boolean storePartials = xo.getAttribute(STORE_PARTIALS, true);
            boolean forceJavaCore = xo.getAttribute(FORCE_JAVA_CORE, false);

            if (Boolean.valueOf(System.getProperty("java.only"))) {
                forceJavaCore = true;
            }

            PatternList patternList = (PatternList) xo.getChild(PatternList.class);
            TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            SiteModel siteModel = (SiteModel) xo.getChild(SiteModel.class);

            BranchRateModel branchRateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);

            TipStatesModel tipStatesModel = (TipStatesModel) xo.getChild(TipStatesModel.class);
            if (tipStatesModel != null && tipStatesModel.getPatternList() != null) {
                throw new XMLParseException("The same sequence error model cannot be used for multiple partitions");
            }
            if (tipStatesModel != null && tipStatesModel.getModelType() == TipStatesModel.Type.STATES) {
                throw new XMLParseException("The state emitting TipStateModel requires BEAGLE");
            }


            boolean forceRescaling = xo.getAttribute(FORCE_RESCALING, false);

            return new TreeLikelihood(
                    patternList,
                    treeModel,
                    siteModel,
                    branchRateModel,
                    tipStatesModel,
                    useAmbiguities, allowMissingTaxa, storePartials, forceJavaCore, forceRescaling);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents the likelihood of a patternlist on a tree given the site model.";
        }

        public Class getReturnType() {
            return TreeLikelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newBooleanRule(USE_AMBIGUITIES, true),
                AttributeRule.newBooleanRule(ALLOW_MISSING_TAXA, true),
                AttributeRule.newBooleanRule(STORE_PARTIALS, true),
                AttributeRule.newBooleanRule(FORCE_JAVA_CORE, true),
                AttributeRule.newBooleanRule(FORCE_RESCALING, true),
                new ElementRule(PatternList.class),
                new ElementRule(TreeModel.class),
                new ElementRule(SiteModel.class),
                new ElementRule(BranchRateModel.class, true),
                new ElementRule(TipStatesModel.class, true)
        };
    };
}
