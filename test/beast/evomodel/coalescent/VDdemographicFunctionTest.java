/*
 * VDdemographicFunctionTest.java
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

package beast.evomodel.coalescent;

import beast.evolution.util.Units;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Joseph Heled
 *         Date: 23/06/2009
 */
public class VDdemographicFunctionTest {
    @Test
    public void testExp() {
        // test that numerical and exact integration match (up to a point, numerical is not that good for those 
        // exponential gone to constant transitions.
        {
            double[] times = {1, 3};
            double[] logPops = {0, 2, 3};
            VariableDemographicModel.Type type = VariableDemographicModel.Type.EXPONENTIAL;
            Units.Type units = Units.Type.SUBSTITUTIONS;
            final VDdemographicFunction f = new VDdemographicFunction(times, logPops, units, type);

            double[][] vals = {{0, 2, 1e-9}, {1, 2, 1e-9}, {0, 5, 1e-6}, {2, 6, 1e-6}};
            for(double[] c : vals) {
                double v1 = f.getIntegral(c[0], c[1]);
                double v2 = f.getNumericalIntegral(c[0], c[1]);
                assertEquals(Math.abs(1 - v1 / v2), 0, c[2]);
            }
        }

        {
            double[] times = {1, 3};
            // try a const interval
            double[] logPops = {0, 0, 3};
            VariableDemographicModel.Type type = VariableDemographicModel.Type.EXPONENTIAL;
            Units.Type units = Units.Type.SUBSTITUTIONS;
            final VDdemographicFunction f = new VDdemographicFunction(times, logPops, units, type);

            double[][] vals = {{0, .7, 1e-9}, {1, 2, 1e-9}, {0, 5, 1e-6}, {2, 6, 1e-6}};
            for(double[] c : vals) {
                double v1 = f.getIntegral(c[0], c[1]);
                double v2 = f.getNumericalIntegral(c[0], c[1]);
                assertEquals(Math.abs(1 - v1 / v2), 0, c[2]);
            }
        }
    }
}
