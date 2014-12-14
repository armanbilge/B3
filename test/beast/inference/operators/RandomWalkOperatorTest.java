/*
 * RandomWalkOperatorTest.java
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

package beast.inference.operators;

import beast.inference.model.Parameter;
import beast.math.MachineAccuracy;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * RandomWalkOperatorTest.
 *
 * @author Andrew Rambaut
 * @version 1.0
 * @since <pre>08/26/2007</pre>
 */
public class RandomWalkOperatorTest {

    @Test
    public void testRandomWalkOperator() {
        Parameter parameter = new Parameter.Default("test", 0.5, 0.0, 1.0);
        RandomWalkOperator rwo = new RandomWalkOperator(parameter, 1.0, RandomWalkOperator.BoundaryCondition.reflecting, 1.0, CoercionMode.COERCION_OFF);
        
        double test1 = rwo.reflectValue(8.7654321, 3.14159265, Double.POSITIVE_INFINITY);
        double test2 = rwo.reflectValue(8.7654321, Double.NEGATIVE_INFINITY, 3.14159265);
        double test3 = rwo.reflectValue(1.2345678, 3.14159265, 2.0 * 3.14159265);
        double test4 = rwo.reflectValue(12345678.987654321, 3.14159265, 2.0 * 3.14159265);

        double test1b = rwo.reflectValueLoop(8.7654321, 3.14159265, Double.POSITIVE_INFINITY);
        double test2b = rwo.reflectValueLoop(8.7654321, Double.NEGATIVE_INFINITY, 3.14159265);
        double test3b = rwo.reflectValueLoop(1.2345678, 3.14159265, 2.0 * 3.14159265);
        double test4b = rwo.reflectValueLoop(12345678.987654321, 3.14159265, 2.0 * 3.14159265);

        assertEquals(test1, test1b, MachineAccuracy.EPSILON);
        assertEquals(test2, test2b, MachineAccuracy.EPSILON);
        assertEquals(test3, test3b, MachineAccuracy.EPSILON);
        assertTrue(Math.abs(test4 - test4b) < 0.001);

    }

}