/*
 * HomogeneousBranchModel.java
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
import beast.inference.model.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Rambaut
 * @author Filip Bielejec
 * @author Marc Suchard
 * @version $Id$
 */
public class HomogeneousBranchModel extends BranchModel {
    private final SubstitutionModel substitutionModel;
    private final FrequencyModel rootFrequencyModel;

    public HomogeneousBranchModel(SubstitutionModel substitutionModel) {
        this(substitutionModel, null);
    }

    public HomogeneousBranchModel(SubstitutionModel substitutionModel, FrequencyModel rootFrequencyModel) {
        super("HomogeneousBranchModel");
        this.substitutionModel = substitutionModel;
        addModel(substitutionModel);
        if (rootFrequencyModel != null) {
            addModel(rootFrequencyModel);
            this.rootFrequencyModel = rootFrequencyModel;
        } else {
            this.rootFrequencyModel = substitutionModel.getFrequencyModel();
        }
    }

    public Mapping getBranchModelMapping(NodeRef node) {
        return DEFAULT;
    }

//    @Override // use java 1.5
    public List<SubstitutionModel> getSubstitutionModels() {
        List<SubstitutionModel> substitutionModels = new ArrayList<SubstitutionModel>();
        substitutionModels.add(substitutionModel);
        return substitutionModels;
    }

//    @Override
    public SubstitutionModel getRootSubstitutionModel() {
        return substitutionModel;
    }

    public FrequencyModel getRootFrequencyModel() {
        return rootFrequencyModel;
    }

//    @Override
    public boolean requiresMatrixConvolution() {
        return false;
    }

    @Override
    protected void handleModelChangedEvent(Model model, Object object, int index) {
        fireModelChanged();
    }

    @Override
    protected void handleVariableChangedEvent(Variable variable, int index, Variable.ChangeType type) {
    }

    @Override
    protected void storeState() {
    }

    @Override
    protected void restoreState() {
    }

    @Override
    protected void acceptState() {
    }
}
