/*
 * GibbsOperator.java
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

package beast.inference.operators;

/**
 * An interface for a Gibbs operator.
 *
 * @author Roald Forsberg
 */
public interface GibbsOperator extends MCMCOperator {

   /**
	* @return the number of steps the operator performs in one go.
	*/
	int getStepCount();

    /**
     * Set the path parameter for sampling from power-posterior
     */
    void setPathParameter(double beta);
}
