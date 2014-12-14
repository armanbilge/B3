/*
 * SpeciationModel.java
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

package beast.evomodel.speciation;

import beast.evolution.tree.Tree;
import beast.evolution.util.Taxon;
import beast.evolution.util.Units;
import beast.inference.model.AbstractModel;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;

import java.util.Set;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public abstract class SpeciationModel extends AbstractModel implements Units {
    /**
     * Units in which population size is measured.
     */
    private Units.Type units;

    public SpeciationModel(String name, Type units) {
        super(name);
        setUnits(units);
    }

    public abstract double calculateTreeLogLikelihood(Tree tree);

    public abstract double calculateTreeLogLikelihood(Tree tree, Set<Taxon> exclude);

    // True if Yule.
    //
    // Not abstract - non supporting derived classes do not need to override anything
    public boolean isYule() {
        return false;
    }

    // Likelihood for the speciation model conditional on monophyly and calibration densities in
    // 'calibration'.
    //
    // The likelihood enforces the monophyly, so there is no need to specify it again in the XML.
    //
    public double calculateTreeLogLikelihood(Tree tree, CalibrationPoints calibration) {
        return Double.NEGATIVE_INFINITY;
    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        // no intermediates need to be recalculated...
    }

    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // no intermediates need to be recalculated...
    }

    protected void storeState() {
    } // no additional state needs storing

    protected void restoreState() {
    } // no additional state needs restoring

    protected void acceptState() {
    } // no additional state needs accepting

    /**
     * sets units of measurement.
     *
     * @param u units
     */
    public void setUnits(Units.Type u) {
        units = u;
    }

    /**
     * returns units of measurement.
     */
    public Units.Type getUnits() {
        return units;
    }
}
