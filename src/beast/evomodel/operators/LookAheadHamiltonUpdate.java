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

package beast.evomodel.operators;

import beast.evomodel.tree.TreeModel;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.inference.model.Parameter.Default;
import beast.inference.operators.CoercionMode;
import beast.inference.operators.CoercionMode.CoercionModeAttribute;
import beast.inference.operators.OperatorFailedException;
import beast.xml.DoubleArrayAttribute;
import beast.xml.DoubleAttribute;
import beast.xml.IntegerAttribute;
import beast.xml.ObjectArrayElement;
import beast.xml.ObjectElement;
import beast.xml.Parseable;
import beast.xml.SimpleXMLObjectParser;
import beast.xml.XMLObjectParser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Arman Bilge
 */
public class LookAheadHamiltonUpdate extends beast.inference.hamilton.LookAheadHamiltonUpdate {

    protected final TreeModel[] trees;

    @Parseable
    public LookAheadHamiltonUpdate(@ObjectElement(name = "potential") Likelihood U,
                                   @ObjectArrayElement(name = "dimensions", min = 0) Parameter[] parameters,
                                   @ObjectArrayElement(name = "trees", min = 0) TreeModel[] trees,
                                   @DoubleArrayAttribute(name = "mass", optional = true) double[] mass,
                                   @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0) double epsilon,
                                   @IntegerAttribute(name = "iterations", optional = true, defaultValue = 0) int M,
                                   @DoubleAttribute(name = "alpha", optional = true, defaultValue = 0.25) double alpha,
                                   @IntegerAttribute(name = "attempts", optional = true, defaultValue = 4) int K,
                                   @OperatorWeightAttribute double weight) {
        super(U, fixParameters(parameters, trees), mass, epsilon, M, alpha, K, weight);
        if (parameters.length == 0 && trees.length == 0)
            throw new IllegalArgumentException("Must have at least one parameter or tree!");
        this.trees = trees;
        x.removeParameter(x.getParameter(parameters.length));
    }

    protected static Parameter[] fixParameters(final Parameter[] parameters, final TreeModel[] trees) {
        final int treeDim = Arrays.stream(trees).mapToInt(TreeModel::getInternalNodeCount).sum();
        final Parameter[] fixedParameters = new Parameter[parameters.length + 1];
        Arrays.setAll(fixedParameters, i -> i < parameters.length ? parameters[i] : new Default(treeDim));
        return fixedParameters;
    }

    @Override
    public double doOperation() throws OperatorFailedException {

        final List<Parameter> heights = Arrays.stream(trees).map(t -> t.createPreOrderNodeHeightsParameter(true, true, false)).collect(Collectors.toList());

        for (final Parameter p : heights)
            x.addParameter(p);

        final double hr = super.doOperation();

        for (final Parameter p : heights)
            x.removeParameter(p);

        return hr;
    }

    public static final XMLObjectParser<LookAheadHamiltonUpdate> PARSER = new SimpleXMLObjectParser<>(LookAheadHamiltonUpdate.class,
            "A special HamiltonUpdate operator that is efficient on trees.");
}
