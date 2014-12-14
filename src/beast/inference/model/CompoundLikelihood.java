/*
 * CompoundLikelihood.java
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

package beast.inference.model;

import beast.util.NumberFormatter;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.Reportable;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * A likelihood function which is simply the product of a set of likelihood functions.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: CompoundLikelihood.java,v 1.19 2005/05/25 09:14:36 rambaut Exp $
 */
public class CompoundLikelihood implements Likelihood, Reportable {

    public final static boolean UNROLL_COMPOUND = true;

    public final static boolean EVALUATION_TIMERS = true;
    public final long[] evaluationTimes;
    public final int[] evaluationCounts;

    public CompoundLikelihood(int threads, Collection<Likelihood> likelihoods) {

        int i = 0;
        for (Likelihood l : likelihoods) {
            addLikelihood(l, i, true);
            i++;
        }

        if (threads < 0 && this.likelihoods.size() > 1) {
            // asking for an automatic threadpool size and there is more than one likelihood to compute
            threadCount = this.likelihoods.size();
        } else if (threads > 0) {
            threadCount = threads;
        } else {
            // no thread pool requested or only one likelihood
            threadCount = 0;
        }

        if (threadCount > 0) {
            pool = Executors.newFixedThreadPool(threadCount);
//        } else if (threads < 0) {
//            // create a cached thread pool which should create one thread per likelihood...
//            pool = Executors.newCachedThreadPool();
        } else {
            pool = null;
        }

        if (EVALUATION_TIMERS) {
            evaluationTimes = new long[this.likelihoods.size()];
            evaluationCounts = new int[this.likelihoods.size()];
        } else {
            evaluationTimes = null;
            evaluationCounts = null;
        }
    }

    public CompoundLikelihood(Collection<Likelihood> likelihoods) {

        pool = null;
        threadCount = 0;

        int i = 0;
        for (Likelihood l : likelihoods) {
            addLikelihood(l, i, false);
            i++;
        }

        if (EVALUATION_TIMERS) {
            evaluationTimes = new long[this.likelihoods.size()];
            evaluationCounts = new int[this.likelihoods.size()];
        } else {
            evaluationTimes = null;
            evaluationCounts = null;
        }
    }

    protected void addLikelihood(Likelihood likelihood, int index, boolean addToPool) {

        // unroll any compound likelihoods
        if (UNROLL_COMPOUND && addToPool && likelihood instanceof CompoundLikelihood) {
            for (Likelihood l : ((CompoundLikelihood)likelihood).getLikelihoods()) {
                addLikelihood(l, index, addToPool);
            }
        } else {
            if (!likelihoods.contains(likelihood)) {

                likelihoods.add(likelihood);
                if (likelihood.getModel() != null) {
                    compoundModel.addModel(likelihood.getModel());
                }

                if (likelihood.evaluateEarly()) {
                    earlyLikelihoods.add(likelihood);
                } else {
                    // late likelihood list is used to evaluate them if the thread pool is not being used...
                    lateLikelihoods.add(likelihood);

                    if (addToPool) {
                        likelihoodCallers.add(new LikelihoodCaller(likelihood, index));
                    }
                }
            }
        }
    }

    public int getLikelihoodCount() {
        return likelihoods.size();
    }

    public final Likelihood getLikelihood(int i) {
        return likelihoods.get(i);
    }

    public List<Likelihood> getLikelihoods() {
        return likelihoods;
    }

    public List<Callable<Double>> getLikelihoodCallers() {
        return likelihoodCallers;
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    public Model getModel() {
        return compoundModel;
    }

//    // todo: remove in release
//    static int DEBUG = 0;

    public double getLogLikelihood() {

        double logLikelihood = evaluateLikelihoods(earlyLikelihoods);

        if( logLikelihood == Double.NEGATIVE_INFINITY ) {
            return Double.NEGATIVE_INFINITY;
        }

        if (pool == null) {
            // Single threaded
            logLikelihood += evaluateLikelihoods(lateLikelihoods);
        } else {

            try {
                List<Future<Double>> results = pool.invokeAll(likelihoodCallers);

                for (Future<Double> result : results) {
                    double logL = result.get();
                    logLikelihood += logL;
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

//        if( DEBUG > 0 ) {
//            int t = DEBUG; DEBUG = 0;
//            System.err.println(getId() + ": " + getDiagnosis(0) + " = " + logLikelihood);
//            DEBUG = t;
//        }

        if (DEBUG_PARALLEL_EVALUATION) {
            System.err.println("");
        }
        return logLikelihood;
    }

    private double evaluateLikelihoods(ArrayList<Likelihood> likelihoods) {
        double logLikelihood = 0.0;
        int i = 0;
        for (Likelihood likelihood : likelihoods) {
            if (EVALUATION_TIMERS) {
                // this code is only compiled if EVALUATION_TIMERS is true
                long time = System.nanoTime();
                double l = likelihood.getLogLikelihood();
                evaluationTimes[i] += System.nanoTime() - time;
                evaluationCounts[i] ++;

                if( l == Double.NEGATIVE_INFINITY )
                    return Double.NEGATIVE_INFINITY;

                logLikelihood += l;

                i++;
            } else {
                final double l = likelihood.getLogLikelihood();
                // if the likelihood is zero then short cut the rest of the likelihoods
                // This means that expensive likelihoods such as TreeLikelihoods should
                // be put after cheap ones such as BooleanLikelihoods
                if( l == Double.NEGATIVE_INFINITY )
                    return Double.NEGATIVE_INFINITY;
                logLikelihood += l;
            }
        }

        return logLikelihood;
    }

    public void makeDirty() {
        for( Likelihood likelihood : likelihoods ) {
            likelihood.makeDirty();
        }
    }

    public boolean evaluateEarly() {
        return false;
    }

    public String getDiagnosis() {
        return getDiagnosis(0);
    }

    public String getDiagnosis(int indent) {
        String message = "";
        boolean first = true;

        final NumberFormatter nf = new NumberFormatter(6);

        for( Likelihood lik : likelihoods ) {

            if( !first ) {
                message += ", ";
            } else {
                first = false;
            }

            if (indent >= 0) {
                message += "\n";
                for (int i = 0; i < indent; i++) {
                    message += " ";
                }
            }
            message += lik.prettyName() + "=";

            if( lik instanceof CompoundLikelihood ) {
                final String d = ((CompoundLikelihood) lik).getDiagnosis(indent < 0 ? -1 : indent + 2);
                if( d != null && d.length() > 0 ) {
                    message += "(" + d;

                    if (indent >= 0) {
                        message += "\n";
                        for (int i = 0; i < indent; i++) {
                            message += " ";
                        }
                    }
                    message += ")";
                }
            } else {

                final double logLikelihood = lik.getLogLikelihood();
                if( logLikelihood == Double.NEGATIVE_INFINITY ) {
                    message += "-Inf";
                } else if( Double.isNaN(logLikelihood) ) {
                    message += "NaN";
                } else if( logLikelihood == Double.POSITIVE_INFINITY ) {
                    message += "+Inf";
                } else {
                    message += nf.formatDecimal(logLikelihood, 4);
                }
            }
        }
        message += "\n";
        for (int i = 0; i < indent; i++) {
            message += " ";
        }
        message += "Total = " + this.getLogLikelihood();

        return message;
    }

    public String toString() {
        return getId();
        // really bad for debugging
        //return Double.toString(getLogLikelihood());
    }

    public String prettyName() {
        return Abstract.getPrettyName(this);
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed() {
        used = true;
        for (Likelihood l : likelihoods) {
            l.setUsed();
        }
    }

    public int getThreadCount() {
        return threadCount;
    }

    public long[] getEvaluationTimes() {
    	return evaluationTimes;
    }

    public int[] getEvaluationCounts() {
    	return evaluationCounts;
    }

    public void resetEvaluationTimes() {
    	for (int i = 0; i < evaluationTimes.length; i++) {
    		evaluationTimes[i] = 0;
    		evaluationCounts[i] = 0;
    	}
    }


    // **************************************************************
    // Loggable IMPLEMENTATION
    // **************************************************************

    /**
     * @return the log columns.
     */
    public beast.inference.loggers.LogColumn[] getColumns() {
        return new beast.inference.loggers.LogColumn[]{
                new LikelihoodColumn(getId() == null ? "likelihood" : getId())
        };
    }

    private class LikelihoodColumn extends beast.inference.loggers.NumberColumn {
        public LikelihoodColumn(String label) {
            super(label);
        }

        public double getDoubleValue() {
            return getLogLikelihood();
        }
    }

    // **************************************************************
    // Reportable IMPLEMENTATION
    // **************************************************************

    public String getReport() {
        return getReport(0);
    }

    public String getReport(int indent) {
        if (EVALUATION_TIMERS) {
            String message = "\n";
            boolean first = true;

            final NumberFormatter nf = new NumberFormatter(6);

            int index = 0;
            for( Likelihood lik : likelihoods ) {

                if( !first ) {
                    message += ", ";
                } else {
                    first = false;
                }

                if (indent >= 0) {
                    message += "\n";
                    for (int i = 0; i < indent; i++) {
                        message += " ";
                    }
                }
                message += lik.prettyName() + "=";

                if( lik instanceof CompoundLikelihood ) {
                    final String d = ((CompoundLikelihood) lik).getReport(indent < 0 ? -1 : indent + 2);
                    if( d != null && d.length() > 0 ) {
                        message += "(" + d;

                        if (indent >= 0) {
                            message += "\n";
                            for (int i = 0; i < indent; i++) {
                                message += " ";
                            }
                        }
                        message += ")";
                    }
                } else {
                    double secs = (double)evaluationTimes[index] / 1.0E9;
                    message += evaluationCounts[index] + " evaluations in " +
                            nf.format(secs) + " secs (" +
                            nf.format(secs / evaluationCounts[index]) + " secs/eval)";
                }
                index++;
            }

            return message;
        } else {
            return "No evaluation timer report available";
        }
    }



    // **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

    private String id = null;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    private boolean used = false;

    private final int threadCount;

    private final ExecutorService pool;

    private final ArrayList<Likelihood> likelihoods = new ArrayList<Likelihood>();
    private final CompoundModel compoundModel = new CompoundModel("compoundModel");

    private final ArrayList<Likelihood> earlyLikelihoods = new ArrayList<Likelihood>();
    private final ArrayList<Likelihood> lateLikelihoods = new ArrayList<Likelihood>();

    private final List<Callable<Double>> likelihoodCallers = new ArrayList<Callable<Double>>();

    class LikelihoodCaller implements Callable<Double> {

        public LikelihoodCaller(Likelihood likelihood, int index) {
            this.likelihood = likelihood;
            this.index = index;
        }

        public Double call() throws Exception {
            if (DEBUG_PARALLEL_EVALUATION) {
                System.err.print("Invoking thread #" + index + " for " + likelihood.getId() + ": ");
            }
            if (EVALUATION_TIMERS) {
                long time = System.nanoTime();
                double logL = likelihood.getLogLikelihood();
                evaluationTimes[index] += System.nanoTime() - time;
                evaluationCounts[index] ++;
                return logL;
            }
            return likelihood.getLogLikelihood();
        }

        private final Likelihood likelihood;
        private final int index;
    }

    public static final boolean DEBUG_PARALLEL_EVALUATION = false;

    public static final XMLObjectParser<CompoundLikelihood> PARSER = new AbstractXMLObjectParser<CompoundLikelihood>() {
        public static final String COMPOUND_LIKELIHOOD = "compoundLikelihood";
        public static final String THREADS = "threads";
        public static final String POSTERIOR = "posterior";
        public static final String PRIOR = "prior";
        public static final String LIKELIHOOD = "likelihood";
        public static final String PSEUDO_PRIOR = "pseudoPrior";
        public static final String WORKING_PRIOR = "referencePrior";

        public String getParserName() {
            return COMPOUND_LIKELIHOOD;
        }

        public String[] getParserNames() {
            return new String[]{getParserName(), POSTERIOR, PRIOR, LIKELIHOOD, PSEUDO_PRIOR, WORKING_PRIOR};
        }

        public CompoundLikelihood parseXMLObject(XMLObject xo) throws XMLParseException {

            // the default is -1 threads (automatic thread pool size) but an XML attribute can override it
            int threads = xo.getAttribute(THREADS, -1);

            // both the XML attribute and a system property can override it
            if (System.getProperty("thread.count") != null) {

                threads = Integer.parseInt(System.getProperty("thread.count"));
                if (threads < -1 || threads > 1000) {
                    // put an upper limit here - may be unnecessary?
                    threads = -1;
                }
            }
//        }

            List<Likelihood> likelihoods = new ArrayList<Likelihood>();
            for (int i = 0; i < xo.getChildCount(); i++) {
                final Object child = xo.getChild(i);
                if (child instanceof Likelihood) {
                    likelihoods.add((Likelihood) child);
                } else {

                    throw new XMLParseException("An element (" + child + ") which is not a likelihood has been added to a "
                            + COMPOUND_LIKELIHOOD + " element");
                }
            }

            CompoundLikelihood compoundLikelihood;

            if (xo.getName().equalsIgnoreCase(LIKELIHOOD)) {
                compoundLikelihood = new CompoundLikelihood(threads, likelihoods);
                switch (threads) {
                    case -1:
                        Logger.getLogger("beast.evomodel").info("Likelihood computation is using an auto sizing thread pool.");
                        break;
                    case 0:
                        Logger.getLogger("beast.evomodel").info("Likelihood computation is using a single thread.");
                        break;
                    default:
                        Logger.getLogger("beast.evomodel").info("Likelihood computation is using a pool of " + threads + " threads.");
                        break;
                }
            } else {
                compoundLikelihood = new CompoundLikelihood(likelihoods);
            }

            return compoundLikelihood;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A likelihood function which is simply the product of its component likelihood functions.";
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newIntegerRule(THREADS, true),
                new ElementRule(Likelihood.class, -1, Integer.MAX_VALUE)
        };

        public Class getReturnType() {
            return CompoundLikelihood.class;
        }
    };

}

