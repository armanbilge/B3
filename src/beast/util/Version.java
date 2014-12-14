/*
 * Version.java
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

package beast.util;

/**
 * Version last changed 2004/05/07 by AER
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Version.java,v 1.13 2005/07/11 14:06:25 rambaut Exp $
 */
public interface Version {

    String getVersion();

	String getVersionString();

	String getBuildString();

	String getDateString();

    String[] getCredits();

    String getHTMLCredits();
}
