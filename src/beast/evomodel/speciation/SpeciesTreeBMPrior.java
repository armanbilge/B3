/*
 * SpeciesTreeBMPrior.java
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

import beast.evolution.tree.NodeRef;
import beast.inference.distribution.ParametricDistributionModel;
import beast.inference.model.CompoundModel;
import beast.inference.model.Likelihood;
import beast.inference.model.Parameter;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * @author Joseph Heled
 *         Date: 8/12/2008
 */
public class SpeciesTreeBMPrior extends Likelihood {

    private final SpeciesTreeModel sTree;
    //private final ParametricDistributionModel dist;
    private final ParametricDistributionModel tips;
    private final Parameter popSigma;
    private final Parameter stSigma;

    private static final double d1 = (1 - Math.exp(-1));
    private static final double f2 = -0.5 * Math.log(2*Math.PI);
    private final boolean logRoot;

//    public SpeciesTreeSimplePrior(SpeciesTreeModel sTree, ParametricDistributionModel dist, ParametricDistributionModel tipsPrior) {
//        super(new CompoundModel("STprior"));
//        this.sTree = sTree;
//        this.dist = dist;
//        this.tips = tipsPrior;
//
//        final CompoundModel cm = (CompoundModel)this.getModel();
//        cm.addModel(tipsPrior);
//        cm.addModel(dist);
//        cm.addModel(sTree);
//    }

    public SpeciesTreeBMPrior(SpeciesTreeModel sTree, Parameter popSigma, Parameter stSigma,
                              ParametricDistributionModel tipsPrior, boolean logRoot) {
        super(new CompoundModel("STBMprior"));

        this.sTree = sTree;
        this.popSigma = popSigma;
        this.stSigma = stSigma;
        this.tips = tipsPrior;
        this.logRoot = logRoot;

        final CompoundModel cm = (CompoundModel)this.getModel();

        cm.addModel(tipsPrior);
        cm.addModel(sTree);
    }

    protected double calculateLogLikelihood() {
        double logLike = 0;

        //if( true ) {
        final NodeRef root = sTree.getRoot();
        final double stRootHeight = sTree.getNodeHeight(root);

        final SpeciesTreeModel.RawPopulationHelper helper = sTree.getPopulationHelper();
        final double lim = stRootHeight ;//helper.geneTreesRootHeight() ;

        {
            final double sigma = stSigma.getParameterValue(0);
            final double s2 = 2 * sigma * sigma;
            int count = 0;
            double[] pops = new double[2];

            for(int nn = 0; nn < sTree.getNodeCount(); ++nn) {
                final NodeRef n = sTree.getNode(nn);

                if( sTree.isExternal(n) ) {
                    logLike += tips.logPdf(helper.tipPopulation(n));
                } else {
                    for(int nc = 0; nc < 2; ++nc) {
                        final NodeRef child = sTree.getChild(n, nc);
                        helper.getPopulations(n, nc, pops);
                        final double dt = sTree.getBranchLength(child) / lim;
                        final double pDiff = (pops[1] - pops[0]) / lim;
                        logLike -= pDiff * pDiff / (s2 * dt);
                        //need to adjest for dt!
                        logLike -= .5 * Math.log(dt);
                        count += 1;
                    }
                }
            }

            if( ! helper.perSpeciesPopulation() ) {
                helper.getRootPopulations(pops);
                final double dt = helper.geneTreesRootHeight()/lim - 1;
                //final double dt = (lim - stRootHeight) / lim;
                //  log(p1/lim) - log(p0/lim)  = log(p1/p0)
                final double pDiff = logRoot ? Math.log(pops[1]/pops[0]) : (pops[1] - pops[0])/lim;

                logLike -= pDiff * pDiff / (s2 * dt);
                //need to adjust for dt!
                logLike -= .5 * Math.log(dt);
                count += 1;
            }

            logLike += count * (f2 - Math.log(sigma));
        }

        if( helper.perSpeciesPopulation() ) {
            final double sigma = (popSigma != null ? popSigma : stSigma).getParameterValue(0);
            final double s2 = 2 * sigma * sigma;

            for(int ns = 0; ns < helper.nSpecies(); ++ns) {
                double[] times = helper.getTimes(ns);
                double[] pops = helper.getPops(ns);
                final double tMax = times[times.length-1];

                double ll = 0.0;
                double x = pops[0]/tMax;
                double t0 = 0.0;
                for(int k = 1; k < times.length; ++k) {
                    final double y = pops[k]/tMax;
                    final double dt = (times[k-1] - t0) / tMax;
                    ll -= (y - x)*(y-x) / (s2*dt);
                    // need to adjest for dt!
                    ll -= .5 * Math.log(dt);
                    x = y;
                    t0 = times[k-1];
                }
                ll += (times.length-1) * (f2 - Math.log(sigma));

                logLike += ll;
            }
        }
        //}
        return logLike;
    }

    @Override
    protected void cacheCalculations() {
        // Nothing to do
    }

    @Override
    protected void uncacheCalculations() {
        // Nothing to do
    }

    protected boolean getLikelihoodKnown() {
		return false;
	}

    public static final XMLObjectParser<SpeciesTreeBMPrior> PARSER = new AbstractXMLObjectParser<SpeciesTreeBMPrior>() {
        public static final String TIPS = "tipsDistribution";

        public static final String STPRIOR = "STPopulationPrior";

        public static final String LOG_ROOT = "log_root";
        public static final String STSIGMA = "STsigma";
        public static final String SIGMA = "sigma";

        public String getParserDescription() {
            return "";
        }

        public Class getReturnType() {
            return SpeciesTreeBMPrior.class;
        }

        public String getParserName() {
            return STPRIOR;
        }

        public SpeciesTreeBMPrior parseXMLObject(XMLObject xo) throws XMLParseException {
            final SpeciesTreeModel st = (SpeciesTreeModel) xo.getChild(SpeciesTreeModel.class);

            //ParametricDistributionModel pr = (ParametricDistributionModel) xo.getChild(ParametricDistributionModel.class);
            final Object child = xo.getChild(SIGMA);
            Parameter popSigma = child != null ? (Parameter)((XMLObject) child).getChild(Parameter.class) : null;
            Parameter stSigma = (Parameter)((XMLObject)xo.getChild(STSIGMA)).getChild(Parameter.class);

            final XMLObject cxo = (XMLObject) xo.getChild(TIPS);
            final ParametricDistributionModel tipsPrior =
                    (ParametricDistributionModel) cxo.getChild(ParametricDistributionModel.class);
            final boolean logRoot = xo.getAttribute(LOG_ROOT, false);
            return new SpeciesTreeBMPrior(st, popSigma, stSigma, tipsPrior, logRoot);
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return new XMLSyntaxRule[]{
                    AttributeRule.newBooleanRule(LOG_ROOT, true),
                    new ElementRule(SpeciesTreeModel.class),
                    new ElementRule(TIPS,
                            new XMLSyntaxRule[] { new ElementRule(ParametricDistributionModel.class) }),
                    //new ElementRule(ParametricDistributionModel.class),
                    new ElementRule(SIGMA, new XMLSyntaxRule[] { new ElementRule(Parameter.class) }, true),
                    new ElementRule(STSIGMA, new XMLSyntaxRule[] { new ElementRule(Parameter.class) })
            };
        }
    };

}