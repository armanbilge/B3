/*
 * LogNormalDistributionTest.java
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

package beast.math.distribution;

import beast.math.distributions.LogNormalDistribution;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexei Drummond
 */
public class LogNormalDistributionTest {

    LogNormalDistribution logNormal;

    @Before
    public void setUp() {

        logNormal = new LogNormalDistribution(1.0, 2.0);
    }

    @Test
    public void testPdf() {

        System.out.println("Testing 10000 random pdf calls");

        for (int i = 0; i < 10000; i++) {
            double M = Math.random() * 10.0 - 5.0;
            double S = Math.random() * 10;

            double x = Math.log(Math.random() * 10);

            logNormal.setM(M);
            logNormal.setS(S);

            double pdf = 1.0 / (x * S * Math.sqrt(2 * Math.PI)) * Math.exp(-Math.pow(Math.log(x) - M, 2) / (2 * S * S));
            if (x <= 0) pdf = 0; // see logNormal.pdf(x)

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].pdf(" + x + ")");

            assertEquals(pdf, logNormal.pdf(x), 1e-10);
        }
    }

    @Test
    public void testMean() {

        for (int i = 0; i < 1000; i++) {
            double M = Math.random() * 10.0 - 5.0;
            double S = Math.random() * 10;

            logNormal.setM(M);
            logNormal.setS(S);

            double mean = Math.exp(M + S * S / 2);

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].mean()");

            assertEquals(mean, logNormal.mean(), 1e-10);
        }
    }

    @Test
    public void testVariance() {

        for (int i = 0; i < 1000; i++) {
            double M = Math.random() * 10.0 - 5.0;
            double S = Math.random() * 10;

            logNormal.setM(M);
            logNormal.setS(S);

            double variance = (Math.exp(S * S) - 1) * Math.exp(2 * M + S * S);

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].variance()");

            assertEquals(variance, logNormal.variance(), 1e-10);
        }
    }

    @Test
    public void testMedian() {

        System.out.println("Testing 10000 random quantile(0.5) calls");

        for (int i = 0; i < 10000; i++) {
            double M = Math.random() * 10.0 - 5.0;
            double S = Math.random() * 10;

            logNormal.setM(M);
            logNormal.setS(S);

            double median = Math.exp(M);

            //System.out.println("Testing logNormal[M=" + M + " S=" + S + "].median()");

            assertEquals(median, logNormal.quantile(0.5), median / 1e6);
        }
    }

    @Test
    public void testCDFAndQuantile() {

        System.out.println("Testing 10000 random quantile/cdf pairs");

        for (int i = 0; i < 10000; i++) {

            double M = Math.random() * 10.0 - 5.0;
            double S = Math.random() * 10;

            logNormal.setM(M);
            logNormal.setS(S);

            double p = Math.random();
            double quantile = logNormal.quantile(p);

            double cdf = logNormal.cdf(quantile);

            assertEquals(p, cdf, 1e-8);
        }
    }

//    public void testCDFAndQuantile2() {
//
//        final LogNormalDistribution f = new LogNormalDistribution(1, 1);
//        for (double i = 0.01; i < 0.95; i += 0.01) {
//            final double y = i;
//
//            BisectionZeroFinder zeroFinder = new BisectionZeroFinder(new OneVariableFunction() {
//                public double value(double x) {
//                    return f.cdf(x) - y;
//                }
//            }, 0.01, 100);
//            zeroFinder.evaluate();
//
//            assertEquals(f.quantile(i), zeroFinder.getResult(), 1e-6);
//        }
//    }

}
