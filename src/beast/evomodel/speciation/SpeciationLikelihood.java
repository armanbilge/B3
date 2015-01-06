/*
 * SpeciationLikelihood.java
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

package beast.evomodel.speciation;

import beast.evolution.tree.Tree;
import beast.evolution.util.Taxa;
import beast.evolution.util.Taxon;
import beast.evolution.util.TaxonList;
import beast.evolution.util.Units;
import beast.inference.distribution.DistributionLikelihood;
import beast.inference.model.AbstractModelLikelihood;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Statistic;
import beast.inference.model.Variable;
import beast.math.distributions.Distribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A likelihood function for speciation processes. Takes a tree and a speciation model.
 * <p/>
 * Parts of this class were derived from C++ code provided by Oliver Pybus.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: SpeciationLikelihood.java,v 1.10 2005/05/18 09:51:11 rambaut Exp $
 */
public class SpeciationLikelihood extends AbstractModelLikelihood implements Units {

    // PUBLIC STUFF

    public static final String SPECIATION_LIKELIHOOD = "speciationLikelihood";

    /**
     * @param tree            the tree
     * @param speciationModel the model of speciation
     * @param id              a unique identifier for this likelihood
     * @param exclude         taxa to exclude from this model
     */
    public SpeciationLikelihood(Tree tree, SpeciationModel speciationModel, Set<Taxon> exclude, String id) {
        this(SPECIATION_LIKELIHOOD, tree, speciationModel, exclude);
        setId(id);
    }

    public SpeciationLikelihood(Tree tree, SpeciationModel speciationModel, String id) {
        this(tree, speciationModel, null, id);
    }

    public SpeciationLikelihood(String name, Tree tree, SpeciationModel speciationModel, Set<Taxon> exclude) {

        super(name);

        this.tree = tree;
        this.speciationModel = speciationModel;
        this.exclude = exclude;

        if (tree instanceof Model) {
            addModel((Model) tree);
        }
        if (speciationModel != null) {
            addModel(speciationModel);
        }
    }

    public SpeciationLikelihood(Tree tree, SpeciationModel specModel, String id, CalibrationPoints calib) {
        this(tree, specModel, id);
        this.calibration = calib;
    }

    // **************************************************************
    // ModelListener IMPLEMENTATION
    // **************************************************************

    protected final void handleModelChangedEvent(Model model, Object object, int index) {
        likelihoodKnown = false;
    }

    // **************************************************************
    // VariableListener IMPLEMENTATION
    // **************************************************************

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
    } // No parameters to respond to

    // **************************************************************
    // Model IMPLEMENTATION
    // **************************************************************

    /**
     * Stores the precalculated state: likelihood
     */
    protected final void storeState() {
        storedLikelihoodKnown = likelihoodKnown;
        storedLogLikelihood = logLikelihood;
    }

    /**
     * Restores the precalculated state: computed likelihood
     */
    protected final void restoreState() {
        likelihoodKnown = storedLikelihoodKnown;
        logLikelihood = storedLogLikelihood;
    }

    protected final void acceptState() {
    } // nothing to do

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    public final Model getModel() {
        return this;
    }

    public final double getLogLikelihood() {
        if (!likelihoodKnown) {
            logLikelihood = calculateLogLikelihood();
            likelihoodKnown = true;
        }
        return logLikelihood;
    }

    public final double differentiate(final Variable<Double> var, final int index) {
        if (exclude != null) {
            return speciationModel.differentiateTreeLogLikelihood(tree, exclude, var, index);
        }

        if ( calibration != null ) {
            return speciationModel.differentiateTreeLogLikelihood(tree, calibration, var, index);
        }

        return speciationModel.differentiateTreeLogLikelihood(tree, var, index);
    }

    public final void makeDirty() {
        likelihoodKnown = false;
    }

    /**
     * Calculates the log likelihood of this set of coalescent intervals,
     * given a demographic model.
     *
     * @return the log likelihood
     */
    private double calculateLogLikelihood() {
        if (exclude != null) {
            return speciationModel.calculateTreeLogLikelihood(tree, exclude);
        }

        if ( calibration != null ) {
            return speciationModel.calculateTreeLogLikelihood(tree, calibration);
        }

        return speciationModel.calculateTreeLogLikelihood(tree);
    }

    // **************************************************************
    // Loggable IMPLEMENTATION
    // **************************************************************

    /**
     * @return the log columns.
     */
    public final beast.inference.loggers.LogColumn[] getColumns() {

        String columnName = getId();
        if (columnName == null) columnName = getModelName() + ".likelihood";

        return new beast.inference.loggers.LogColumn[]{
                new LikelihoodColumn(columnName)
        };
    }

    private final class LikelihoodColumn extends beast.inference.loggers.NumberColumn {
        public LikelihoodColumn(String label) {
            super(label);
        }

        public double getDoubleValue() {
            return getLogLikelihood();
        }
    }

    // **************************************************************
    // Units IMPLEMENTATION
    // **************************************************************

    /**
     * Sets the units these coalescent intervals are
     * measured in.
     */
    public final void setUnits(Type u) {
        speciationModel.setUnits(u);
    }

    /**
     * Returns the units these coalescent intervals are
     * measured in.
     */
    public final Type getUnits() {
        return speciationModel.getUnits();
    }

    @Override
    public String prettyName() {
        String s = speciationModel.getClass().getName();
        String[] parts = s.split("\\.");
        s = parts[parts.length - 1];
        if( speciationModel.getId() != null ) {
           s = s + '/' + speciationModel.getId();
        }
        s = s + '(' + tree.getId() + ')';
        return s;
    }
    // ****************************************************************
    // Private and protected stuff
    // ****************************************************************

    /**
     * The speciation model.
     */
    SpeciationModel speciationModel = null;

    /**
     * The tree.
     */
    Tree tree = null;
    private final Set<Taxon> exclude;

    private CalibrationPoints calibration;

    private double logLikelihood;
    private double storedLogLikelihood;
    private boolean likelihoodKnown = false;
    private boolean storedLikelihoodKnown = false;

    public static final XMLObjectParser<SpeciationLikelihood> PARSER = new AbstractXMLObjectParser<SpeciationLikelihood>() {
        public static final String MODEL = "model";
        public static final String TREE = "speciesTree";
        public static final String INCLUDE = "include";
        public static final String EXCLUDE = "exclude";

        public static final String CALIBRATION = "calibration";
        public static final String CORRECTION = "correction";
        public static final String POINT = "point";

        private final String EXACT = CalibrationPoints.CorrectionType.EXACT.toString();
        private final String APPROX = CalibrationPoints.CorrectionType.APPROXIMATED.toString();
        private final String PEXACT = CalibrationPoints.CorrectionType.PEXACT.toString();
        private final String NONE = CalibrationPoints.CorrectionType.NONE.toString();

        public static final String PARENT = "parent";

        public String getParserName() {
            return SPECIATION_LIKELIHOOD;
        }

        public SpeciationLikelihood parseXMLObject(XMLObject xo) throws XMLParseException {

            XMLObject cxo = xo.getChild(MODEL);
            final SpeciationModel specModel = (SpeciationModel) cxo.getChild(SpeciationModel.class);

            cxo = xo.getChild(TREE);
            final Tree tree = (Tree) cxo.getChild(Tree.class);

            Set<Taxon> excludeTaxa = null;

            if (xo.hasChildNamed(INCLUDE)) {
                excludeTaxa = new HashSet<Taxon>();
                for (int i = 0; i < tree.getTaxonCount(); i++) {
                    excludeTaxa.add(tree.getTaxon(i));
                }

                cxo = xo.getChild(INCLUDE);
                for (int i = 0; i < cxo.getChildCount(); i++) {
                    TaxonList taxonList = (TaxonList) cxo.getChild(i);
                    for (int j = 0; j < taxonList.getTaxonCount(); j++) {
                        excludeTaxa.remove(taxonList.getTaxon(j));
                    }
                }
            }

            if (xo.hasChildNamed(EXCLUDE)) {
                excludeTaxa = new HashSet<Taxon>();
                cxo = xo.getChild(EXCLUDE);
                for (int i = 0; i < cxo.getChildCount(); i++) {
                    TaxonList taxonList = (TaxonList) cxo.getChild(i);
                    for (int j = 0; j < taxonList.getTaxonCount(); j++) {
                        excludeTaxa.add(taxonList.getTaxon(j));
                    }
                }
            }
            if (excludeTaxa != null) {
                Logger.getLogger("beast.evomodel").info("Speciation model excluding " + excludeTaxa.size() + " taxa from prior - " +
                        (tree.getTaxonCount() - excludeTaxa.size()) + " taxa remaining.");
            }

            final XMLObject cal = xo.getChild(CALIBRATION);
            if( cal != null ) {
                if( excludeTaxa != null ) {
                    throw new XMLParseException("Sorry, not implemented: internal calibration prior + excluded taxa");
                }

                List<Distribution> dists = new ArrayList<Distribution>();
                List<Taxa> taxa = new ArrayList<Taxa>();
                List<Boolean> forParent = new ArrayList<Boolean>();
                Statistic userPDF = null; // (Statistic) cal.getChild(Statistic.class);

                for(int k = 0; k < cal.getChildCount(); ++k) {
                    final Object ck = cal.getChild(k);
                    if ( DistributionLikelihood.class.isInstance(ck) ) {
                        dists.add( ((DistributionLikelihood) ck).getDistribution() );
                    } else if ( Distribution.class.isInstance(ck) ) {
                        dists.add((Distribution) ck);
                    } else if ( Taxa.class.isInstance(ck) ) {
                        final Taxa tx = (Taxa) ck;
                        taxa.add(tx);
                        forParent.add( tx.getTaxonCount() == 1 );
                    } else if ( Statistic.class.isInstance(ck) ) {
                        if( userPDF != null ) {
                            throw new XMLParseException("more than one userPDF correction???");
                        }
                        userPDF = (Statistic) cal.getChild(Statistic.class);
                    }
                    else {
                        XMLObject cko = (XMLObject) ck;
                        assert cko.getChildCount() == 2;

                        for(int i = 0; i < 2; ++i) {
                            final Object chi = cko.getChild(i);
                            if ( DistributionLikelihood.class.isInstance(chi) ) {
                                dists.add( ((DistributionLikelihood) chi).getDistribution() );
                            } else if ( Distribution.class.isInstance(chi) ) {
                                dists.add((Distribution) chi);
                            } else if ( Taxa.class.isInstance(chi) ) {
                                taxa.add((Taxa) chi);
                                boolean fp = ((Taxa) chi).getTaxonCount() == 1;
                                if( cko.hasAttribute(PARENT) ) {
                                    boolean ufp = cko.getBooleanAttribute(PARENT);
                                    if( fp && ! ufp ) {
                                        throw new XMLParseException("forParent==false for a single taxon?? (must be true)");
                                    }
                                    fp = ufp;
                                }
                                forParent.add(fp);
                            } else {
                                assert false;
                            }
                        }
                    }
                }

                if( dists.size() != taxa.size() ) {
                    throw new XMLParseException("Mismatch in number of distributions and taxa specs");
                }

                try {
                    final String correction = cal.getAttribute(CORRECTION, EXACT);

                    final CalibrationPoints.CorrectionType type = correction.equals(EXACT) ? CalibrationPoints.CorrectionType.EXACT :
                            (correction.equals(APPROX) ? CalibrationPoints.CorrectionType.APPROXIMATED :
                                    (correction.equals(NONE) ? CalibrationPoints.CorrectionType.NONE :
                                            (correction.equals(PEXACT) ? CalibrationPoints.CorrectionType.PEXACT :  null)));

                    if( cal.hasAttribute(CORRECTION) && type == null ) {
                        throw new XMLParseException("correction type == " + correction + "???");
                    }

                    final CalibrationPoints calib =
                            new CalibrationPoints(tree, specModel.isYule(), dists, taxa, forParent, userPDF, type);
                    final SpeciationLikelihood speciationLikelihood = new SpeciationLikelihood(tree, specModel, null, calib);
                    return speciationLikelihood;
                } catch( IllegalArgumentException e ) {
                    throw new XMLParseException( e.getMessage() );
                }
            }

            return new SpeciationLikelihood(tree, specModel, excludeTaxa, null);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents the likelihood of the tree given the speciation.";
        }

        public Class getReturnType() {
            return SpeciationLikelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] calibrationPoint = {
                AttributeRule.newBooleanRule(PARENT, true),
                new XORRule(
                        new ElementRule(Distribution.class),
                        new ElementRule(DistributionLikelihood.class)),
                new ElementRule(Taxa.class)
        };

        private final XMLSyntaxRule[] calibration = {
//            AttributeRule.newDoubleArrayRule(COEFFS,true, "use log(lam) -lam * c[0] + sum_k=1..n (c[k+1] * e**(-k*lam*x)) " +
//                    "as a calibration correction instead of default - used when additional constarints are put on the topology."),
                AttributeRule.newStringRule(CORRECTION, true),
                new ElementRule(Statistic.class, true),
                new XORRule(
                        new ElementRule(Distribution.class, 1, 100),
                        new ElementRule(DistributionLikelihood.class, 1, 100)),
                new ElementRule(Taxa.class, 1, 100),
                new ElementRule("point", calibrationPoint, 0, 100)
        };

        private final XMLSyntaxRule[] rules = {
                new ElementRule(MODEL, new XMLSyntaxRule[]{
                        new ElementRule(SpeciationModel.class)
                }),
                new ElementRule(TREE, new XMLSyntaxRule[]{
                        new ElementRule(Tree.class)
                }),

                new ElementRule(INCLUDE, new XMLSyntaxRule[]{
                        new ElementRule(Taxa.class, 1, Integer.MAX_VALUE)
                }, "One or more subsets of taxa which should be included from calculate the likelihood (the remaining taxa are excluded)", true),

                new ElementRule(EXCLUDE, new XMLSyntaxRule[]{
                        new ElementRule(Taxa.class, 1, Integer.MAX_VALUE)
                }, "One or more subsets of taxa which should be excluded from calculate the likelihood (which is calculated on the remaining subtree)", true),

                new ElementRule(CALIBRATION, calibration, true),
        };
    };
}