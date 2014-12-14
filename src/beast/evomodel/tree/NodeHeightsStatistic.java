/*
 * NodeHeightsStatistic.java
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

import beast.evolution.tree.Tree;
import beast.inference.model.Parameter;
import beast.inference.model.Statistic;

import java.util.Arrays;

/**
 * A statistic that reports the heights of all the nodes in sorted order with the option
 * of grouping (for skyline-type plots).
 *
 * @author Andrew Rambaut
 * @version $Id:$
 */
public class NodeHeightsStatistic extends Statistic.Abstract implements TreeStatistic {

    public NodeHeightsStatistic(String name, Tree tree) {
        this(name, tree, null);
    }

    public NodeHeightsStatistic(String name, Tree tree, Parameter groupSizes) {
        super(name);
        this.tree = tree;
        this.groupSizes = groupSizes;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public Tree getTree() {
        return tree;
    }

    public int getDimension() {
        if (groupSizes != null) {
            return groupSizes.getDimension();
        }
        return tree.getInternalNodeCount();
    }

    /**
     * @return the total length of all the branches in the tree
     */
    public double getStatisticValue(int dim) {
        if (dim == 0) {
            // This assumes that each dimension will be called in turn, so
            // the call for dim 0 updates the array.
            calculateHeights();
        }

        return heights[dim];
    }

    private void calculateHeights() {
        heights = new double[tree.getInternalNodeCount()];

        for (int i = 0; i < heights.length; i++) {
            heights[i] = tree.getNodeHeight(tree.getInternalNode(i));
        }
        Arrays.sort(heights);

        if (groupSizes != null) {
            double[] allHeights = heights;
            heights = new double[groupSizes.getDimension()];
            int k = 0;
            for (int i = 0; i < groupSizes.getDimension(); i++) {
                k += groupSizes.getValue(i);
                heights[i] = allHeights[k - 1];
            }
        }
    }

    private Tree tree = null;
    private Parameter groupSizes = null;
    private double[] heights = null;
}