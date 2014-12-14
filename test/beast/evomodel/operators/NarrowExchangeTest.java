/*
 * NarrowExchangeTest.java
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

//import static org.junit.Assert.*;

import beast.evolution.io.Importer.ImportException;
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

//import beast.evomodel.operators.NNI;

/**
 * @author shhn001
 *
 */
public class NarrowExchangeTest  extends OperatorAssert {
    

	/**
	 * Test method for {@link beast.evomodel.operators.ExchangeOperator#narrow()}.
	 */
    @Test
	public void testNarrow() throws IOException, ImportException {
		// probability of picking B node is 1/(2n-4) = 1/6
        // probability of swapping it with C is 1/1
        // total = 1/6
    	
    	System.out.println("Test 1: Forward");

        String treeMatch = "((((A,C),B),D),E);";
        
        int count = 0;
        int reps = 100000;

        for (int i = 0; i < reps; i++) {

            try {
                TreeModel treeModel = new TreeModel("treeModel", tree5);
                ExchangeOperator operator = new ExchangeOperator(ExchangeOperator.Mode.NARROW, treeModel, 1);
                operator.doOperation();

                String tree = Tree.Utils.newickNoLengths(treeModel);

                if (tree.equals(treeMatch)) {
                    count += 1;
                }

            } catch (OperatorFailedException e) {
                e.printStackTrace();
            }

        }
        double p_1 = (double) count / (double) reps;

        System.out.println("Number of proposals:\t" + count);
        System.out.println("Number of tries:\t" + reps);
        System.out.println("Number of ratio:\t" + p_1);
        System.out.println("Number of expected ratio:\t" + 1.0/6.0);
        assertExpectation(1.0/6.0, p_1, reps);
        
	}
	
	public OperatorSchedule getOperatorSchedule(TreeModel treeModel) {

        Parameter rootParameter = treeModel.createNodeHeightsParameter(true, false, false);
        Parameter internalHeights = treeModel.createNodeHeightsParameter(false, true, false);

        ExchangeOperator operator = new ExchangeOperator(ExchangeOperator.Mode.NARROW, treeModel, 1.0);
        ScaleOperator scaleOperator = new ScaleOperator(rootParameter, 0.75, CoercionMode.COERCION_ON, 1.0);
        UniformOperator uniformOperator = new UniformOperator(internalHeights, 1.0);

        OperatorSchedule schedule = new SimpleOperatorSchedule();
        schedule.addOperator(operator);
        schedule.addOperator(scaleOperator);
        schedule.addOperator(uniformOperator);

        return schedule;
    }
}
