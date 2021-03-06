/*
 * TreeStatistic.java
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

/**
 * An interface for statistics on trees
 *
 * @version $Id: TreeStatistic.java,v 1.14 2005/05/24 20:25:58 rambaut Exp $
 *
 * @author Andrew Rambaut
 *
 */
public interface TreeStatistic {
       
	public static final String TREE = "tree";

	void setTree(Tree tree);
	Tree getTree();
}
