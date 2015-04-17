/*
 * BranchModel.java
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

package beast.beagle.branchmodel;

import beast.beagle.substmodel.FrequencyModel;
import beast.beagle.substmodel.SubstitutionModel;
import beast.evolution.tree.NodeRef;
import beast.inference.model.Model;

import java.util.List;

/**
 * This is an interface which provides a mapping of substitution models to branches.
 *
 * @author Andrew Rambaut
 * @author Filip Bielejec
 * @author Marc A. Suchard
 * @version $Id$
 */
public abstract class BranchModel extends Model {

    /**
     * @param name Model Name
     */
    public BranchModel(String name) {
        super(name);
    }

    /**
     * Returns a mapping of substitution models to the given branch. The Mapping
     * contains a list of substitution models in order from tipward to rootward
     * and a set of relative weights for each (may be times or proportions).
     *
     * @param branch the branch
     * @return a Mapping object
     */
    public abstract Mapping getBranchModelMapping(final NodeRef branch);

    /**
     * Gets the list of substitution models in order they will be referred to
     * by the indices returned by the mappings.
     * @return the list of substitution models
     */
    public abstract List<SubstitutionModel> getSubstitutionModels();

    /**
     * Gets the substitution model that will be applied at the root.
     * @return the substitution model
     */
    public abstract SubstitutionModel getRootSubstitutionModel();

    /**
     * Gets the frequency model that will be applied at the root.
     * @return the substitution model
     */
    public abstract FrequencyModel getRootFrequencyModel();

    /**
     * Is this model going to require convolution of matrices along any branches (essentially
     * are the mappings ever going to return more than one substitution model.
     * @return does it?
     */
    public abstract boolean requiresMatrixConvolution();

    public interface Mapping {
        int[] getOrder();
        double[] getWeights();
    }

    public final static Mapping DEFAULT = new Mapping() {
        public int[] getOrder() {
            return new int[] { 0 };
        }

        public double[] getWeights() {
            return new double[] { 1.0 };
        }
    };
}
