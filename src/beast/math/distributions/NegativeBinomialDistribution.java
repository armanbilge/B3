/*
 * NegativeBinomialDistribution.java
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
import beast.math.GammaFunction;
import beast.math.UnivariateFunction;
import org.apache.commons.math3.special.Beta;

/**
 * @author Trevor Bedford
 * @version $Id$
 */
public class NegativeBinomialDistribution implements Distribution {

    double mean;
    double stdev;

    public NegativeBinomialDistribution(double mean, double stdev) {
        this.mean = mean;
        this.stdev = stdev;
    }

    public double pdf(double x) {
        if (x < 0)  return 0;
        return Math.exp(logPdf(x));
    }

    public double logPdf(double x) {
        if (x < 0)  return Double.NEGATIVE_INFINITY;
        double r = -1 * (mean*mean) / (mean - stdev*stdev);
        double p = mean / (stdev*stdev);
        return Math.log(Math.pow(1-p,x)) + Math.log(Math.pow(p, r)) + GammaFunction.lnGamma(r+x) - GammaFunction.lnGamma(r) - GammaFunction.lnGamma(x+1);
    }

    public double cdf(double x) {
        double r = -1 * (mean*mean) / (mean - stdev*stdev);
        double p = mean / (stdev*stdev);
        return Beta.regularizedBeta(p, r, x + 1);
    }

    public double quantile(double y) {
        // TB - I'm having trouble implementing this
        return Double.NaN;
    }

    public double mean() {
        return mean;
    }

    public double variance() {
        return stdev*stdev;
    }

    public UnivariateFunction getProbabilityDensityFunction() {
        throw new RuntimeException();
    }

    public static void main(String[] args) {
        System.out.println("Test negative binomial");
        System.out.println("Mean 5, sd 5, x 5, pdf 0.074487, logPdf -2.59713");
        NegativeBinomialDistribution dist = new NegativeBinomialDistribution(5, 5);
        System.out.println("pdf = " + dist.pdf(5));
        System.out.println("logPdf = " + dist.logPdf(5));
    }

}
