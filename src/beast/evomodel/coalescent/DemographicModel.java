/*
 * DemographicModel.java
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

package beast.evomodel.coalescent;

import beast.evolution.coalescent.DemographicFunction;
import beast.evolution.util.Units;
import beast.inference.model.AbstractModel;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;

/**
 * This interface provides methods that describe a demographic model.
 * <p/>
 * Parts of this class were derived from C++ code provided by Oliver Pybus.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Korbinian Strimmer
 * @version $Id: DemographicModel.java,v 1.28 2005/09/26 14:27:38 rambaut Exp $
 */
public abstract class DemographicModel extends AbstractModel implements Units {

    public DemographicModel(String name) {
        super(name);
    }

    // general functions

    public abstract DemographicFunction getDemographicFunction();

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

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

    // **************************************************************
    // Units IMPLEMENTATION
    // **************************************************************

    /**
     * Units in which population size is measured.
     */
    private Type units;

    /**
     * sets units of measurement.
     *
     * @param u units
     */
    public void setUnits(Type u) {
        units = u;
    }

    /**
     * returns units of measurement.
     */
    public Type getUnits() {
        return units;
    }
}