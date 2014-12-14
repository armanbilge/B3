/*
 * OperatorAssert.java
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

/**
 *
 */
package beast.evomodel.operators;

import beast.evolution.io.Importer;
import beast.evolution.io.NewickImporter;
import beast.evolution.io.NexusImporter;
import beast.evolution.tree.FlexibleTree;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeLogger;
import beast.evolution.util.Units;
import beast.evomodel.speciation.BirthDeathModel;
import beast.evomodel.speciation.SpeciationLikelihood;
import beast.evomodel.speciation.SpeciationModel;
import beast.evomodel.tree.TreeHeightStatistic;
import beast.evomodel.tree.TreeLengthStatistic;
import beast.evomodel.tree.TreeModel;
import beast.inference.loggers.MCLogger;
import beast.inference.loggers.TabDelimitedFormatter;
import beast.inference.mcmc.MCMC;
import beast.inference.mcmc.MCMCOptions;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.inference.operators.OperatorSchedule;
import beast.math.MathUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastian Hoehna
 *
 */
public abstract class OperatorAssert {

	static final String TL = "TL";
    static final String TREE_HEIGHT = "treeHeight";

    protected FlexibleTree tree5;
    protected FlexibleTree tree6;

    @Before
    public void setUp() throws Exception {

        MathUtils.setSeed(666);

        NewickImporter importer = new NewickImporter(
                "((((A:1.0,B:1.0):1.0,C:2.0):1.0,D:3.0):1.0,E:4.0);");
        tree5 = (FlexibleTree) importer.importTree(null);

        importer = new NewickImporter(
                "(((((A:1.0,B:1.0):1.0,C:2.0):1.0,D:3.0):1.0,E:4.0),F:5.0);");
        tree6 = (FlexibleTree) importer.importTree(null);
    }

 // 5 taxa trees should sample all 105 topologies
    @Test
    public void testIrreducibility5() throws IOException, Importer.ImportException {
        irreducibilityTester(tree5, 105, 1000000, 10);
    }

    // 6 taxa trees should sample all 945 topologies
    @Test
    public void testIrreducibility6() throws IOException, Importer.ImportException {
        irreducibilityTester(tree6, 945, 2000000, 4);
    }

    /**
     * @param ep    the expected (binomial) probability of success
     * @param ap    the actual proportion of successes
     * @param count the number of attempts
     */
    protected void assertExpectation(double ep, double ap, int count) {

        if (count * ap < 5 || count * (1 - ap) < 5) throw new IllegalArgumentException();

        double stdev = Math.sqrt(ap * (1.0 - ap) * count) / count;
        double upper = ap + 2 * stdev;
        double lower = ap - 2 * stdev;

        assertTrue("Expected p=" + ep + " but got " + ap + " +/- " + stdev,
                upper > ep && lower < ep);

    }

    private void irreducibilityTester(Tree tree, int numLabelledTopologies, int chainLength, int sampleTreeEvery)
            throws IOException, Importer.ImportException {

    	MCMC mcmc = new MCMC("mcmc1");
        MCMCOptions options = new MCMCOptions(chainLength);

        TreeModel treeModel = new TreeModel("treeModel", tree);
        TreeLengthStatistic tls = new TreeLengthStatistic(TL, treeModel);
        TreeHeightStatistic rootHeight = new TreeHeightStatistic(TREE_HEIGHT, treeModel);

        OperatorSchedule schedule = getOperatorSchedule(treeModel);

        Parameter b = new Parameter.Default("b", 2.0, 0.0, Double.MAX_VALUE);
        Parameter d = new Parameter.Default("d", 0.0, 0.0, Double.MAX_VALUE);

        SpeciationModel speciationModel = new BirthDeathModel(b, d, null, BirthDeathModel.TreeType.UNSCALED,
                Units.Type.YEARS);
        Likelihood likelihood = new SpeciationLikelihood(treeModel, speciationModel, "yule.like");

        MCLogger[] loggers = new MCLogger[2];
//        loggers[0] = new MCLogger(new ArrayLogFormatter(false), 100, false);
//        loggers[0].add(likelihood);
//        loggers[0].add(rootHeight);
//        loggers[0].add(tls);

        loggers[0] = new MCLogger(new TabDelimitedFormatter(System.out), 10000, false);
        loggers[0].add(likelihood);
        loggers[0].add(rootHeight);
        loggers[0].add(tls);

        File file = new File("yule.trees");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);

        loggers[1] = new TreeLogger(treeModel, new TabDelimitedFormatter(out), sampleTreeEvery, true, true, false);

        mcmc.setShowOperatorAnalysis(true);

        mcmc.init(options, likelihood, schedule, loggers);

        mcmc.run();
        out.flush();
        out.close();

        Set<String> uniqueTrees = new HashSet<String>();
        HashMap<String, Integer> topologies = new HashMap<String, Integer>();
        HashMap<String, HashMap<String, Integer>> treeCounts = new HashMap<String, HashMap<String, Integer>>();

        NexusImporter importer = new NexusImporter(new FileReader(file));
        int sampleSize = 0;
        while (importer.hasTree()) {
        	sampleSize++;
            Tree t = importer.importNextTree();
            String uniqueNewick = Tree.Utils.uniqueNewick(t, t.getRoot());
            String topology = uniqueNewick.replaceAll("\\w+", "X");
            if (!uniqueTrees.contains(uniqueNewick)){
            	uniqueTrees.add(uniqueNewick);
            }
            HashMap<String, Integer> counts;
            if (topologies.containsKey(topology)){
            	topologies.put(topology, topologies.get(topology)+1);
            	counts = treeCounts.get(topology);
            }
            else {
            	topologies.put(topology, 1);
            	counts = new HashMap<String, Integer>();
            	treeCounts.put(topology, counts);
            }
            if (counts.containsKey(uniqueNewick)){
            	counts.put(uniqueNewick, counts.get(uniqueNewick)+1);
            }
            else {
            	counts.put(uniqueNewick, 1);
            }
        }

        assertEquals(numLabelledTopologies, uniqueTrees.size());

        assertEquals(sampleSize, chainLength / sampleTreeEvery + 1);

        Set<String> keys = topologies.keySet();
    	double ep = 1.0 / topologies.size();
        for (String topology : keys){
          	double ap = ((double)topologies.get(topology)) / (sampleSize);
//          	assertExpectation(ep, ap, sampleSize);

          	HashMap<String, Integer> counts = treeCounts.get(topology);
          	Set<String> trees = counts.keySet();

          	double MSE = 0;
        	double ep1 = 1.0 / counts.size();
            for (String t : trees){
              	double ap1 = ((double)counts.get(t)) / (topologies.get(topology));
//              	assertExpectation(ep1, ap1, topologies.get(topology));
              	MSE += (ep1-ap1)*(ep1-ap1);
            }
            MSE /= counts.size();

            System.out.println("The Mean Square Error for the topolgy " + topology + " is " + MSE);
        }
    }

    public abstract OperatorSchedule getOperatorSchedule(TreeModel treeModel);
}
