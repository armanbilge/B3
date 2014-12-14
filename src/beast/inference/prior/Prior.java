/*
 * Prior.java
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

package beast.inference.prior;

import beast.inference.model.Model;

/**
 * This interface provides for general priors on models.
 *
 * @author Alexei Drummond
 * @version $Id: Prior.java,v 1.3 2005/05/24 20:26:00 rambaut Exp $
 */
public interface Prior {

    public static final class UniformPrior implements Prior {
        public double getLogPrior(Model m) {
            return 0.0;
        }

        public String getPriorName() {
            return "Uniform";
        }

        public String toString() {
            return "Uniform";
        }
    }

    public static final UniformPrior UNIFORM_PRIOR = new UniformPrior();


    /**
     * @param model the model under inspection.
     * @return the log prior of some aspect of the given model.
     */
    public double getLogPrior(Model model);

    /**
     * Returns the logical name of this prior. This name should be
     * the same as the string returned in the name attribute of the
     * XML prior.
     *
     * @return the logical name of this prior.
     */
    public String getPriorName();
}
