/*
 * AbstractBranchRateModel.java
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

package beast.evomodel.branchratemodel;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeTrait;
import beast.inference.model.AbstractModelLikelihood;
import beast.inference.model.Model;
import beast.inference.model.Variable;

/**
 * An abstract base class for BranchRateModels to help implement some of the interfaces
 * @author Andrew Rambaut
 * @version $Id:$
 */
public abstract class AbstractBranchRateModel extends AbstractModelLikelihood implements BranchRateModel {
    /**
     * @param name Model Name
     */
    public AbstractBranchRateModel(String name) {
        super(name);
    }

    public String getTraitName() {
        return BranchRateModel.RATE;
    }

    public Intent getIntent() {
        return Intent.BRANCH;
    }

    public TreeTrait getTreeTrait(final String key) {
        return this;
    }

    public TreeTrait[] getTreeTraits() {
        return new TreeTrait[] { this };
    }

    public Class getTraitClass() {
        return Double.class;
    }

    public boolean getLoggable() {
        return true;
    }

    public Double getTrait(final Tree tree, final NodeRef node) {
        return getBranchRate(tree, node);
    }

    public String getTraitString(final Tree tree, final NodeRef node) {
        return Double.toString(getBranchRate(tree, node));
    }

    public Model getModel() {
        return this;
    }

    public double getLogLikelihood() {
        return 0;
    }

    public double differentiate(Variable<Double> var, int index) {
        return 0;
    }

    public void makeDirty() {
        // Do nothing
    }
}