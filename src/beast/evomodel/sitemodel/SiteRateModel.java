/*
 * SiteRateModel.java
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

package beast.evomodel.sitemodel;

import beast.inference.model.Model;

/**
 * @author Alexei J. Drummond
 * @author Andrew Rambaut
 * @author Marc A. Suchard
 *         <p/>
 *         An attempt to harmonize **.beagle.**.SiteRateModel and **.SiteModel
 */

public interface SiteRateModel extends Model {

    /**
     * @return the number of categories of substitution processes
     */
    int getCategoryCount();

    /**
     * Get an array of the relative rates of sites in each category. These
     * may include the 'mu' parameter, an overall scaling of the siteRateModel.
     *
     * @return an array of the rates.
     */
    double[] getCategoryRates();

    /**
     * Get an array of the expected proportion of sites in each category.
     *
     * @return an array of the proportions.
     */
    double[] getCategoryProportions();

    /**
     * Get the rate for a particular category. This may include the 'mu'
     * parameter, an overall scaling of the siteRateModel.
     *
     * @param category the category number
     * @return the rate.
     */
    double getRateForCategory(int category);

    /**
     * Get the expected proportion of sites in this category.
     *
     * @param category the category number
     * @return the proportion.
     */
    double getProportionForCategory(int category);

}
