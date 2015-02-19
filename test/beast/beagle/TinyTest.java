/*
 * TinyTest.java
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

import beast.beagle.branchmodel.BranchModel;
import beast.beagle.branchmodel.HomogeneousBranchModel;
import beast.beagle.sitemodel.BranchSubstitutionModel;
import beast.beagle.sitemodel.GammaSiteRateModel;
import beast.beagle.substmodel.FrequencyModel;
import beast.beagle.substmodel.HKY;
import beast.beagle.treelikelihood.BeagleTreeLikelihood;
import beast.beagle.treelikelihood.PartialsRescalingScheme;
import beast.evolution.alignment.SitePatterns;
import beast.evolution.datatype.Nucleotides;
import beast.evomodel.branchratemodel.BranchRateModel;
import beast.inference.model.Parameter;
import beast.inference.trace.TraceCorrelationAssert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Marc A. Suchard
 */
public class TinyTest extends TraceCorrelationAssert {

    @Test
    public void testTiny() {

        createAlignment(sequences, Nucleotides.INSTANCE);

        try {
            createSpecifiedTree("((human:0.1,chimp:0.1):0.1,gorilla:0.2)");
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse Newick tree");
        }

        System.out.println("\nTest Likelihood using JC69:");

        //substitutionModel
        Parameter freqs = new Parameter.Default(new double[]{0.25, 0.25, 0.25, 0.25});
        Parameter kappa = new Parameter.Default("kappa", 1.0, 0, 100);
        FrequencyModel f = new FrequencyModel(Nucleotides.INSTANCE, freqs);
        HKY hky = new HKY(kappa, f);

        //siteModel
        double alpha = 0.5;
        GammaSiteRateModel siteRateModel = new GammaSiteRateModel("gammaModel", alpha, 4);
        siteRateModel.setSubstitutionModel(hky);
        Parameter mu = new Parameter.Default("mutationRate", 1.0, 0, Double.POSITIVE_INFINITY);
        siteRateModel.setMutationRateParameter(mu);

        //treeLikelihood
        SitePatterns patterns = new SitePatterns(alignment, null, 0, -1, 1, true);

        BranchModel branchModel = new HomogeneousBranchModel(hky, f);

        BranchRateModel branchRateModel = null;

        BeagleTreeLikelihood treeLikelihood = new BeagleTreeLikelihood(patterns,
                treeModel,
                branchModel,
                siteRateModel,
                branchRateModel,
                null,
                false,
                PartialsRescalingScheme.DEFAULT);

        double logLikelihood = treeLikelihood.getLogLikelihood();

        System.out.println("logLikelihood = " + logLikelihood);

        assertEquals(-1504.51794, logLikelihood, 1E-3);
    }


    static private String sequences[][] = {
            {"human", "chimp", "gorilla"},
            {
                    "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGGAGCTTAAACCCCCTTATTTCTACTAGGACTATGAGAATCGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTATACCCTTCCCGTACTAAGAAATTTAGGTTAAATACAGACCAAGAGCCTTCAAAGCCCTCAGTAAGTTG-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGACCAATGGGACTTAAACCCACAAACACTTAGTTAACAGCTAAGCACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCGGAGCTTGGTAAAAAGAGGCCTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGGCCTCCATGACTTTTTCAAAAGGTATTAGAAAAACCATTTCATAACTTTGTCAAAGTTAAATTATAGGCT-AAATCCTATATATCTTA-CACTGTAAAGCTAACTTAGCATTAACCTTTTAAGTTAAAGATTAAGAGAACCAACACCTCTTTACAGTGA",
                    "AGAAATATGTCTGATAAAAGAATTACTTTGATAGAGTAAATAATAGGAGTTCAAATCCCCTTATTTCTACTAGGACTATAAGAATCGAACTCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTATCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTTACACCCTTCCCGTACTAAGAAATTTAGGTTAAGCACAGACCAAGAGCCTTCAAAGCCCTCAGCAAGTTA-CAATACTTAATTTCTGTAAGGACTGCAAAACCCCACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATTAATGGGACTTAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAATCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAA-TCACCTCAGAGCTTGGTAAAAAGAGGCTTAACCCCTGTCTTTAGATTTACAGTCCAATGCTTCA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCTAAAGCTGGTTTCAAGCCAACCCCATGACCTCCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAAGTTAAATTACAGGTT-AACCCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCATTAACCTTTTAAGTTAAAGATTAAGAGGACCGACACCTCTTTACAGTGA",
                    "AGAAATATGTCTGATAAAAGAGTTACTTTGATAGAGTAAATAATAGAGGTTTAAACCCCCTTATTTCTACTAGGACTATGAGAATTGAACCCATCCCTGAGAATCCAAAATTCTCCGTGCCACCTGTCACACCCCATCCTAAGTAAGGTCAGCTAAATAAGCTATCGGGCCCATACCCCGAAAATGTTGGTCACATCCTTCCCGTACTAAGAAATTTAGGTTAAACATAGACCAAGAGCCTTCAAAGCCCTTAGTAAGTTA-CAACACTTAATTTCTGTAAGGACTGCAAAACCCTACTCTGCATCAACTGAACGCAAATCAGCCACTTTAATTAAGCTAAGCCCTTCTAGATCAATGGGACTCAAACCCACAAACATTTAGTTAACAGCTAAACACCCTAGTCAAC-TGGCTTCAATCTAAAGCCCCGGCAGG-TTTGAAGCTGCTTCTTCGAATTTGCAATTCAATATGAAAT-TCACCTCGGAGCTTGGTAAAAAGAGGCCCAGCCTCTGTCTTTAGATTTACAGTCCAATGCCTTA-CTCAGCCATTTTACCACAAAAAAGGAAGGAATCGAACCCCCCAAAGCTGGTTTCAAGCCAACCCCATGACCTTCATGACTTTTTCAAAAGATATTAGAAAAACTATTTCATAACTTTGTCAAGGTTAAATTACGGGTT-AAACCCCGTATATCTTA-CACTGTAAAGCTAACCTAGCGTTAACCTTTTAAGTTAAAGATTAAGAGTATCGGCACCTCTTTGCAGTGA"
            }
    };
}