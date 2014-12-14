/*
 * MCMCTest.java
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

import beast.evolution.alignment.SitePatterns;
import beast.evolution.datatype.Nucleotides;
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
import beast.inference.model.Parameter;
import beast.inference.operators.CoercionMode;
import beast.inference.operators.MCMCOperator;
import beast.inference.operators.OperatorSchedule;
import beast.inference.operators.ScaleOperator;
import beast.inference.operators.SimpleOperatorSchedule;
import beast.inference.operators.UniformOperator;
import beast.inference.trace.ArrayTraceList;
import beast.inference.trace.Trace;
import beast.inference.trace.TraceCorrelation;
import beast.inference.trace.TraceCorrelationAssert;
import beast.math.MathUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @author Walter Xie
 * convert testMCMC.xml in the folder /example
 */

public class MCMCTest extends TraceCorrelationAssert {

    private static final String KAPPA = "kappa";
    private static final String MUTATION_RATE = "mutationRate";
    private static final String TREE_HEIGHT = "treeHeight";

    @Before
    public void setUp() throws Exception {

        MathUtils.setSeed(666);

        createAlignment(PRIMATES_TAXON_SEQUENCE, Nucleotides.INSTANCE);

        createRandomInitialTree(0.0001); // popSize

//        createSpecifiedTree("((((human:0.02124198428146588,(bonobo:0.010505698073024256,chimp:0.010505698073024256)" +
//                ":0.010736286208441624):0.011019735965429791,gorilla:0.03226172024689567):0.022501552046463147," +
//                "orangutan:0.05476327229335882):0.009440823865408586,siamang:0.0642040961587674);");
    }


    @Test
    public void testMCMC() {
        // Sub model
        Parameter freqs = new Parameter.Default(alignment.getStateFrequencies());//new double[]{0.25, 0.25, 0.25, 0.25});
        Parameter kappa = new Parameter.Default(KAPPA, 1.0, 1.0E-8, Double.POSITIVE_INFINITY);

        FrequencyModel f = new FrequencyModel(Nucleotides.INSTANCE, freqs);
        HKY hky = new HKY(kappa, f);

        //siteModel
        GammaSiteModel siteModel = new GammaSiteModel(hky);
        Parameter mu = new Parameter.Default(MUTATION_RATE, 1.0, 0, Double.POSITIVE_INFINITY);
        siteModel.setMutationRateParameter(mu);

        //treeLikelihood
        SitePatterns patterns = new SitePatterns(alignment, null, 0, -1, 1, true);

        TreeLikelihood treeLikelihood = new TreeLikelihood(patterns, treeModel, siteModel, null, null,
                false, false, true, false, false);
        treeLikelihood.setId(TreeLikelihood.TREE_LIKELIHOOD);

        // Operators
        OperatorSchedule schedule = new SimpleOperatorSchedule();

        MCMCOperator operator = new ScaleOperator(kappa, 0.5);
        operator.setWeight(1.0);
        schedule.addOperator(operator);

//        Parameter rootParameter = treeModel.createNodeHeightsParameter(true, false, false);
//        ScaleOperator scaleOperator = new ScaleOperator(rootParameter, 0.75, CoercionMode.COERCION_ON, 1.0);

        Parameter rootHeight = treeModel.getRootHeightParameter();
        rootHeight.setId(TREE_HEIGHT);
        operator = new ScaleOperator(rootHeight, 0.5);
        operator.setWeight(1.0);
        schedule.addOperator(operator);

        Parameter internalHeights = treeModel.createNodeHeightsParameter(false, true, false);
        operator = new UniformOperator(internalHeights, 10.0);
        schedule.addOperator(operator);

        operator = new SubtreeSlideOperator(treeModel, 1, 1, true, false, false, false, CoercionMode.COERCION_ON);
        schedule.addOperator(operator);

        operator = new ExchangeOperator(ExchangeOperator.Mode.NARROW, treeModel, 1.0);
//        operator.doOperation();
        schedule.addOperator(operator);

        operator = new ExchangeOperator(ExchangeOperator.Mode.WIDE, treeModel, 1.0);
//        operator.doOperation();
        schedule.addOperator(operator);

        operator = new WilsonBalding(treeModel, 1.0);
//        operator.doOperation();
        schedule.addOperator(operator);

        // Log
        ArrayLogFormatter formatter = new ArrayLogFormatter(false);

        MCLogger[] loggers = new MCLogger[2];
        loggers[0] = new MCLogger(formatter, 1000, false);
        loggers[0].add(treeLikelihood);
        loggers[0].add(rootHeight);
        loggers[0].add(kappa);

        loggers[1] = new MCLogger(new TabDelimitedFormatter(System.out), 100000, false);
        loggers[1].add(treeLikelihood);
        loggers[1].add(rootHeight);
        loggers[1].add(kappa);

        // MCMC
        MCMC mcmc = new MCMC("mcmc1");
        MCMCOptions options = new MCMCOptions(10000000);

        mcmc.setShowOperatorAnalysis(true);
        mcmc.init(options, treeLikelihood, schedule, loggers);
        mcmc.run();

        // time
        System.out.println(mcmc.getTimer().toString());

        // Tracer
        List<Trace> traces = formatter.getTraces();
        ArrayTraceList traceList = new ArrayTraceList("MCMCTest", traces, 0);

        for (int i = 1; i < traces.size(); i++) {
            traceList.analyseTrace(i);
        }

//      <expectation name="likelihood" value="-1815.75"/>
//		<expectation name="treeModel.rootHeight" value="6.42048E-2"/>
//		<expectation name="hky.kappa" value="32.8941"/>

        TraceCorrelation likelihoodStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TreeLikelihood.TREE_LIKELIHOOD));
        assertExpectation(TreeLikelihood.TREE_LIKELIHOOD, likelihoodStats, -1815.75);

        TraceCorrelation treeHeightStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TREE_HEIGHT));
        assertExpectation(TREE_HEIGHT, treeHeightStats, 6.42048E-2);

        TraceCorrelation kappaStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(KAPPA));
        assertExpectation(KAPPA, kappaStats, 32.8941);
    }

}