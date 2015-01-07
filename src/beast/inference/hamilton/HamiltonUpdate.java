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
import beast.inference.operators.OperatorFailedException;
import beast.inference.operators.SimpleMCMCOperator;
import beast.math.MathUtils;
import beast.xml.DoubleAttribute;
import beast.xml.IntegerAttribute;
import beast.xml.ObjectArrayElement;
import beast.xml.ObjectElement;
import beast.xml.Parseable;
import beast.xml.SimpleXMLObjectParser;
import beast.xml.XMLObjectParser;
import org.apache.commons.math3.linear.ArrayRealVector;

import java.util.Arrays;

/**
 * @author Arman Bilge
 */
public class HamiltonUpdate extends SimpleMCMCOperator {

    protected final Likelihood U;
    protected final CompoundParameter q;

    private double epsilon;
    private int L;

    {
        setTargetAcceptanceProbability(0.65);
    }

    @Parseable
    public HamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood U,
            @ObjectArrayElement(name = "dimensions") Parameter[] parameters,
            @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0.125) double epsilon,
            @IntegerAttribute(name = "iterations", optional = true, defaultValue = 100) int L,
            @OperatorWeightAttribute double weight) {
        this(U, new CompoundParameter("hamilton", parameters), epsilon, L, weight);
    }

    @Parseable
    public HamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood U,
            @ObjectElement(name = "space") CompoundParameter q,
            @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0.125) double epsilon,
            @IntegerAttribute(name = "iterations", optional = true, defaultValue = 100) int L,
            @OperatorWeightAttribute double weight) {
        this.U = U;
        this.q = q;
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
        sb.append(q);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        adjustEpsilon();
        adjustL();

        final int dim = q.getDimension();

        final double[] p = new double[dim];
        final double[] storedP = new double[dim];
        Arrays.setAll(p, i -> MathUtils.nextGaussian());
        Arrays.setAll(storedP, i -> p[i]);

        for (int i = 0; i < dim; ++i)
            p[i] += epsilon/2 * U.differentiate(q.getMaskedParameter(i), q.getMaskedIndex(i));

        final Bounds<Double> bounds = q.getBounds();
        for (int l = 0; l < L; ++l) {
            for (int i = 0; i < dim; ++i) {
                double q_ = q.getValue(i) - epsilon * p[i];
                final double lower = bounds.getLowerLimit(i);
                final double upper = bounds.getUpperLimit(i);
                if (q_ < lower) {
                    q_ = lower + (lower - q_);
                    p[i] *= -1;
                } else if (q_ > upper) {
                    q_ = upper - (upper - q_);
                    p[i] *= -1;
                }
                q.setValue(i, q_);
            }

            if (l < L - 1)
                for (int i = 0; i < dim; ++i)
                    p[i] += epsilon * U.differentiate(q.getMaskedParameter(i), q.getMaskedIndex(i));
        }

        for (int i = 0; i < dim; ++i) {
            p[i] += epsilon/2 * U.differentiate(q.getMaskedParameter(i), q.getMaskedIndex(i));
            p[i] *= -1;
        }

        final double storedK = Arrays.stream(storedP).map(d -> d * d).sum() / 2;
        final double proposedK = Arrays.stream(p).map(d -> d * d).sum() / 2;

        return storedK - proposedK;
    }

    final private int adjustEvery = 100;
    private int lastAccepted = 0;
    private void adjustEpsilon() {
        final int count = getCount();
        if (count != 0 && count % adjustEvery == 0) {
            final double alpha = (getAcceptCount() - lastAccepted) / (double) adjustEvery;
            if (alpha < getTargetAcceptanceProbability())
                epsilon /= 2;
            else
                epsilon *= 2;
            lastAccepted = getAcceptCount();
        }
    }

    private void adjustL() {
        // TODO
    }

    public static final XMLObjectParser<HamiltonUpdate> PARSER = new SimpleXMLObjectParser<>(HamiltonUpdate.class,
            "An operator that simulates Hamiltonian dynamics to make proposals.");
}
