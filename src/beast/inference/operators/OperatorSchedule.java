/*
 * OperatorSchedule.java
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

package beast.inference.operators;

import java.util.List;

/**
 * An interface the defines an operator schedule for use in
 * choosing the next operator during an MCMC run.
 *
 * @author Alexei Drummond
 * @version $Id: OperatorSchedule.java,v 1.3 2005/05/24 20:26:00 rambaut Exp $
 */
public interface OperatorSchedule {
    /**
     * @return Choose the next operator.
     */
    int getNextOperatorIndex();

    /**
     * @return Total number of operators
     */
    int getOperatorCount();

    /**
     * @param index
     * @return the index'th operator
     */
    MCMCOperator getOperator(int index);

    void addOperator(MCMCOperator op);

    void addOperators(List<MCMCOperator> v);

    /**
     * Should be called after operators weight is externally changed.
     */
    void operatorsHasBeenUpdated();

    /**
     * @return the optimization schedule
     */
    double getOptimizationTransform(double d);

    /**
     * @return the minimum number of times an operator has been called
     */
    int getMinimumAcceptAndRejectCount();

    final int DEFAULT_SCHEDULE = 0;
    final int LOG_SCHEDULE = 1;
    final int SQRT_SCHEDULE = 2;

    final String DEFAULT_STRING = "default";
    final String LOG_STRING = "log";
    final String SQRT_STRING = "sqrt";
}
