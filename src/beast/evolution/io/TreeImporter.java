/*
 * TreeImporter.java
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

package beast.evolution.io;

import beast.evolution.tree.Tree;
import beast.evolution.util.TaxonList;

import java.io.IOException;

/**
 * Interface for importers that do trees
 *
 * @version $Id: TreeImporter.java,v 1.7 2005/05/24 20:25:56 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public interface TreeImporter { 

	/**
	 * return whether another tree is available. 
	 */
	boolean hasTree() throws IOException, Importer.ImportException;

	/**
	 * import the next tree. 
	 * return the tree or null if no more trees are available
	 */
	Tree importNextTree() throws IOException, Importer.ImportException;

	/**
	 * import a single tree. 
	 */
	Tree importTree(TaxonList taxonList) throws IOException, Importer.ImportException;

	/**
	 * import an array of all trees. 
	 */
	Tree[] importTrees(TaxonList taxonList) throws IOException, Importer.ImportException;
}
