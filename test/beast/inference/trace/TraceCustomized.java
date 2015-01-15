/*
 * TraceCustomized.java
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
 * Make a new trace calculated from an existing trace in log
 *
 * @author Walter Xie
 */
public class TraceCustomized extends Trace{

    public TraceCustomized(String name) { // traceType = TraceFactory.TraceType.DOUBLE;
        super(name);
    }

    public void addValues(Trace<Double> t) {
        Double r = 1.0;
        for (Double v : t.values) {
            Double newV = 2.0 / (1.0 + v * r);
            super.values.add(newV);
        }
    }
}