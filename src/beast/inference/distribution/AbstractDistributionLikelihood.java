/*
 * AbstractDistributionLikelihood.java
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

package beast.inference.distribution;

import beast.inference.model.Likelihood;
import beast.inference.model.Model;
import beast.util.Attribute;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: AbstractDistributionLikelihood.java,v 1.4 2005/05/24 20:25:59 rambaut Exp $
 */

public abstract class AbstractDistributionLikelihood extends Likelihood.Abstract {

    public AbstractDistributionLikelihood(Model model) {

        super(model);
    }

    /**
     * Adds a statistic, this is the data for which the likelihood is calculated.
     *
     * @param data to add
     */
    public void addData(Attribute<double[]> data) {
        dataList.add(data);
    }

    protected ArrayList<Attribute<double[]>> dataList = new ArrayList<Attribute<double[]>>();

    public abstract double calculateLogLikelihood();

    /**
     * Overridden to always return false.
     */
    protected boolean getLikelihoodKnown() {
        return false;
	}

    public List<Attribute<double[]>> getDataList() {
        return dataList;
    }
}

