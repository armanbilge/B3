/*
 * BirthDeathSSLikelihoodTest.java
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
import beast.inference.model.Variable;
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
public class BirthDeathSSLikelihoodTest {

    private FlexibleTree tree;
    private FlexibleTree tree2;

    @Before
    public void setUp() throws Exception {

        NewickImporter importer = new NewickImporter("((1:1.0,2:1.0):1.0,3:2.0);");
        tree = (FlexibleTree) importer.importTree(null);

        importer = new NewickImporter("((3 : 1.5, 4 : 0.5) : 1 , (1 : 2, 2 : 1) : 3);");
        tree2 = (FlexibleTree) importer.importTree(null);
    }

    @Test
    public void testBirthDeathLikelihoodBEAST2() {
        System.out.println("RootHeight = " + tree2.getRootHeight());
        Variable<Double> origin = new Variable.D("origin", 6.0);

        final double birthRate = 2.0;
        final double deathRate = 1.0;
        final double psiRate = 0.5; // rate of sampling taxa through time
        final double sampleProbability = 0.0; // the proportion of taxa sampled, default to fix to 0
        final boolean hasFinalSample = false;
        Variable<Double> b = new Variable.D("b", birthRate);
        Variable<Double> d = new Variable.D("d", deathRate);
        Variable<Double> psi = new Variable.D("psi", psiRate);
        Variable<Double> p = new Variable.D("p", sampleProbability);
        Variable<Double> r = new Variable.D("r", 0.0); // sampleBecomesNonInfectiousProb

        SpeciationModel speciationModel = new BirthDeathSerialSamplingModel(b, d, psi, p, false, r, hasFinalSample, origin, Units.Type.YEARS);
        Likelihood likelihood = new SpeciationLikelihood(tree2, speciationModel, "bdss.like");

        assertEquals(-19.0198, likelihood.getLogLikelihood(), 1e-5);
    }

    final double birthRate = 1.0;
    final double deathRate = 0.5;
    final double p = 1.0;
    final double psi = 0.0;

    @Test
    public void testBirthDeathLikelihoodP0() {
        assertEquals(BirthDeathSerialSamplingModel.p0(birthRate, deathRate, p, psi, 1.0), 0.28236670080320814, MachineAccuracy.EPSILON);
    }

//    public void testP1() {
//        assertEquals(BirthDeathSerialSamplingModel.p1(birthRate, deathRate, p, psi, 1.0), 0.31236180503535266);
//    }

//    public void testPureBirthLikelihood() {
//        likelihoodTester(tree, 1, 0, -2.8219461696520542);
//    }

//    public void testBirthDeathLikelihood() {
//        likelihoodTester(tree, birthRate, deathRate, null, -4.633233508436623);
//    }

    @Test
    public void testBirthDeathLikelihoodOrigin() {
        System.out.println("RootHeight = " + tree.getRootHeight());
        Variable<Double> origin = new Variable.D("origin", 50);
        likelihoodTester(tree, birthRate, deathRate, origin, -29.529647743897872);
    }

    private void likelihoodTester(Tree tree, double birthRate, double deathRate, Variable<Double> origin, double logL) {

        Variable<Double> b = new Variable.D("b", birthRate);
        Variable<Double> d = new Variable.D("d", deathRate);
        Variable<Double> psi = new Variable.D("psi", this.psi);
        Variable<Double> p = new Variable.D("p", this.p);
        Variable<Double> r = new Variable.D("r", 0.5);
        Variable<Double> fTime = new Variable.D("time", 0.0);

        SpeciationModel speciationModel = new BirthDeathSerialSamplingModel(b, d, psi, p, false, r, true, origin, Units.Type.YEARS);
        Likelihood likelihood = new SpeciationLikelihood(tree, speciationModel, "bdss.like");

        assertEquals(logL, likelihood.getLogLikelihood(), MachineAccuracy.EPSILON);
    }

}