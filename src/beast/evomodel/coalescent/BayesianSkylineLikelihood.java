/*
 * BayesianSkylineLikelihood.java
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

package beast.evomodel.coalescent;

import beast.evolution.coalescent.ConstantPopulation;
import beast.evolution.coalescent.ExponentialBSPGrowth;
import beast.evolution.tree.Tree;
import beast.evolution.util.Units;
import beast.evomodel.tree.TreeModel;
import beast.inference.model.Parameter;
import beast.inference.model.Statistic;
import beast.math.MathUtils;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.Date;
import java.util.logging.Logger;

/**
 * A likelihood function for the generalized skyline plot coalescent. Takes a tree and population size and group size parameters.
 *
 * @version $Id: BayesianSkylineLikelihood.java,v 1.5 2006/03/06 11:26:49 rambaut Exp $
 *
 * @author Alexei Drummond
 */
public class BayesianSkylineLikelihood extends OldAbstractCoalescentLikelihood {

    // PUBLIC STUFF

    public static final String SKYLINE_LIKELIHOOD = "generalizedSkyLineLikelihood";

    private enum Type {
        STEPWISE,
        LINEAR,
        EXPONENTIAL;
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    public BayesianSkylineLikelihood(Tree tree,
                                     Parameter popSizeParameter,
                                     Parameter groupSizeParameter,
                                     Type type) {
        super(SKYLINE_LIKELIHOOD);

        this.groupSizeParameter = groupSizeParameter;
        this.popSizeParameter = popSizeParameter;
        int events = tree.getExternalNodeCount() - 1;
        int paramDim1 = popSizeParameter.getDimension();
        int paramDim2 = groupSizeParameter.getDimension();
        this.type = type;

        switch (type) {
            case EXPONENTIAL:
                if (paramDim1 != (paramDim2+1)) {
                    throw new IllegalArgumentException("Dimension of population parameter must be one greater than dimension of group size parameter.");
                }
                break;
            case LINEAR:
                if (paramDim1 != (paramDim2+1)) {
                    throw new IllegalArgumentException("Dimension of population parameter must be one greater than dimension of group size parameter.");
                }
                break;
            case STEPWISE:
                if (paramDim1 != paramDim2) {
                    throw new IllegalArgumentException("Dimension of population parameter and group size parameters should be the same.");
                }
                break;
        }

        if (paramDim2 > events) {
            throw new IllegalArgumentException("There are more groups than coalescent nodes in the tree.");
        }

        int eventsCovered = 0;
        for (int i = 0; i < getGroupCount(); i++) {
            eventsCovered += getGroupSize(i);
        }

        if (eventsCovered != events) {

            if (eventsCovered == 0 || eventsCovered == paramDim2) {
                double[] uppers = new double[paramDim2];
                double[] lowers = new double[paramDim2];

                // For these special cases we assume that the XML has not specified initial group sizes
                // or has set all to 1 and we set them here automatically...
                int eventsEach = events / paramDim2;
                int eventsExtras = events % paramDim2;
                for (int i = 0; i < paramDim2; i++) {
                    if (i < eventsExtras) {
                        groupSizeParameter.setParameterValue(i, eventsEach + 1);
                    } else {
                        groupSizeParameter.setParameterValue(i, eventsEach);
                    }
                    uppers[i] = Double.MAX_VALUE;
                    lowers[i] = 1.0;
                }

                if (type == Type.EXPONENTIAL || type == Type.LINEAR) {
                    lowers[0] = 2.0;
                }
                groupSizeParameter.addBounds(new Parameter.DefaultBounds(uppers, lowers));
            } else {
                // ... otherwise assume the user has made a mistake setting initial group sizes.
                throw new IllegalArgumentException("The sum of the initial group sizes does not match the number of coalescent events in the tree.");
            }
        }

        if ((type == Type.EXPONENTIAL || type == Type.LINEAR) && groupSizeParameter.getParameterValue(0) < 2.0) {
            throw new IllegalArgumentException("For linear or exponential model first group size must be >= 2.");
        }

        this.tree = tree;
        if (tree instanceof TreeModel) {
            addModel((TreeModel)tree);
        }
        addVariable(popSizeParameter);

        addVariable(groupSizeParameter);

        setupIntervals();

        addStatistic(new GroupHeightStatistic());
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a demographic model.
     */
    public double getLogLikelihood() {

        setupIntervals();

        double logL = 0.0;

        double currentTime = 0.0;

        int groupIndex=0;
        int[] groupSizes = getGroupSizes();
        double[] groupEnds = getGroupHeights();

        int subIndex = 0;

        if (type == Type.EXPONENTIAL) {
            ExponentialBSPGrowth eg = new ExponentialBSPGrowth(Units.Type.YEARS);

            for (int j = 0; j < intervalCount; j++) {
                double startGroupPopSize = popSizeParameter.getParameterValue(groupIndex);
                double endGroupPopSize = popSizeParameter.getParameterValue(groupIndex+1);
                double startTime = currentTime;
                double endTime = currentTime + intervals[j];

                eg.setup(startGroupPopSize, endGroupPopSize, endTime - startTime);

                if (getIntervalType(j) == CoalescentEventType.COALESCENT) {
                    subIndex += 1;
                    if (subIndex >= groupSizes[groupIndex]) {
                        groupIndex += 1;
                        subIndex = 0;
                    }
                }

                logL += calculateIntervalLikelihood(eg, intervals[j], currentTime, lineageCounts[j], getIntervalType(j));

                // insert zero-length coalescent intervals
                int diff = getCoalescentEvents(j)-1;
                for (int k = 0; k < diff; k++) {
                    eg.setup(startGroupPopSize, startGroupPopSize, endTime - startTime);
                    logL += calculateIntervalLikelihood(eg, 0.0, currentTime, lineageCounts[j]-k-1,
                            CoalescentEventType.COALESCENT);
                    subIndex += 1;
                    if (subIndex >= groupSizes[groupIndex]) {
                        groupIndex += 1;
                        subIndex = 0;
                    }
                }

                currentTime += intervals[j];
            }
        } else {
            ConstantPopulation cp = new ConstantPopulation(Units.Type.YEARS);

            for (int j = 0; j < intervalCount; j++) {

                // set the population size to the size of the middle of the current interval
                final double ps = getPopSize(groupIndex, currentTime + (intervals[j]/2.0), groupEnds);
                cp.setN0(ps);
                if (getIntervalType(j) == CoalescentEventType.COALESCENT) {
                    subIndex += 1;
                    if (subIndex >= groupSizes[groupIndex]) {
                        groupIndex += 1;
                        subIndex = 0;
                    }
                }

                logL += calculateIntervalLikelihood(cp, intervals[j], currentTime, lineageCounts[j], getIntervalType(j));

                // insert zero-length coalescent intervals
                int diff = getCoalescentEvents(j)-1;
                for (int k = 0; k < diff; k++) {
                    cp.setN0(getPopSize(groupIndex, currentTime, groupEnds));
                    logL += calculateIntervalLikelihood(cp, 0.0, currentTime, lineageCounts[j]-k-1,
                            CoalescentEventType.COALESCENT);
                    subIndex += 1;
                    if (subIndex >= groupSizes[groupIndex]) {
                        groupIndex += 1;
                        subIndex = 0;
                    }
                }

                currentTime += intervals[j];
            }
        }
        return logL;
    }

    /**
     * @return the pop size for the given time. If linear model is being used then this pop size is
     * interpolated between the two pop sizes at either end of the grouped interval.
     */
    public final double getPopSize(int groupIndex, double midTime, double[] groupHeights) {
        if (type == Type.LINEAR) {

            double startGroupPopSize = popSizeParameter.getParameterValue(groupIndex);
            double endGroupPopSize = popSizeParameter.getParameterValue(groupIndex+1);

            double startGroupTime = 0.0;
            if (groupIndex > 0) {
                startGroupTime = groupHeights[groupIndex-1];
            }
            double endGroupTime = groupHeights[groupIndex];

            // calculate the gradient
            double m = (endGroupPopSize-startGroupPopSize)/(endGroupTime-startGroupTime);

            // calculate the population size at midTime using linear interpolation
            final double midPopSize = (m * (midTime-startGroupTime)) + startGroupPopSize;

            return midPopSize;
        } else {
            return popSizeParameter.getParameterValue(groupIndex);
        }
    }

    /* GAL: made public to give BayesianSkylineGibbsOperator access */
    public final int[] getGroupSizes() {
        if ((type == Type.EXPONENTIAL || type == Type.LINEAR) && groupSizeParameter.getParameterValue(0) < 2.0) {
            throw new IllegalArgumentException("For linear model first group size must be >= 2.");
        }

        int[] groupSizes = new int[groupSizeParameter.getDimension()];

        for (int i = 0; i < groupSizes.length; i++) {
            double g = groupSizeParameter.getParameterValue(i);
            if (g != Math.round(g)) {
                throw new RuntimeException("Group size " + i + " should be integer but found:" + g);
            }
            groupSizes[i] = (int)Math.round(g);
        }
        return groupSizes;
    }

    private  int getGroupCount() {
        return groupSizeParameter.getDimension();
    }

    private  int getGroupSize(int groupIndex) {
        double g = groupSizeParameter.getParameterValue(groupIndex);
        if (g != Math.round(g)) {
            throw new RuntimeException("Group size " + groupIndex + " should be integer but found:" + g);
        }
        return (int)Math.round(g);
    }

    /* GAL: made public to give BayesianSkylineGibbsOperator access */
    public final double[] getGroupHeights() {
        double[] groupEnds = new double[getGroupCount()];

        double timeEnd = 0.0;
        int groupIndex = 0;
        int subIndex = 0;
        for (int i = 0; i < intervalCount; i++) {

            timeEnd += intervals[i];

            if (getIntervalType(i) == CoalescentEventType.COALESCENT) {
                subIndex += 1;
                if (subIndex >= getGroupSize(groupIndex)) {
                    groupEnds[groupIndex] = timeEnd;
                    groupIndex += 1;
                    subIndex = 0;
                }
            }
        }
        groupEnds[getGroupCount()-1] = timeEnd;

        return groupEnds;
    }

    private double getGroupHeight(int groupIndex) {
        return getGroupHeights()[groupIndex];
    }

    final public Type getType() {
        return type;
    }

    final public Parameter getPopSizeParameter() {
        return popSizeParameter;
    }

    final public Parameter getGroupSizeParameter() {
        return groupSizeParameter;
    }

    // ****************************************************************
    // Implementing Demographic Reconstructor
    // ****************************************************************

    public String getTitle() {
        final String title = "Bayesian Skyline (" + (type == Type.STEPWISE ? "stepwise" : "linear") + ")\n" +
                "Generated " + (new Date()).toString() + " [seed=" + MathUtils.getSeed() + "]";
        return title;
    }

    // ****************************************************************
    // Inner classes
    // ****************************************************************

    public class GroupHeightStatistic extends Statistic.Abstract {

        public GroupHeightStatistic() {
            super("groupHeight");
        }

        public int getDimension() { return getGroupCount(); }

        public double getStatisticValue(int i) {
            return getGroupHeight(i);
        }

    }

    // ****************************************************************
    // Private and protected stuff
    // ****************************************************************

    /** The demographic model. */
    private final Parameter popSizeParameter;

    private final Parameter groupSizeParameter;

    private final Type type;

    public static final XMLObjectParser<BayesianSkylineLikelihood> PARSER = new AbstractXMLObjectParser<BayesianSkylineLikelihood>() {
        public static final String POPULATION_SIZES = "populationSizes";
        public static final String GROUP_SIZES = "groupSizes";

        public static final String TYPE = "type";

        public String getParserName() {
            return SKYLINE_LIKELIHOOD;
        }

        public BayesianSkylineLikelihood parseXMLObject(XMLObject xo) throws XMLParseException {

            XMLObject cxo = xo.getChild(POPULATION_SIZES);
            Parameter param = (Parameter) cxo.getChild(Parameter.class);

            cxo = xo.getChild(GROUP_SIZES);
            Parameter param2 = (Parameter) cxo.getChild(Parameter.class);

            cxo = xo.getChild(CoalescentLikelihood.POPULATION_TREE);
            TreeModel treeModel = (TreeModel) cxo.getChild(TreeModel.class);

            Type type = Type.LINEAR;
            if (xo.hasAttribute(Type.LINEAR.toString()) && !xo.getBooleanAttribute(Type.LINEAR.toString())) {
                type = Type.STEPWISE;
            }

            if (xo.hasAttribute(TYPE)) {
                try {
                    type = Type.valueOf(xo.getStringAttribute(xo.getStringAttribute(TYPE).toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    throw new XMLParseException("Unknown Bayesian Skyline type: " + xo.getStringAttribute(TYPE));
                }
            }

            if (param2.getDimension() > (treeModel.getExternalNodeCount()-1)) {
                throw new XMLParseException("There are more groups (" + param2.getDimension()
                        + ") than coalescent nodes in the tree (" + (treeModel.getExternalNodeCount()-1) + ").");
            }

            Logger.getLogger("beast.evomodel").info("Bayesian skyline plot: " + param.getDimension() + " " + type.toString() + " control points");

            return new BayesianSkylineLikelihood(treeModel, param, param2, type);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents the likelihood of the tree given the population size vector.";
        }

        public Class getReturnType() {
            return BayesianSkylineLikelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules;{
            rules = new XMLSyntaxRule[]{
                    new XORRule(
                            AttributeRule.newBooleanRule(Type.LINEAR.toString()),
                            AttributeRule.newStringRule(TYPE)
                    ),
                    new ElementRule(POPULATION_SIZES, new XMLSyntaxRule[]{
                            new ElementRule(Parameter.class)
                    }),
                    new ElementRule(GROUP_SIZES, new XMLSyntaxRule[]{
                            new ElementRule(Parameter.class)
                    }),
                    new ElementRule(CoalescentLikelihood.POPULATION_TREE, new XMLSyntaxRule[]{
                            new ElementRule(TreeModel.class)
                    }),
            };
        }
    };

}
