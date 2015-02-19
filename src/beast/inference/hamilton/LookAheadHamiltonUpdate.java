/*
 * LookAheadHamiltonUpdate.java
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
import beast.inference.model.CompoundParameter;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.inference.model.Parameter.Default;
import beast.inference.operators.OperatorFailedException;
import beast.inference.operators.SimpleMCMCOperator;
import beast.math.MathUtils;
import beast.xml.Description;
import beast.xml.DoubleArrayAttribute;
import beast.xml.DoubleAttribute;
import beast.xml.IntegerAttribute;
import beast.xml.ObjectArrayElement;
import beast.xml.ObjectElement;
import beast.xml.Parseable;
import beast.xml.SimpleXMLObjectParser;
import beast.xml.XMLObjectParser;

import java.util.Arrays;

/**
 * @author Arman Bilge
 */
@Description("An optimistic Hamiltonian operator.")
public class LookAheadHamiltonUpdate extends SimpleMCMCOperator {

    protected final Likelihood E;
    protected final CompoundParameter x;
    protected final Parameter v;
    protected final double[] logPi;
    protected final int dim;
    protected final double[] mass;
    protected double epsilon;
    protected int M;
    protected double beta;
    protected int K;

    @Parseable
    public LookAheadHamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood E,
            @ObjectArrayElement(name = "dimensions") Parameter[] parameters,
            @DoubleArrayAttribute(name = "mass", optional = true) double[] mass,
            @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0.0) double epsilon,
            @IntegerAttribute(name = "iterations", optional = true, defaultValue = 0) int M,
            @DoubleAttribute(name = "alpha", optional = true, defaultValue = 0.25) double alpha,
            @IntegerAttribute(name = "attempts", optional = true, defaultValue = 4) int K,
            @OperatorWeightAttribute double weight) {
        this(E, new CompoundParameter("x", parameters), mass, epsilon, M, alpha, K, weight);
    }

    public LookAheadHamiltonUpdate(Likelihood E, CompoundParameter x, double[] mass, double epsilon, int M, double alpha, int K, double weight) {

        this.E = E;
        this.x = x;
        dim = x.getDimension();
        this.v = new Default(dim, 0.0);

        if (mass != null) {
            if (mass.length != dim)
                throw new IllegalArgumentException("mass.length != q.getDimension()");
            else if (!Arrays.stream(mass).allMatch(m -> m > 0))
                throw new IllegalArgumentException("All masses must be m_i > 0.");
            else
                this.mass = mass;
        } else {
            this.mass = new double[dim];
            setDefaultMass();
        }

        if (epsilon > 0)
            this.epsilon = epsilon;
        else
            setDefaultEpsilon();

        if (M > 0)
            this.M = M;
        else
            setDefaultM();

        beta = Math.pow(alpha, 1.0 / (this.epsilon * this.M));

        this.K = K;

        logPi = new double[K + 2];

        setWeight(weight);

        R();
    }

    protected void setDefaultMass() {
        Arrays.fill(this.mass, 1);
    }

    private static final double EPSILON_CONSTANT = 0.015625;
    protected void setDefaultEpsilon() {
        epsilon = EPSILON_CONSTANT * Math.pow(dim, -0.25);
    }

    private static final int L_CONSTANT = 8;
    protected void setDefaultM() {
        M = (int) Math.round(L_CONSTANT * Math.pow(dim, 0.25));
    }

    @Override
    public String getPerformanceSuggestion() {
        return "No performance suggestion.";
    }

    @Override
    public String getOperatorName() {
        StringBuilder sb = new StringBuilder("lookAheadHamiltonUpdate");
        sb.append("(");
        sb.append(x.getId() != null ? x.getId() : x.getParameterName());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        v.storeParameterValues();

        final double H_0 = H();

        final double r = MathUtils.randomLogDouble();

        logPi[0] = Double.NEGATIVE_INFINITY;
        int k;
        for (k = 0; logPi[k] < r && k <= K; ++k) {
            L();
            final double dH = H() - H_0;
            logPi[k+1] = Math.max(logPi[k], Math.min(0, dH));
        }

        final double accept;

        if (r <= logPi[k]) {
            accept = Double.POSITIVE_INFINITY;
            v.acceptParameterValues();
        } else {
            accept = Double.NEGATIVE_INFINITY;
            v.restoreParameterValues();
            F();
        }

        R();

        return accept;
    }

    protected double H() {
        return E.getLogLikelihood() - K();
    }

    private double K() {
        double K = 0;
        for (int i = 0; i < dim; ++i) {
            final double v = this.v.getParameterValue(i);
            K += v * v / mass[i];
        }
        return K / 2;
    }

    protected void F() {
        for (int i = 0; i < dim; ++i)
            v.setParameterValueQuietly(i, -v.getParameterValue(i));
    }

    protected void L() {

        final double halfEpsilon = epsilon / 2;

        for (int i = 0; i < dim; ++i)
            v.setParameterValueQuietly(i, v.getParameterValue(i) - halfEpsilon * E.differentiate(x.getMaskedParameter(i), x.getMaskedIndex(i)));

        final Bounds<Double> bounds = x.getBounds();
        for (int l = 0; l < M; ++l) {
            for (int i = 0; i < dim; ++i) {
                double x_ = x.getParameterValue(i) - epsilon * v.getParameterValue(i);
                final double lower = bounds.getLowerLimit(i);
                final double upper = bounds.getUpperLimit(i);
                boolean qllower = x_ < lower;
                boolean qgupper = x_ > upper;
                do {
                    if (qllower) {
                        x_ = 2 * lower - x_;
                        v.setParameterValueQuietly(i, -v.getParameterValue(i));
                    } else if (qgupper) {
                        x_ = 2 * upper - x_;
                        v.setParameterValueQuietly(i, -v.getParameterValue(i));
                    }
                    qllower = x_ < lower;
                    qgupper = x_ > upper;
                } while (qllower || qgupper);
                // Quiet due to the large overhead of multiple calls
                x.setParameterValueQuietly(i, x_);
            }

            // Make up for quiet behaviour above
            x.fireParameterChangedEvent();

            if (l < M - 1)
                for (int i = 0; i < dim; ++i)
                    v.setParameterValueQuietly(i, v.getParameterValue(i) - epsilon * E.differentiate(x.getMaskedParameter(i), x.getMaskedIndex(i)));
        }

        for (int i = 0; i < dim; ++i)
            v.setParameterValueQuietly(i, v.getParameterValue(i) - halfEpsilon * E.differentiate(x.getMaskedParameter(i), x.getMaskedIndex(i)));
    }

    protected void R() {
        final double sqrtbeta = Math.sqrt(beta);
        final double sqrt1mbeta = Math.sqrt(1 - beta);
        for (int i = 0; i < dim; ++i) {
            final double n = MathUtils.nextGaussian() * mass[i];
            v.setParameterValueQuietly(i, v.getParameterValue(i) * sqrt1mbeta + n * sqrtbeta);
        }
    }

}
