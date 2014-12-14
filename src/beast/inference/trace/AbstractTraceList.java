/*
 * AbstractTraceList.java
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

package beast.inference.trace;

/**
 * @author Alexei Drummond
 */
public abstract class AbstractTraceList extends FilteredTraceList {
    public TraceDistribution getDistributionStatistics(int index) {
        return getCorrelationStatistics(index);
    }

    public TraceCorrelation getCorrelationStatistics(int index) {
        Trace trace = getTrace(index);
        if (trace == null) {
            return null;
        }
        return trace.getTraceStatistics();
    }

    public void analyseTrace(int index) {
        int start = (getBurnIn() / getStepSize());

//        if (traceStatistics == null) {
//            traceStatistics = new TraceCorrelation[getTraceCount()];
//            initFilters();
//        }

        Trace trace = getTrace(index);        
        TraceCorrelation traceCorrelation = new TraceCorrelation(
                trace.getValues(start, trace.getValuesSize()),
                trace.getTraceType(), getStepSize());
        trace.setTraceStatistics(traceCorrelation);

//        System.out.println("index = " + index + " :  " + trace.getName() + "     " + trace.getTraceType());
    }

//    public void setBurnIn(int burnIn) {
//        traceStatistics = null;
//    }

//    abstract Trace getTrace(int index);

//    private TraceCorrelation[] traceStatistics = null;
}
