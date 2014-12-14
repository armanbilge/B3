/*
 * FNPR.java
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
 * This is an implementation of the Subtree Prune and Regraft (SPR) operator for
 * trees. It assumes explicitely bifurcating rooted trees.
 *
 * @author Sebastian Hoehna
 * @version 1.0
 */
public class FNPR extends AbstractTreeOperator {

    public static final String FNPR = "fixedNodeheightSubtreePruneRegraft";

    private TreeModel tree = null;
    /**
     *
     */
    public FNPR(TreeModel tree, double weight) {
        this.tree = tree;
        setWeight(weight);
        // distances = new int[tree.getNodeCount()];
    }

    /*
    * (non-Javadoc)
    *
    * @see beast.inference.operators.SimpleMCMCOperator#doOperation()
    */
    @Override
    public double doOperation() throws OperatorFailedException {
        NodeRef iGrandfather, iBrother;
        double heightFather;
        final int tipCount = tree.getExternalNodeCount();

        final int nNodes = tree.getNodeCount();
        final NodeRef root = tree.getRoot();

        NodeRef i;

        int MAX_TRIES = 1000;

        for (int tries = 0; tries < MAX_TRIES; ++tries) {
           // get a random node whose father is not the root - otherwise
           // the operation is not possible
           do {
              i = tree.getNode(MathUtils.nextInt(nNodes));
           } while (root == i || tree.getParent(i) == root);

           // int childIndex = (MathUtils.nextDouble() >= 0.5 ? 1 : 0);
           // int otherChildIndex = 1 - childIndex;
           // NodeRef iOtherChild = tree.getChild(i, otherChildIndex);

           NodeRef iFather = tree.getParent(i);
           iGrandfather = tree.getParent(iFather);
           iBrother = getOtherChild(tree, iFather, i);
           heightFather = tree.getNodeHeight(iFather);

           // NodeRef newChild = getRandomNode(possibleChilds, iFather);
           NodeRef newChild = tree.getNode(MathUtils.nextInt(nNodes));

           if (tree.getNodeHeight(newChild) < heightFather
                 && root != newChild
                 && tree.getNodeHeight(tree.getParent(newChild)) > heightFather
                 && newChild != iFather
                 && tree.getParent(newChild) != iFather) {
              NodeRef newGrandfather = tree.getParent(newChild);

              tree.beginTreeEdit();

              // prune
              tree.removeChild(iFather, iBrother);
              tree.removeChild(iGrandfather, iFather);
              tree.addChild(iGrandfather, iBrother);

              // reattach
              tree.removeChild(newGrandfather, newChild);
              tree.addChild(iFather, newChild);
              tree.addChild(newGrandfather, iFather);

              // ****************************************************

              tree.endTreeEdit();

              tree.pushTreeChangedEvent(i);

              assert tree.getExternalNodeCount() == tipCount;

              return 0.0;
           }
        }

        throw new OperatorFailedException("Couldn't find valid SPR move on this tree!");
     }

    /*
    * (non-Javadoc)
    *
    * @see beast.inference.operators.SimpleMCMCOperator#getOperatorName()
    */
    @Override
    public String getOperatorName() {
        return FNPR;
    }

    public double getTargetAcceptanceProbability() {

        return 0.0234;
    }

    public double getMaximumAcceptanceLevel() {

        return 0.04;
    }

    public double getMaximumGoodAcceptanceLevel() {

        return 0.03;
    }

    public double getMinimumAcceptanceLevel() {

        return 0.005;
    }

    public double getMinimumGoodAcceptanceLevel() {

        return 0.01;
    }

    /*
    * (non-Javadoc)
    *
    * @see beast.inference.operators.MCMCOperator#getPerformanceSuggestion()
    */
    public String getPerformanceSuggestion() {
        return "";
    }

    public static final XMLObjectParser<FNPR> PARSER = new AbstractXMLObjectParser<FNPR>() {

        public String getParserName() {
            return FNPR;
        }

        public String[] getParserNames() {
            return new String[]{getParserName(), "FixedNodeheightSubtreePruneRegraft"};
        }

        public FNPR parseXMLObject(XMLObject xo) throws XMLParseException {

            TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            double weight = xo.getDoubleAttribute(MCMCOperator.WEIGHT);

            return new FNPR(treeModel, weight);
        }

        // ************************************************************************
        // AbstractXMLObjectParser implementation
        // ************************************************************************

        public String getParserDescription() {
            return "This element represents a FNPR operator. "
                    + "This operator swaps a random subtree with its uncle.";
        }

        public Class getReturnType() {
            return FNPR.class;
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
