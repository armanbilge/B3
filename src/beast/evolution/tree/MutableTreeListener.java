/*
 * MutableTreeListener.java
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

package beast.evolution.tree;

/**
 * Interface for a listener to a mutable tree.
 *
 * @version $Id: MutableTreeListener.java,v 1.4 2005/05/24 20:25:56 rambaut Exp $
 *
 * @author Andrew Rambaut
 */
public interface MutableTreeListener {

	/** Called when the tree changes in some way*/
	void treeChanged(Tree tree);
}
