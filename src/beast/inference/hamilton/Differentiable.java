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

package beast.inference.hamilton;

import beast.inference.model.Bounds;
import beast.inference.model.Variable;
import beast.math.MachineAccuracy;

import java.util.function.DoubleSupplier;

/**
 * @author Arman Bilge
 */
public interface Differentiable {

    final boolean BRUTE_FORCE = true;

    double evaluate();

    default double differentiate(Variable<Double> var, int index) {
        if (BRUTE_FORCE) return differentiate(this::evaluate, var, index);
        return 0.0;
    }

    static double differentiate(DoubleSupplier f, Variable<Double> var, int index) {
        return differentiate(f, var, index, MachineAccuracy.SQRT_EPSILON * Math.max(var.getValue(index), 1));
    }

    static double differentiate(DoubleSupplier f, Variable<Double> var, int index, double epsilon) {
        final Bounds<Double> bounds = var.getBounds();
        final double upper = bounds.getUpperLimit(index);
        final double lower = bounds.getLowerLimit(index);
        final double value = var.getValue(index);
        final double vpe = value + epsilon;
        final double b = vpe <= upper ? vpe : value;
        var.setValue(index, b);
        final double fb = f.getAsDouble();
        final double vme = value - epsilon;
        final double a = vme >= lower ? vme : value;
        var.setValue(index, a);
        final double fa = f.getAsDouble();
        var.setValue(index, value);
        return (fb - fa) / (b - a);
    }


}
