/*
 * MultiParameterMCMCOperator.java
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

package beast.inference.operators;

import beast.inference.model.Likelihood;
import beast.inference.prior.Prior;
import beast.math.MathUtils;

import java.util.Arrays;

/**
 * @author Arman Bilge
 */
public abstract class MultiParameterMCMCOperator implements CoercableMCMCOperator {

    private CoercionMode mode = CoercionMode.DEFAULT;
    private final int dim;
    private int currentDimension;

    public final CoercionMode getMode() {
        return mode;
    }

    public MultiParameterMCMCOperator(CoercionMode mode, final int dim) {
        this.mode = mode;
        this.dim = dim;
        acceptCount = new int[dim];
        rejectCount = new int[dim];
    }

    public double getTargetAcceptanceProbability() {
        return targetAcceptanceProb;
    }

    public void setTargetAcceptanceProbability(double tap) {
        targetAcceptanceProb = tap;
    }

    public double getMinimumAcceptanceLevel() {
        return 0.05;
    }

    public double getMaximumAcceptanceLevel() {
        return 0.50;
    }

    public double getMinimumGoodAcceptanceLevel() {
        return 0.10;
    }

    public double getMaximumGoodAcceptanceLevel() {
        return 0.40;
    }

    public abstract String getOperatorName();

    /**
     * @return the weight of this operator.
     */
    public final double getWeight() {
        return weight;
    }

    public void setPathParameter(double beta) {
        throw new IllegalArgumentException("Path parameter has no effect on Metropolis-Hastings kernels." +
                "\nGibbs samplers need an implementation for use in power-posteriors");
    }

    /**
     * Sets the weight of this operator.
     */
    public final void setWeight(double w) {
        if( w > 0 ) {
            weight = w;
        } else {
            throw new IllegalArgumentException(
                    "Weight must be a positive real, but tried to set weight to "
                            + w);
        }
    }

    public void accept(double deviation) {
        lastDeviation = deviation;

        if( !operateAllowed ) {
            operateAllowed = true;
            acceptCount[currentDimension] += 1;
            sumDeviation += deviation;

//            spanDeviation[0] = Math.min(spanDeviation[0], deviation);
//            spanDeviation[1] = Math.max(spanDeviation[1], deviation);
//            spanCount += 1;
        } else {
            throw new RuntimeException(
                    "Accept/reject methods called twice without operate called in between!");
        }
    }

    public void reject() {
        if( !operateAllowed ) {
            operateAllowed = true;
            rejectCount[currentDimension] += 1;
        } else {
            throw new RuntimeException(
                    "Accept/reject methods called twice without operate called in between!");
        }
    }

    public void reset() {
        operateAllowed = true;
        acceptCount = new int[dim];
        rejectCount = new int[dim];
        lastDeviation = 0.0;
        sumDeviation = 0.0;
    }

    public final int getCount() {
        return acceptCount[currentDimension] + rejectCount[currentDimension];
    }

    public final int getAcceptCount() {
        return acceptCount[currentDimension];
    }

    public final void setAcceptCount(int acceptCount) {
        this.acceptCount[currentDimension] = acceptCount;
    }

    public final int getRejectCount() {
        return rejectCount[currentDimension];
    }

    public final void setRejectCount(int rejectCount) {
        this.rejectCount[currentDimension] = rejectCount;
    }

    public final double getMeanDeviation() {
        throw new UnsupportedOperationException();
    }

    public final double getDeviation() {
        return lastDeviation;
    }

    public final double getSumDeviation() {
        return sumDeviation;
    }

    public final void setSumDeviation(double sumDeviation) {
        this.sumDeviation = sumDeviation;
    }

//    public double getSpan(boolean reset) {
//        double span = 0;
//        if( spanDeviation[1] > spanDeviation[0] && spanCount > 2000 ) {
//            span = spanDeviation[1] - spanDeviation[0];
//
//            if( reset ) {
//                spanDeviation[0] = Double.MAX_VALUE;
//                spanDeviation[1] = -Double.MAX_VALUE;
//                spanCount = 0;
//            }
//        }
//        return span;
//    }

    public final double operate() throws OperatorFailedException {
        if( operateAllowed ) {
            operateAllowed = false;
            currentDimension = MathUtils.nextInt(dim);
            return doOperation();
        } else {
            throw new RuntimeException(
                    "Operate called twice without accept/reject in between!");
        }
    }

    public final double operate(Prior prior, Likelihood likelihood)
            throws OperatorFailedException {
        if( operateAllowed ) {
            operateAllowed = false;
            return doOperation(prior, likelihood);
        } else {
            throw new RuntimeException(
                    "Operate called twice without accept/reject in between!");
        }
    }

    public final double getAcceptanceProbability() {
        return (double) acceptCount[currentDimension] / (double) (acceptCount[currentDimension] + rejectCount[currentDimension]);
    }

    /**
     * Called by operate(), does the actual operation.
     *
     * @return the hastings ratio
     * @throws OperatorFailedException if operator fails and should be rejected
     */
    public double doOperation(Prior prior, Likelihood likelihood)
            throws OperatorFailedException {
        return 0.0;
    }

    public double getMeanEvaluationTime() {
        return (double) sumEvaluationTime / (double) (Arrays.stream(acceptCount).sum() + Arrays.stream(rejectCount).sum());
    }

    public long getTotalEvaluationTime() {
        return sumEvaluationTime;
    }

    public void addEvaluationTime(long time) {
        sumEvaluationTime += time;
    }

    /**
     * Called by operate(), does the actual operation.
     *
     * @return the hastings ratio
     * @throws OperatorFailedException if operator fails and should be rejected
     */
    public abstract double doOperation() throws OperatorFailedException;

    protected int getCurrentDimension() {
        return currentDimension;
    }

    private double weight = 1.0;
    private int[] acceptCount;
    private int[] rejectCount;

    private double sumDeviation = 0.0;
    private double lastDeviation = 0.0;

    private boolean operateAllowed = true;
    private double targetAcceptanceProb = 0.234;

    private long sumEvaluationTime = 0;

}
