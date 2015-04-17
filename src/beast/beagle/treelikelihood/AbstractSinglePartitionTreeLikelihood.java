/*
 * AbstractSinglePartitionTreeLikelihood.java
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

package beast.beagle.treelikelihood;
/**
 * ${CLASS_NAME}
 *
 * @author Andrew Rambaut
 * @version $Id$
 *
 * $HeadURL$
 *
 * $LastChangedBy$
 * $LastChangedDate$
 * $LastChangedRevision$
 */

import beast.evolution.alignment.PatternList;
import beast.evolution.datatype.DataType;
import beast.evomodel.tree.TreeModel;

public abstract class AbstractSinglePartitionTreeLikelihood extends AbstractTreeLikelihood {
    public AbstractSinglePartitionTreeLikelihood(String name, PatternList patternList, TreeModel treeModel) {
        super(name, treeModel);

        this.patternList = patternList;
        this.dataType = patternList.getDataType();
        patternCount = patternList.getPatternCount();
        stateCount = dataType.getStateCount();

        patternWeights = patternList.getPatternWeights();

    }

    /**
     * Set update flag for a pattern
     */
    protected void updatePattern(int i) {
        if (updatePattern != null) {
            updatePattern[i] = true;
        }
        super.makeDirty();
    }

    /**
     * Set update flag for all patterns
     */
    protected void updateAllPatterns() {
        if (updatePattern != null) {
            for (int i = 0; i < patternCount; i++) {
                updatePattern[i] = true;
            }
        }
        super.makeDirty();
    }

    public final double[] getPatternWeights() {
        return patternWeights;
    }

    /**
     * Forces a complete recalculation of the likelihood next time getLikelihood is called
     */
    public void makeDirty() {
        super.makeDirty();
        updateAllPatterns();
    }

    /**
     * the patternList
     */
    protected PatternList patternList = null;
    protected DataType dataType = null;

    /**
     * the pattern weights
     */
    protected double[] patternWeights;

    /**
     * the number of patterns
     */
    protected int patternCount;

    /**
     * the number of states in the data
     */
    protected int stateCount;

    /**
     * Flags to specify which patterns are to be updated
     */
    protected boolean[] updatePattern = null;


}
