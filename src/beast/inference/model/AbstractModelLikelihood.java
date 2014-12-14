/*
 * AbstractModelLikelihood.java
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

import beast.inference.loggers.LogColumn;
import beast.inference.loggers.NumberColumn;

/**
 * @author Joseph Heled
 *         Date: 16/04/2009
 */
public abstract class AbstractModelLikelihood extends AbstractModel implements Likelihood {
    /**
     * @param name Model Name
     */
    public AbstractModelLikelihood(String name) {
        super(name);
    }

    public String prettyName() {
        return Likelihood.Abstract.getPrettyName(this);
    }

    @Override
    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed() {
        isUsed = true;
    }

    public boolean evaluateEarly() {
        return false;
    }

    private boolean isUsed = false;

    // **************************************************************
    // Loggable IMPLEMENTATION
    // **************************************************************

    public LogColumn[] getColumns() {
        return new LogColumn[]{
                new LikelihoodColumn(getId())
        };
    }

    protected class LikelihoodColumn extends NumberColumn {
        public LikelihoodColumn(String label) {
            super(label);
        }

        public double getDoubleValue() {
            return getLogLikelihood();
        }
    }
}
