/*
 * NNI.java
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
 * Implements the Nearest Neighbor Interchange (NNI) operation. This particular
 * implementation assumes explicitely bifurcating trees. It works similar to the
 * Narrow Exchange but with manipulating the height of a node if necessary.
 *
 * @author Sebastian Hoehna
 * @version 1.0
 */
public class NNI extends AbstractTreeOperator {

    public static final String NNI = "nearestNeighborInterchange";

    private TreeModel tree = null;

    /**
     *
     */
    public NNI(TreeModel tree, double weight) {
        this.tree = tree;
        setWeight(weight);
    }

    /*
     * (non-Javadoc)
     * 
     * @see beast.inference.operators.SimpleMCMCOperator#doOperation()
     */
    @Override
    public double doOperation() throws OperatorFailedException {
        final int nNodes = tree.getNodeCount();
        final NodeRef root = tree.getRoot();

        NodeRef i;

        // get a random node where neither you or your father is the root
        do {
            i = tree.getNode(MathUtils.nextInt(nNodes));
        } while( root == i || tree.getParent(i) == root );

        // get parent node
        final NodeRef iParent = tree.getParent(i);
        // get parent of parent -> grant parent :)
        final NodeRef iGrandParent = tree.getParent(iParent);
        // get left child of grant parent -> uncle
        NodeRef iUncle = tree.getChild(iGrandParent, 0);
        // check if uncle == father
        if( iUncle == iParent ) {
            // if so take right child -> sibling of father
            iUncle = tree.getChild(iGrandParent, 1);
        }

        // change the height of my father to be randomly between my uncle's
        // heights and my grandfather's height
        // this is necessary for the hastings ratio to do also if the uncle is
        // younger anyway

        final double heightGrandfather = tree.getNodeHeight(iGrandParent);
        final double heightUncle = tree.getNodeHeight(iUncle);
        final double minHeightFather = Math.max(heightUncle, tree.getNodeHeight(getOtherChild(tree, iParent, i)));
        final double heightI = tree.getNodeHeight(i);
        final double minHeightReverse = Math.max(heightI, tree.getNodeHeight(getOtherChild(tree, iParent, i)));

        double ran;
        do {
            ran = Math.random();
        } while( ran == 0.0 || ran == 1.0 );

        // now calculate the new height for father between the height of the
        // uncle and the grandparent
        final double newHeightFather = minHeightFather + (ran * (heightGrandfather - minHeightFather));
        // set the new height for the father
        tree.setNodeHeight(iParent, newHeightFather);

        // double prForward = 1 / (heightGrandfather - minHeightFather);
        // double prBackward = 1 / (heightGrandfather - minHeightReverse);
        // hastings ratio = backward Prob / forward Prob
        final double hastingsRatio = Math.log((heightGrandfather - minHeightFather) / (heightGrandfather - minHeightReverse));
        // now change the nodes
        exchangeNodes(tree, i, iUncle, iParent, iGrandParent);

        tree.pushTreeChangedEvent(iParent);
        tree.pushTreeChangedEvent(iGrandParent);

        return hastingsRatio;
    }

    /*
     * (non-Javadoc)
     * 
     * @see beast.inference.operators.SimpleMCMCOperator#getOperatorName()
     */
    @Override
    public String getOperatorName() {
        return NNI;
    }

    public double getMinimumAcceptanceLevel() {
        return 0.025;
    }

    public double getMinimumGoodAcceptanceLevel() {
        return 0.05;
    }

    /*
     * (non-Javadoc)
     * 
     * @see beast.inference.operators.MCMCOperator#getPerformanceSuggestion()
     */
    public String getPerformanceSuggestion() {
        // TODO Auto-generated method stub
        return null;
    }

    public static final XMLObjectParser<NNI> PARSER = new AbstractXMLObjectParser<NNI>() {

        public String getParserName() {
            return NNI;
        }

        @Override
        public String[] getParserNames() {
            return new String[]{getParserName(), "NearestNeighborInterchange"};
        }

        public NNI parseXMLObject(XMLObject xo) throws XMLParseException {

            TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);

            return new NNI(treeModel, weight);
        }

        // ************************************************************************
        // AbstractXMLObjectParser
        // implementation
        // ************************************************************************

        public String getParserDescription() {
            return "This element represents a NNI operator. "
                    + "This operator swaps a random subtree with its uncle.";
        }

        public Class getReturnType() {
            return NNI.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule(MCMCOperator.WEIGHT),
                new ElementRule(TreeModel.class)
        };
    };

}
