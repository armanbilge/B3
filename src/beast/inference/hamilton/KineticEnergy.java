/*
 * KineticEnergy.java
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

import beast.inference.model.Variable;
import beast.inference.model.Variable.ChangeType;
import beast.inference.model.VariableListener;
import beast.math.distributions.MultivariateNormalDistribution;

/**
 * @author Arman Bilge
 */
public interface KineticEnergy {

    double energy(double[] x);
    double[] gradient(double[] x);
    double[] next();

    class Fixed implements KineticEnergy {

        private final MultivariateNormalDistribution K;

        public Fixed(final double[][] mass) {
            this.K = new MultivariateNormalDistribution(new double[mass.length], mass, false);
        }

        @Override
        public double energy(final double[] x) {
            return - K.logPdf(x);
        }

        @Override
        public double[] gradient(final double[] x) {
            final double[] grad = K.gradientLogPdf(x);
            for (int i = 0; i < grad.length; ++i)
                grad[i] *= -1;
            return grad;
        }

        @Override
        public double[] next() {
            return K.nextMultivariateNormal();
        }
    }

    class OnlineDiagonal implements KineticEnergy, VariableListener {

        private final Variable<Double> var;
        private final double[] mean;
        private MultivariateNormalDistribution K;
        private boolean dirty = true;

        public OnlineDiagonal(final Variable<Double> var) {
            this.var = var;
            mean = new double[var.getSize()];
        }

        private void update() {
            if (dirty) {
                final int dim = var.getSize();
                final double[][] mass = new double[dim][dim];
                for (int i = 0; i < dim; ++i) {
                    final double var = this.var.getValue(i);
                    if (var <= 1E-10)
                        mass[i][i] = 1E-9;
                    else
                        mass[i][i] = var;
                }
                K = new MultivariateNormalDistribution(mean, mass, false);
            }
        }

        @Override
        public double energy(final double[] x) {
            update();
            return - K.logPdf(x);
        }

        @Override
        public double[] gradient(final double[] x) {
            update();
            final double[] grad = K.gradientLogPdf(x);
            for (int i = 0; i < grad.length; ++i)
                grad[i] *= -1;
            return grad;
        }

        @Override
        public double[] next() {
            update();
            return K.nextMultivariateNormal();
        }

        @Override
        public void variableChangedEvent(final Variable variable, final int index, final ChangeType type) {
            dirty = true;
        }
    }

}
