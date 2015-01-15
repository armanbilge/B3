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
import beast.math.MathUtils;
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
public class HamiltonUpdate extends AbstractCoercableOperator {

    protected final Likelihood U;
    protected final CompoundParameter q;

    private final double[] mass;
    private double epsilon;
    private int L;

    {
        setTargetAcceptanceProbability(0.65);
    }

    @Parseable
    public HamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood U,
            @ObjectArrayElement(name = "dimensions") Parameter[] parameters,
            @DoubleArrayAttribute(name = "mass", optional = true) double[] mass,
            @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0.125) double epsilon,
            @IntegerAttribute(name = "iterations", optional = true, defaultValue = 100) int L,
            @OperatorWeightAttribute double weight,
            @CoercionModeAttribute CoercionMode mode) {
        this(U, new CompoundParameter("q", parameters), mass, epsilon, L, weight, mode);
    }

    @Parseable
    public HamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood U,
            @ObjectElement(name = "space") CompoundParameter q,
            @DoubleArrayAttribute(name = "mass", optional = true) double[] mass,
            @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0.125) double epsilon,
            @IntegerAttribute(name = "iterations", optional = true, defaultValue = 100) int L,
            @OperatorWeightAttribute double weight,
            @CoercionModeAttribute CoercionMode mode) {
        super(mode);
        this.U = U;
        this.q = q;
        if (mass != null) {
            if (mass.length != q.getDimension())
                throw new IllegalArgumentException("mass.length != q.getDimension()");
            else if (!Arrays.stream(mass).allMatch(d -> d > 0))
                throw new IllegalArgumentException("All masses must be m_i > 0.");
            else
                this.mass = mass;
        } else {
            this.mass = new double[q.getDimension()];
            Arrays.fill(this.mass, 1);
        }
        this.epsilon = epsilon;
        this.L = L;
        setWeight(weight);
    }

    @Override
    public String getPerformanceSuggestion() {
        return "No performance suggestion.";
    }

    @Override
    public String getOperatorName() {
        StringBuilder sb = new StringBuilder(PARSER.getParserName());
        sb.append("(");
        sb.append(q.getId() != null ? q.getId() : q.getParameterName());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        final int dim = q.getDimension();
        final double halfEpsilon = epsilon / 2;

        final double[] p = new double[dim];
        final double[] storedP = new double[dim];
        Arrays.setAll(p, i -> MathUtils.nextGaussian() * mass[i]);
        Arrays.setAll(storedP, i -> p[i]);

        for (int i = 0; i < dim; ++i)
            p[i] -= halfEpsilon * U.differentiate(q.getMaskedParameter(i), q.getMaskedIndex(i));

        final Bounds<Double> bounds = q.getBounds();
        for (int l = 0; l < L; ++l) {
            for (int i = 0; i < dim; ++i) {
                double q_ = q.getValue(i) - epsilon * p[i];
                final double lower = bounds.getLowerLimit(i);
                final double upper = bounds.getUpperLimit(i);
                boolean qllower = q_ < lower;
                boolean qgupper = q_ > upper;
                do {
                    if (qllower) {
                        q_ = 2 * lower - q_;
                        p[i] *= -1;
                    } else if (qgupper) {
                        q_ = 2 * upper - q_;
                        p[i] *= -1;
                    }
                    qllower = q_ < lower;
                    qgupper = q_ > upper;
                } while (qllower || qgupper);
                // Quiet due to the large overhead of multiple calls
                q.setParameterValueQuietly(i, q_);
            }

            if (l < L - 1)
                for (int i = 0; i < dim; ++i)
                    p[i] -= epsilon * U.differentiate(q.getMaskedParameter(i), q.getMaskedIndex(i));
        }

        // Make up for quiet behaviour above
        q.fireParameterChangedEvent();

        for (int i = 0; i < dim; ++i) {
            p[i] -= halfEpsilon * U.differentiate(q.getMaskedParameter(i), q.getMaskedIndex(i));
            p[i] *= -1;
        }

        final double storedK = logPDFNormal(storedP);
        final double proposedK = logPDFNormal(p);

        return storedK - proposedK;
    }

    private double logPDFNormal(double[] p) {
        double logPDF = 0;
        for (int i = 0; i < p.length; ++i)
            logPDF += p[i] * p[i] / mass[i];
        return logPDF / 2;
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

    public double getMinimumAcceptanceLevel() {
        return 0.3;
    }

    public double getMaximumAcceptanceLevel() {
        return 1.00;
    }

    public double getMinimumGoodAcceptanceLevel() {
        return 0.5;
    }

    public double getMaximumGoodAcceptanceLevel() {
        return 0.8;
    }

    public static final XMLObjectParser<HamiltonUpdate> PARSER = new SimpleXMLObjectParser<>(HamiltonUpdate.class,
            "An operator that simulates Hamiltonian dynamics to make proposals.");
}
