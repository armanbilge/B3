/*
 * Distribution.java
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

import beast.math.UnivariateFunction;

/**
 * an interface for a distribution.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Distribution.java,v 1.7 2005/05/24 20:26:00 rambaut Exp $
 */
public interface Distribution {
    /**
     * probability density function of the distribution
     *
     * @param x argument
     * @return pdf value
     */
    public double pdf(double x);

    /**
     * the natural log of the probability density function of the distribution
     *
     * @param x argument
     * @return log pdf value
     */
    public double logPdf(double x);

    /**
     * cumulative density function of the distribution
     *
     * @param x argument
     * @return cdf value
     */
    public double cdf(double x);

    /**
     * quantile (inverse cumulative density function) of the distribution
     *
     * @param y argument
     * @return icdf value
     */
    public double quantile(double y);

    /**
     * mean of the distribution
     *
     * @return mean
     */
    public double mean();

    /**
     * variance of the distribution
     *
     * @return variance
     */
    public double variance();

    /**
     * @return a probability density function representing this distribution
     */
    public UnivariateFunction getProbabilityDensityFunction();

}
