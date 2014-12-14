/*
 * ExchangeOperatorTest.java
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

package beast.evomodel.operators;

import beast.evolution.io.Importer.ImportException;
import beast.evolution.io.NewickImporter;
import beast.evolution.tree.FlexibleTree;
import beast.evolution.tree.Tree;
import beast.evomodel.tree.TreeModel;
import beast.inference.model.Parameter;
import beast.inference.operators.CoercionMode;
import beast.inference.operators.OperatorFailedException;
import beast.inference.operators.OperatorSchedule;
import beast.inference.operators.ScaleOperator;
import beast.inference.operators.SimpleOperatorSchedule;
import beast.inference.operators.UniformOperator;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Alexei Drummond
 */
public class ExchangeOperatorTest extends OperatorAssert {

    static final String TL = "TL";
    //static final String TREE_HEIGHT = "rootHeight";

    @Test
    public void testWideExchangeOperator2() throws IOException, ImportException {

        // probability of picking (A,B) node is 1/(2n-2) = 1/8
        // probability of swapping with D is 1/2
        // total = 1/16

        //probability of picking {D} node is 1/(2n-2) = 1/8
        //probability of picking {A,B} is 1/5
        // total = 1/40

        //total = 1/16 + 1/40 = 0.0625 + 0.025 = 0.0875
    	
    	// new test:
    	// probability of picking (A,B) node is 1/(2n-2) = 1/8
        // probability of swapping with D is 1/(2n-3) = 1/7
        // total = 1/56

        //probability of picking {D} node is 1/(2n-2) = 1/8
        //probability of picking {A,B} is 1/(2n-3) = 1/7
        // total = 1/56

        //total = 1/56 + 1/56 = 1/28
    	
    	
    	System.out.println("Test 1: Forward");

        String treeMatch = "(((D,C),(A,B)),E);";
        
        int count = 0;
        int reps = 1000000;

        for (int i = 0; i < reps; i++) {

            try {
                TreeModel treeModel = new TreeModel("treeModel", tree5);
                ExchangeOperator operator = new ExchangeOperator(ExchangeOperator.Mode.WIDE, treeModel, 1.0);
                operator.doOperation();

                String tree = Tree.Utils.newickNoLengths(treeModel);

                if (tree.equals(treeMatch)) {
                    count += 1;
                }

            } catch (OperatorFailedException e) {
                //e.printStackTrace();
            }

        }
        double p_1 = (double) count / (double) reps;

        System.out.println("Number of proposals:\t" + count);
        System.out.println("Number of tries:\t" + reps);
        System.out.println("Number of ratio:\t" + p_1);
        System.out.println("Number of expected ratio:\t" + 1.0/28.0);
        assertExpectation(1.0/28.0, p_1, reps);
        
        // since this operator is supposed to be symmetric it got a hastings ratio of one
        // this means, it should propose the same move just backwards with the same probability
        
        // BUT:
        
        // (((D:2.0,C:2.0):1.0,(A:1.0,B:1.0):2.0):1.0,E:4.0) -> ((((A,B),C),D),E)
        
        // probability of picking (A,B) node is 1/(2n-2) = 1/8
        // probability of swapping with D is 1/3
        // total = 1/24

        //probability of picking {D} node is 1/(2n-2) = 1/8
        //probability of picking {A,B} is 1/4
        // total = 1/32

        //total = 1/24 + 1/32 = 7/96 = 0.07291666666
        
        // new test:
    	// probability of picking (A,B) node is 1/(2n-2) = 1/8
        // probability of swapping with D is 1/(2n-3) = 1/7
        // total = 1/56

        //probability of picking {D} node is 1/(2n-2) = 1/8
        //probability of picking {A,B} is 1/(2n-3) = 1/7
        // total = 1/56

        //total = 1/56 + 1/56 = 1/28
        
    	System.out.println("Test 2: Backward");
        
        treeMatch = "((((A,B),C),D),E);";
        NewickImporter importer = new NewickImporter("(((D:2.0,C:2.0):1.0,(A:1.0,B:1.0):2.0):1.0,E:4.0);");
        FlexibleTree tree5_2 = (FlexibleTree) importer.importTree(null);

        count = 0;

        for (int i = 0; i < reps; i++) {

            try {
                TreeModel treeModel = new TreeModel("treeModel", tree5_2);
                ExchangeOperator operator = new ExchangeOperator(ExchangeOperator.Mode.WIDE, treeModel, 1.0);
                operator.doOperation();

                String tree = Tree.Utils.newickNoLengths(treeModel);

                if (tree.equals(treeMatch)) {
                    count += 1;
                }

            } catch (OperatorFailedException e) {
//                e.printStackTrace();
            }

        }
        double p_2 = (double) count / (double) reps;

        System.out.println("Number of proposals:\t" + count);
        System.out.println("Number of tries:\t" + reps);
        System.out.println("Number of ratio:\t" + p_2);
        System.out.println("Number of expected ratio:\t" + 1.0/28.0);
        assertExpectation(1.0/28.0, p_2, reps);
    }

    // STATIC METHODS

    public OperatorSchedule getOperatorSchedule(TreeModel treeModel) {

        Parameter rootParameter = treeModel.createNodeHeightsParameter(true, false, false);
        Parameter internalHeights = treeModel.createNodeHeightsParameter(false, true, false);

        ExchangeOperator operator = new ExchangeOperator(ExchangeOperator.Mode.WIDE, treeModel, 1.0);
        ScaleOperator scaleOperator = new ScaleOperator(rootParameter, 0.75, CoercionMode.COERCION_ON, 1.0);
        UniformOperator uniformOperator = new UniformOperator(internalHeights, 1.0);

        OperatorSchedule schedule = new SimpleOperatorSchedule();
        schedule.addOperator(operator);
        schedule.addOperator(scaleOperator);
        schedule.addOperator(uniformOperator);

        return schedule;
    }
}
