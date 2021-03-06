/*
 * CoalescentLikelihood.java
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

import beast.evolution.coalescent.Coalescent;
import beast.evolution.coalescent.DemographicFunction;
import beast.evolution.tree.Tree;
import beast.evolution.util.Taxa;
import beast.evolution.util.TaxonList;
import beast.evolution.util.Units;
import beast.evomodel.tree.TreeModel;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * A likelihood function for the coalescent. Takes a tree and a demographic model.
 *
 * Parts of this class were derived from C++ code provided by Oliver Pybus.
 *
 * @version $Id: NewCoalescentLikelihood.java,v 1.6 2005/05/24 20:25:57 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public final class CoalescentLikelihood extends AbstractCoalescentLikelihood implements Units {

	public static final String COALESCENT_LIKELIHOOD = "coalescentLikelihood";
	public static final String POPULATION_TREE = "populationTree";

	// PUBLIC STUFF
	public CoalescentLikelihood(Tree tree,
	                            TaxonList includeSubtree,
	                            List<TaxonList> excludeSubtrees,
	                            DemographicModel demoModel) throws Tree.MissingTaxonException {

		super(COALESCENT_LIKELIHOOD, tree, includeSubtree, excludeSubtrees);

		this.demoModel = demoModel;

		addModel(demoModel);
	}

    // **************************************************************
	// Likelihood IMPLEMENTATION
	// **************************************************************

	/**
	 * Calculates the log likelihood of this set of coalescent intervals,
	 * given a demographic model.
	 */
	public double calculateLogLikelihood() {

		DemographicFunction demoFunction = demoModel.getDemographicFunction();

		//double lnL =  Coalescent.calculateLogLikelihood(getIntervals(), demoFunction);
        double lnL =  Coalescent.calculateLogLikelihood(getIntervals(), demoFunction, demoFunction.getThreshold());

		if (Double.isNaN(lnL) || Double.isInfinite(lnL)) {
			Logger.getLogger("warning").severe("CoalescentLikelihood is " + Double.toString(lnL));
		}

		return lnL;
	}

	// **************************************************************
	// Units IMPLEMENTATION
	// **************************************************************

	/**
	 * Sets the units these coalescent intervals are
	 * measured in.
	 */
	public final void setUnits(Type u)
	{
		demoModel.setUnits(u);
	}

	/**
	 * Returns the units these coalescent intervals are
	 * measured in.
	 */
	public final Type getUnits()
	{
		return demoModel.getUnits();
	}

	// ****************************************************************
	// Private and protected stuff
	// ****************************************************************

	/** The demographic model. */
	private DemographicModel demoModel = null;

	public static final XMLObjectParser<CoalescentLikelihood> PARSER = new AbstractXMLObjectParser<CoalescentLikelihood>() {

		public static final String COALESCENT_LIKELIHOOD = "coalescentLikelihood";
		public static final String MODEL = "model";
		public static final String POPULATION_FACTOR = "factor";

		public static final String INCLUDE = "include";
		public static final String EXCLUDE = "exclude";

		public String getParserName() {
			return COALESCENT_LIKELIHOOD;
		}

		public CoalescentLikelihood parseXMLObject(XMLObject xo) throws XMLParseException {

			XMLObject cxo = xo.getChild(MODEL);
			DemographicModel demoModel = (DemographicModel) cxo.getChild(DemographicModel.class);

			List<TreeModel> trees = new ArrayList<TreeModel>();
			List<Double> popFactors = new ArrayList<Double>();
			MultiLociTreeSet treesSet = demoModel instanceof MultiLociTreeSet ? (MultiLociTreeSet) demoModel : null;

			for (int k = 0; k < xo.getChildCount(); ++k) {
				final Object child = xo.getChild(k);
				if (child instanceof XMLObject) {
					cxo = (XMLObject) child;
					if (cxo.getName().equals(POPULATION_TREE)) {
						final TreeModel t = (TreeModel) cxo.getChild(TreeModel.class);
						assert t != null;
						trees.add(t);

						popFactors.add(cxo.getAttribute(POPULATION_FACTOR, 1.0));
					}
				}
//                in the future we may have arbitrary multi-loci element
//                else if( child instanceof MultiLociTreeSet )  {
//                    treesSet = (MultiLociTreeSet)child;
//                }
			}

			TreeModel treeModel = null;
			if (trees.size() == 1 && popFactors.get(0) == 1.0) {
				treeModel = trees.get(0);
			} else if (trees.size() > 1) {
				treesSet = new MultiLociTreeSet.Default(trees, popFactors);
			} else if (!(trees.size() == 0 && treesSet != null)) {
				throw new XMLParseException("Incorrectly constructed likelihood element");
			}

			TaxonList includeSubtree = null;

			if (xo.hasChildNamed(INCLUDE)) {
				includeSubtree = (TaxonList) xo.getElementFirstChild(INCLUDE);
			}

			List<TaxonList> excludeSubtrees = new ArrayList<TaxonList>();

			if (xo.hasChildNamed(EXCLUDE)) {
				cxo = xo.getChild(EXCLUDE);
				for (int i = 0; i < cxo.getChildCount(); i++) {
					excludeSubtrees.add((TaxonList) cxo.getChild(i));
				}
			}

			if (treeModel != null) {
				try {
					return new CoalescentLikelihood(treeModel, includeSubtree, excludeSubtrees, demoModel);
				} catch (Tree.MissingTaxonException mte) {
					throw new XMLParseException("treeModel missing a taxon from taxon list in " + getParserName() + " element");
				}
			} else {
				if (includeSubtree != null || excludeSubtrees.size() > 0) {
					throw new XMLParseException("Include/Exclude taxa not supported for multi locus sets");
				}
				// Use old code for multi locus sets.
				// This is a little unfortunate but the current code is using AbstractCoalescentLikelihood as
				// a base - and modifing it will probsbly result in a bigger mess.
				throw new XMLParseException("Multilocus datasets not currently supported.");
//				return new OldAbstractCoalescentLikelihood(treesSet, demoModel);
			}
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "This element represents the likelihood of the tree given the demographic function.";
		}

		public Class getReturnType() {
			return CoalescentLikelihood.class;
		}

		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		private final XMLSyntaxRule[] rules = {
				new ElementRule(MODEL, new XMLSyntaxRule[]{
						new ElementRule(DemographicModel.class)
				}, "The demographic model which describes the effective population size over time"),

				new ElementRule(POPULATION_TREE, new XMLSyntaxRule[]{
						AttributeRule.newDoubleRule(POPULATION_FACTOR, true),
						new ElementRule(TreeModel.class)
				}, "Tree(s) to compute likelihood for", 0, Integer.MAX_VALUE),

				new ElementRule(INCLUDE, new XMLSyntaxRule[]{
						new ElementRule(Taxa.class)
				}, "An optional subset of taxa on which to calculate the likelihood (should be monophyletic)", true),

				new ElementRule(EXCLUDE, new XMLSyntaxRule[]{
						new ElementRule(Taxa.class, 1, Integer.MAX_VALUE)
				}, "One or more subsets of taxa which should be excluded from calculate the likelihood (should be monophyletic)", true)
		};
	};
}