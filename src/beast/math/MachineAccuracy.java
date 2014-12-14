/*
 * MachineAccuracy.java
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
 * determines machine accuracy
 *
 * @version $Id: MachineAccuracy.java,v 1.4 2005/05/24 20:26:01 rambaut Exp $
 *
 * @author Korbinian Strimmer
 * @author Alexei Drummond
 */
public final class MachineAccuracy
{
	//
	// Public stuff
	//

	/** machine accuracy constant */
	public static final double EPSILON;// = 2.220446049250313E-16;
	public static final double SQRT_EPSILON;// = 1.4901161193847656E-8;
//	public static final double SQRT_SQRT_EPSILON = 1.220703125E-4;

	static {
		EPSILON = computeEpsilon();
		SQRT_EPSILON = Math.sqrt(EPSILON);
	}

	/** compute EPSILON from scratch */
	public static double computeEpsilon()
	{
		double eps = 1.0;

		while( eps + 1.0 != 1.0 )
		{
			eps /= 2.0;
		}
		eps *= 2.0;
		
		return eps;
	}

	/**
	 * @return true if the relative difference between the two parameters
	 * is smaller than SQRT_EPSILON.
	 */
	public static boolean same(double a, double b) {
		return Math.abs((a/b)-1.0) <= SQRT_EPSILON;
	}
}
