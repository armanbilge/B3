/*
 * Reportable.java
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

package beast.xml;

/**
 * This is an alternative interface for items added to reports. By default 'toString' will be called
 * but for some objects this causes issues with debugging because the debugger also calls toString which
 * may cause a whole lot of computation which messes up the debugging. If a class implements this interface
 * then this will be called instead.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id$
 */
public interface Reportable {
    String getReport();
}
