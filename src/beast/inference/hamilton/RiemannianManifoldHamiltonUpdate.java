/*
 * RiemannianManifoldHamiltonUpdate.java
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
import beast.xml.DoubleAttribute;
import beast.xml.EnumAttribute;
import beast.xml.IntegerAttribute;
import beast.xml.ObjectArrayElement;
import beast.xml.ObjectElement;
import beast.xml.Parseable;
import beast.xml.SimpleXMLObjectParser;
import beast.xml.XMLObjectParser;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Arman Bilge
 */
public class RiemannianManifoldHamiltonUpdate extends SimpleMCMCOperator {

    protected final Likelihood V;
    protected final CompoundParameter q;
    protected MultivariateNormalDistribution T;
    protected final RealVector p;

    private final int dim;
    private final double[][] mass;
    private double epsilon;
    private int L;
    private double alpha;
    private final Approximation approximation;

    private RealMatrix H;
    private RealMatrix[] gradH;
    private RealMatrix Q;
    private RealMatrix lambda;

    public enum Approximation {
        NONE, DIAGONAL, OUTER_PRODUCT, DIAGONAL_OUTER_PRODUCT;
        @Override
        public String toString() {
            final Matcher m = Pattern.compile("_(.)").matcher(super.toString());
            final StringBuffer sb = new StringBuffer();
            while (m.find())
                m.appendReplacement(sb, m.group(1).toUpperCase());
            m.appendTail(sb);
            return sb.toString();
        }
    }

    {
        setTargetAcceptanceProbability(0.65);
    }

    @Parseable
    public RiemannianManifoldHamiltonUpdate(
            @ObjectElement(name = "potential") Likelihood V,
            @ObjectArrayElement(name = "dimensions") Parameter[] parameters,
            @DoubleAttribute(name = "epsilon", optional = true, defaultValue = 0.0) double epsilon,
            @IntegerAttribute(name = "iterations", optional = true, defaultValue = 0) int L,
            @DoubleAttribute(name = "alpha", optional = true, defaultValue = 0.0) double alpha,
            @EnumAttribute(name = "approximation") Approximation approximation,
            @OperatorWeightAttribute double weight) {
        this(V, new CompoundParameter("q", parameters), epsilon, L, alpha, approximation, weight);
    }

    public RiemannianManifoldHamiltonUpdate(Likelihood V, CompoundParameter q, double epsilon, int L, double alpha, Approximation approximation, double weight) {

        this.V = V;
        this.q = q;
        dim = q.getDimension();
        p = new ArrayRealVector(dim);

        mass = new double[dim][dim];

        if (epsilon > 0)
            this.epsilon = epsilon;
        else
            setDefaultEpsilon();

        if (L > 0)
            this.L = L;
        else
            setDefaultL();

        this.alpha = alpha;

        this.approximation = approximation;

        setWeight(weight);

    }

    private static final double EPSILON_CONSTANT = 0.0625;
    protected void setDefaultEpsilon() {
        epsilon = EPSILON_CONSTANT * Math.pow(dim, -0.25);
    }

    private static final int L_CONSTANT = 16;
    protected void setDefaultL() {
        L = (int) Math.round(L_CONSTANT * Math.pow(dim, 0.25));
    }

    private RealVector dtaudp() {
        return Q.multiply(inverseSquiggleLambda()).multiply(Q.transpose()).preMultiply(p);
    }

    private RealVector dtaudq() {
        final RealMatrix J = createJ();
        final RealMatrix QT = Q.transpose();
        final RealMatrix D = new DiagonalMatrix(QT.preMultiply(p).toArray());
        final RealMatrix M = Q.multiply(D).multiply(J).multiply(D).multiply(QT.transpose()).scalarMultiply(-1);
        final RealVector delta = new ArrayRealVector(dim);
        for (int i = 0; i < dim; ++i)
            delta.setEntry(i, 0.5 * trace(M, gradH[i]));
        return delta;
    }

    private RealVector dphidq() {
        final RealMatrix J = createJ();
        final RealMatrix R = inverseSquiggleLambda();
        final RealMatrix M = Q.multiply(hadamard(R, J)).multiply(Q.transpose());
        final RealVector delta = new ArrayRealVector(dim);
        for (int i = 0; i < dim; ++i)
            delta.setEntry(i, 0.5 * trace(M, gradH[i]) + V.differentiate(q.getMaskedParameter(i), q.getMaskedIndex(i)));
        return delta;
    }

    private RealMatrix inverseSquiggleLambda() {
        final RealMatrix squiggleLambda = new DiagonalMatrix(dim);
        for (int i = 0; i < dim; ++i) {
            final double lambda_i = lambda.getEntry(i, i);
            squiggleLambda.setEntry(i, i, 1.0 / (lambda_i * coth(alpha * lambda_i)));
        }
        return squiggleLambda;
    }

    private RealMatrix createJ() {
        final RealMatrix J = new Array2DRowRealMatrix(dim, dim);
        for (int i = 0; i < dim; ++i) {
            for (int j = 0; j < dim; ++j) {
                final double Jij;
                final double lambda_i = lambda.getEntry(i, i);
                if (i != j) {
                    final double lambda_j = lambda.getEntry(j, j);
                    Jij = (lambda_i * coth(alpha * lambda_i) - lambda_j * coth(alpha * lambda_j)) / (lambda_i - lambda_j);
                } else {
                    final double al = alpha * lambda_i;
                    final double cschal = csch(al);
                    Jij = coth(al) - al * cschal * cschal;
                }
                J.setEntry(i, j, Jij);
            }
        }
        return J;
    }

    private double trace(RealMatrix A, RealMatrix B) {
        double trace = 0.0;
        for (int i = 0; i < dim; ++i)
            for (int j = 0; j < dim; ++j)
                trace += A.getEntry(i, j) * B.getEntry(j, i);
        return trace;
    }

    private RealMatrix hadamard(RealMatrix A, RealMatrix B) {
        final RealMatrix C = new Array2DRowRealMatrix(dim, dim);
        for (int i = 0; i < dim; ++i)
            for (int j = 0; j < dim; ++j)
                C.setEntry(i, j, A.getEntry(i, j) * B.getEntry(i, j));
        return C;
    }

    private double coth(final double z) {
        final double e2z = Math.exp(2 * z);
        return (e2z + 1) / (e2z - 1);
    }

    private double csch(final double z) {
        return 1.0 / Math.sinh(z);
    }

    @Override
    public String getPerformanceSuggestion() {
        return null;
    }

    @Override
    public String getOperatorName() {
        return null;
    }

    @Override
    public double doOperation() throws OperatorFailedException {
        return 0;
    }

    public static final XMLObjectParser<RiemannianManifoldHamiltonUpdate> PARSER =
            new SimpleXMLObjectParser<>(RiemannianManifoldHamiltonUpdate.class, "");
}
