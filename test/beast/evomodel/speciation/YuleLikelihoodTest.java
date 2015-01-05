/*
 * YuleLikelihoodTest.java
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
import beast.evolution.tree.Tree;
import beast.evolution.util.Units;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.math.MachineAccuracy;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * YuleModel Tester.
 *
 * @author Alexei Drummond
 * @version 1.0
 * @since <pre>08/26/2007</pre>
 */
public class YuleLikelihoodTest {

    static final String TL = "TL";
    static final String TREE_HEIGHT = "rootHeight";

    private FlexibleTree tree;

    @Before
    public void setUp() throws Exception {

        NewickImporter importer = new NewickImporter("((1:1.0,2:1.0):1.0,(3:1.0,4:1.0):1.0);");
        tree = (FlexibleTree) importer.importTree(null);
    }

    @Test
    public void testYuleLikelihood() {

        yuleLikelihoodTester(tree, 1.0, -2.8219461696520542);

    }

    private void yuleLikelihoodTester(Tree tree, double birthRate, double logL) {

        Parameter b = new Parameter.Default("b", birthRate, 0.0, Double.MAX_VALUE);
        Parameter d = new Parameter.Default("d", 0.0, 0.0, Double.MAX_VALUE);

        SpeciationModel speciationModel = new BirthDeathModel(b, d, null, null, BirthDeathModel.TreeType.TIMESONLY,
                Units.Type.YEARS);
        Likelihood likelihood = new SpeciationLikelihood(tree, speciationModel, "yule.like");

        assertEquals(likelihood.getLogLikelihood(), logL, MachineAccuracy.EPSILON);

    }

}