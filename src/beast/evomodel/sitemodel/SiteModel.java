/*
 * SiteModel.java
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

import beast.evomodel.substmodel.FrequencyModel;
import beast.evomodel.substmodel.SubstitutionModel;

/**
 * SiteModel - Specifies how rates and substitution models vary across sites.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: SiteModel.java,v 1.77 2005/05/24 20:25:58 rambaut Exp $
 */

public abstract class SiteModel extends SiteRateModel {

    public static final String SITE_MODEL = "siteModel";

    /**
     * @param name Model Name
     */
    public SiteModel(String name) {
        super(name);
    }

    /**
     * Get this site model's substitution model
     *
     * @return the substitution model
     */
    public abstract SubstitutionModel getSubstitutionModel();

    /**
     * Specifies whether SiteModel should integrate over the different categories at
     * each site. If true, the SiteModel will calculate the likelihood of each site
     * for each category. If false it will assume that there is each site can have a
     * different category.
     *
     * @return the boolean
     */
    public abstract boolean integrateAcrossCategories();  // TODO Consider moving into SiteRateModel

    /**
     * Get the category of a particular site. If integrateAcrossCategories is true.
     * then throws an IllegalArgumentException.
     *
     * @param site the index of the site
     * @return the index of the category
     */
    public abstract int getCategoryOfSite(int site);    // TODO Consider moving into SiteRateModel

    /**
     * Get the frequencyModel for this SiteModel.
     *
     * @return the frequencyModel.
     */
    public abstract FrequencyModel getFrequencyModel();
}