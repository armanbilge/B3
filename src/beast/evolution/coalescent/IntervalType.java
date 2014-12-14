/*
 * IntervalType.java
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

package beast.evolution.coalescent;

/**
 * Specifies the interval types.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: IntervalType.java,v 1.9 2005/05/24 20:25:56 rambaut Exp $
 */
public enum IntervalType {

    /**
     * Denotes an interval at the end of which a new sample addition is
     * observed (i.e. the number of lineages is larger in the next interval).
     */
    SAMPLE("sample"),

    /**
     * Denotes an interval after which a coalescent event is observed
     * (i.e. the number of lineages is smaller in the next interval)
     */
    COALESCENT("coalescent"),

    /**
     * Denotes an interval at the end of which a migration event occurs.
     * This means that the colour of one lineage changes.
     */
    MIGRATION("migration"),

    /**
     * Denotes an interval at the end of which nothing is
     * observed (i.e. the number of lineages is the same in the next interval).
     */
    NOTHING("nothing");

    /**
     * private constructor.
     */
    private IntervalType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    private final String name;
}