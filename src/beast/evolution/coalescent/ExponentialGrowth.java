/*
 * ExponentialGrowth.java
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
 * This class models an exponentially growing (or shrinking) population
 * (Parameters: N0=present-day population size; r=growth rate).
 * This model is nested with the constant-population size model (r=0).
 *
 * @version $Id: ExponentialGrowth.java,v 1.7 2005/05/24 20:25:56 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class ExponentialGrowth extends ConstantPopulation {

    /**
     * Construct demographic model with default settings
     * @param units of time
     */
    public ExponentialGrowth(Type units) {

        super(units);
    }

    /**
     * @return growth rate.
     */
    public final double getGrowthRate() { return r; }

    /**
     * sets growth rate to r.
     * @param r
     */
    public void setGrowthRate(double r) { this.r = r; }

    /**
     * An alternative parameterization of this model. This
     * function sets growth rate for a given doubling time.
     * @param doublingTime
     */
    public void setDoublingTime(double doublingTime) {
        setGrowthRate( Math.log(2) / doublingTime );
    }

    // Implementation of abstract methods

    public double getDemographic(double t) {

        double r = getGrowthRate();
        if (r == 0) {
            return getN0();
        } else {
            return getN0() * Math.exp(-t * r);
        }
    }

    /**
     * Calculates the integral 1/N(x) dx between start and finish.
     */
    @Override
    public double getIntegral(double start, double finish) {
        double r = getGrowthRate();
        if (r == 0.0) {
            return (finish - start)/getN0();
        } else {
            return (Math.exp(finish*r) - Math.exp(start*r))/getN0()/r;
        }
    }

    public double getIntensity(double t)
    {
        double r = getGrowthRate();
        if (r == 0.0) {
            return t/getN0();
        } else {
            return (Math.exp(t*r)-1.0)/getN0()/r;
        }
    }

    public double getInverseIntensity(double x) {

        double r = getGrowthRate();
        if (r == 0.0) {
            return getN0()*x;
        } else {
            return Math.log(1.0+getN0()*x*r)/r;
        }
    }

    public int getNumArguments() {
        return 2;
    }

    public String getArgumentName(int n) {
        if (n == 0) {
            return "N0";
        } else {
            return "r";
        }
    }

    public double getArgument(int n) {
        if (n == 0) {
            return getN0();
        } else {
            return getGrowthRate();
        }
    }

    public void setArgument(int n, double value) {
        if (n == 0) {
            setN0(value);
        } else {
            setGrowthRate(value);
        }
    }

    public double getLowerBound(int n) {
        return 0.0;
    }

    public double getUpperBound(int n) {
        return Double.POSITIVE_INFINITY;
    }

    public DemographicFunction getCopy() {
        ExponentialGrowth df = new ExponentialGrowth(getUnits());
        df.setN0(getN0());
        df.r = r;

        return df;
    }

    //
    // private stuff
    //

    private double r;
}
