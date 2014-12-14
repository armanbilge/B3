/*
 * EigenDecomposition.java
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
 * @author Alexei Drummond
 * @Author Marc A. Suchard
 * @version $Id$
 */
public class EigenDecomposition implements Serializable {

    public EigenDecomposition(double[] evec, double[] ievc, double[] eval) {
        Evec = evec;
        Ievc = ievc;
        Eval = eval;
    }

    public EigenDecomposition copy() {
        double[] evec = Evec.clone();
        double[] ievc = Ievc.clone();
        double[] eval = Eval.clone();

        return new EigenDecomposition(evec, ievc, eval);
    }

    /**
     * This function returns the Eigen vectors.
     * @return the array
     */
    public final double[] getEigenVectors() {
        return Evec;
    }

    /**
     * This function returns the inverse Eigen vectors.
     * @return the array
     */
    public final double[] getInverseEigenVectors() {
        return Ievc;
    }

    /**
     * This function returns the Eigen values.
     * @return the Eigen values
     */
    public final double[] getEigenValues() {
        return Eval;
    }

    /**
     * This function rescales the eigen values; this is more stable than
     * rescaling the original Q matrix, also O(stateCount) instead of O(stateCount^2)
     */
    public void normalizeEigenValues(double scale) {
        int dim = Eval.length;
        for (int i = 0; i < dim; i++)

            Eval[i] /= scale;
    }

    // Eigenvalues, eigenvectors, and inverse eigenvectors
    private final double[] Evec;
    private final double[] Ievc;
    private final double[] Eval;

}
