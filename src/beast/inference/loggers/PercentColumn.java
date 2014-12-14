/*
 * PercentColumn.java
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
 * Percent column - multiplies by 100 and prepends a '%'.
 *
 * Values outside [0,1] are shown as is.
 *
 * @author Joseph Heled
 *         Date: 4/06/2008
 */
public class PercentColumn extends NumberColumn {
    private final NumberColumn column;

    public PercentColumn(NumberColumn col) {
        super(col.getLabel());
        this.column = col;
    }

    public void setSignificantFigures(int sf) {
        column.setSignificantFigures(sf);
    }
    
    public int getSignificantFigures() {
        return column.getSignificantFigures();
    }

    public void setMinimumWidth(int minimumWidth) {
      column.setMinimumWidth(minimumWidth);
    }

    public int getMinimumWidth() {
        return column.getMinimumWidth();
    }

    public String getFormattedValue() {
        double val = column.getDoubleValue();
        if( val >= 0 && val <= 1 ) {
            return column.formatValue(val * 100) + "%";
        }
        return column.getFormattedValue();
    }

    public double getDoubleValue() {
        return column.getDoubleValue();
    }
}
