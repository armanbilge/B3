/*
 * GeneralSubstitutionModelTest.java
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

package beast.evomodel.substmodel;

import beast.evolution.alignment.SitePatterns;
import beast.evolution.datatype.GeneralDataType;
import beast.evomodel.operators.ExchangeOperator;
import beast.evomodel.operators.SubtreeSlideOperator;
import beast.evomodel.operators.WilsonBalding;
import beast.evomodel.sitemodel.GammaSiteModel;
import beast.evomodel.treelikelihood.TreeLikelihood;
import beast.inference.loggers.ArrayLogFormatter;
import beast.inference.loggers.MCLogger;
import beast.inference.loggers.TabDelimitedFormatter;
import beast.inference.mcmc.MCMC;
import beast.inference.mcmc.MCMCOptions;
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Walter Xie
 * convert testGeneralSubstitutionModel.xml in the folder /example
 */
public class GeneralSubstitutionModelTest extends TraceCorrelationAssert {

    private static final String TREE_HEIGHT = "treeHeight";
    private static final String RATES = "rates";

    private GeneralDataType dataType;

    @Before
    public void setUp() throws Exception {

        MathUtils.setSeed(666);

        List<String> states = new ArrayList<String>();
        states.add("A");
        states.add("C");
        states.add("G");
        states.add("T");
        //states.addAll(Arrays.asList("A", "C", "G", "T"));
        dataType = new GeneralDataType(states);
        dataType.addAmbiguity("-", new String[] {"A", "C", "G", "T"});
        dataType.addAmbiguity("?", new String[] {"A", "C", "G", "T"});

        createAlignment(PRIMATES_TAXON_SEQUENCE, dataType);


        createRandomInitialTree(0.0001); // popSize

//        createSpecifiedTree("(((((chimp:0.010464222027296717,bonobo:0.010464222027296717):0.010716369046616688," +
//                "human:0.021180591073913405):0.010988083344422011,gorilla:0.032168674418335416):0.022421978632286572," +
//                "orangutan:0.05459065305062199):0.009576302472349953,siamang:0.06416695552297194);");
        
    }


    @Test
    public void testGeneralSubstitutionModel() {

        // Sub model
        FrequencyModel freqModel = new FrequencyModel(dataType, alignment.getStateFrequencies());
        Parameter ratesPara = new Parameter.Default(RATES, 5, 1.0); // dimension="5" value="1.0"
        GeneralSubstitutionModel generalSubstitutionModel = new GeneralSubstitutionModel(dataType, freqModel, ratesPara, 4); // relativeTo="5"

        //siteModel
        GammaSiteModel siteModel = new GammaSiteModel(generalSubstitutionModel);
        Parameter mu = new Parameter.Default("mutationRate", 1.0, 0, Double.POSITIVE_INFINITY);
        siteModel.setMutationRateParameter(mu);

        //treeLikelihood
        SitePatterns patterns = new SitePatterns(alignment, null, 0, -1, 1, true);
        
        TreeLikelihood treeLikelihood = new TreeLikelihood(patterns, treeModel, siteModel, null, null,
                false, false, true, false, false);
        treeLikelihood.setId(TreeLikelihood.TREE_LIKELIHOOD);

        // Operators
        OperatorSchedule schedule = new SimpleOperatorSchedule();

        MCMCOperator operator = new ScaleOperator(ratesPara, 0.5);
        operator.setWeight(1.0);
        schedule.addOperator(operator);

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
        loggers[0].add(ratesPara);

        loggers[1] = new MCLogger(new TabDelimitedFormatter(System.out), 100000, false);
        loggers[1].add(treeLikelihood);
        loggers[1].add(rootHeight);
        loggers[1].add(ratesPara);

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
        ArrayTraceList traceList = new ArrayTraceList("GeneralSubstitutionModelTest", traces, 0);

        for (int i = 1; i < traces.size(); i++) {
            traceList.analyseTrace(i);
        }

//      <expectation name="likelihood" value="-1815.75"/>
//		<expectation name="treeModel.rootHeight" value="6.42048E-2"/>
//		<expectation name="rateAC" value="6.08986E-2"/>

        TraceCorrelation likelihoodStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TreeLikelihood.TREE_LIKELIHOOD));
        assertExpectation(TreeLikelihood.TREE_LIKELIHOOD, likelihoodStats, -1815.75);

        TraceCorrelation treeHeightStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(TREE_HEIGHT));
        assertExpectation(TREE_HEIGHT, treeHeightStats, 0.0640787258170083);

        TraceCorrelation rateACStats = traceList.getCorrelationStatistics(traceList.getTraceIndex(RATES + "1"));
        assertExpectation(RATES + "1", rateACStats, 0.061071756742081366);
    }

}