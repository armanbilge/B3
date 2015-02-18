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

    final boolean NUMERICAL = true;

    double evaluate();

    default double differentiate(Variable<Double> var, int index) {
        if (NUMERICAL) return differentiate(this::evaluate, var, index);
        return 0.0;
    }

    static double differentiate(DoubleSupplier f, Variable<Double> var, int index) {
        return differentiate(f, var, index, MachineAccuracy.SQRT_EPSILON * Math.max(var.getValue(index), 1));
    }

    static double differentiate(DoubleSupplier f, Variable<Double> var, int index, double epsilon) {
        final Bounds<Double> bounds = var.getBounds();
        final double upper = bounds.getUpperLimit(index);
        final double lower = bounds.getLowerLimit(index);
        final double x = var.getValue(index);
        final double xpe = x + epsilon;
        final double b = xpe <= upper ? xpe : upper;
        var.setValue(index, b);
        final double fb = f.getAsDouble();
        final double xme = x - epsilon;
        final double a = xme >= lower ? xme : lower;
        var.setValue(index, a);
        final double fa = f.getAsDouble();
        var.setValue(index, x);
        return (fb - fa) / (b - a);
    }

    static double differentiate2(DoubleSupplier f, Variable<Double> var0, int index0, Variable<Double> var1, int index1, double epsilon) {

        if (var0 == var1 && index0 == index1) {
            return differentiate2(f, var0, index0, epsilon);
        } else {

            final Bounds<Double> bounds = var0.getBounds();
            final double upper = bounds.getUpperLimit(index0);
            final double lower = bounds.getLowerLimit(index0);
            final double x = var0.getValue(index0);
            final double xpe = x + epsilon;
            final double b = xpe <= upper ? xpe : upper;
            var0.setValue(index0, b);
            final double dfb = differentiate(f, var1, index1);
            final double xme = x - epsilon;
            final double a = xme >= lower ? xme : lower;
            var0.setValue(index0, a);
            final double dfa = differentiate(f, var1, index1);
            var0.setValue(index0, x);
            return (dfb - dfa) / (b - a);

        }

    }

    static double differentiate2(DoubleSupplier f, Variable<Double> var, int index, double epsilon) {
        final Bounds<Double> bounds = var.getBounds();
        final double upper = bounds.getUpperLimit(index);
        final double lower = bounds.getLowerLimit(index);
        final double x = var.getValue(index);
        final double fx = var.getValue(index);
        final double xpe = x + epsilon;
        final double b = xpe <= upper ? xpe : upper;
        var.setValue(index, b);
        final double fb = f.getAsDouble();
        final double xme = x - epsilon;
        final double a = xme >= lower ? xme : lower;
        var.setValue(index, a);
        final double fa = f.getAsDouble();
        var.setValue(index, x);
        final double h = b - a;
        return (fb - fx + fa) / (h * h);
    }

}
