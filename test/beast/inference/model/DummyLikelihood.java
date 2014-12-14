/*
 * DummyLikelihood.java
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

package beast.inference.model;

/**
 * A class that always returns a log likelihood of 0 but contains models that would otherwise
 * be unregistered with the MCMC. This is an ugly solution to a rare problem.
 *
 * @author Andrew Rambaut
 *
 * @version $Id: DummyLikelihood.java,v 1.3 2005/05/24 20:26:00 rambaut Exp $
 */
public class DummyLikelihood extends Likelihood.Abstract {

	public DummyLikelihood(Model model) {
		super(model);
	}

	// **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

	/**
	 * Overridden to always return false.
	 */
	protected boolean getLikelihoodKnown() {
		return false;
	}

	/**
     * Calculate the log likelihood of the current state.
	 * If all the statistics are true then it returns 0.0 otherwise -INF.
     * @return the log likelihood.
     */
	public double calculateLogLikelihood() {
		return 0.0;
	}

}

