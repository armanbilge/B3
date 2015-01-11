/*
 * ConstantPopulation.java
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

/**
 * This class models coalescent intervals for a constant population
 * (parameter: N0=present-day population size). <BR>
 * If time units are set to Units.EXPECTED_SUBSTITUTIONS then
 * the N0 parameter will be interpreted as N0 * mu. <BR>
 * Also note that if you are dealing with a diploid population
 * N0 will be out by a factor of 2.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 *
 * @version $Id: ConstantPopulation.java,v 1.9 2005/05/24 20:25:55 rambaut Exp $
 *
 */
public class ConstantPopulation extends DemographicFunction.Abstract
{
	//
	// Public stuff
	//
	
	/**
	 * Construct demographic model with default settings
     * @param units
     */
	public ConstantPopulation(Type units) {

		super(units);
	}

	/**
	 * @return initial population size.
	 */
	public double getN0() { return N0; }

	/**
	 * sets initial population size.
     * @param N0 new size
     */
	public void setN0(double N0) { this.N0 = N0; }

	public boolean respectingN0() {
		return respectN0;
	}

	public void setRespectingN0(boolean respect) {
		respectN0 = respect;
	}

	// Implementation of abstract methods
	
	public double getDemographic(double t) { return getN0(); }
	public double getIntensity(double t) { return t/getN0(); }
	public double getDifferentiatedIntensity(double t) {
		if (respectingN0()) {
			final double N0 = getN0();
			return t / (N0 * N0);
		} else {
			return 0;
		}
	}
	public double getInverseIntensity(double x) { return getN0()*x; }

    // same as abstract
//	/**
//	 * Calculates the integral 1/N(x) dx between start and finish. The
//	 * inherited function in DemographicFunction.Abstract calls a
//	 * numerical integrater which is unecessary.
//	 */
//	public double getIntegral(double start, double finish) {
//		return getIntensity(finish) - getIntensity(start);
//	}
//
	public int getNumArguments() {
		return 1;
	}
	
	public String getArgumentName(int n) {
		return "N0";
	}
	
	public double getArgument(int n) {
		return getN0();
	}
	
	public void setArgument(int n, double value) {
		setN0(value);
	}

	public double getLowerBound(int n) {
		return 0.0;
	}
	
	public double getUpperBound(int n) {
		return Double.POSITIVE_INFINITY;
	}

	public DemographicFunction getCopy() {
		ConstantPopulation df = new ConstantPopulation(getUnits());
		df.N0 = N0;
		
		return df;
	}

	//
	// private stuff
	//
	
	private double N0;
	private boolean respectN0;
}
