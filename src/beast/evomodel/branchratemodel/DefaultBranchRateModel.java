/*
 * DefaultBranchRateModel.java
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
import beast.inference.model.Model;
import beast.inference.model.ModelListener;
import beast.inference.model.Variable;
import beast.inference.model.Variable.ChangeType;

/**
 * @author Andrew Rambaut
 * @version $Id: DefaultBranchRateModel.java,v 1.4 2005/05/24 20:25:57 rambaut Exp $
 */
public final class DefaultBranchRateModel extends BranchRateModel {

    public DefaultBranchRateModel() {
        super("defaultBranchRates");
    }

    public double getBranchRate(Tree tree, NodeRef node) {
        return 1.0;
    }

    public void addModelListener(ModelListener listener) {
        // nothing to do
    }

    public void removeModelListener(ModelListener listener) {
        // nothing to do
    }

    public String getId() {
        return null;
    }

    public void setId(String id) {
        // nothing to do
    }

    public boolean isUsed() {
        return false;
    }

    @Override
    protected void handleModelChangedEvent(Model model, Object object, int index) {
        // Nothing to do
    }

    @Override
    protected void handleVariableChangedEvent(Variable variable, int index, ChangeType type) {
        // Nothing to do
    }

    @Override
    protected void storeState() {
        // Nothing to do
    }

    @Override
    protected void restoreState() {
        // Nothing to do
    }

    @Override
    protected void acceptState() {
        // Nothing to do
    }

    public String getTraitName() {
        return RATE;
    }

    public Intent getIntent() {
        return Intent.BRANCH;
    }

    public boolean getLoggable() {
        return true;
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

    public int getDimension() {
        return 1;
    }

    public Double getTrait(final Tree tree, final NodeRef node) {
        return getBranchRate(tree, node);
    }

    public String getTraitString(final Tree tree, final NodeRef node) {
        return Double.toString(getBranchRate(tree, node));
    }
}
