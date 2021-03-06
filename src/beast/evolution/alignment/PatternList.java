/*
 * PatternList.java
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

package beast.evolution.alignment;

import beast.evolution.datatype.DataType;
import beast.evolution.util.TaxonList;
import beast.util.Identifiable;

/**
 * interface for any list of patterns with weights.
 *
 * @author Andrew Rambaut
 * @version $Id: PatternList.java,v 1.12 2005/05/24 20:25:55 rambaut Exp $
 */
public interface PatternList extends TaxonList, Identifiable {
    /**
     * @return number of patterns
     */
    int getPatternCount();

    /**
     * @return number of states for this PatternList
     */
    int getStateCount();

    /**
     * Gets the length of the pattern strings which will usually be the
     * same as the number of taxa
     *
     * @return the length of patterns
     */
    int getPatternLength();

    /**
     * Gets the pattern as an array of state numbers (one per sequence)
     *
     * @param patternIndex the index of the pattern to return
     * @return the site pattern at patternIndex
     */
    int[] getPattern(int patternIndex);

    /**
     * @param taxonIndex   the taxon
     * @param patternIndex the pattern
     * @return state at (taxonIndex, patternIndex)
     */
    int getPatternState(int taxonIndex, int patternIndex);

    /**
     * Gets the weight of a site pattern
     *
     * @param patternIndex the pattern
     * @return the weight of the specified pattern
     */
    double getPatternWeight(int patternIndex);

    /**
     * @return the array of pattern weights
     */
    double[] getPatternWeights();

    /**
     * @return the DataType of this PatternList
     */
    DataType getDataType();

    /**
     * @return the frequency of each state
     */
    double[] getStateFrequencies();

    /**
     * Returns a double array containing the empirically estimated frequencies
     * for the states of patternList. This currently calls the version that maps
     * to PAUP's estimates.
     *
     * @return the empirical state frequencies of the given pattern list
     */
    default double[] empiricalStateFrequencies() {
        return empiricalStateFrequenciesPAUP();
    }

    /**
     * Returns a double array containing the empirically estimated frequencies
     * for the states of patternList. This version of the routine should match
     * the values produced by PAUP.
     *
     * @return the empirical state frequencies of the given pattern list
     */
    default double[] empiricalStateFrequenciesPAUP() {
        int i, j, k;
        double total, sum, x, w, difference;

        DataType dataType = getDataType();

        int stateCount = getStateCount();
        int patternLength = getPatternLength();
        int patternCount = getPatternCount();

        double[] freqs = equalStateFrequencies();

        double[] tempFreq = new double[stateCount];
        int[] pattern;
        boolean[] state;

        int count = 0;
        do {
            for (i = 0; i < stateCount; i++)
                tempFreq[i] = 0.0;

            total = 0.0;
            for (i = 0; i < patternCount; i++) {
                pattern = getPattern(i);
                w = getPatternWeight(i);

                for (k = 0; k < patternLength; k++) {
                    state = dataType.getStateSet(pattern[k]);

                    sum = 0.0;
                    for (j = 0; j < stateCount; j++)
                        if (state[j])
                            sum += freqs[j];

                    for (j = 0; j < stateCount; j++) {
                        if (state[j]) {
                            x = (freqs[j] * w) / sum;
                            tempFreq[j] += x;
                            total += x;
                        }
                    }
                }

            }

            difference = 0.0;
            for (i = 0; i < stateCount; i++) {
                difference += Math.abs((tempFreq[i] / total) - freqs[i]);
                freqs[i] = tempFreq[i] / total;
            }
            count ++;
        } while (difference > 1E-8 && count < 1000);

        return freqs;
    }

    /**
     * Returns a double array containing the empirically estimated frequencies
     * for the states of patternList. This version of the routine should match
     * the values produced by MrBayes.
     *
     * @return the empirical state frequencies of the given pattern list
     */
    default double[] empiricalStateFrequenciesMrBayes() {

        DataType dataType = getDataType();

        int stateCount = getStateCount();
        int patternLength = getPatternLength();
        int patternCount = getPatternCount();

        double[] freqs = equalStateFrequencies();

        double sumTotal = 0.0;

        double[] sumFreq = new double[stateCount];

        for (int i = 0; i < patternCount; i++) {
            int[] pattern = getPattern(i);
            double w = getPatternWeight(i);

            for (int k = 0; k < patternLength; k++) {
                boolean[] state = dataType.getStateSet(pattern[k]);

                double sum = 0.0;
                for (int j = 0; j < stateCount; j++) {
                    if (state[j]) {
                        sum += freqs[j];
                    }
                }

                for (int j = 0; j < stateCount; j++) {
                    if (state[j]) {
                        double x = (freqs[j] * w) / sum;
                        sumFreq[j] += x;
                        sumTotal += x;
                    }
                }
            }

        }

        for (int i = 0; i < stateCount; i++) {
            freqs[i] = sumFreq[i] / sumTotal;
        }

        return freqs;
    }

    /**
     * Returns a double array containing the equal frequencies
     * for the states of patternList.
     *
     * @return return equal state frequencies based on the data type of
     *         the patternlist
     */
    default double[] equalStateFrequencies() {
        int i, n = getStateCount();
        double[] freqs = new double[n];
        double f = 1.0 / n;

        for (i = 0; i < n; i++)
            freqs[i] = f;

        return freqs;
    }

    /**
     * Helper routines for pattern lists.
     */
    @Deprecated
    public static class Utils {
        /**
         * Returns a double array containing the empirically estimated frequencies
         * for the states of patternList. This currently calls the version that maps
         * to PAUP's estimates.
         *
         * @param patternList the pattern list to calculate the empirical state
         *                    frequencies from
         * @return the empirical state frequencies of the given pattern list
         */
        @Deprecated
        public static double[] empiricalStateFrequencies(PatternList patternList) {
            return patternList.empiricalStateFrequencies();
        }

        /**
         * Returns a double array containing the empirically estimated frequencies
         * for the states of patternList. This version of the routine should match
         * the values produced by PAUP.
         *
         * @param patternList the pattern list to calculate the empirical state
         *                    frequencies from
         * @return the empirical state frequencies of the given pattern list
         */
        @Deprecated
        public static double[] empiricalStateFrequenciesPAUP(PatternList patternList) {
            return patternList.empiricalStateFrequenciesPAUP();
        }

        /**
         * Returns a double array containing the empirically estimated frequencies
         * for the states of patternList. This version of the routine should match
         * the values produced by MrBayes.
         *
         * @param patternList the pattern list to calculate the empirical state
         *                    frequencies from
         * @return the empirical state frequencies of the given pattern list
         */
        @Deprecated
        public static double[] empiricalStateFrequenciesMrBayes(PatternList patternList) {
            return patternList.empiricalStateFrequenciesMrBayes();
        }

        /**
         * Returns a double array containing the equal frequencies
         * for the states of patternList.
         *
         * @param patternList the pattern list
         * @return return equal state frequencies based on the data type of
         *         the patternlist
         */
        @Deprecated
        public static double[] equalStateFrequencies(PatternList patternList) {
            return patternList.equalStateFrequencies();
        }
    }

}
