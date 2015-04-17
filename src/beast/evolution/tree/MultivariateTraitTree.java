/*
 * MultivariateTraitTree.java
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

import beast.inference.model.Model;

/**
 * @author Marc A. Suchard
 */
public abstract class MultivariateTraitTree extends Model implements MutableTree {

    /**
     * @param name Model Name
     */
    public MultivariateTraitTree(String name) {
        super(name);
    }

    public abstract double[] getMultivariateNodeTrait(NodeRef node, String name);

    public abstract void setMultivariateTrait(NodeRef n, String name, double[] value);
}
