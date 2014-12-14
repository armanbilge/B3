/*
 * YuleModelTest.java
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

package beast.evomodel.speciation;

import beast.evolution.io.NewickImporter;
import beast.evolution.tree.FlexibleTree;
import beast.evolution.util.Units;
import beast.evomodel.operators.SubtreeSlideOperator;
import beast.evomodel.tree.TreeHeightStatistic;
import beast.evomodel.tree.TreeLengthStatistic;
import beast.evomodel.tree.TreeModel;
import beast.inference.loggers.ArrayLogFormatter;
import beast.inference.loggers.MCLogger;
import beast.inference.loggers.TabDelimitedFormatter;
import beast.inference.mcmc.MCMC;
import beast.inference.mcmc.MCMCOptions;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.inference.operators.CoercionMode;
import beast.inference.operators.MCMCOperator;
import beast.inference.operators.OperatorSchedule;
import beast.inference.operators.SimpleOperatorSchedule;
import beast.inference.trace.ArrayTraceList;
import beast.inference.trace.Trace;
import beast.inference.trace.TraceCorrelation;
import beast.inference.trace.TraceCorrelationAssert;
import beast.math.MathUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * YuleModel Tester.
 *
 * @author Alexei Drummond
 * @version 1.0
 * @since <pre>08/26/2007</pre>
 */
public class YuleModelTest extends TraceCorrelationAssert {

    static final String TL = "TL";
    static final String TREE_HEIGHT = "rootHeight";

    private FlexibleTree tree;

    @Before
    public void setUp() throws Exception {

        MathUtils.setSeed(666);
        
        NewickImporter importer = new NewickImporter("((1:1.0,2:1.0):1.0,(3:1.0,4:1.0):1.0);");
        tree = (FlexibleTree) importer.importTree(null);
    }

    @Test
    public void testYuleWithSubtreeSlide() {

        TreeModel treeModel = new TreeModel("treeModel", tree);

        OperatorSchedule schedule = new SimpleOperatorSchedule();
        MCMCOperator operator =
                new SubtreeSlideOperator(treeModel, 1, 1, true, false, false, false, CoercionMode.COERCION_ON);
        schedule.addOperator(operator);

        yuleTester(treeModel, schedule);

    }

//    public void testYuleWithWideExchange() {
//
//        TreeModel treeModel = new TreeModel("treeModel", tree);

        // Doesn't compile...
  //      yuleTester(treeModel, ExchangeOperatorTest.getWideExchangeSchedule(treeModel));
//    }

    private void yuleTester(TreeModel treeModel, OperatorSchedule schedule) {

        MCMC mcmc = new MCMC("mcmc1");
        MCMCOptions options = new MCMCOptions(1000000);

        TreeLengthStatistic tls = new TreeLengthStatistic(TL, treeModel);
        TreeHeightStatistic rootHeight = new TreeHeightStatistic(TREE_HEIGHT, treeModel);

        Parameter b = new Parameter.Default("b", 2.0, 0.0, Double.MAX_VALUE);
        Parameter d = new Parameter.Default("d", 0.0, 0.0, Double.MAX_VALUE);

        SpeciationModel speciationModel = new BirthDeathModel(b, d, null, BirthDeathModel.TreeType.TIMESONLY,
                Units.Type.YEARS);
        Likelihood likelihood = new SpeciationLikelihood(treeModel, speciationModel, "yule.like");

        ArrayLogFormatter formatter = new ArrayLogFormatter(false);

        MCLogger[] loggers = new MCLogger[2];
        loggers[0] = new MCLogger(formatter, 100, false);
        loggers[0].add(likelihood);
        loggers[0].add(rootHeight);
        loggers[0].add(tls);

        loggers[1] = new MCLogger(new TabDelimitedFormatter(System.out), 100000, false);
        loggers[1].add(likelihood);
        loggers[1].add(rootHeight);
        loggers[1].add(tls);

        mcmc.setShowOperatorAnalysis(true);

        mcmc.init(options, likelihood, schedule, loggers);

        mcmc.run();

        List<Trace> traces = formatter.getTraces();
        ArrayTraceList traceList = new ArrayTraceList("yuleModelTest", traces, 0);

        for (int i = 1; i < traces.size(); i++) {
            traceList.analyseTrace(i);
        }

        // expectation of root height for 4 tips and lambda = 2
        // rootHeight = 0.541666
        // TL = 1.5

        TraceCorrelation tlStats =
                traceList.getCorrelationStatistics(traceList.getTraceIndex(TL));

        assertExpectation(TL, tlStats, 1.5);

        TraceCorrelation treeHeightStats =
                traceList.getCorrelationStatistics(traceList.getTraceIndex(TREE_HEIGHT));

        assertExpectation(TREE_HEIGHT, treeHeightStats, 0.5416666);


    }

}
