/*
 * TN93Test.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2014 BEAST Developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.beagle;

import beast.beagle.substmodel.EigenDecomposition;
import beast.beagle.substmodel.FrequencyModel;
import beast.beagle.substmodel.TN93;
import beast.evolution.datatype.Nucleotides;
import beast.inference.model.Parameter;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;


/**
 * @author Marc Suchard
 */
public class TN93Test {

    @Test
    public void testTN93() {

        Parameter kappa1 = new Parameter.Default(5.0);
        Parameter kappa2 = new Parameter.Default(2.0);
        double[] pi = new double[]{0.40, 0.20, 0.30, 0.10};
        double time = 0.1;

        FrequencyModel freqModel = new FrequencyModel(Nucleotides.INSTANCE, pi);
        TN93 tn = new TN93(kappa1, kappa2, freqModel);

        EigenDecomposition decomp = tn.getEigenDecomposition();

//        Vector eval = new Vector(decomp.getEigenValues());
//        System.out.println("Eval = " + eval);

        double[] probs = new double[16];
        tn.getTransitionProbabilities(time, probs);
//        System.out.println("new probs = " + new Vector(probs));

        // check against old implementation
        beast.evomodel.substmodel.FrequencyModel oldFreq = new beast.evomodel.substmodel.FrequencyModel(Nucleotides.INSTANCE, pi);
        beast.evomodel.substmodel.TN93 oldTN = new beast.evomodel.substmodel.TN93(kappa1, kappa2, oldFreq);

        double[] oldProbs = new double[16];
        oldTN.getTransitionProbabilities(time, oldProbs);
//        System.out.println("old probs = " + new Vector(oldProbs));
        assertArrayEquals(probs, oldProbs, 10E-6);
    }

}
