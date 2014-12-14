/*
 * SubstitutionProcess.java
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

import beast.evolution.datatype.DataType;

/**
 * <b>model of sequence substitution (rate matrix)</b>.
 * provides a convenient interface for the computation of transition probabilities
 */
public interface SubstitutionProcess {

    /**
     * Get the complete transition probability matrix for the given distance.
     *
     * @param distance the time (branch length)
     * @param matrix   an array to store the matrix
     */
    void getTransitionProbabilities(double distance, double[] matrix);

    /**
     * This function returns the Eigen vectors.
     *
     * @return the array
     */
    EigenDecomposition getEigenDecomposition();

    /**
     * get the state frequencies
     *
     * @return the frequencies
     */
    FrequencyModel getFrequencyModel();

    /**
     * Get the infinitesimal rate matrix
     *
     * @return the rates
     */
    public void getInfinitesimalMatrix(double[] matrix);

    /**
     * @return the data type
     */
    DataType getDataType();

    /**
     * @return if substitution model can return complex diagonalizations
     */
    boolean canReturnComplexDiagonalization();
}