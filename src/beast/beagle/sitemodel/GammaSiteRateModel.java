/*
 * GammaSiteRateModel.java
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

package beast.beagle.sitemodel;

import beast.beagle.substmodel.SubstitutionModel;
import beast.evomodel.sitemodel.SiteModel;
import beast.evomodel.sitemodel.SiteRateModel;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.math.distributions.GammaDistribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.logging.Logger;

/**
 * GammaSiteModel - A SiteModel that has a gamma distributed rates across sites.
 *
 * @author Andrew Rambaut
 * @version $Id: GammaSiteModel.java,v 1.31 2005/09/26 14:27:38 rambaut Exp $
 */

public class GammaSiteRateModel extends SiteRateModel {

    public GammaSiteRateModel(String name) {
        this(   name,
                null,
                null,
                0,
                null);
    }

    public GammaSiteRateModel(String name, double alpha, int categoryCount) {
        this(   name,
                null,
                new Parameter.Default(alpha),
                categoryCount,
                null);
    }

    public GammaSiteRateModel(String name, double pInvar) {
        this(   name,
                null,
                null,
                0,
                new Parameter.Default(pInvar));
    }

    public GammaSiteRateModel(String name, double alpha, int categoryCount, double pInvar) {
        this(   name,
                null,
                new Parameter.Default(alpha),
                categoryCount,
                new Parameter.Default(pInvar));
    }

    /**
     * Constructor for gamma+invar distributed sites. Either shapeParameter or
     * invarParameter (or both) can be null to turn off that feature.
     */
    public GammaSiteRateModel(
            String name,
            Parameter muParameter,
            Parameter shapeParameter, int gammaCategoryCount,
            Parameter invarParameter) {

        super(name);

        this.muParameter = muParameter;
        if (muParameter != null) {
            addVariable(muParameter);
            muParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        }

        this.shapeParameter = shapeParameter;
        if (shapeParameter != null) {
            this.categoryCount = gammaCategoryCount;

            addVariable(shapeParameter);
            shapeParameter.addBounds(new Parameter.DefaultBounds(1.0E3, 1.0E-3, 1));
        } else {
            this.categoryCount = 1;
        }

        this.invarParameter = invarParameter;
        if (invarParameter != null) {
            this.categoryCount += 1;

            addVariable(invarParameter);
            invarParameter.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));
        }

        categoryRates = new double[this.categoryCount];
        categoryProportions = new double[this.categoryCount];

        ratesKnown = false;
    }

    /**
     * set mu
     */
    public void setMu(double mu) {
        muParameter.setParameterValue(0, mu);
    }

    /**
     * @return mu
     */
    public final double getMu() {
        return muParameter.getParameterValue(0);
    }

    /**
     * set alpha
     */
    public void setAlpha(double alpha) {
        shapeParameter.setParameterValue(0, alpha);
        ratesKnown = false;
    }

    /**
     * @return alpha
     */
    public final double getAlpha() {
        return shapeParameter.getParameterValue(0);
    }

    public Parameter getMutationRateParameter() {
        return muParameter;
    }

    public Parameter getAlphaParameter() {
        return shapeParameter;
    }

    public Parameter getPInvParameter() {
        return invarParameter;
    }

    public void setMutationRateParameter(Parameter parameter) {
        if (muParameter != null) removeVariable(muParameter);
        muParameter = parameter;
        if (muParameter != null) addVariable(muParameter);
    }

    public void setAlphaParameter(Parameter parameter) {
        if (shapeParameter != null) removeVariable(shapeParameter);
        shapeParameter = parameter;
        if (shapeParameter != null) addVariable(shapeParameter);
    }

    public void setPInvParameter(Parameter parameter) {
        if (invarParameter != null) removeVariable(invarParameter);
        invarParameter = parameter;
        if (invarParameter != null) addVariable(invarParameter);
    }

    // *****************************************************************
    // Interface SiteRateModel
    // *****************************************************************

    public int getCategoryCount() {
        return categoryCount;
    }

    public double[] getCategoryRates() {
        synchronized (this) {
            if (!ratesKnown) {
                calculateCategoryRates();
            }
        }

        return categoryRates;
    }

    public double[] getCategoryProportions() {
        synchronized (this) {
            if (!ratesKnown) {
                calculateCategoryRates();
            }
        }

        return categoryProportions;
    }

    public double getRateForCategory(int category) {
        synchronized (this) {
            if (!ratesKnown) {
                calculateCategoryRates();
            }
        }

        return categoryRates[category];
    }

    public double getProportionForCategory(int category) {
        synchronized (this) {
            if (!ratesKnown) {
                calculateCategoryRates();
            }
        }

        return categoryProportions[category];
    }

    /**
     * discretization of gamma distribution with equal proportions in each
     * category
     */
    private void calculateCategoryRates() {

        double propVariable = 1.0;
        int cat = 0;

        if (invarParameter != null) {
            categoryRates[0] = 0.0;
            categoryProportions[0] = invarParameter.getParameterValue(0);

            propVariable = 1.0 - categoryProportions[0];
            cat = 1;
        }

        if (shapeParameter != null) {

            final double a = shapeParameter.getParameterValue(0);
            double mean = 0.0;
            final int gammaCatCount = categoryCount - cat;

            for (int i = 0; i < gammaCatCount; i++) {

                categoryRates[i + cat] = GammaDistribution.quantile((2.0 * i + 1.0) / (2.0 * gammaCatCount), a, 1.0 / a);
                mean += categoryRates[i + cat];

                categoryProportions[i + cat] = propVariable / gammaCatCount;
            }

            mean = (propVariable * mean) / gammaCatCount;

            for (int i = 0; i < gammaCatCount; i++) {

                categoryRates[i + cat] /= mean;
            }
        } else {
            categoryRates[cat] = 1.0 / propVariable;
            categoryProportions[cat] = propVariable;
        }

        if (muParameter != null) { // Moved multiplication by mu to here; it also
                                   // needed by double[] getCategoryRates() -- previously ignored
            double mu = muParameter.getParameterValue(0);
             for (int i=0; i < categoryCount; i++)
                categoryRates[i] *= mu;
        }

        ratesKnown = true;
    }

    // *****************************************************************
    // Interface ModelComponent
    // *****************************************************************

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        // Substitution model has changed so fire model changed event
        listenerHelper.fireModelChanged(this, object, index);
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        if (variable == shapeParameter) {
            ratesKnown = false;
        } else if (variable == invarParameter) {
            ratesKnown = false;
        } else if (variable == muParameter) {
            ratesKnown = false; // MAS: I changed this because the rate parameter can affect the categories if the parameter is in siteModel and not clockModel
        } else {
        	throw new RuntimeException("Unknown variable in GammaSiteRateModel.handleVariableChangedEvent");
        }
        listenerHelper.fireModelChanged(this, variable, index);
    }

    protected void storeState() {
    } // no additional state needs storing

    protected void restoreState() {
        ratesKnown = false;
    }

    protected void acceptState() {
    } // no additional state needs accepting

    /**
     * mutation rate parameter
     */
    private Parameter muParameter;

    /**
     * shape parameter
     */
    private Parameter shapeParameter;

    /**
     * invariant sites parameter
     */
    private Parameter invarParameter;

    private boolean ratesKnown;

    private int categoryCount;

    private double[] categoryRates;

    private double[] categoryProportions;



    // This is here solely to allow the GammaSiteModelParser to pass on the substitution model to the
    // HomogenousBranchSubstitutionModel so that the XML will be compatible with older BEAST versions. To be removed
    // at some point.
    public SubstitutionModel getSubstitutionModel() {
        return substitutionModel;
    }

    public void setSubstitutionModel(SubstitutionModel substitutionModel) {
        this.substitutionModel = substitutionModel;
    }

    private SubstitutionModel substitutionModel;

    public static final XMLObjectParser<GammaSiteRateModel> PARSER = new AbstractXMLObjectParser<GammaSiteRateModel>() {

        public final String SITE_MODEL = SiteModel.SITE_MODEL;
        public static final String SUBSTITUTION_MODEL = "substitutionModel";
        public static final String BRANCH_SUBSTITUTION_MODEL = "branchSubstitutionModel";
        public static final String MUTATION_RATE = "mutationRate";
        public static final String SUBSTITUTION_RATE = "substitutionRate";
        public static final String RELATIVE_RATE = "relativeRate";
        public static final String GAMMA_SHAPE = "gammaShape";
        public static final String GAMMA_CATEGORIES = "gammaCategories";
        public static final String PROPORTION_INVARIANT = "proportionInvariant";

        public String getParserName() {
            return SITE_MODEL;
        }

        public GammaSiteRateModel parseXMLObject(XMLObject xo) throws XMLParseException {

            String msg = "";
            SubstitutionModel substitutionModel = null;

            Parameter muParam = null;
            if (xo.hasChildNamed(SUBSTITUTION_RATE)) {
                muParam = (Parameter) xo.getElementFirstChild(SUBSTITUTION_RATE);

                msg += "\n  with initial substitution rate = " + muParam.getParameterValue(0);
            } else  if (xo.hasChildNamed(MUTATION_RATE)) {
                muParam = (Parameter) xo.getElementFirstChild(MUTATION_RATE);

                msg += "\n  with initial substitution rate = " + muParam.getParameterValue(0);
            } else if (xo.hasChildNamed(RELATIVE_RATE)) {
                muParam = (Parameter) xo.getElementFirstChild(RELATIVE_RATE);

                msg += "\n  with initial relative rate = " + muParam.getParameterValue(0);
            }

            Parameter shapeParam = null;
            int catCount = 4;
            if (xo.hasChildNamed(GAMMA_SHAPE)) {
                XMLObject cxo = xo.getChild(GAMMA_SHAPE);
                catCount = cxo.getIntegerAttribute(GAMMA_CATEGORIES);
                shapeParam = (Parameter) cxo.getChild(Parameter.class);

                msg += "\n  " + catCount + " category discrete gamma with initial shape = " + shapeParam.getParameterValue(0);
            }

            Parameter invarParam = null;
            if (xo.hasChildNamed(PROPORTION_INVARIANT)) {
                invarParam = (Parameter) xo.getElementFirstChild(PROPORTION_INVARIANT);
                msg += "\n  initial proportion of invariant sites = " + invarParam.getParameterValue(0);
            }

            if (msg.length() > 0) {
                Logger.getLogger("dr.evomodel").info("Creating site model: " + msg);
            } else {
                Logger.getLogger("dr.evomodel").info("Creating site model.");
            }

            GammaSiteRateModel siteRateModel = new GammaSiteRateModel(SITE_MODEL, muParam, shapeParam, catCount, invarParam);

            if (xo.hasChildNamed(SUBSTITUTION_MODEL)) {

//        	System.err.println("Doing the substitution model stuff");

                // set this to pass it along to the OldTreeLikelihoodParser...
                substitutionModel = (SubstitutionModel) xo.getElementFirstChild(SUBSTITUTION_MODEL);
                siteRateModel.setSubstitutionModel(substitutionModel);

            }

            return siteRateModel;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A SiteModel that has a gamma distributed rates across sites";
        }

        public Class<GammaSiteRateModel> getReturnType() {
            return GammaSiteRateModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {

                new XORRule(
                        new ElementRule(SUBSTITUTION_MODEL, new XMLSyntaxRule[]{
                                new ElementRule(SubstitutionModel.class)
                        }),
                        new ElementRule(BRANCH_SUBSTITUTION_MODEL, new XMLSyntaxRule[]{
                                new ElementRule(BranchSubstitutionModel.class)
                        }), true
                ),

                new XORRule(
                        new XORRule(
                                new ElementRule(SUBSTITUTION_RATE, new XMLSyntaxRule[]{
                                        new ElementRule(Parameter.class)
                                }),
                                new ElementRule(MUTATION_RATE, new XMLSyntaxRule[]{
                                        new ElementRule(Parameter.class)
                                })
                        ),
                        new ElementRule(RELATIVE_RATE, new XMLSyntaxRule[]{
                                new ElementRule(Parameter.class)
                        }), true
                ),

                new ElementRule(GAMMA_SHAPE, new XMLSyntaxRule[]{
                        AttributeRule.newIntegerRule(GAMMA_CATEGORIES, true),
                        new ElementRule(Parameter.class)
                }, true),

                new ElementRule(PROPORTION_INVARIANT, new XMLSyntaxRule[]{
                        new ElementRule(Parameter.class)
                }, true)

        };

    };
}