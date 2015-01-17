/*
 * DiscretizedBranchRates.java
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

package beast.evomodel.branchratemodel;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evomodel.tree.TreeModel;
import beast.evomodel.tree.TreeParameterModel;
import beast.inference.distribution.ParametricDistributionModel;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.math.MathUtils;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.logging.Logger;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @author Michael Defoin Platel
 * @version $Id: DiscretizedBranchRates.java,v 1.11 2006/01/09 17:44:30 rambaut Exp $
 */
public class DiscretizedBranchRates extends AbstractBranchRateModel {


    public static final String DISCRETIZED_BRANCH_RATES = "discretizedBranchRates";

    // Turn on an off the caching on rates for categories -
    // if off then the rates will be flagged to update on
    // a restore.
    // Currently turned off as it is not working with multiple partitions for
    // some reason.
    private static final boolean CACHE_RATES = false;

    private final ParametricDistributionModel distributionModel;

    // The rate categories of each branch
    final TreeParameterModel rateCategories;

    private final int categoryCount;
    private final double step;
    private final double[][] rates;
    private final boolean normalize;
    private final double normalizeBranchRateTo;

    private final TreeModel treeModel;
    private final double logDensityNormalizationConstant;

    private double scaleFactor = 1.0;
    private double storedScaleFactor;

    private boolean updateRateCategories = true;
    private int currentRateArrayIndex = 0;
    private int storedRateArrayIndex;

    //overSampling control the number of effective categories

    public DiscretizedBranchRates(
            TreeModel tree,
            Parameter rateCategoryParameter,
            ParametricDistributionModel model,
            int overSampling) {
        this(tree, rateCategoryParameter, model, overSampling, false, Double.NaN, false, false);

    }

    public DiscretizedBranchRates(
            TreeModel tree,
            Parameter rateCategoryParameter,
            ParametricDistributionModel model,
            int overSampling,
            boolean normalize,
            double normalizeBranchRateTo,
            boolean randomizeRates,
            boolean keepRates) {

        super(DISCRETIZED_BRANCH_RATES);

        this.rateCategories = new TreeParameterModel(tree, rateCategoryParameter, false);

        categoryCount = (tree.getNodeCount() - 1) * overSampling;
        step = 1.0 / (double) categoryCount;

        rates = new double[2][categoryCount];

        this.normalize = normalize;

        this.treeModel = tree;
        this.distributionModel = model;
        this.normalizeBranchRateTo = normalizeBranchRateTo;

        //Force the boundaries of rateCategoryParameter to match the category count
        Parameter.DefaultBounds bound = new Parameter.DefaultBounds(categoryCount - 1, 0, rateCategoryParameter.getDimension());
        rateCategoryParameter.addBounds(bound);

        for (int i = 0; i < rateCategoryParameter.getDimension(); i++) {
            if (!keepRates) {
                int index = (randomizeRates) ?
                        MathUtils.nextInt(rateCategoryParameter.getDimension() * overSampling) : // random rate
                        (int) Math.floor((i + 0.5) * overSampling); // default behavior
                rateCategoryParameter.setParameterValue(i, index);
            }
        }

        addModel(model);
        addModel(rateCategories);

        updateRateCategories = true;

        // Each parameter take any value in [1, \ldots, categoryCount]
        // NB But this depends on the transition kernel employed.  Using swap-only results in a different constant
        logDensityNormalizationConstant = -rateCategoryParameter.getDimension() * Math.log(categoryCount);
    }

    // compute scale factor

    private void computeFactor() {

        //scale mean rate to 1.0 or separate parameter

        double treeRate = 0.0;
        double treeTime = 0.0;

        //normalizeBranchRateTo = 1.0;
        for (int i = 0; i < treeModel.getNodeCount(); i++) {
            NodeRef node = treeModel.getNode(i);
            if (!treeModel.isRoot(node)) {
                int rateCategory = (int) Math.round(rateCategories.getNodeValue(treeModel, node));
                treeRate += rates[currentRateArrayIndex][rateCategory] * treeModel.getBranchLength(node);
                treeTime += treeModel.getBranchLength(node);

                //System.out.println("rates and time\t" + rates[rateCategory] + "\t" + treeModel.getBranchLength(node));
            }
        }
        //treeRate /= treeTime;

        scaleFactor = normalizeBranchRateTo / (treeRate / treeTime);
        //System.out.println("scaleFactor\t\t\t\t\t" + scaleFactor);
    }

    public void handleModelChangedEvent(Model model, Object object, int index) {
        if (model == distributionModel) {
            updateRateCategories = true;
            fireModelChanged();
        } else if (model == rateCategories) {
            fireModelChanged(null, index);
        }
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // nothing to do here
   }

    protected void storeState() {
        if (CACHE_RATES) {
            storedRateArrayIndex = currentRateArrayIndex;
            storedScaleFactor = scaleFactor;
        }
    }

    protected void restoreState() {
        if (CACHE_RATES) {
            currentRateArrayIndex = storedRateArrayIndex;
            scaleFactor = storedScaleFactor;
        } else {
            updateRateCategories = true;
        }
    }

    protected void acceptState() {
    }

    public final double getBranchRate(final Tree tree, final NodeRef node) {

        assert !tree.isRoot(node) : "root node doesn't have a rate!";

        if (updateRateCategories) {
            setupRates();
        }

        int rateCategory = (int) Math.round(rateCategories.getNodeValue(tree, node));

        //System.out.println(rates[rateCategory] + "\t"  + rateCategory);
        return rates[currentRateArrayIndex][rateCategory] * scaleFactor;
    }

    public final boolean isVariableForNode(final Tree tree, final NodeRef node, final Variable<Double> var, final int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Calculates the actual rates corresponding to the category indices.
     */
    private void setupRates() {

        if (CACHE_RATES) {
            // flip the current array index
            currentRateArrayIndex = 1 - currentRateArrayIndex;
        }

        double z = step / 2.0;
        for (int i = 0; i < categoryCount; i++) {
            rates[currentRateArrayIndex][i] = distributionModel.quantile(z);
            //System.out.print(rates[i]+"\t");
            z += step;
        }

        if (normalize) computeFactor();

        updateRateCategories = false;
    }

    public double getLogLikelihood() {
        return logDensityNormalizationConstant;
    }

    public static final XMLObjectParser<DiscretizedBranchRates> PARSER = new AbstractXMLObjectParser<DiscretizedBranchRates>() {

        public static final String DISTRIBUTION = "distribution";
        public static final String RATE_CATEGORIES = "rateCategories";
        public static final String SINGLE_ROOT_RATE = "singleRootRate";
        public static final String OVERSAMPLING = "overSampling";
        public static final String NORMALIZE = "normalize";
        public static final String NORMALIZE_BRANCH_RATE_TO = "normalizeBranchRateTo";
        public static final String RANDOMIZE_RATES = "randomizeRates";
        public static final String KEEP_RATES = "keepRates";
        //public static final String NORMALIZED_MEAN = "normalizedMean";


        public String getParserName() {
            return DISCRETIZED_BRANCH_RATES;
        }

        public DiscretizedBranchRates parseXMLObject(XMLObject xo) throws XMLParseException {

            final int overSampling = xo.getAttribute(OVERSAMPLING, 1);

            //final boolean normalize = xo.getBooleanAttribute(NORMALIZE, false);
            final boolean normalize = xo.getAttribute(NORMALIZE, false);
        /*if(xo.hasAttribute(NORMALIZE))
            normalize = xo.getBooleanAttribute(NORMALIZE);
        }*/
            //final double normalizeBranchRateTo = xo.getDoubleAttribute(NORMALIZE_BRANCH_RATE_TO);
            final double normalizeBranchRateTo = xo.getAttribute(NORMALIZE_BRANCH_RATE_TO, Double.NaN);

            TreeModel tree = (TreeModel) xo.getChild(TreeModel.class);
            ParametricDistributionModel distributionModel = (ParametricDistributionModel) xo.getElementFirstChild(DISTRIBUTION);

            Parameter rateCategoryParameter = (Parameter) xo.getElementFirstChild(RATE_CATEGORIES);

            Logger.getLogger("beast.evomodel").info("Using discretized relaxed clock model.");
            Logger.getLogger("beast.evomodel").info("  over sampling = " + overSampling);
            Logger.getLogger("beast.evomodel").info("  parametric model = " + distributionModel.getModelName());
            Logger.getLogger("beast.evomodel").info("   rate categories = " + rateCategoryParameter.getDimension());
            if (normalize) {
                Logger.getLogger("beast.evomodel").info("   mean rate is normalized to " + normalizeBranchRateTo);
            }

            if (xo.hasAttribute(SINGLE_ROOT_RATE)) {
                //singleRootRate = xo.getBooleanAttribute(SINGLE_ROOT_RATE);
                Logger.getLogger("beast.evomodel").warning("   WARNING: single root rate is not implemented!");
            }

            final boolean randomizeRates = xo.getAttribute(RANDOMIZE_RATES, false);
            final boolean keepRates = xo.getAttribute(KEEP_RATES, false);

            if (randomizeRates && keepRates) {
                throw new XMLParseException("Unable to both randomize and keep current rate categories");
            }

        /* if (xo.hasAttribute(NORMALIZED_MEAN)) {
            dbr.setNormalizedMean(xo.getDoubleAttribute(NORMALIZED_MEAN));
        }*/

            return new DiscretizedBranchRates(tree, rateCategoryParameter, distributionModel, overSampling, normalize,
                    normalizeBranchRateTo, randomizeRates, keepRates);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return
                    "This element returns an discretized relaxed clock model." +
                            "The branch rates are drawn from a discretized parametric distribution.";
        }

        public Class getReturnType() {
            return DiscretizedBranchRates.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newBooleanRule(SINGLE_ROOT_RATE, true, "Whether only a single rate should be used for the two children branches of the root"),
                //AttributeRule.newDoubleRule(NORMALIZED_MEAN, true, "The mean rate to constrain branch rates to once branch lengths are taken into account"),
                AttributeRule.newIntegerRule(OVERSAMPLING, true, "The integer factor for oversampling the distribution model (1 means no oversampling)"),
                AttributeRule.newBooleanRule(NORMALIZE, true, "Whether the mean rate has to be normalized to a particular value"),
                AttributeRule.newDoubleRule(NORMALIZE_BRANCH_RATE_TO, true, "The mean rate to normalize to, if normalizing"),
                AttributeRule.newBooleanRule(RANDOMIZE_RATES, true, "Randomize initial categories"),
                AttributeRule.newBooleanRule(KEEP_RATES, true, "Keep current rate category specification"),
                new ElementRule(TreeModel.class),
                new ElementRule(DISTRIBUTION, ParametricDistributionModel.class, "The distribution model for rates among branches", false),
                new ElementRule(RATE_CATEGORIES, Parameter.class, "The rate categories parameter", false),
        };
    };
}
