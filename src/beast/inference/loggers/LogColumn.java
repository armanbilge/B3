/*
 * LogColumn.java
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

/**
 * An interface for a column in a log.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: LogColumn.java,v 1.5 2005/05/24 20:25:59 rambaut Exp $
 */

public interface LogColumn {

    /**
     * Set the label (heading) for this column
     *
     * @param label the column label
     */
    void setLabel(String label);

    /**
     * @return the label (heading) for this column
     */
    String getLabel();

    /**
     * Set the minimum width in characters for this column
     *
     * @param minimumWidth the minimum width in characters
     */
    void setMinimumWidth(int minimumWidth);

    /**
     * @return the minimum width in characters for this column
     */
    int getMinimumWidth();

    /**
     * Returns a string containing the current value for this column with
     * appropriate formatting.
     *
     * @return the formatted string.
     */
    String getFormatted();

    public abstract class Abstract implements LogColumn {

        private String label;
        private int minimumWidth;

        public Abstract(String label) {

            setLabel(label);
            minimumWidth = -1;
        }

        public void setLabel(String label) {
            if (label == null) throw new IllegalArgumentException("column label is null");
            this.label = label;
        }

        public String getLabel() {
            StringBuffer buffer = new StringBuffer(label);

            if (minimumWidth > 0) {
                while (buffer.length() < minimumWidth) {
                    buffer.append(' ');
                }
            }

            return buffer.toString();
        }

        public void setMinimumWidth(int minimumWidth) {
            this.minimumWidth = minimumWidth;
        }

        public int getMinimumWidth() {
            return minimumWidth;
        }

        public final String getFormatted() {
            StringBuffer buffer = new StringBuffer(getFormattedValue());

            if (minimumWidth > 0) {
                while (buffer.length() < minimumWidth) {
                    buffer.append(' ');
                }
            }

            return buffer.toString();
        }

        protected abstract String getFormattedValue();
    }

    public class Default extends Abstract {

        private Object object;

        public Default(String label, Object object) {
            super(label);
            this.object = object;
        }

        protected String getFormattedValue() {
            return object.toString();
        }
    }

}
