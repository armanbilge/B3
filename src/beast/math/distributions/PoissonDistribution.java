/*
 * PoissonDistribution.java
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

package beast.math.distributions;

import beast.math.Poisson;
import beast.math.UnivariateFunction;

/**
 * @author Alexei Drummond
 * @version $Id$
 */
public class PoissonDistribution implements Distribution {

    org.apache.commons.math3.distribution.PoissonDistribution distribution;

    public PoissonDistribution(double mean) {
        distribution = new org.apache.commons.math3.distribution.PoissonDistribution(mean);
    }

    public double pdf(double x) {
        return distribution.probability((int) x);
    }

    public double logPdf(double x) {

        double pdf = distribution.probability((int) x);
        if (pdf == 0 || Double.isNaN(pdf)) { // bad estimate
            final double mean = mean();
            return x * Math.log(mean) - Poisson.gammln(x + 1) - mean;
        }
        return Math.log(pdf);

    }

    public double differentiateLogPdf(double x) {
        throw new RuntimeException("Can't differentiate against discrete variables!");
    }

    public double cdf(double x) {
        return distribution.cumulativeProbability((int) x);
    }

    public double quantile(double y) {
        return distribution.inverseCumulativeProbability(y);
    }

    public double mean() {
        return distribution.getMean();
    }

    public double variance() {
        return distribution.getMean();
    }

    public UnivariateFunction getProbabilityDensityFunction() {
        throw new RuntimeException();
    }

    public double truncatedMean(int max) {

        double CDF = 0;
        double mean = 0;
        for(int i=0; i<=max; i++) {
            double p = distribution.probability(i);
            mean += i*p;
            CDF += p;
        }
        return mean / CDF;        
    }
}
