/*
 * TreeHeightStatistic.java
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

package beast.evomodel.tree;

import beast.evolution.tree.Tree;
import beast.inference.model.Statistic;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * A statistic that reports the height of a tree
 *
 * @author Alexei Drummond
 * @version $Id: RateStatistic.java,v 1.9 2005/07/11 14:06:25 rambaut Exp $
 */
public class TreeHeightStatistic extends Statistic.Abstract implements TreeStatistic {

    public TreeHeightStatistic(String name, Tree tree) {
        super(name);
        this.tree = tree;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    public Tree getTree() {
        return tree;
    }

    public int getDimension() {
        return 1;
    }

    /**
     * @return the height of the tree
     */
    public double getStatisticValue(int dim) {

        return tree.getNodeHeight(tree.getRoot());
    }

    private Tree tree = null;

    public static final XMLObjectParser<TreeHeightStatistic> PARSER = new AbstractXMLObjectParser<TreeHeightStatistic>() {

        public static final String TREE_HEIGHT_STATISTIC = "treeHeightStatistic";

        public String getParserName() {
            return TREE_HEIGHT_STATISTIC;
        }

        public TreeHeightStatistic parseXMLObject(XMLObject xo) throws XMLParseException {

            final String name = xo.getAttribute(Statistic.NAME, xo.getId());
            final Tree tree = (Tree) xo.getChild(Tree.class);

            return new TreeHeightStatistic(name, tree);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A statistic that returns the height of the tree";
        }

        public Class getReturnType() {
            return TreeHeightStatistic.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newStringRule(Statistic.NAME, true),
                new ElementRule(TreeModel.class),
        };
    };

}
