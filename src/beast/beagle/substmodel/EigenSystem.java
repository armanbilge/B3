/*
 * EigenSystem.java
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

package beast.beagle.substmodel;

import java.io.Serializable;

/**
 * @author Andrew Rambaut
 * @author Marc Suchard
 * @version $Id$
 */
public interface EigenSystem extends Serializable {
    /**
     * Set the instantaneous rate matrix
     *
     * @param matrix
     */
    EigenDecomposition decomposeMatrix(double[][] matrix);

    void computeExponential(EigenDecomposition ed, double time, double[] matrix);

    double computeExponential(EigenDecomposition ed, double time, int i, int j);
}
