/*
 * SequenceLikelihoodTest.java
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

package beast.evomodel.treelikelihood;

import beast.evolution.alignment.SimpleAlignment;
import beast.evolution.alignment.SitePatterns;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.Nucleotides;
import beast.evolution.sequence.Sequence;
import beast.evolution.util.Date;
import beast.evolution.util.Taxon;
import beast.evolution.util.Units;
import beast.evomodel.sitemodel.GammaSiteModel;
import beast.evomodel.substmodel.FrequencyModel;
import beast.evomodel.substmodel.HKY;
import beast.evomodel.tree.TreeModel;
import beast.inference.model.Parameter;
import beast.inference.trace.TraceCorrelationAssert;
import org.junit.Ignore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Marc A. Suchard
 */
@Ignore
public abstract class SequenceLikelihoodTest extends TraceCorrelationAssert {
    protected NumberFormat format = NumberFormat.getNumberInstance(Locale.ENGLISH);
    protected TreeModel treeModel;
    protected static double tolerance = 1E-8;

    private void recursivelyAddCharacter(String[] sequences, List<Integer> pattern,
                                        DataType dataType) {
        final int nTaxa = sequences.length;
        if (pattern.size() == nTaxa) {
            // Add pattern
            for (int i = 0; i < nTaxa; i++) {
                sequences[i] = sequences[i] + dataType.getCode(pattern.get(i));
            }
        } else {
            // Continue recursion
            final int stateCount = dataType.getStateCount();
            for (int i = 0; i < stateCount; i++) {
                List<Integer> newPattern = new ArrayList<Integer>();
                newPattern.addAll(pattern);
                newPattern.add(i);
                recursivelyAddCharacter(sequences, newPattern, dataType);
            }
        }
    }

    private String[] createAllUniquePatterns(int nTaxa, DataType dataType) {
        String[] result = new String[nTaxa];
        for (int i = 0; i < nTaxa; i++) {
            result[i] = "";
        }
        List<Integer> pattern = new ArrayList<Integer>();
        recursivelyAddCharacter(result, pattern, dataType);
        return result;
    }

    protected void createAlignmentWithAllUniquePatterns(Object[][] taxa_sequence, DataType dataType) {

        alignment = new SimpleAlignment();
        alignment.setDataType(dataType);

        int nTaxa = taxa_sequence[0].length;

        String[] allUniquePatterns = createAllUniquePatterns(nTaxa, dataType);
        taxa_sequence[1] = allUniquePatterns;

        taxa = new Taxon[nTaxa]; // 6, 17
        System.out.println("Taxon len = " + taxa_sequence[0].length);
        System.out.println("Alignment len = " + taxa_sequence[1].length);
        if (taxa_sequence.length > 2) System.out.println("Date len = " + taxa_sequence[2].length);

        for (int i=0; i < taxa_sequence[0].length; i++) {
            taxa[i] = new Taxon(taxa_sequence[0][i].toString());

            if (taxa_sequence.length > 2) {
                Date date = new Date((Double) taxa_sequence[2][i], Units.Type.YEARS, (Boolean) taxa_sequence[3][0]);
                taxa[i].setDate(date);
            }

            //taxonList.addTaxon(taxon);
            Sequence sequence = new Sequence(taxa_sequence[1][i].toString());
            sequence.setTaxon(taxa[i]);
            sequence.setDataType(dataType);

            alignment.addSequence(sequence);
        }

        System.out.println("Sequence pattern count = " + alignment.getPatternCount());
    }

    protected double[] computeSitePatternLikelihoods(SitePatterns patterns) {
        // Sub model
        Parameter freqs = new Parameter.Default(alignment.getStateFrequencies());
        Parameter kappa = new Parameter.Default("kappa", 29.739445, 0, 100);

        FrequencyModel f = new FrequencyModel(Nucleotides.INSTANCE, freqs);
        HKY hky = new HKY(kappa, f);

        //siteModel
        GammaSiteModel siteModel = new GammaSiteModel(hky);
        Parameter mu = new Parameter.Default("mutationRate", 1.0, 0, Double.POSITIVE_INFINITY);
        siteModel.setMutationRateParameter(mu);

        //treeLikelihood
        TreeLikelihood treeLikelihood = new TreeLikelihood(patterns, treeModel, siteModel, null, null,
                false, false, true, false, false);

        return treeLikelihood.getPatternLogLikelihoods();
    }

    protected double computeSumOfPatterns(SitePatterns patterns) {

        double[] patternLogLikelihoods = computeSitePatternLikelihoods(patterns);
        double total = 0;
        for (double x: patternLogLikelihoods) {
            total += Math.exp(x);
        }
        return total;
    }
}
