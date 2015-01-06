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

    protected final Likelihood q;
    protected final CompoundParameter parameter;

    private double epsilon;
    private int L;

    {
        setTargetAcceptanceProbability(0.65);
    }

    @Parseable
    public HamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood q,
            @ObjectArrayElement(name = "dimensions") Parameter[] parameters,
            @DoubleAttribute(name = "epsilon", defaultValue = 0.125) double epsilon,
            @IntegerAttribute(name = "iterations", defaultValue = 100) int L,
            @OperatorWeightAttribute double weight) {
        this(q, new CompoundParameter("hamilton", parameters), epsilon, L, weight);
    }

    @Parseable
    public HamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood q,
            @ObjectElement(name = "space") CompoundParameter parameter,
            @DoubleAttribute(name = "epsilon", defaultValue = 0.125) double epsilon,
            @IntegerAttribute(name = "iterations", defaultValue = 100) int L,
            @OperatorWeightAttribute double weight) {
        this.q = q;
        this.parameter = parameter;
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
        sb.append(parameter);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        final int count = getCount();
        if (count > 0 && count % 100 == 0) {
            adjustEpsilon();
            adjustL();
        }

        final int dim = parameter.getDimension();

        final double[] p = new double[dim];
        final double[] storedP = new double[dim];
        Arrays.setAll(p, i -> MathUtils.nextGaussian());
        Arrays.setAll(storedP, i -> p[i]);

        for (int i = 0; i < dim; ++i)
            p[i] += epsilon/2 * q.differentiate(parameter.getMaskedParameter(i), parameter.getMaskedIndex(i));

        for (int l = 0; l < L; ++l) {
            for (int i = 0; i < dim; ++i)
                parameter.setValue(i, parameter.getValue(i) - epsilon * p[i]);
            if (l < L - 1)
                for (int i = 0; i < dim; ++i)
                    p[i] += epsilon * q.differentiate(parameter.getMaskedParameter(i), parameter.getMaskedIndex(i));
        }

        for (int i = 0; i < dim; ++i) {
            p[i] += epsilon/2 * q.differentiate(parameter.getMaskedParameter(i), parameter.getMaskedIndex(i));
            p[i] *= -1;
        }

        final double storedK = new ArrayRealVector(storedP).getNorm() / 2;
        final double proposedK = new ArrayRealVector(p).getNorm() / 2;

        return storedK - proposedK;
    }

    private void adjustEpsilon() {
        if (getAcceptanceProbability() < getTargetAcceptanceProbability())
            epsilon /= 2;
        else
            epsilon *= 2;
    }

    private void adjustL() {
        // TODO
    }

    public static final XMLObjectParser<HamiltonUpdate> PARSER = new SimpleXMLObjectParser<>(HamiltonUpdate.class,
            "An operator that simulates Hamiltonian dynamics to make proposals.");
}
