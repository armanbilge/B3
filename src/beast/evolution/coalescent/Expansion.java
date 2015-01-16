/*
 * Expansion.java
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

package beast.evolution.coalescent;

import beast.evomodel.coalescent.ExpansionModel;
import beast.inference.model.Parameter.Default;

/**
 * This class models exponential growth from an initial ancestral population size.
 * (Parameters: N0=present-day population size; N1=ancestral population size; r=growth rate).
 * This model is nested with the exponential-growth population size model (N1=0).
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Expansion.java,v 1.4 2005/05/24 20:25:56 rambaut Exp $
 */
@Deprecated
public class Expansion extends ExpansionModel {

    /**
     * Construct demographic model with default settings
     *
     * @param units of time
     */
    public Expansion(Type units) {
        super(new Default(1), new Default(1), new Default(1), units, false);
    }

}
