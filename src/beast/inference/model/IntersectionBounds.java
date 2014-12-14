/*
 * IntersectionBounds.java
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

package beast.inference.model;

import java.util.ArrayList;


public class IntersectionBounds implements Bounds<Double> {

    IntersectionBounds(int dimension) {
        this.dimension = dimension;
    }

    public void addBounds(Bounds<Double> boundary) {
        if (boundary.getBoundsDimension() != dimension) {
            throw new IllegalArgumentException("Incorrect dimension of bounds, expected " +
                    dimension + " but received " + boundary.getBoundsDimension());
        }
        if (bounds == null) {
            bounds = new ArrayList<Bounds<Double>>();
        }
        bounds.add(boundary);
    }

    /**
     * Gets the maximum lower limit of this parameter and all its slave parameters
     */
    public Double getLowerLimit(int index) {

        double lower = Double.NEGATIVE_INFINITY;
        if (bounds != null) {
            for (Bounds<Double> boundary : bounds) {
                if (boundary.getLowerLimit(index) > lower) {
                    lower = boundary.getLowerLimit(index);
                }
            }
        }
        return lower;
    }

    /**
     * Gets the minimum upper limit of this parameter and all its slave parameters
     */
    public Double getUpperLimit(int index) {

        double upper = Double.POSITIVE_INFINITY;
        if (bounds != null) {
            for (Bounds<Double> boundary : bounds) {
                if (boundary.getUpperLimit(index) < upper) {
                    upper = boundary.getUpperLimit(index);
                }
            }
        }
        return upper;
    }

    public int getBoundsDimension() {
        return dimension;
    }

    public String toString() {
        String str = "upper=[" + getUpperLimit(0);
        for (int i = 1; i < getBoundsDimension(); i++) {
            str += ", " + getUpperLimit(i);
        }
        str += "] lower=[" + getLowerLimit(0);
        for (int i = 1; i < getBoundsDimension(); i++) {
            str += ", " + getLowerLimit(i);
        }

        str += "]";
        return str;
    }

    private ArrayList<Bounds<Double>> bounds = null;
    private final int dimension;
}
