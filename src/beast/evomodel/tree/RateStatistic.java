/*
 * RateStatistic.java
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

package beast.evomodel.tree;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evomodel.branchratemodel.BranchRateModel;
import beast.inference.model.Statistic;
import beast.math.DiscreteStatistics;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * A statistic that tracks the mean, variance and coefficent of variation of the rates.
 *
 * @author Alexei Drummond
 * @version $Id: RateStatistic.java,v 1.9 2005/07/11 14:06:25 rambaut Exp $
 */
public class RateStatistic extends Statistic.Abstract implements TreeStatistic {

    public enum Mode {
        MEAN("mean"),
        VARIANCE("variance"),
        COEFFICIENT_OF_VARIATION("coefficientOfVariation");

        final String name;

        Mode(final String s) {
            name = s;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    public RateStatistic(String name, Tree tree, BranchRateModel branchRateModel, boolean external, boolean internal, Mode mode) {
        super(name);
        this.tree = tree;
        this.branchRateModel = branchRateModel;
        this.internal = internal;
        this.external = external;
        this.mode = mode;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public Tree getTree() {
        return tree;
    }

    public int getDimension() {
        return 1;
    }

    /**
     * @return the height of the MRCA node.
     */
    public double getStatisticValue(int dim) {

        int length = 0;
        int offset = 0;
        if (external) {
            length += tree.getExternalNodeCount();
            offset = length;
        }
        if (internal) {
            length += tree.getInternalNodeCount() - 1;
        }

        final double[] rates = new double[length];
        // need those only for mean
        final double[] branchLengths = new double[length];

        for (int i = 0; i < offset; i++) {
            NodeRef child = tree.getExternalNode(i);
            NodeRef parent = tree.getParent(child);
            branchLengths[i] = tree.getNodeHeight(parent) - tree.getNodeHeight(child);
            rates[i] = branchRateModel.getBranchRate(tree, child);
        }
        if (internal) {
            final int n = tree.getInternalNodeCount();
            int k = offset;
            for (int i = 0; i < n; i++) {
                NodeRef child = tree.getInternalNode(i);
                if (!tree.isRoot(child)) {
                    NodeRef parent = tree.getParent(child);
                    branchLengths[k] = tree.getNodeHeight(parent) - tree.getNodeHeight(child);
                    rates[k] = branchRateModel.getBranchRate(tree, child);
                    k++;
                }
            }
        }

        switch (mode) {
            case MEAN:
                double totalWeightedRate = 0.0;
                double totalTreeLength = 0.0;
                for (int i = 0; i < rates.length; i++) {
                    totalWeightedRate += rates[i] * branchLengths[i];
                    totalTreeLength += branchLengths[i];
                }
                return totalWeightedRate / totalTreeLength;
            case VARIANCE:
                return DiscreteStatistics.variance(rates);
            case COEFFICIENT_OF_VARIATION:
                // don't compute mean twice
                final double mean = DiscreteStatistics.mean(rates);
                return Math.sqrt(DiscreteStatistics.variance(rates, mean)) / mean;
        }

        throw new IllegalArgumentException();
    }

    private Tree tree = null;
    private BranchRateModel branchRateModel = null;
    private boolean internal = true;
    private boolean external = true;
    private Mode mode = Mode.MEAN;

    public static final XMLObjectParser<RateStatistic> PARSER = new AbstractXMLObjectParser<RateStatistic>() {

        public static final String RATE_STATISTIC = "rateStatistic";
        public static final String MODE = "mode";

        public String getParserName() {
            return RATE_STATISTIC;
        }

        public RateStatistic parseXMLObject(XMLObject xo) throws XMLParseException {

            final String name = xo.getAttribute(Statistic.NAME, xo.getId());
            final Tree tree = (Tree) xo.getChild(Tree.class);
            final BranchRateModel branchRateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);

            final boolean internal = xo.getBooleanAttribute("internal");
            final boolean external = xo.getBooleanAttribute("external");

            if (!(internal || external)) {
                throw new XMLParseException("At least one of internal and external must be true!");
            }

            final Mode mode = Mode.valueOf(xo.getStringAttribute(MODE));

            return new RateStatistic(name, tree, branchRateModel, external, internal, mode);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A statistic that returns the average of the branch rates";
        }

        public Class getReturnType() {
            return RateStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(TreeModel.class),
                new ElementRule(BranchRateModel.class),
                AttributeRule.newBooleanRule("internal"),
                AttributeRule.newBooleanRule("external"),
                new StringAttributeRule("mode", "This attribute determines how the rates are summarized, can be one of (mean, variance, coefficientOfVariance)", Mode.values(), false),
                new StringAttributeRule("name", "A name for this statistic primarily for the purposes of logging", true),
        };

    };
}
