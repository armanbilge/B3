/*
 * AttributeBranchRateModel.java
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
import beast.evomodel.tree.TreeModel;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class AttributeBranchRateModel extends AbstractBranchRateModel {

    public AttributeBranchRateModel(final TreeModel treeModel, final String rateAttributeName) {
        super(ATTRIBUTE_BRANCH_RATE_MODEL);

        this.treeModel = treeModel;
        this.rateAttributeName = rateAttributeName;

        addModel(treeModel);
    }

    @Override
    protected void handleModelChangedEvent(Model model, Object object, int index) {
        fireModelChanged();
    }

    @Override
    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // nothing to do
    }

    @Override
    protected void storeState() {
        // nothing to do
    }

    @Override
    protected void restoreState() {
        // nothing to do
    }

    @Override
    protected void acceptState() {
        // nothing to do
    }

    @Override
    public double getBranchRate(Tree tree, NodeRef node) {
        Object value = tree.getNodeAttribute(node, rateAttributeName);
        return Double.parseDouble((String)value);
    }

    @Override
    public String getTraitName() {
        return rateAttributeName;
    }

    public static final String ATTRIBUTE_BRANCH_RATE_MODEL = "attributeBranchRateModel";

    private final TreeModel treeModel;
    private final String rateAttributeName;

}
