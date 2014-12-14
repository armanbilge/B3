/*
 * BranchRateModel.java
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

package beast.evomodel.branchratemodel;

import beast.evolution.tree.BranchRates;
import beast.evolution.tree.TreeTrait;
import beast.evolution.tree.TreeTraitProvider;
import beast.inference.model.Model;

/**
 * Date: Dec 13, 2004
 * Time: 1:59:24 PM
 *
 * @author Alexei Drummond
 * @version $Id: BranchRateModel.java,v 1.4 2005/05/24 20:25:57 rambaut Exp $
 */
public interface BranchRateModel extends Model, BranchRates, TreeTraitProvider, TreeTrait<Double> {
    public static final String BRANCH_RATES = "branchRates";
    public static final String RATE = "rate";

    // This is inherited from BranchRates:
    // double getBranchRate(Tree tree, NodeRef node);
}
