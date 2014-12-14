/*
 * ArrayTraceList.java
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Alexei Drummond
 */
public class ArrayTraceList extends AbstractTraceList {

    String name;
    Map<String, Integer> traceIndex = new HashMap<String, Integer>();
    int burnin = 0;
    List<Trace> traces;
    int stepSize;

    public ArrayTraceList(String name, List<Trace> traces, int burnin) {
        this.name = name;
        this.traces = traces;
        this.burnin = burnin;

        for (int i = 0; i < traces.size(); i++) {
            traceIndex.put(traces.get(i).getName(), i);
        }

        Trace stateTrace = traces.get(0);
        this.stepSize = (int) Math.round((Double) stateTrace.getValue(1) - (Double) stateTrace.getValue(0));
    }

    public String getName() {
        return name;
    }

    public int getTraceCount() {
        return traces.size();
    }

    public int getTraceIndex(String name) {
        Integer index = traceIndex.get(name);
        if (index == null) return -1;
        return index;
    }

    public String getTraceName(int index) {
        return traces.get(index).getName();
    }

    /**
     * @return the burn-in for this trace list (the number of sampled states to discard)
     */
    public int getBurnIn() {
        return burnin;
    }

    /**
     * @return the number of states in the traces (after burnin removed)
     */
    public int getStateCount() {
        return traces.get(0).getValuesSize();
    }

    /**
     * @return the number of states in the burnin
     */
    public int getBurninStateCount() {
        return (getBurnIn() / stepSize);
    }

    public int getStepSize() {
        return stepSize;
    }

    public int getMaxState() {
        return getStateCount() * getStepSize();
    }

    public boolean isIncomplete() {
        return false;
    }

    public List getValues(int index, int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("not available");
    }

    public List getValues(int index) {
        throw new UnsupportedOperationException("not available");
    }

    public List getBurninValues(int index) {
        throw new UnsupportedOperationException("not available");
    }

    public Trace getTrace(int index) {
        return traces.get(index);
    }
}
