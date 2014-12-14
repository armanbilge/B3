/*
 * StrictClockTest.java
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

package beast.evomodel.branchratemodel;

import beast.evolution.alignment.SitePatterns;
import beast.evolution.datatype.Nucleotides;
import beast.evolution.util.TaxonList;
import beast.evomodel.coalescent.CoalescentLikelihood;
import beast.evomodel.coalescent.ConstantPopulationModel;
import beast.evomodel.operators.ExchangeOperator;
import beast.evomodel.operators.SubtreeSlideOperator;
import beast.evomodel.operators.WilsonBalding;
import beast.evomodel.sitemodel.GammaSiteModel;
import beast.evomodel.substmodel.FrequencyModel;
import beast.evomodel.substmodel.HKY;
import beast.evomodel.treelikelihood.TreeLikelihood;
import beast.inference.loggers.ArrayLogFormatter;
import beast.inference.loggers.MCLogger;
import beast.inference.loggers.TabDelimitedFormatter;
import beast.inference.mcmc.MCMC;
import beast.inference.mcmc.MCMCOptions;
import beast.inference.model.CompoundLikelihood;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.inference.operators.CoercionMode;
import beast.inference.operators.MCMCOperator;
import beast.inference.operators.OperatorSchedule;
import beast.inference.operators.Scalable;
import beast.inference.operators.ScaleOperator;
import beast.inference.operators.SimpleOperatorSchedule;
import beast.inference.operators.UniformOperator;
import beast.inference.operators.UpDownOperator;
import beast.inference.trace.ArrayTraceList;
import beast.inference.trace.Trace;
import beast.inference.trace.TraceCorrelation;
import beast.inference.trace.TraceCorrelationAssert;
import beast.math.MathUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 * convert testStrictClock.xml in the folder /example
 */
public class StrictClockTest extends TraceCorrelationAssert {

    private static final String POPULATION_SIZE = "populationSize";
    private static final String KAPPA = "kappa";
    private static final String MUTATION_RATE = "mutationRate";
    private static final String TREE_HEIGHT = "treeHeight";
    private static final String POSTERIOR = "posterior";

    @Before
    public void setUp() throws Exception {

        MathUtils.setSeed(666);

        createAlignment(DENGUE4_TAXON_SEQUENCE, Nucleotides.INSTANCE);
    }


    @Test
    public void testStrictClock() throws Exception {
        Parameter popSize = new Parameter.Default(POPULATION_SIZE, 380.0, 0, 38000.0);
        ConstantPopulationModel constantModel = createRandomInitialTree(popSize);

        CoalescentLikelihood coalescent = new CoalescentLikelihood(treeModel, null, new ArrayList<TaxonList>(), constantModel);
        coalescent.setId("coalescent");

        // clock model
        Parameter rateParameter =  new Parameter.Default(StrictClockBranchRates.RATE, 2.3E-5, 0, 100.0);

        StrictClockBranchRates branchRateModel = new StrictClockBranchRates(rateParameter);

        // Sub model
        Parameter freqs = new Parameter.Default(alignment.getStateFrequencies());
        Parameter kappa = new Parameter.Default(KAPPA, 1.0, 0, 100.0);

        FrequencyModel f = new FrequencyModel(Nucleotides.INSTANCE, freqs);
        HKY hky = new HKY(kappa, f);

        //siteModel
        GammaSiteModel siteModel = new GammaSiteModel(hky);
        Parameter mu = new Parameter.Default(MUTATION_RATE, 1.0, 0, Double.POSITIVE_INFINITY);
        siteModel.setMutationRateParameter(mu);

        //treeLikelihood
        SitePatterns patterns = new SitePatterns(alignment, null, 0, -1, 1, true);

        TreeLikelihood treeLikelihood = new TreeLikelihood(patterns, treeModel, siteModel, branchRateModel, null,
                false, false, true, false, false);
        treeLikelihood.setId(TreeLikelihood.TREE_LIKELIHOOD);

        // Operators
        OperatorSchedule schedule = new SimpleOperatorSchedule();

        MCMCOperator operator = new ScaleOperator(kappa, 0.75);
        operator.setWeight(1.0);
        schedule.addOperator(operator);

        operator = new ScaleOperator(rateParameter, 0.75);
        operator.setWeight(3.0);
        schedule.addOperator(operator);

        Parameter allInternalHeights = treeModel.createNodeHeightsParameter(true, true, false);
        operator = new UpDownOperator(new Scalable[]{new Scalable.Default(rateParameter)},
                new Scalable[] {new Scalable.Default(allInternalHeights)}, 0.75, 3.0, CoercionMode.COERCION_ON);
        schedule.addOperator(operator);

        operator = new ScaleOperator(popSize, 0.75);
        operator.setWeight(3.0);
        schedule.addOperator(operator);

        Parameter rootHeight = treeModel.getRootHeightParameter();
        rootHeight.setId(TREE_HEIGHT);
        operator = new ScaleOperator(rootHeight, 0.75);
        operator.setWeight(3.0);
        schedule.addOperator(operator);

        Parameter internalHeights = treeModel.createNodeHeightsParameter(false, true, false);
        operator = new UniformOperator(internalHeights, 30.0);
        schedule.addOperator(operator);

        operator = new SubtreeSlideOperator(treeModel, 15.0, 1.0, true, false, false, false, CoercionMode.COERCION_ON);
        schedule.addOperator(operator);

        operator = new ExchangeOperator(ExchangeOperator.Mode.NARROW, treeModel, 15.0);
//        operator.doOperation();
        schedule.addOperator(operator);

        operator = new ExchangeOperator(ExchangeOperator.Mode.WIDE, treeModel, 3.0);
//        operator.doOperation();
        schedule.addOperator(operator);

        operator = new WilsonBalding(treeModel, 3.0);
//        operator.doOperation();
        schedule.addOperator(operator);

        //CompoundLikelihood
        List<Likelihood> likelihoods = new ArrayList<Likelihood>();        
        likelihoods.add(coalescent);
        Likelihood prior = new CompoundLikelihood(0, likelihoods);
        prior.setId("prior");

        likelihoods.clear();
        likelihoods.add(treeLikelihood);
        Likelihood likelihood = new CompoundLikelihood(-1, likelihoods);

        likelihoods.clear();
        likelihoods.add(prior);
        likelihoods.add(likelihood);
        Likelihood posterior = new CompoundLikelihood(0, likelihoods);
        posterior.setId(POSTERIOR);

        // Log
        ArrayLogFormatter formatter = new ArrayLogFormatter(false);

        MCLogger[] loggers = new MCLogger[2];
        loggers[0] = new MCLogger(formatter, 500, false);
        loggers[0].add(posterior);
        loggers[0].add(treeLikelihood);
        loggers[0].add(rootHeight);
        loggers[0].add(rateParameter);
        loggers[0].add(popSize);
        loggers[0].add(kappa);
        loggers[0].add(coalescent);

        loggers[1] = new MCLogger(new TabDelimitedFormatter(System.out), 10000, false);
        loggers[1].add(posterior);
        loggers[1].add(treeLikelihood);
        loggers[1].add(rootHeight);
        loggers[1].add(rateParameter);
        loggers[1].add(coalescent);

        // MCMC
        MCMC mcmc = new MCMC("mcmc1");
        MCMCOptions options = new MCMCOptions(1000000);

        mcmc.setShowOperatorAnalysis(true);
        mcmc.init(options, posterior, schedule, loggers);
        mcmc.run();

        // time
        System.out.println(mcmc.getTimer().toString());

        // Tracer
        List<Trace> traces = formatter.getTraces();
        ArrayTraceList traceList = new ArrayTraceList("RandomLocalClockTest", traces, 0);

        for (int i = 1; i < traces.size(); i++) {
            traceList.analyseTrace(i);
        }

//        <expectation name="posterior" value="-3928.71"/>
//        <expectation name="clock.rate" value="8.04835E-4"/>
//        <expectation name="constant.popSize" value="37.3762"/>
//        <expectation name="hky.kappa" value="18.2782"/>
//        <expectation name="treeModel.rootHeight" value="69.0580"/>
//        <expectation name="treeLikelihood" value="-3856.59"/>
//        <expectation name="coalescent" value="-72.1285"/>

        TraceCorrelation likelihoodStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(POSTERIOR));
        assertExpectation(POSTERIOR, likelihoodStats, -3928.71);

        likelihoodStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TreeLikelihood.TREE_LIKELIHOOD));
        assertExpectation(TreeLikelihood.TREE_LIKELIHOOD, likelihoodStats, -3856.59);

        TraceCorrelation treeHeightStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TREE_HEIGHT));
        assertExpectation(TREE_HEIGHT, treeHeightStats, 69.0580);

        TraceCorrelation kappaStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(KAPPA));
        assertExpectation(KAPPA, kappaStats, 18.2782);

        TraceCorrelation rateStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(StrictClockBranchRates.RATE));
        assertExpectation(StrictClockBranchRates.RATE, rateStats, 8.04835E-4);        

        TraceCorrelation popStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(POPULATION_SIZE));
        assertExpectation(POPULATION_SIZE, popStats, 37.3762);

        TraceCorrelation coalescentStats = traceList.getCorrelationStatistics(traceList.getTraceIndex("coalescent"));
        assertExpectation("coalescent", coalescentStats, -72.1285);
    }

}

