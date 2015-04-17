/*
 * BranchSubstitutionModel.java
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

package beast.beagle.sitemodel;

import beagle.Beagle;
import beast.beagle.substmodel.EigenDecomposition;
import beast.beagle.substmodel.SubstitutionModel;
import beast.beagle.treelikelihood.BufferIndexHelper;
import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evomodel.tree.TreeModel;
import beast.inference.model.Model;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Marc A. Suchard
 * @version $Id$
 */

@Deprecated // Switching to BranchModel
public abstract class BranchSubstitutionModel extends Model {

    public BranchSubstitutionModel(String name) {
        super(name);
    }

    public abstract EigenDecomposition getEigenDecomposition(int modelIndex, int categoryIndex);

    public abstract SubstitutionModel getSubstitutionModel(int modelIndex, int categoryIndex);

    public abstract double[] getStateFrequencies(int categoryIndex);

    public abstract int getBranchIndex(final Tree tree, final NodeRef node, int bufferIndex);

    public abstract int getEigenCount();

    public abstract boolean canReturnComplexDiagonalization();

    public abstract void updateTransitionMatrices(
            Beagle beagle,
            int eigenIndex,
            BufferIndexHelper bufferHelper,
            final int[] probabilityIndices,
            final int[] firstDerivativeIndices,
            final int[] secondDervativeIndices,
            final double[] edgeLengths,
            int count);

    public abstract int getExtraBufferCount(TreeModel treeModel);

    public abstract void setFirstBuffer(int bufferCount);

    public abstract void setEigenDecomposition(Beagle beagle, int eigenIndex, BufferIndexHelper bufferHelper, int dummy);
	
	

}
