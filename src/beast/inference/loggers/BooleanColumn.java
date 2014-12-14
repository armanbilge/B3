/*
 * BooleanColumn.java
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
 *
 * Boolean formating (C style) 0 is false, anything else true;
 * @author Joseph Heled
 *         Date: 4/06/2008
 */

public class BooleanColumn implements LogColumn {
    private final NumberColumn column;

    public BooleanColumn(NumberColumn column) {
        this.column = column;
    }

    public void setLabel(String label) {
        column.setLabel(label);
    }

    public String getLabel() {
        return column.getLabel();
    }

    public void setMinimumWidth(int minimumWidth) {
        // ignore
    }

    public int getMinimumWidth() {
        return "false".length();
    }

    public String getFormatted() {
        if( column.getDoubleValue() == 0.0 ) {
            return "false";
        }
        return "true";
    }
}
