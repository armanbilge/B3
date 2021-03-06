/*
 * DemographicFunction.java
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

import beast.evolution.util.Units;
import beast.math.MathUtils;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * This interface provides methods that describe a demographic function.
 *
 * Parts of this class were derived from C++ code provided by Oliver Pybus.
 *
 * @version $Id: DemographicFunction.java,v 1.12 2005/05/24 20:25:55 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Korbinian Strimmer
 */
public interface DemographicFunction extends UnivariateFunction, Units {

    /**
     * @param t time
     * @return value of the demographic function N(t) at time t
     */
	double getDemographic(double t);

    default double getLogDemographic(double t) {
        return Math.log(getDemographic(t));
    }

    /**
     * @return value of demographic intensity function at time t (= integral 1/N(x) dx from 0 to t).
     * @param t time
     */
	double getIntensity(double t);

	/**
	 * @return value of inverse demographic intensity function
	 * (returns time, needed for simulation of coalescent intervals).
	 */
	double getInverseIntensity(double x);

	/**
	 * Calculates the integral 1/N(x) dx between start and finish.
     * @param start  point
     * @param finish point
     * @return integral value
     */
	default double getIntegral(double start, double finish) {
        return getIntensity(finish) - getIntensity(start);
    }

	/**
	 * @return the number of arguments for this function.
	 */
	int getNumArguments();

	/**
	 * @return the name of the n'th argument of this function.
	 */
	String getArgumentName(int n);

	/**
	 * @return the value of the n'th argument of this function.
	 */
	double getArgument(int n);

	/**
	 * Sets the value of the nth argument of this function.
	 */
	void setArgument(int n, double value);

	/**
	 * @return the lower bound of the nth argument of this function.
	 */
	double getLowerBound(int n);

	/**
	 * Returns the upper bound of the nth argument of this function.
	 */
	double getUpperBound(int n);

	/**
	 * Returns a copy of this function.
	 */
//	DemographicFunction getCopy();

    /**
     * A threshold for underflow on calculation of likelihood of internode intervals.
     * Most demo functions could probably return 0.0 but (e.g.,) the Extended Skyline
     * needs a non zero value to prevent a numerical problem. 
     * @return
     */
    default double getThreshold() {
        return 0;
    }

    RombergIntegrator numericalIntegrator = new RombergIntegrator();

    /**
     * Returns the integral of 1/N(x) between start and finish, calling either the getAnalyticalIntegral or
     * getNumericalIntegral function as appropriate.
     */
    default double getNumericalIntegral(double start, double finish) {
        // AER 19th March 2008: I switched this to use the RombergIntegrator from
        // commons-math v1.2.

        if (start > finish) {
            throw new RuntimeException("NumericalIntegration start > finish");
        }

        if (start == finish) {
            return 0.0;
        }

        try {
            return numericalIntegrator.integrate(Integer.MAX_VALUE, this, start, finish);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }

    // **************************************************************
    // UnivariateRealFunction IMPLEMENTATION
    // **************************************************************

    /**
     * Return the intensity at a given time for numerical integration
     * @param x the time
     * @return the intensity
     */
    default double value(double x) {
        return 1.0 / getDemographic(x);
    }

    default double getInterval(double U, int lineageCount, double timeOfLastCoalescent) {
        final double intensity = getIntensity(timeOfLastCoalescent);
        final double tmp = -Math.log(U)/CombinatoricsUtils.binomialCoefficientDouble(lineageCount, 2) + intensity;

        return getInverseIntensity(tmp) - timeOfLastCoalescent;
    }

    default double getInterval(double U, int lineageCount, double timeOfLastCoalescent, double earliestTimeOfFinalCoalescent){
        if(timeOfLastCoalescent>earliestTimeOfFinalCoalescent){
            throw new IllegalArgumentException("Given maximum height is smaller than given final coalescent time");
        }
        final double fullIntegral = getIntegral(timeOfLastCoalescent,
                earliestTimeOfFinalCoalescent);
        final double normalisation = 1-Math.exp(-CombinatoricsUtils.binomialCoefficientDouble(lineageCount, 2)*fullIntegral);
        final double intensity = getIntensity(timeOfLastCoalescent);

        double tmp = -Math.log(1-U*normalisation)/CombinatoricsUtils.binomialCoefficientDouble(lineageCount, 2) + intensity;

        return getInverseIntensity(tmp) - timeOfLastCoalescent;

    }

    /**
     * @return a random interval size selected from the Kingman prior of the demographic model.
     */
    default double getSimulatedInterval(int lineageCount, double timeOfLastCoalescent)
    {
        final double U = MathUtils.nextDouble(); // create unit uniform random variate
        return getInterval(U, lineageCount, timeOfLastCoalescent);
    }

    default double getSimulatedInterval(int lineageCount, double timeOfLastCoalescent, double earliestTimeOfFirstCoalescent){
        final double U = MathUtils.nextDouble();
        return getInterval(U, lineageCount, timeOfLastCoalescent, earliestTimeOfFirstCoalescent);
    }

    default double getMedianInterval(int lineageCount, double timeOfLastCoalescent)
    {
        return getInterval(0.5, lineageCount, timeOfLastCoalescent);
    }

    /**
     * This function tests the consistency of the
     * getIntensity and getInverseIntensity methods
     * of this demographic model. If the model is
     * inconsistent then a RuntimeException will be thrown.
     * @param steps the number of steps between 0.0 and maxTime to test.
     * @param maxTime the maximum time to test.
     */
    default void testConsistency( int steps, double maxTime) {

        double delta = maxTime / (double)steps;

        for (int i = 0; i <= steps; i++) {
            double time = (double)i * delta;
            double intensity = getIntensity(time);
            double newTime = getInverseIntensity(intensity);

            if (Math.abs(time - newTime) > 1e-12) {
                throw new RuntimeException(
                        "Demographic model not consistent! error size = " +
                                Math.abs(time-newTime));
            }
        }
    }

    public abstract class Abstract implements DemographicFunction
	{
       // private static final double LARGE_POSITIVE_NUMBER = 1.0e50;
//        private static final double LARGE_NEGATIVE_NUMBER = -1.0e50;
//        private static final double INTEGRATION_PRECISION = 1.0e-5;
//        private static final double INTEGRATION_MAX_ITERATIONS = 50;


        /**
		 * Construct demographic model with default settings
		 */
		public Abstract(Type units) {
			setUnits(units);
        }

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
		public void setUnits(Type u)
		{
			units = u;
		}

		/**
		 * returns units of measurement.
		 */
		public Type getUnits()
		{
			return units;
		}
	}

    @Deprecated
	public static class Utils
	{
        /**
         * @return a random interval size selected from the Kingman prior of the demographic model.
         */
        @Deprecated
		public static double getSimulatedInterval(DemographicFunction demographicFunction,
                                                  int lineageCount, double timeOfLastCoalescent)
		{
			return demographicFunction.getSimulatedInterval(lineageCount, timeOfLastCoalescent);
		}

        @Deprecated
        public static double getSimulatedInterval(DemographicFunction demographicFunction, int lineageCount,
                                                  double timeOfLastCoalescent, double earliestTimeOfFirstCoalescent){
            return demographicFunction.getSimulatedInterval(lineageCount, timeOfLastCoalescent, earliestTimeOfFirstCoalescent);
        }

        @Deprecated
		public static double getMedianInterval(DemographicFunction demographicFunction,
                                               int lineageCount, double timeOfLastCoalescent)
		{
             return demographicFunction.getMedianInterval(lineageCount, timeOfLastCoalescent);
		}

		/**
		 * This function tests the consistency of the
		 * getIntensity and getInverseIntensity methods
		 * of this demographic model. If the model is
		 * inconsistent then a RuntimeException will be thrown.
		 * @param demographicFunction the demographic model to test.
		 * @param steps the number of steps between 0.0 and maxTime to test.
		 * @param maxTime the maximum time to test.
		 */
        @Deprecated
		public static void testConsistency(DemographicFunction demographicFunction, int steps, double maxTime) {
            demographicFunction.testConsistency(steps, maxTime);
		}
	}
}