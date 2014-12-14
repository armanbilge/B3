/*
 * HalfTDistribution.java
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

/**
 * @author Marc A. Suchard
 */
public class HalfTDistribution extends TDistribution {

    public HalfTDistribution(double scale, double df) {
        super(0.0, scale, df);
    }

    public double pdf(double x) {
        return x < 0.0 ? 0.0 : super.pdf(x) * 2.0;
    }

    public double logPdf(double x) {
        return x < 0.0 ? Double.NEGATIVE_INFINITY : super.logPdf(x) + Math.log(2.0);
    }

    public double mean() {
        throw new RuntimeException("Not yet implemented");
    }

    public double variance() {
        throw new RuntimeException("Not yet implemented");
    }

}
