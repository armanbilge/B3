/*
 * Differentiable.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
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

package beast.math;

import beast.inference.model.Variable;

import java.util.function.DoubleSupplier;

/**
 * @author Arman Bilge
 */
public interface Differentiable {

    final boolean BRUTE_FORCE = false;

    double evaluate();

    default double differentiate(Variable<Double> var, int index) {
        if (BRUTE_FORCE) return differentiate(this::evaluate, var, index);
        return 0.0;
    }

    static double differentiate(DoubleSupplier f, Variable<Double> var, int index) {
        return differentiate(f, var, index, MachineAccuracy.SQRT_EPSILON * Math.max(var.getValue(index), 1));
    }

    static double differentiate(DoubleSupplier f, Variable<Double> var, int index, double epsilon) {
        final double value = var.getValue(index);
        final double vpe = value + epsilon;
        var.setValue(index, vpe);
        final double b = f.getAsDouble();
        final double vme = value - epsilon;
        var.setValue(index, vme);
        final double a = f.getAsDouble();
        var.setValue(index, value);
        return (b - a) / (vpe - vme);
    }


}
