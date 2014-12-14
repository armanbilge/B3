/*
 * RateCovarianceStatistic.java
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
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * A statistic that tracks the covariance of rates on branches
 *
 * @author Alexei Drummond
 * @version $Id: RateCovarianceStatistic.java,v 1.5 2005/07/11 14:06:25 rambaut Exp $
 */
public class RateCovarianceStatistic extends Statistic.Abstract implements TreeStatistic {

    public RateCovarianceStatistic(String name, Tree tree, BranchRateModel branchRateModel) {
        super(name);
        this.tree = tree;
        this.branchRateModel = branchRateModel;

        int n = tree.getExternalNodeCount();
        childRate = new double[2 * n - 4];
        parentRate = new double[childRate.length];
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

        int n = tree.getNodeCount();
        int index = 0;
        for (int i = 0; i < n; i++) {
            NodeRef child = tree.getNode(i);
            NodeRef parent = tree.getParent(child);
            if (parent != null & !tree.isRoot(parent)) {
                childRate[index] = branchRateModel.getBranchRate(tree, child);
                parentRate[index] = branchRateModel.getBranchRate(tree, parent);
                index += 1;
            }
        }
        return DiscreteStatistics.covariance(childRate, parentRate);
    }

    private Tree tree = null;
    private BranchRateModel branchRateModel = null;
    private double[] childRate = null;
    private double[] parentRate = null;

    public static final XMLObjectParser<RateCovarianceStatistic> PARSER = new AbstractXMLObjectParser<RateCovarianceStatistic>() {

        public static final String RATE_COVARIANCE_STATISTIC = "rateCovarianceStatistic";

        public String getParserName() {
            return RATE_COVARIANCE_STATISTIC;
        }

        public RateCovarianceStatistic parseXMLObject(XMLObject xo) throws XMLParseException {

            String name = xo.getAttribute(Statistic.NAME, xo.getId());
            Tree tree = (Tree) xo.getChild(Tree.class);
            BranchRateModel branchRateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);

            return new RateCovarianceStatistic(name, tree, branchRateModel);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A statistic that has as its value the covariance of parent and child branch rates";
        }

        public Class getReturnType() {
            return RateCovarianceStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(TreeModel.class),
                new ElementRule(BranchRateModel.class),
                new StringAttributeRule("name", "A name for this statistic primarily for the purposes of logging", true),
        };

    };
}
