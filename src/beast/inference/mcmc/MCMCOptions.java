/*
 * MCMCOptions.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
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

package beast.inference.mcmc;

import beast.inference.markovchain.MarkovChain;

/**
 * A class that brings together the auxillary information associated
 * with an MCMC analysis.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: MCMCOptions.java,v 1.7 2005/05/24 20:25:59 rambaut Exp $
 */
public class MCMCOptions {

    private final long chainLength;
    private final long fullEvaluationCount;
    private final int minOperatorCountForFullEvaluation;
    private final double evaluationTestThreshold;
    private final boolean coercion;
    private final long coercionDelay;
    private final double temperature;
    private final int storeEvery;

    /**
     * constructor
     * @param chainLength
     */
    public MCMCOptions(long chainLength) {
        this(chainLength, 2000, 1, MarkovChain.EVALUATION_TEST_THRESHOLD, true, 0, 1.0, 0);
    }

    public MCMCOptions(final long chainLength, final MCMCOptions other) {
        this(chainLength,
                other.getFullEvaluationCount(),
                other.minOperatorCountForFullEvaluation(),
                other.getEvaluationTestThreshold(),
                other.useCoercion(),
                other.getCoercionDelay(),
                other.getTemperature(),
                other.getStoreEvery());
    }

    /**
     * constructor
     * @param chainLength
     * @param fullEvaluationCount
     * @param minOperatorCountForFullEvaluation
     * @param evaluationTestThreshold
     * @param coercion
     * @param coercionDelay
     * @param temperature
     */
    public MCMCOptions(long chainLength, long fullEvaluationCount, int minOperatorCountForFullEvaluation, double evaluationTestThreshold, boolean coercion, long coercionDelay, double temperature, int storeEvery) {
        this.chainLength = chainLength;
        this.fullEvaluationCount = fullEvaluationCount;
        this.minOperatorCountForFullEvaluation = minOperatorCountForFullEvaluation;
        this.evaluationTestThreshold = evaluationTestThreshold;
        this.coercion = coercion;
        this.coercionDelay = coercionDelay;
        this.temperature = temperature;
        this.storeEvery = storeEvery;
    }

    /**
     * @return the chain length of the MCMC analysis
     */
    public final long getChainLength() {
        return chainLength;
    }

    public final long getFullEvaluationCount() {
        return fullEvaluationCount;
    }

    public double getEvaluationTestThreshold() {
        return evaluationTestThreshold;
    }

    public final boolean useCoercion() {
        return coercion;
    }


    public final long getCoercionDelay() {
        return coercionDelay;
    }

    public final double getTemperature() {
        return temperature;
    }

    public int minOperatorCountForFullEvaluation() {
        return minOperatorCountForFullEvaluation;
    }

    public int getStoreEvery() {
        return storeEvery;
    }
}
