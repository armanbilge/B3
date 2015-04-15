/*
 * OnlineVariance.java
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

import beast.inference.markovchain.MarkovChain;
import beast.inference.markovchain.MarkovChainDelegate;
import beast.inference.mcmc.MCMCOptions;
import beast.inference.model.Statistic;
import beast.inference.model.Variable;
import beast.inference.model.Variable.D;
import beast.inference.operators.OperatorSchedule;
import beast.xml.ObjectElement;
import beast.xml.Parseable;

import java.util.Arrays;

/**
 * @author Arman Bilge
 */
public class OnlineVariance extends Statistic.Abstract implements MarkovChainDelegate {

    private final Variable<Double> x;
    private int dim;
    private int n = 0;
    private final double[] mean;
    private final double[] M2;
    private final Variable<Double> variance;

    @Parseable
    public OnlineVariance(@ObjectElement(name = "variable") final Variable<Double> x) {
        super("var(" + (x.getId() != null ? x.getId() : x.getVariableName()) + ")");
        this.x = x;
        dim = x.getSize();
        mean = new double[dim];
        M2 = new double[dim];
        variance = new D(0.0, dim);
    }

    private void update() {
        ++n;
        for (int i = 0; i < dim; ++i) {
            final double x = this.x.getValue(i);
            final double delta = x - mean[i];
            mean[i] += delta / n;
            M2[i] += delta * (x - mean[i]);
            if (n > 1)
                variance.setValue(i, M2[i] / (n - 1));
        }
    }

    public Variable<Double> getVariance() {
        return variance;
    }

    @Override
    public int getDimension() {
        return dim;
    }

    @Override
    public double getStatisticValue(final int dim) {
        return variance.getValue(dim);
    }

    @Override
    public void setup(final MCMCOptions options, final OperatorSchedule schedule, final MarkovChain markovChain) {
        Arrays.fill(mean, 0);
        Arrays.fill(M2, 0);
        for (int i = 0; i < dim; ++i)
            variance.setValue(i, 0.0);
    }

    @Override
    public void currentState(final long state) {
        update();
    }

    @Override
    public void currentStateEnd(final long state) {
        // Nothing to do
    }

    @Override
    public void finished(final long chainLength) {
        update();
    }
}
