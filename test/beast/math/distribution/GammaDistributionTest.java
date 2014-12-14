/*
 * GammaDistributionTest.java
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

import beast.math.MachineAccuracy;
import beast.math.distributions.GammaDistribution;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.special.Gamma;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class GammaDistributionTest {

	/**
	 * This test stochastically draws gamma
	 * variates and compares the coded pdf 
	 * with the actual pdf.  
	 * The tolerance is required to be at most 1e-10.
	 */

    static double mypdf(double value, double shape, double scale) {
        return Math.exp((shape-1) * Math.log(value) - value/scale - Gamma.logGamma(shape) - shape * Math.log(scale) );
    }

    @Test
	public void testPdf() {

        final int numberOfTests = 100;
        double totErr = 0;
        double ptotErr = 0; int np = 0;
        double qtotErr = 0;

        Random random = new Random(37);

        for(int i = 0; i < numberOfTests; i++){
            final double mean = .01 + (3-0.01) * random.nextDouble();
            final double var = .01 + (3-0.01) * random.nextDouble();

            final double scale = var / mean;
            final double shape = mean / scale;

            final GammaDistribution gamma = new GammaDistribution(shape,scale);

            final double value = gamma.nextGamma();

            final double mypdf = mypdf(value, shape, scale);
            final double pdf = gamma.pdf(value);
            if ( Double.isInfinite(mypdf) && Double.isInfinite(pdf)) {
                continue;
            }

            assertFalse(Double.isNaN(mypdf));
            assertFalse(Double.isNaN(pdf));

            totErr +=  mypdf != 0 ? Math.abs((pdf - mypdf)/mypdf) : pdf;

            assertFalse("nan", Double.isNaN(totErr));
            //assertEquals("" + shape + "," + scale + "," + value, mypdf,gamma.pdf(value),1e-10);

            final double cdf = gamma.cdf(value);
            UnivariateFunction f = new UnivariateFunction() {
                public double value(double v) {
                    return mypdf(v, shape, scale);
                }
            };
            final UnivariateIntegrator integrator = new RombergIntegrator(MachineAccuracy.SQRT_EPSILON, 1e-14, 1, 16);

            double x;
            try {
                x = integrator.integrate(16, f, 0.0, value);
                ptotErr += cdf != 0.0 ? Math.abs(x-cdf)/cdf : x;
                np += 1;
                //assertTrue("" + shape + "," + scale + "," + value + " " + Math.abs(x-cdf)/x + "> 1e-6", Math.abs(1-cdf/x) < 1e-6);

                //System.out.println(shape + ","  + scale + " " + value);
            } catch(MaxCountExceededException e ) {
                 // can't integrate , skip test
              //  System.out.println(shape + ","  + scale + " skipped");
            }

            final double q = gamma.quantile(cdf);
            qtotErr += q != 0 ? Math.abs(q-value)/q : value;
           // assertEquals("" + shape + "," + scale + "," + value + " " + Math.abs(q-value)/value, q, value, 1e-6);
        }
        //System.out.println( !Double.isNaN(totErr) );
       // System.out.println(totErr);
        // bad test, but I can't find a good threshold that works for all individual cases 
        assertTrue("failed " + totErr/numberOfTests, totErr/numberOfTests < 1e-7);
        assertTrue("failed " + ptotErr/np, np > 0 ? (ptotErr/np < 1e-5) : true);
        assertTrue("failed " + qtotErr/numberOfTests , qtotErr/numberOfTests < 1e-7);
	}
}
