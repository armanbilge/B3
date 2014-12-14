/*
 * UncorrelatedRelaxedClockTest.java
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
import beast.evomodel.tree.RateCovarianceStatistic;
import beast.evomodel.tree.RateStatistic;
import beast.evomodel.treelikelihood.TreeLikelihood;
import beast.inference.distribution.ExponentialDistributionModel;
import beast.inference.distribution.LogNormalDistributionModel;
import beast.inference.distribution.ParametricDistributionModel;
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
import beast.inference.operators.RandomWalkIntegerOperator;
import beast.inference.operators.Scalable;
import beast.inference.operators.ScaleOperator;
import beast.inference.operators.SimpleOperatorSchedule;
import beast.inference.operators.SwapOperator;
import beast.inference.operators.UniformIntegerOperator;
import beast.inference.operators.UniformOperator;
import beast.inference.operators.UpDownOperator;
import beast.inference.trace.ArrayTraceList;
import beast.inference.trace.Trace;
import beast.inference.trace.TraceCorrelation;
import beast.inference.trace.TraceCorrelationAssert;
import beast.math.MathUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 * convert testUncorrelatedRelaxedClock.xml in the folder /example
 */
@Ignore // TODO
public class UncorrelatedRelaxedClockTest extends TraceCorrelationAssert {

    private static final String MEAN = "mean";
    private static final String STDEV = "stdev";
    private static final String POSTERIOR = "posterior";
    private static final String TREE_HEIGHT = "treeHeight";
    private static final String KAPPA = "kappa";
    private static final String COEFFICIENT_OF_VARIATION = "coefficientOfVariation";
    private static final String POPULATION_SIZE = "populationSize";
    private static final String MUTATION_RATE = "mutationRate";

    private Parameter meanParam;
    private Parameter stdevParam;

    @Before
    public void setUp() throws Exception {

        MathUtils.setSeed(123);

        createAlignment(DENGUE4_TAXON_SEQUENCE, Nucleotides.INSTANCE);
    }

    @Test
    public void testLogNormal() throws Exception {
        meanParam = new Parameter.Default(MEAN, 2.3E-5, 0, 100.0);
        stdevParam = new Parameter.Default(STDEV, 0.1, 0, 10.0);
        ParametricDistributionModel distributionModel = new LogNormalDistributionModel(meanParam, stdevParam, 0.0, true, false); // meanInRealSpace="true"

        ArrayTraceList traceList = UncorrelatedRelaxedClock(distributionModel);

//        <expectation name="posterior" value="-3927.81"/>
//        <expectation name="ucld.mean" value="8.28472E-4"/>
//        <expectation name="ucld.stdev" value="0.17435"/>
//        <expectation name="meanRate" value="8.09909E-4"/>
//        <expectation name="coefficientOfVariation" value="0.15982"/>
//        <expectation name="covariance" value="-3.81803E-2"/>
//        <expectation name="constant.popSize" value="37.3524"/>
//        <expectation name="hky.kappa" value="18.3053"/>
//        <expectation name="treeModel.rootHeight" value="69.2953"/>
//        <expectation name="treeLikelihood" value="-3855.78"/>
//        <expectation name="skyline" value="-72.0313"/>   ???

        TraceCorrelation likelihoodStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(POSTERIOR));
        assertExpectation(POSTERIOR, likelihoodStats, -3927.81);

        likelihoodStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TreeLikelihood.TREE_LIKELIHOOD));
        assertExpectation(TreeLikelihood.TREE_LIKELIHOOD, likelihoodStats, -3855.78);

        TraceCorrelation treeHeightStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TREE_HEIGHT));
        assertExpectation(TREE_HEIGHT, treeHeightStats, 69.2953);

        TraceCorrelation kappaStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(KAPPA));
        assertExpectation(KAPPA, kappaStats, 18.06518);

        TraceCorrelation ucldStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(MEAN));
        assertExpectation(MEAN, ucldStats, 8.0591451486E-4);

        ucldStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(STDEV));
        assertExpectation(STDEV, ucldStats, 0.16846023066431434);

        TraceCorrelation rateStats = traceList.getCorrelationStatistics(traceList.getTraceIndex("meanRate"));
        assertExpectation("meanRate", rateStats, 8.010906E-4);

        TraceCorrelation coefficientOfVariationStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(COEFFICIENT_OF_VARIATION));
        assertExpectation(COEFFICIENT_OF_VARIATION, coefficientOfVariationStats, 0.15982);

        TraceCorrelation covarianceStats = traceList.getCorrelationStatistics(traceList.getTraceIndex("covariance"));
        assertExpectation("covariance", covarianceStats, -0.0260333026);

        TraceCorrelation popStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(POPULATION_SIZE));
        assertExpectation(POPULATION_SIZE, popStats, 37.3524);

        TraceCorrelation coalescentStats = traceList.getCorrelationStatistics(traceList.getTraceIndex("coalescent"));
        assertExpectation("coalescent", coalescentStats, -72.0313);
    }

    @Test
    public void testExponential() throws Exception {
        meanParam = new Parameter.Default(1.0);
        meanParam.setId(ParametricDistributionModel.DistributionModelParser.MEAN);
        stdevParam = null;
        ParametricDistributionModel distributionModel = new ExponentialDistributionModel(meanParam); // offset = 0

        ArrayTraceList traceList = UncorrelatedRelaxedClock(distributionModel);

        TraceCorrelation likelihoodStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(POSTERIOR));
        assertExpectation(POSTERIOR, likelihoodStats, -3958.7409);
//        System.out.println("likelihoodStats = " + likelihoodStats.getMean());

        likelihoodStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TreeLikelihood.TREE_LIKELIHOOD));
        assertExpectation(TreeLikelihood.TREE_LIKELIHOOD, likelihoodStats, -3885.26939);
//        System.out.println("treelikelihoodStats = " + likelihoodStats.getMean());

        TraceCorrelation treeHeightStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TREE_HEIGHT));
        assertExpectation(TREE_HEIGHT, treeHeightStats, 84.3529526);

        TraceCorrelation kappaStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(KAPPA));
        assertExpectation(KAPPA, kappaStats, 18.38065);

        TraceCorrelation ucedStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(MEAN));
        assertExpectation(MEAN, ucedStats, 0.0019344134887784579);
//        System.out.println("ucedStats = " + ucedStats.getMean());

        TraceCorrelation rateStats = traceList.getCorrelationStatistics(traceList.getTraceIndex("meanRate"));
        assertExpectation("meanRate", rateStats, 0.0020538802366337084);
//        System.out.println("rateStats = " + rateStats.getMean());

        TraceCorrelation coefficientOfVariationStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(COEFFICIENT_OF_VARIATION));
        assertExpectation(COEFFICIENT_OF_VARIATION, coefficientOfVariationStats, 0.773609960455);
//        System.out.println("coefficientOfVariationStats = " + coefficientOfVariationStats.getMean());

        TraceCorrelation covarianceStats = traceList.getCorrelationStatistics(traceList.getTraceIndex("covariance"));
        assertExpectation("covariance", covarianceStats, -0.07042030641301375);
//        System.out.println("covarianceStats = " + covarianceStats.getMean());

        TraceCorrelation popStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(POPULATION_SIZE));
        assertExpectation(POPULATION_SIZE, popStats, 43.4478);
//        System.out.println("popStats = " + popStats.getMean());

        TraceCorrelation coalescentStats = traceList.getCorrelationStatistics(traceList.getTraceIndex("coalescent"));
        assertExpectation("coalescent", coalescentStats, -73.4715);
//        System.out.println("coalescentStats = " + coalescentStats.getMean());
    }

    private ArrayTraceList UncorrelatedRelaxedClock(ParametricDistributionModel distributionModel) throws Exception {
        Parameter popSize = new Parameter.Default(POPULATION_SIZE, 380.0, 0, 38000.0);
        ConstantPopulationModel constantModel = createRandomInitialTree(popSize);

        CoalescentLikelihood coalescent = new CoalescentLikelihood(treeModel, null, new ArrayList<TaxonList>(), constantModel);
        coalescent.setId("coalescent");

        // clock model        
        Parameter rateCategoryParameter = new Parameter.Default(32);
        rateCategoryParameter.setId(DiscretizedBranchRates.BRANCH_RATES); 

        DiscretizedBranchRates branchRateModel = new DiscretizedBranchRates(treeModel, rateCategoryParameter, 
                distributionModel, 1, false, Double.NaN, false, false);

        RateStatistic meanRate = new RateStatistic("meanRate", treeModel, branchRateModel, true, true, RateStatistic.Mode.MEAN);
        RateStatistic coefficientOfVariation = new RateStatistic(COEFFICIENT_OF_VARIATION, treeModel, branchRateModel,
                true, true, RateStatistic.Mode.COEFFICIENT_OF_VARIATION);
        RateCovarianceStatistic covariance = new RateCovarianceStatistic("covariance", treeModel, branchRateModel);

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

        operator = new ScaleOperator(meanParam, 0.75);
        operator.setWeight(3.0);
        schedule.addOperator(operator);

        if (stdevParam != null) {
            operator = new ScaleOperator(stdevParam, 0.75);
            operator.setWeight(3.0);
            schedule.addOperator(operator);
        }

        Parameter allInternalHeights = treeModel.createNodeHeightsParameter(true, true, false);
        operator = new UpDownOperator(new Scalable[]{new Scalable.Default(meanParam)},
                new Scalable[] {new Scalable.Default(allInternalHeights)}, 0.75, 3.0, CoercionMode.COERCION_ON);
        schedule.addOperator(operator);

        operator = new SwapOperator(rateCategoryParameter, 10);
        operator.setWeight(1.0);
        schedule.addOperator(operator);

        operator = new RandomWalkIntegerOperator(rateCategoryParameter, 1, 10.0);
        schedule.addOperator(operator);

        operator = new UniformIntegerOperator(rateCategoryParameter, (int) (double)rateCategoryParameter.getBounds().getLowerLimit(0),
                (int) (double)rateCategoryParameter.getBounds().getUpperLimit(0), 10.0);
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

        operator = new SubtreeSlideOperator(treeModel, 15.0, 38.0, true, false, false, false, CoercionMode.COERCION_ON);
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
        loggers[0] = new MCLogger(formatter, 10000, false);
        loggers[0].add(posterior);
        loggers[0].add(treeLikelihood);
        loggers[0].add(rootHeight);
        loggers[0].add(meanParam);
        if (stdevParam != null) loggers[0].add(stdevParam);
        loggers[0].add(meanRate);
        loggers[0].add(coefficientOfVariation);
        loggers[0].add(covariance);
        loggers[0].add(popSize);
        loggers[0].add(kappa);
        loggers[0].add(coalescent);

        loggers[1] = new MCLogger(new TabDelimitedFormatter(System.out), 100000, false);
        loggers[1].add(posterior);
        loggers[1].add(treeLikelihood);
        loggers[1].add(rootHeight);
        loggers[1].add(meanRate);
        loggers[1].add(coalescent);

        // MCMC
        MCMC mcmc = new MCMC("mcmc1");
        MCMCOptions options = new MCMCOptions(10000000);

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

        return traceList;
    }

}

