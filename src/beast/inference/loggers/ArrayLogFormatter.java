/*
 * ArrayLogFormatter.java
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

package beast.inference.loggers;

import beast.inference.trace.Trace;
import beast.inference.trace.TraceFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexei Drummond
 */
public class ArrayLogFormatter implements LogFormatter {

    String heading;
    String[] labels = null;
    List<String> lines = new ArrayList<String>();
    List<Trace> traces = new ArrayList<Trace>();
    boolean echo = false;

    public ArrayLogFormatter(boolean echo) {
        this.echo = echo;
    }

    public void startLogging(String title) {
    }

    public void logHeading(String heading) {
        this.heading = heading;
        echo(heading);
    }

    public void logLine(String line) {
        lines.add(line);
        echo(line);
    }

    public void logLabels(String[] labels) {
        if (this.labels == null) {
            this.labels = labels;
            for (String label : labels) {
                traces.add(new Trace<Double>(label, TraceFactory.TraceType.DOUBLE));
            }
            echo(labels);
        } else throw new RuntimeException("logLabels() method should only be called once!");
    }

    public void logValues(String[] values) {
        for (int i = 0; i < values.length; i++) {
//            Double v = Double.parseDouble(values[i]);
            traces.get(i).add(Double.parseDouble(values[i]));
        }
        echo(values);
    }

    public void stopLogging() {
    }

    public List<Trace> getTraces() {
        return traces;
    }

    private void echo(String s) {
        if (echo) System.out.println(s);
    }

    private void echo(String[] strings) {
        if (echo) {
            for (String s : strings) {
                System.out.print(s + "\t");
            }
            System.out.println();
        }
    }


}
