/*
 * ExchangeOperator.java
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

import beast.evolution.tree.NodeRef;
import beast.evomodel.tree.TreeModel;
import beast.inference.operators.MCMCOperator;
import beast.inference.operators.OperatorFailedException;
import beast.math.MathUtils;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * Implements branch exchange operations. There is a NARROW and WIDE variety.
 * The narrow exchange is very similar to a rooted-tree nearest-neighbour
 * interchange but with the restriction that node height must remain consistent.
 * <p/>
 * KNOWN BUGS: WIDE operator cannot be used on trees with 4 or fewer tips!
 */
public class ExchangeOperator extends AbstractTreeOperator {

    public enum Mode {
        NARROW,
        WIDE,
        INTERMEDIATE;
    }

    private static final int MAX_TRIES = 100;

    private Mode mode = Mode.NARROW;
    private final TreeModel tree;

    private double[] distances;

    public ExchangeOperator(Mode mode, TreeModel tree, double weight) {
        this.mode = mode;
        this.tree = tree;
        setWeight(weight);
    }

    public double doOperation() throws OperatorFailedException {

        final int tipCount = tree.getExternalNodeCount();

        double hastingsRatio = 0;

        switch( mode ) {
            case NARROW:
                narrow();
                break;
            case WIDE:
                wide();
                break;
            case INTERMEDIATE:
                hastingsRatio = intermediate();
                break;
        }

        assert tree.getExternalNodeCount() == tipCount :
                "Lost some tips in " + ((mode == Mode.NARROW) ? "NARROW mode." : "WIDE mode.");

        return hastingsRatio;
    }

    /**
     * WARNING: Assumes strictly bifurcating tree.
     */
    public void narrow() throws OperatorFailedException {
        final int nNodes = tree.getNodeCount();
        final NodeRef root = tree.getRoot();

        NodeRef i = root;

        while( root == i || tree.getParent(i) == root ) {
            i = tree.getNode(MathUtils.nextInt(nNodes));
        }

        final NodeRef iParent = tree.getParent(i);
        final NodeRef iGrandParent = tree.getParent(iParent);
        NodeRef iUncle = tree.getChild(iGrandParent, 0);
        if( iUncle == iParent ) {
            iUncle = tree.getChild(iGrandParent, 1);
        }
        assert iUncle == getOtherChild(tree, iGrandParent, iParent);

        assert tree.getNodeHeight(i) <= tree.getNodeHeight(iGrandParent);

        if( tree.getNodeHeight(iUncle) < tree.getNodeHeight(iParent) ) {
            exchangeNodes(tree, i, iUncle, iParent, iGrandParent);

            // exchangeNodes generates the events
            //tree.pushTreeChangedEvent(iParent);
            //tree.pushTreeChangedEvent(iGrandParent);
        } else {
          throw new OperatorFailedException("Couldn't find valid narrow move on this tree!!");
        }
    }

    /**
     * WARNING: Assumes strictly bifurcating tree.
     */
    public void wide() throws OperatorFailedException {

        final int nodeCount = tree.getNodeCount();
        final NodeRef root = tree.getRoot();

        NodeRef i = root;

        while( root == i ) {
            i = tree.getNode(MathUtils.nextInt(nodeCount));
        }

        NodeRef j = i;
        while( j == i || j == root ) {
            j = tree.getNode(MathUtils.nextInt(nodeCount));
        }

        final NodeRef iP = tree.getParent(i);
        final NodeRef jP = tree.getParent(j);

        if( (iP != jP) && (i != jP) && (j != iP)
                && (tree.getNodeHeight(j) < tree.getNodeHeight(iP))
                && (tree.getNodeHeight(i) < tree.getNodeHeight(jP)) ) {
            exchangeNodes(tree, i, j, iP, jP);
            // System.out.println("tries = " + tries+1);
            return;
        }

        throw new OperatorFailedException("Couldn't find valid wide move on this tree!");
    }

    /**
     * @deprecated WARNING: SHOULD NOT BE USED!
     *             WARNING: Assumes strictly bifurcating tree.
     */
    public double intermediate() throws OperatorFailedException {

        final int nodeCount = tree.getNodeCount();
        final NodeRef root = tree.getRoot();

        for(int tries = 0; tries < MAX_TRIES; ++tries) {
            NodeRef i, j;
            NodeRef[] possibleNodes;
            do {

                // get a random node
                i = root; // tree.getNode(MathUtils.nextInt(nodeCount));
                // if (root != i) {
                // possibleNodes = tree.getNodes();
                // }

                // check if we got the root
                while( root == i ) {
                    // if so get another one till we haven't got anymore the
                    // root
                    i = tree.getNode(MathUtils.nextInt(nodeCount));
                    // if (root != i) {
                    // possibleNodes = tree.getNodes();
                    // }
                }
                possibleNodes = tree.getNodes();

                // get another random node
                // NodeRef j = tree.getNode(MathUtils.nextInt(nodeCount));
                j = getRandomNode(possibleNodes, i);
                // check if they are the same and if the new node is the root
            } while( j == null || j == i || j == root );

            double forward = getWinningChance(indexOf(possibleNodes, j));

            // possibleNodes = getPossibleNodes(j);
            calcDistances(possibleNodes, j);
            forward += getWinningChance(indexOf(possibleNodes, i));

            // get the parent of both of them
            final NodeRef iP = tree.getParent(i);
            final NodeRef jP = tree.getParent(j);

            // check if both parents are equal -> we are siblings :) (this
            // wouldnt effect a change on topology)
            // check if I m your parent or vice versa (this would destroy the
            // tree)
            // check if you are younger then my father
            // check if I m younger then your father
            if( (iP != jP) && (i != jP) && (j != iP)
                    && (tree.getNodeHeight(j) < tree.getNodeHeight(iP))
                    && (tree.getNodeHeight(i) < tree.getNodeHeight(jP)) ) {
                // if 1 & 2 are false and 3 & 4 are true then we found a valid
                // candidate
                exchangeNodes(tree, i, j, iP, jP);

                // possibleNodes = getPossibleNodes(i);
                calcDistances(possibleNodes, i);
                double backward = getWinningChance(indexOf(possibleNodes, j));

                // possibleNodes = getPossibleNodes(j);
                calcDistances(possibleNodes, j);
                backward += getWinningChance(indexOf(possibleNodes, i));

                // System.out.println("tries = " + tries+1);
                return Math.log(Math.min(1, (backward) / (forward)));
                // return 0.0;
            }
        }

        throw new OperatorFailedException("Couldn't find valid wide move on this tree!");
    }

    /* why not use Arrays.asList(a).indexOf(n) ? */
    private int indexOf(NodeRef[] a, NodeRef n) {

        for(int i = 0; i < a.length; i++) {
            if( a[i] == n ) {
                return i;
            }
        }
        return -1;
    }

    private double getWinningChance(int index) {

        double sum = 0;
        for( double distance : distances ) {
            sum += (1.0 / distance);
        }

        return (1.0 / distances[index]) / sum;

    }

    private void calcDistances(NodeRef[] nodes, NodeRef ref) {
        distances = new double[nodes.length];
        for(int i = 0; i < nodes.length; i++) {
            distances[i] = getNodeDistance(ref, nodes[i]) + 1;
        }
    }

    private NodeRef getRandomNode(NodeRef[] nodes, NodeRef ref) {

        calcDistances(nodes, ref);
        double sum = 0;
        for( double distance : distances ) {
            sum += 1.0 / distance;
        }

        double randomValue = MathUtils.nextDouble() * sum;
        NodeRef n = null;
        for(int i = 0; i < distances.length; i++) {
            randomValue -= 1.0 / distances[i];

            if( randomValue <= 0 ) {
                n = nodes[i];
                break;
            }
        }
        return n;
    }

    private int getNodeDistance(NodeRef i, NodeRef j) {
        int count = 0;

        while( i != j ) {
            count++;
            if( tree.getNodeHeight(i) < tree.getNodeHeight(j) ) {
                i = tree.getParent(i);
            } else {
                j = tree.getParent(j);
            }
        }
        return count;
    }

    public Mode getMode() {
        return mode;
    }

    public String getOperatorName() {
        return ((mode == Mode.NARROW) ? "Narrow" : "Wide") + " Exchange" + "(" + tree.getId() + ")";
    }

    public double getMinimumAcceptanceLevel() {
        if( mode == Mode.NARROW ) {
            return 0.05;
        } else {
            return 0.01;
        }
    }

    public double getMinimumGoodAcceptanceLevel() {
        if( mode == Mode.NARROW ) {
            return 0.05;
        } else {
            return 0.01;
        }
    }

    public String getPerformanceSuggestion() {
        return "";

//        if( MCMCOperator.Utils.getAcceptanceProbability(this) < getMinimumAcceptanceLevel() ) {
//            return "";
//        } else if( MCMCOperator.Utils.getAcceptanceProbability(this) > getMaximumAcceptanceLevel() ) {
//            return "";
//        } else {
//            return "";
//        }
    }

//    public static final String INTERMEDIATE_EXCHANGE = "intermediateExchange";
/* The INTERMEDIATE_EXCHANGE is deprecated for unknown reason, therefore comment out its parser to make other 2 parsers registered properly
    public static XMLObjectParser INTERMEDIATE_EXCHANGE_PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return INTERMEDIATE_EXCHANGE;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            final TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            final double weight = xo.getDoubleAttribute("weight");

            return new ExchangeOperator(INTERMEDIATE, treeModel, weight);
        }

        // ************************************************************************
        // AbstractXMLObjectParser implementation
        // ************************************************************************

        public String getParserDescription() {
            return "This element represents a intermediate exchange operator. "
                    + "This operator swaps two random subtrees.";
        }

        public Class getReturnType() {
            return ExchangeOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule("weight"),
                new ElementRule(TreeModel.class)};
    }; */

    public static final XMLObjectParser NARROW_EXCHANGE_OPERATOR_PARSER = new AbstractXMLObjectParser() {

        public static final String NARROW_EXCHANGE = "narrowExchange";

        public String getParserName() {
            return NARROW_EXCHANGE;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            final TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            final double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);
            return new ExchangeOperator(Mode.NARROW, treeModel, weight);
        }

        // ************************************************************************
        // AbstractXMLObjectParser implementation
        // ************************************************************************

        public String getParserDescription() {
            return "This element represents a narrow exchange operator. "
                    + "This operator swaps a random subtree with its uncle.";
        }

        public Class getReturnType() {
            return ExchangeOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                new ElementRule(TreeModel.class)
        };
    };

    public static final XMLObjectParser INTERMEDIATE_EXCHANGE_OPERATOR_PARSER = new AbstractXMLObjectParser() {

        public static final String INTERMEDIATE_EXCHANGE = "intermediateExchange";

        public String getParserName() {
            return INTERMEDIATE_EXCHANGE;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            final TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            final double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);
            return new ExchangeOperator(Mode.INTERMEDIATE, treeModel, weight);
        }

        // ************************************************************************
        // AbstractXMLObjectParser implementation
        // ************************************************************************

        public String getParserDescription() {
            return "This element represents an intermediate exchange operator. "
                    + "This operator swaps two subtree random subtrees with a bias towards nearby subtrees.";
        }

        public Class getReturnType() {
            return ExchangeOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                new ElementRule(TreeModel.class)
        };
    };


    public static final XMLObjectParser WIDE_EXCHANGE_OPERATOR_PARSER = new AbstractXMLObjectParser() {

        public static final String WIDE_EXCHANGE = "wideExchange";

        public String getParserName() {
            return WIDE_EXCHANGE;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            final TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            if (treeModel.getExternalNodeCount() <= 2) {
                throw new XMLParseException("Tree with fewer than 3 taxa");
            }
            final double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);

            return new ExchangeOperator(Mode.WIDE, treeModel, weight);
        }

        // ************************************************************************
        // AbstractXMLObjectParser implementation
        // ************************************************************************

        public String getParserDescription() {
            return "This element represents a wide exchange operator. "
                    + "This operator swaps two random subtrees.";
        }

        public Class getReturnType() {
            return ExchangeOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules;{
            rules = new XMLSyntaxRule[]{
                    AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                    new ElementRule(TreeModel.class)
            };
        }
    };

}
