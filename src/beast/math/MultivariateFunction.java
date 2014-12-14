/*
 * MultivariateFunction.java
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

package beast.math;


/**
 * interface for a function of several variables
 *
 * @author Korbinian Strimmer
 */
public interface MultivariateFunction
{
	/**
	 * compute function value
	 *
	 * @param argument  function argument (vector)
	 *
	 * @return function value
	 */
	double evaluate(double[] argument);


	/**
	 * get number of arguments
	 *
	 * @return number of arguments
	 */
	 int getNumArguments();

	/**
	 * get lower bound of argument n
	 *
	 * @param n argument number
	 *
	 * @return lower bound
	 */
	double getLowerBound(int n);

	/**
	 * get upper bound of argument n
	 *
	 * @param n argument number
	 *
	 * @return upper bound
	 */
	double getUpperBound(int n);

}
