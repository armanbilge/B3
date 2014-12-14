/*
 * LognormalPriorTest.java
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

package beast.inference.prior;

import beast.evolution.util.Units;
import beast.evomodel.coalescent.ConstantPopulationModel;
import beast.inference.distribution.DistributionLikelihood;
import beast.inference.loggers.ArrayLogFormatter;
import beast.inference.loggers.MCLogger;
import beast.inference.loggers.TabDelimitedFormatter;
import beast.inference.mcmc.MCMC;
import beast.inference.mcmc.MCMCOptions;
import beast.inference.model.CompoundLikelihood;
import beast.inference.model.DummyLikelihood;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.inference.operators.MCMCOperator;
import beast.inference.operators.OperatorSchedule;
import beast.inference.operators.ScaleOperator;
import beast.inference.operators.SimpleOperatorSchedule;
import beast.inference.trace.ArrayTraceList;
import beast.inference.trace.Trace;
import beast.inference.trace.TraceCorrelation;
import beast.inference.trace.TraceCorrelationAssert;
import beast.math.MathUtils;
import beast.math.distributions.LogNormalDistribution;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 * convert testLognormalPrior.xml in the folder /example
 */

public class LognormalPriorTest extends TraceCorrelationAssert {

    private static final String POPULATION_SIZE = "populationSize";

    @Before
    public void setUp() throws Exception {
        MathUtils.setSeed(123);
    }


    @Test
    public void testLognormalPrior() {
//        ConstantPopulation constant = new ConstantPopulation(Units.Type.YEARS);
//        constant.setN0(popSize); // popSize
        Parameter popSize = new Parameter.Default(6.0);
        popSize.setId(POPULATION_SIZE);
        ConstantPopulationModel demo = new ConstantPopulationModel(popSize, Units.Type.YEARS);

//        //Likelihood
        Likelihood dummyLikelihood = new DummyLikelihood(demo);

        // Operators
        OperatorSchedule schedule = new SimpleOperatorSchedule();

        MCMCOperator operator = new ScaleOperator(popSize, 0.75);
        operator.setWeight(1.0);
        schedule.addOperator(operator);

        // Log
        ArrayLogFormatter formatter = new ArrayLogFormatter(false);

        MCLogger[] loggers = new MCLogger[2];
        loggers[0] = new MCLogger(formatter, 1000, false);
//        loggers[0].add(treeLikelihood);
        loggers[0].add(popSize);

        loggers[1] = new MCLogger(new TabDelimitedFormatter(System.out), 100000, false);
//        loggers[1].add(treeLikelihood);
        loggers[1].add(popSize);

        // MCMC
        MCMC mcmc = new MCMC("mcmc1");
        MCMCOptions options = new MCMCOptions(1000000);

        DistributionLikelihood logNormalLikelihood = new DistributionLikelihood(new LogNormalDistribution(1.0, 1.0), 0); // meanInRealSpace="false"
        logNormalLikelihood.addData(popSize);

        List<Likelihood> likelihoods = new ArrayList<Likelihood>();
        likelihoods.add(logNormalLikelihood);
        Likelihood prior = new CompoundLikelihood(0, likelihoods);

        likelihoods.clear();
        likelihoods.add(dummyLikelihood);
        Likelihood likelihood = new CompoundLikelihood(-1, likelihoods);

        likelihoods.clear();
        likelihoods.add(prior);
        likelihoods.add(likelihood);
        Likelihood posterior = new CompoundLikelihood(0, likelihoods);

        mcmc.setShowOperatorAnalysis(true);
        mcmc.init(options, posterior, schedule, loggers);
        mcmc.run();

        // time
        System.out.println(mcmc.getTimer().toString());

        // Tracer
        List<Trace> traces = formatter.getTraces();
        ArrayTraceList traceList = new ArrayTraceList("LognormalPriorTest", traces, 0);

        for (int i = 1; i < traces.size(); i++) {
            traceList.analyseTrace(i);
        }

//      <expectation name="param" value="4.48168907"/>

        TraceCorrelation popSizeStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(POPULATION_SIZE));

        System.out.println("Expectation of Log-Normal(1,1) is e^(M+S^2/2) = e^(1.5) = " + Math.exp(1.5));
        assertExpectation(POPULATION_SIZE, popSizeStats, Math.exp(1.5));
    }

}
