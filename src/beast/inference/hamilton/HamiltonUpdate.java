/*
 * HamiltonUpdate.java
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
import beast.inference.operators.AbstractCoercableOperator;
import beast.inference.operators.CoercionMode;
import beast.inference.operators.CoercionMode.CoercionModeAttribute;
import beast.inference.operators.OperatorFailedException;
import beast.xml.Description;
import beast.xml.DoubleArrayAttribute;
import beast.xml.DoubleAttribute;
import beast.xml.IntegerAttribute;
import beast.xml.ObjectArrayElement;
import beast.xml.ObjectElement;
import beast.xml.Parseable;

import java.util.Arrays;

/**
 * @author Arman Bilge
 */
@Description("An operator that simulates Hamiltonian dynamics to make proposals.")
public class HamiltonUpdate extends AbstractCoercableOperator {

    protected final Likelihood U;
    protected final KineticEnergy K;
    protected final CompoundParameter q;
    protected final Parameter.Default p;

    private final int dim;
    private double epsilon;
    private int L;
    private double alpha;

    {
        setTargetAcceptanceProbability(0.651);
    }

    @Parseable
    public HamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood U,
            @ObjectArrayElement(name = "dimensions") Parameter[] parameters,
            @ObjectElement(name = "mass") OnlineVariance variance,
            @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0.0) double epsilon,
            @IntegerAttribute(name = "iterations", optional = true, defaultValue = 0) int L,
            @DoubleAttribute(name = "alpha", optional = true, defaultValue = 1.0) double alpha,
            @OperatorWeightAttribute double weight,
            @CoercionModeAttribute CoercionMode mode) {
        this(U, new CompoundParameter("q", parameters),
                new KineticEnergy.OnlineDiagonal(variance.getVariance()),
                epsilon, L, alpha, weight, mode);
    }

    @Parseable
    public HamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood U,
            @ObjectArrayElement(name = "dimensions") Parameter[] parameters,
            @DoubleArrayAttribute(name = "mass") double[] massAttribute,
            @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0.0) double epsilon,
            @IntegerAttribute(name = "iterations", optional = true, defaultValue = 0) int L,
            @DoubleAttribute(name = "alpha", optional = true, defaultValue = 1.0) double alpha,
            @OperatorWeightAttribute double weight,
            @CoercionModeAttribute CoercionMode mode) {
        this(U, new CompoundParameter("q", parameters),
                new KineticEnergy.Fixed(massAttributeToMass(massAttribute, Arrays.stream(parameters).mapToInt(Parameter::getDimension).sum())),
                epsilon, L, alpha, weight, mode);
    }

    public HamiltonUpdate(final Likelihood U,
                          final CompoundParameter q,
                          final KineticEnergy K,
                          final double epsilon,
                          final int L,
                          final double alpha,
                          final double weight,
                          final CoercionMode mode) {

        super(mode);

        this.U = U;
        this.q = q;
        dim = q.getDimension();
        p = new Parameter.Default("p", dim);

        this.K = K;
        p.adoptParameterValues(new Parameter.Default(K.next()));

        if (epsilon > 0)
            this.epsilon = epsilon;
        else
            setDefaultEpsilon();

        if (L > 0)
            this.L = L;
        else
            setDefaultL();

        this.alpha = alpha;

        setWeight(weight);
    }

    private static double[][] massAttributeToMass(final double[] massAttribute, final int dim) {
        final double[][] mass = new double[dim][dim];
        if (massAttribute != null) {
            if (massAttribute.length == dim) { // Diagonal matrix
                for (int i = 0; i < dim; ++i)
                    mass[i][i] = massAttribute[i];
            } else if (massAttribute.length == dim * (dim + 1) / 2) { // Upper symmetric matrix
                int k = 0;
                for (int i = 0; i < dim; ++i) {
                    for (int j = i; j < dim; ++j) {
                        mass[i][j] = massAttribute[k];
                        if (i != j) mass[j][i] = massAttribute[k];
                        ++k;
                    }
                }
            } else if (massAttribute.length == dim * dim) { // Fully defined matrix
                int k = 0;
                for (int i = 0; i < dim; ++i)
                    for (int j = 0; j < dim; ++j)
                        mass[i][j] = massAttribute[k++];
            } else {
                throw new IllegalArgumentException("Wrong number of elements in mass matrix.");
            }
        } else {
            setDefaultMass(mass);
        }
        return mass;
    }

    private static void setDefaultMass(final double[][] mass) {
        for (int i = 0; i < mass.length; ++i)
            mass[i][i] = 1;
    }

    private static final double EPSILON_CONSTANT = 0.0625;
    protected void setDefaultEpsilon() {
        epsilon = EPSILON_CONSTANT * Math.pow(dim, -0.25);
    }

    private static final int L_CONSTANT = 16;
    protected void setDefaultL() {
        L = (int) Math.round(L_CONSTANT * Math.pow(dim, 0.25));
    }

    protected double getEpsilon() {
        return epsilon;
    }

    protected void setEpsilon(double e) {
        epsilon = e;
    }

    protected int getL() {
        return L;
    }

    protected void setL(int l) {
        L = l;
    }

    protected int getDimension() {
        return dim;
    }

    @Override
    public String getPerformanceSuggestion() {
        return "No performance suggestion.";
    }

    @Override
    public String getOperatorName() {
        return "hamiltonUpdate" + "(" + (q.getId() != null ? q.getId() : q.getParameterName()) + ")";
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        flipMomentum();
        corruptMomentum();

        final double storedK = kineticEnergy();
        p.storeParameterValues();

        simulateDynamics();
        flipMomentum();

        final double proposedK = kineticEnergy();

        return storedK - proposedK;
    }

    protected void corruptMomentum() {
        final double sqrtalpha = Math.sqrt(alpha);
        final double sqrt1malpha = Math.sqrt(1 - alpha);
        final double[] n = K.next();
        for (int i = 0; i < dim; ++i) {
            final double p_ = p.getParameterValue(i) * sqrt1malpha + n[i] * sqrtalpha;
            p.setParameterValueQuietly(i, p_);
        }
        p.fireParameterChangedEvent();
    }

    protected void simulateDynamics() {

        final double halfEpsilon = epsilon / 2;

        for (int i = 0; i < dim; ++i) {
            final double dU = halfEpsilon * differentiatePotentialEnergy(i);
            final double p_ = p.getParameterValue(i) - dU;
            // Quiet due to the large overhead of multiple calls
            p.setParameterValueQuietly(i, p_);
        }
        // Make up for quiet behaviour above
        p.fireParameterChangedEvent();

        final Bounds<Double> bounds = q.getBounds();
        for (int l = 0; l < L; ++l) {
            final double[] dKdt = gradientKineticEnergy();
            for (int i = 0; i < dim; ++i) {
                double q_ = q.getParameterValue(i) + epsilon * dKdt[i];
                final double lower = bounds.getLowerLimit(i);
                final double upper = bounds.getUpperLimit(i);
                while (q_ < lower || q_ > upper) {
                    q_ = 2 * (q_ < lower ? lower : upper) - q_;
                    final double p_i = p.getParameterValue(i);
                    p.setParameterValue(i, -p_i);
                }
                q.setParameterValueQuietly(i, q_);
            }

            q.fireParameterChangedEvent();

            if (l < L - 1)
                for (int i = 0; i < dim; ++i) {
                    final double dU = epsilon * differentiatePotentialEnergy(i);
                    final double p_ = p.getParameterValue(i) - dU;
                    p.setParameterValueQuietly(i, p_);
                }
            p.fireParameterChangedEvent();
        }

        for (int i = 0; i < dim; ++i) {
            final double dU = halfEpsilon * differentiatePotentialEnergy(i);
            final double p_ = p.getParameterValue(i) - dU;
            p.setParameterValueQuietly(i, p_);
        }
        p.fireParameterChangedEvent();

    }

    protected void flipMomentum() {
        for (int i = 0; i < dim; ++i)
            p.setParameterValue(i, - p.getParameterValue(i));
    }

    protected double potentialEnergy() {
        return - U.getLogLikelihood();
    }

    protected double differentiatePotentialEnergy(int i) {
        return - U.differentiate(q.getMaskedParameter(i), q.getMaskedIndex(i));
    }

    protected double kineticEnergy() {
        return K.energy(p.inspectParameterValues());
    }

    protected double[] gradientKineticEnergy() {
        return K.gradient(p.inspectParameterValues());
    }

    @Override
    public void accept(double deviation) {
        super.accept(deviation);
        p.acceptParameterValues();
    }

    @Override
    public void reject() {
        super.reject();
        p.restoreParameterValues();
    }

    @Override
    public double getCoercableParameter() {
        return Math.log(epsilon);
    }

    @Override
    public void setCoercableParameter(double value) {
        epsilon = Math.exp(value);
    }

    @Override
    public double getRawParameter() {
        return epsilon;
    }

    @Override
    public double getMinimumAcceptanceLevel() {
        return 0.3;
    }

    @Override
    public double getMaximumAcceptanceLevel() {
        return 1.00;
    }

    @Override
    public double getMinimumGoodAcceptanceLevel() {
        return 0.5;
    }

    @Override
    public double getMaximumGoodAcceptanceLevel() {
        return 0.8;
    }

}
