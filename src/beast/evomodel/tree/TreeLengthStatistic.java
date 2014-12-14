/*
 * TreeLengthStatistic.java
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
import beast.inference.model.Statistic;

/**
 * A statistic that reports the total length of all the branches in the tree
 *
 * @author Alexei Drummond
 * @version $Id: RateStatistic.java,v 1.9 2005/07/11 14:06:25 rambaut Exp $
 */
public class TreeLengthStatistic extends Statistic.Abstract implements TreeStatistic {

    public TreeLengthStatistic(String name, Tree tree) {
        super(name);
        this.tree = tree;
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
     * @return the total length of all the branches in the tree
     */
    public double getStatisticValue(int dim) {

        double treeLength = 0.0;
        for (int i = 0; i < tree.getNodeCount(); i++) {
            NodeRef node = tree.getNode(i);

            if (node != tree.getRoot()) {
                NodeRef parent = tree.getParent(node);
                treeLength += tree.getNodeHeight(parent) - tree.getNodeHeight(node);
            }
        }
        return treeLength;
    }

    private Tree tree = null;
}
