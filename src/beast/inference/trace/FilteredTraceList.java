/*
 * FilteredTraceList.java
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
 * not available for CombinedTraces yet
 */
public abstract class FilteredTraceList implements TraceList {

//    protected boolean[] selected; // length = values[].length = getValuesSize, all must be true initially

//    private void createSelected() { // will init in updateSelected()
//        if (getTrace(0) != null) {
//            selected = new boolean[getTrace(0).getValuesSize()];
//        } else {
//            throw new RuntimeException("Cannot initial filters ! getTrace(0) failed !");
//        }
//    }

//    private void initSelected() {
//        for (int i = 0; i < selected.length; i++) {
//            selected[i] = true;
//        }
//    }

    public boolean hasFilter(int traceIndex) {
//        if (selected == null) return false;
        return getTrace(traceIndex).getFilter() != null;
    }

    public void setFilter(int traceIndex, Filter filter) {
//        if (selected == null) createSelected();
        getTrace(traceIndex).setFilter(filter);
        refreshStatistics();
    }

    public Filter getFilter(int traceIndex) {
//        if (selected == null) return null;
        return getTrace(traceIndex).getFilter();
    }

    public void removeFilter(int traceIndex) {
        getTrace(traceIndex).setFilter(null);
        refreshStatistics();
    }

    public void removeAllFilters() {
        for (int i = 0; i < getTraceCount(); i++) {
            getTrace(i).setFilter(null);
        }
//        selected = null;
        refreshStatistics();// must be after "selected = null"
    }

    protected void refreshStatistics() {
//        updateSelected();
        for (int i = 0; i < getTraceCount(); i++) {
            analyseTrace(i);
        }
    }

//    private void updateSelected() {
//        if (selected != null) {
//            initSelected();
//            for (int traceIndex = 0; traceIndex < getTraceCount(); traceIndex++) {
//                if (getFilter(traceIndex) != null) {
//                    Trace trace = getTrace(traceIndex);
//                    if (trace.getValuesSize() != selected.length)
//                        throw new RuntimeException("updateSelected: length of values[] is different with selected[] in Trace "
//                                + getTraceName(traceIndex));
//
//                    for (int i = 0; i < trace.getValuesSize(); i++) {
//                        if (!trace.isIn(i)) { // not selected
//                            selected[i] = false;
//                        }
//                    }
//                }
//            }
//        }
//    }
}
