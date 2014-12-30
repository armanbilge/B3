/*
 * MCMC.java
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

package beast.inference.mcmc;

import beast.inference.loggers.Logger;
import beast.inference.markovchain.MarkovChain;
import beast.inference.markovchain.MarkovChainDelegate;
import beast.inference.markovchain.MarkovChainListener;
import beast.inference.model.CompoundLikelihood;
import beast.inference.model.Likelihood;
import beast.inference.model.Model;
import beast.inference.operators.CoercableMCMCOperator;
import beast.inference.operators.CoercionMode;
import beast.inference.operators.GibbsOperator;
import beast.inference.operators.JointOperator;
import beast.inference.operators.MCMCOperator;
import beast.inference.operators.OperatorSchedule;
import beast.inference.operators.SimpleOperatorSchedule;
import beast.inference.prior.Prior;
import beast.math.MathUtils;
import beast.util.FileHelpers;
import beast.util.Identifiable;
import beast.util.NumberFormatter;
import beast.util.Serializer;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.Spawnable;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLParser;
import beast.xml.XMLSyntaxRule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * An MCMC analysis that estimates parameters of a probabilistic model.
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: MCMC.java,v 1.41 2005/07/11 14:06:25 rambaut Exp $
 */
public class MCMC implements Identifiable, Spawnable {

    public MCMC(String id) {
        this.id = id;
    }

    /**
     * Must be called before calling chain.
     *
     * @param options    the options for this MCMC analysis
     * @param schedule   operator schedule to be used in chain.
     * @param likelihood the likelihood for this MCMC
     * @param loggers    an array of loggers to record output of this MCMC run
     */
    public void init(
            MCMCOptions options,
            Likelihood likelihood,
            OperatorSchedule schedule,
            Logger[] loggers) {

        init(options, likelihood, Prior.UNIFORM_PRIOR, schedule, loggers, new MarkovChainDelegate[0]);
    }

    /**
     * Must be called before calling chain.
     *
     * @param options    the options for this MCMC analysis
     * @param schedule   operator schedule to be used in chain.
     * @param likelihood the likelihood for this MCMC
     * @param loggers    an array of loggers to record output of this MCMC run
     * @param delegates    an array of delegates to handle tasks related to the MCMC
     */
    public void init(
            MCMCOptions options,
            Likelihood likelihood,
            OperatorSchedule schedule,
            Logger[] loggers,
            MarkovChainDelegate[] delegates) {

        init(options, likelihood, Prior.UNIFORM_PRIOR, schedule, loggers, delegates);
    }

    /**
     * Must be called before calling chain.
     *
     * @param options    the options for this MCMC analysis
     * @param prior      the prior disitrbution on the model parameters.
     * @param schedule   operator schedule to be used in chain.
     * @param likelihood the likelihood for this MCMC
     * @param loggers    an array of loggers to record output of this MCMC run
     */
    public void init(
            MCMCOptions options,
            Likelihood likelihood,
            Prior prior,
            OperatorSchedule schedule,
            Logger[] loggers) {

        init(options, likelihood, prior, schedule, loggers, new MarkovChainDelegate[0]);
    }

    /**
     * Must be called before calling chain.
     *
     * @param options    the options for this MCMC analysis
     * @param prior      the prior disitrbution on the model parameters.
     * @param schedule   operator schedule to be used in chain.
     * @param likelihood the likelihood for this MCMC
     * @param loggers    an array of loggers to record output of this MCMC run
     * @param delegates    an array of delegates to handle tasks related to the MCMC
     */
    public void init(
            MCMCOptions options,
            Likelihood likelihood,
            Prior prior,
            OperatorSchedule schedule,
            Logger[] loggers,
            MarkovChainDelegate[] delegates) {

        MCMCCriterion criterion = new MCMCCriterion();
        criterion.setTemperature(options.getTemperature());

        mc = new MarkovChain(prior, likelihood, schedule, criterion,
                options.getFullEvaluationCount(), options.minOperatorCountForFullEvaluation(),
                options.getEvaluationTestThreshold(),
                options.useCoercion());

        this.options = options;
        this.loggers = loggers;
        this.schedule = schedule;

        //initialize transients
        currentState = 0;
        chainInitialized = false;
        chainTerminated = false;
        serializing = true;

        // Does not seem to be in use (JH)
/*
        stepsPerReport = 1;
        while ((getChainLength() / stepsPerReport) > 1000) {
            stepsPerReport *= 2;
        }*/

        for(MarkovChainDelegate delegate : delegates) {
            delegate.setup(options, schedule, mc);
        }
        this.delegates = delegates;
    }

    /**
     * Must be called before calling chain.
     *
     * @param chainlength chain length
     * @param likelihood the likelihood for this MCMC
     * @param operators  an array of MCMC operators
     * @param loggers    an array of loggers to record output of this MCMC run
     */
    public void init(long chainlength,
                     Likelihood likelihood,
                     MCMCOperator[] operators,
                     Logger[] loggers) {

        MCMCOptions options = new MCMCOptions(chainlength);
        MCMCCriterion criterion = new MCMCCriterion();
        criterion.setTemperature(1);
        OperatorSchedule schedule = new SimpleOperatorSchedule();
        for (MCMCOperator operator : operators) schedule.addOperator(operator);

        init(options, likelihood, Prior.UNIFORM_PRIOR, schedule, loggers);
    }

    public MarkovChain getMarkovChain() {
        return mc;
    }

    public Logger[] getLoggers() {
        return loggers;
    }

    public MCMCOptions getOptions() {
        return options;
    }

    public OperatorSchedule getOperatorSchedule() {
        return schedule;
    }

    public void run() {
        chain();
    }

    // Experimental
    //public static int ontheflyFreq = 0;

    /**
     * This method actually initiates the MCMC analysis.
     */
    protected void initChain() {

        stopping = false;
        currentState = 0;

        timer.start();

        if (loggers != null) {
            for (Logger logger : loggers) {
                logger.startLogging();
            }
        }

        mc.addMarkovChainListener(chainListener);

        for (MarkovChainDelegate delegate : delegates) {
            mc.addMarkovChainDelegate(delegate);
        }

        chainInitialized = true;

    }

    /**
     * Start (or continue) the chain based on the currentState.
     */
    public void chain() {

        if (stopping) return;

        if (!chainInitialized) initChain();

        final long coercionDelay = getCoercionDelay();
        final long chainLength = getChainLength();

        if (coercionDelay > currentState) {
            // Run the chain for coercionDelay steps with coercion disabled
            mc.runChain(coercionDelay - currentState, true);

            // reset operator acceptance levels
            for (int i = 0; i < schedule.getOperatorCount(); i++) {
                schedule.getOperator(i).reset();
            }
        }

        mc.runChain(chainLength - currentState, false);

        if (!chainTerminated) terminateChain();
    }

    protected void terminateChain() {
        mc.terminateChain();
        mc.removeMarkovChainListener(chainListener);
        for(MarkovChainDelegate delegate : delegates) {
            mc.removeMarkovChainDelegate(delegate);
        }
        timer.stop();
        chainTerminated = true;
    }

    protected final MarkovChainListener chainListener = new MarkovChainListener() {

        // MarkovChainListener interface *******************************************
        // for receiving messages from subordinate MarkovChain

        /**
         * Called to update the current model keepEvery states.
         */
        public void currentState(long state, Model currentModel) {

            currentState = state;

            if (loggers != null) {
                for (Logger logger : loggers) {
                    logger.log(state);
                }
            }

            handleSerialization(state);

        }

        /**
         * Called when a new new best posterior state is found.
         */
        public void bestState(long state, Model bestModel) {
            currentState = state;
        }

        /**
         * cleans up when the chain finishes (possibly early).
         */
        public void finished(long chainLength) {
            currentState = chainLength;

            if (loggers != null) {
                for (Logger logger : loggers) {
                    logger.log(currentState);
                    logger.stopLogging();
                }
            }

            handleSerialization(chainLength);

            // OperatorAnalysisPrinter class can do the job now
            if (showOperatorAnalysis) {
                showOperatorAnalysis(System.out);
            }

            if (operatorAnalysisFile != null) {
                try {
                    PrintStream out = new PrintStream(new FileOutputStream(operatorAnalysisFile));
                    showOperatorAnalysis(out);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // How should premature finish be flagged?
        }

        private void handleSerialization(long state) {
            if (options.getStoreEvery() < 1) serializing = false;
            if (serializing && state % options.getStoreEvery() == 0) {
                if (serializer == null) {
                    final File stateFile = FileHelpers.getFile((getId() != null ? getId() : "mcmc") + ".state");
                    serializer = new Serializer<>(stateFile, MCMC.this);
                }
                try {
                    serializer.serialize();
                    MathUtils.saveState();
                } catch (final Serializer.SerializationException ex) {
                    java.util.logging.Logger.getLogger("error").warning("Storing of state disabled due to following error." +
                            " Please note that restarting this analysis will not be possible!");
                    java.util.logging.Logger.getLogger("error").warning(ex.toString());
                    serializing = false;
                }
            }
        }

    };

    /**
     * Writes ano operator analysis to the provided print stream
     *
     * @param out the print stream to write operator analysis to
     */
    private void showOperatorAnalysis(PrintStream out) {
        out.println();
        out.println("Operator analysis");
            out.println(formatter.formatToFieldWidth("Operator", 50) +
                    formatter.formatToFieldWidth("Tuning", 9) +
                    formatter.formatToFieldWidth("Count", 11) +
                    formatter.formatToFieldWidth("Time", 9) +
                    formatter.formatToFieldWidth("Time/Op", 9) +
                    formatter.formatToFieldWidth("Pr(accept)", 11) +
                    (options.useCoercion() ? "" : " Performance suggestion"));

        for (int i = 0; i < schedule.getOperatorCount(); i++) {

            final MCMCOperator op = schedule.getOperator(i);
            if (op instanceof JointOperator) {
                JointOperator jointOp = (JointOperator) op;
                for (int k = 0; k < jointOp.getNumberOfSubOperators(); k++) {
                    out.println(formattedOperatorName(jointOp.getSubOperatorName(k))
                            + formattedParameterString(jointOp.getSubOperator(k))
                            + formattedCountString(op)
                            + formattedTimeString(op)
                            + formattedTimePerOpString(op)
                            + formattedProbString(jointOp)
                            + (options.useCoercion() ? "" : formattedDiagnostics(jointOp, MCMCOperator.Utils.getAcceptanceProbability(jointOp)))
                    );
                }
            } else {
                out.println(formattedOperatorName(op.getOperatorName())
                        + formattedParameterString(op)
                        + formattedCountString(op)
                        + formattedTimeString(op)
                        + formattedTimePerOpString(op)
                        + formattedProbString(op)
                        + (options.useCoercion() ? "" : formattedDiagnostics(op, MCMCOperator.Utils.getAcceptanceProbability(op)))
                );
            }

        }
        out.println();
    }

    private String formattedOperatorName(String operatorName) {
        return formatter.formatToFieldWidth(operatorName, 50);
    }

    private String formattedParameterString(MCMCOperator op) {
        String pString = "        ";
        if (op instanceof CoercableMCMCOperator && ((CoercableMCMCOperator) op).getMode() != CoercionMode.COERCION_OFF) {
            pString = formatter.formatToFieldWidth(formatter.formatDecimal(((CoercableMCMCOperator) op).getRawParameter(), 3), 8);
        }
        return pString;
    }

    private String formattedCountString(MCMCOperator op) {
        final int count = op.getCount();
        return formatter.formatToFieldWidth(Integer.toString(count), 10) + " ";
    }

    private String formattedTimeString(MCMCOperator op) {
        final long time = op.getTotalEvaluationTime();
        return formatter.formatToFieldWidth(Long.toString(time), 8) + " ";
    }

    private String formattedTimePerOpString(MCMCOperator op) {
        final double time = op.getMeanEvaluationTime();
        return formatter.formatToFieldWidth(formatter.formatDecimal(time, 2), 8) + " ";
    }

    private String formattedProbString(MCMCOperator op) {
        final double acceptanceProb = MCMCOperator.Utils.getAcceptanceProbability(op);
        return formatter.formatToFieldWidth(formatter.formatDecimal(acceptanceProb, 4), 11) + " ";
    }

    private String formattedDiagnostics(MCMCOperator op, double acceptanceProb) {

        String message = "good";
        if (acceptanceProb < op.getMinimumGoodAcceptanceLevel()) {
            if (acceptanceProb < (op.getMinimumAcceptanceLevel() / 10.0)) {
                message = "very low";
            } else if (acceptanceProb < op.getMinimumAcceptanceLevel()) {
                message = "low";
            } else message = "slightly low";

        } else if (acceptanceProb > op.getMaximumGoodAcceptanceLevel()) {
            double reallyHigh = 1.0 - ((1.0 - op.getMaximumAcceptanceLevel()) / 10.0);
            if (acceptanceProb > reallyHigh) {
                message = "very high";
            } else if (acceptanceProb > op.getMaximumAcceptanceLevel()) {
                message = "high";
            } else message = "slightly high";
        }

        String performacsMsg;
        if (op instanceof GibbsOperator) {
            performacsMsg = "none (Gibbs operator)";
        } else {
            final String suggestion = op.getPerformanceSuggestion();
            performacsMsg = message + "\t" + suggestion;
        }

        return performacsMsg;
    }

    /**
     * @return the prior of this MCMC analysis.
     */
    public Prior getPrior() {
        return mc.getPrior();
    }

    /**
     * @return the likelihood function.
     */
    public Likelihood getLikelihood() {
        return mc.getLikelihood();
    }

    /**
     * @return the timer.
     */
    public beast.util.Timer getTimer() {
        return timer;
    }

    /**
     * @return the length of this analysis.
     */
    public final long getChainLength() {
        return options.getChainLength();
    }

    // TRANSIENT PUBLIC METHODS *****************************************

    /**
     * @return the current state of the MCMC analysis.
     */
    public final long getCurrentState() {
        return currentState;
    }

    /**
     * @return the progress (0 to 1) of the MCMC analysis.
     */
    public final double getProgress() {
        return (double) currentState / (double) options.getChainLength();
    }

    /**
     * @return true if this MCMC is currently adapting the operators.
     */
    public final boolean isAdapting() {
        return isAdapting;
    }

    /**
     * Requests that the MCMC chain stop prematurely.
     */
    public void pleaseStop() {
        stopping = true;
        mc.pleaseStop();
    }

    /**
     * @return true if Markov chain is stopped
     */
    public boolean isStopped() {
        return mc.isStopped();
    }

    public boolean getSpawnable() {
        return spawnable;
    }

    private boolean spawnable = true;

    public void setSpawnable(boolean spawnable) {
        this.spawnable = spawnable;
    }


    //PRIVATE METHODS *****************************************
    protected long getCoercionDelay() {

        long delay = options.getCoercionDelay();
        if (delay < 0) {
            delay = (long)(options.getChainLength() / 100);
        }
        if (options.useCoercion()) return delay;

        for (int i = 0; i < schedule.getOperatorCount(); i++) {
            MCMCOperator op = schedule.getOperator(i);

            if (op instanceof CoercableMCMCOperator) {
                if (((CoercableMCMCOperator) op).getMode() == CoercionMode.COERCION_ON) return delay;
            }
        }

        return -1;
    }

    public void setShowOperatorAnalysis(boolean soa) {
        showOperatorAnalysis = soa;
    }


    public void setOperatorAnalysisFile(File operatorAnalysisFile) {
        this.operatorAnalysisFile = operatorAnalysisFile;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOptions(final MCMCOptions options) {
        this.options = options;
    }

    public void setSerializer(final Serializer<MCMC> serializer) {
        this.serializer = serializer;
    }

    // PRIVATE TRANSIENTS

    //private FileLogger operatorLogger = null;
    protected final boolean isAdapting = true;
    protected boolean stopping = false;
    protected boolean showOperatorAnalysis = true;
    protected boolean chainInitialized = false;
    protected boolean chainTerminated = false;
    protected File operatorAnalysisFile = null;
    protected final beast.util.Timer timer = new beast.util.Timer();
    protected long currentState = 0;
    //private int stepsPerReport = 1000;
    protected final NumberFormatter formatter = new NumberFormatter(8);

    /**
     * this markov chain does most of the work.
     */
    protected MarkovChain mc;

    /**
     * the options of this MCMC analysis
     */
    protected MCMCOptions options;

    protected Logger[] loggers;
    protected OperatorSchedule schedule;
    private MarkovChainDelegate[] delegates;

    private String id = null;

    // Must be declared transient to avoid ConcurrentModificationException when serializing
    private transient Serializer<MCMC> serializer = null;
    protected boolean serializing = true;

    public static final XMLObjectParser<MCMC> PARSER =
            new AbstractXMLObjectParser<MCMC>() {
                public String getParserName() {
                    return MCMC;
                }

                /**
                 * @return an mcmc object based on the XML element it was passed.
                 */
                public MCMC parseXMLObject(XMLObject xo) throws XMLParseException {

                    MCMC mcmc = new MCMC(xo.getAttribute(NAME, "mcmc1"));
                    mcmc.setId(xo.getId());

                    long chainLength = xo.getLongIntegerAttribute(CHAIN_LENGTH);
                    boolean useCoercion = xo.getAttribute(COERCION, true);
                    long coercionDelay = chainLength / 100;
                    if (xo.hasAttribute(PRE_BURNIN)) {
                        coercionDelay = xo.getIntegerAttribute(PRE_BURNIN);
                    }
                    coercionDelay = xo.getAttribute(COERCION_DELAY, coercionDelay);
                    double temperature = xo.getAttribute(TEMPERATURE, 1.0);
                    long fullEvaluationCount = xo.getAttribute(FULL_EVALUATION, 2000);

                    double evaluationTestThreshold = MarkovChain.EVALUATION_TEST_THRESHOLD;
                    if (System.getProperty("mcmc.evaluation.threshold") != null) {
                        evaluationTestThreshold = Double.parseDouble(System.getProperty("mcmc.evaluation.threshold"));
                    }
                    evaluationTestThreshold = xo.getAttribute(EVALUATION_THRESHOLD, evaluationTestThreshold);

                    int minOperatorCountForFullEvaluation = xo.getAttribute(MIN_OPS_EVALUATIONS, 1);

                    final int storeEvery = xo.getAttribute(STORE_EVERY, 1000);

                    MCMCOptions options = new MCMCOptions(chainLength,
                            fullEvaluationCount,
                            minOperatorCountForFullEvaluation,
                            evaluationTestThreshold,
                            useCoercion,
                            coercionDelay,
                            temperature,
                            storeEvery);

                    OperatorSchedule opsched = (OperatorSchedule) xo.getChild(OperatorSchedule.class);
                    Likelihood likelihood = (Likelihood) xo.getChild(Likelihood.class);

                    likelihood.setUsed();

                    // check that all models, parameters and likelihoods are being used
//        for (Likelihood l : Likelihood.FULL_LIKELIHOOD_SET) {
//            if (!l.isUsed()) {
//                java.util.logging.Logger.getLogger("beast.inference").warning("Likelihood, " + l.getId() +
//                        ", of class " + l.getClass().getName() + " is not being handled by the MCMC.");
//            }
//        }
//        for (Model m : Model.FULL_MODEL_SET) {
//            if (!m.isUsed()) {
//                java.util.logging.Logger.getLogger("beast.inference").warning("Model, " + m.getId() +
//                        ", of class " + m.getClass().getName() + " is not being handled by the MCMC.");
//            }
//        }
//        for (Parameter p : Parameter.FULL_PARAMETER_SET) {
//            if (!p.isUsed()) {
//                java.util.logging.Logger.getLogger("beast.inference").warning("Parameter, " + p.getId() +
//                        ", of class " + p.getClass().getName() + " is not being handled by the MCMC.");
//            }
//        }


                    ArrayList<Logger> loggers = new ArrayList<Logger>();
                    ArrayList<MarkovChainDelegate> delegates = new ArrayList<MarkovChainDelegate>();

                    for (int i = 0; i < xo.getChildCount(); i++) {
                        Object child = xo.getChild(i);
                        if (child instanceof Logger) {
                            loggers.add((Logger) child);
                        }
                        else if (child instanceof MarkovChainDelegate) {
                            delegates.add((MarkovChainDelegate) child);
                        }
                    }

                    mcmc.setShowOperatorAnalysis(true);
                    if (xo.hasAttribute(OPERATOR_ANALYSIS)) {
                        mcmc.setOperatorAnalysisFile(XMLParser.getLogFile(xo, OPERATOR_ANALYSIS));
                    }


                    Logger[] loggerArray = new Logger[loggers.size()];
                    loggers.toArray(loggerArray);


                    java.util.logging.Logger.getLogger("beast.inference").info("Creating the MCMC chain:" +
                                    "\n  chainLength=" + options.getChainLength() +
                                    "\n  autoOptimize=" + options.useCoercion() +
                                    (options.useCoercion() ? "\n  autoOptimize delayed for " + options.getCoercionDelay() + " steps" : "") +
                                    (options.getFullEvaluationCount() == 0 ? "\n  full evaluation test off" : "")
                    );

                    MarkovChainDelegate[] delegateArray = new MarkovChainDelegate[delegates.size()];
                    delegates.toArray(delegateArray);

                    mcmc.init(options, likelihood, opsched, loggerArray, delegateArray);


                    MarkovChain mc = mcmc.getMarkovChain();
                    double initialScore = mc.getCurrentScore();

                    if (initialScore == Double.NEGATIVE_INFINITY) {
                        String message = "The initial posterior is zero";
                        if (likelihood instanceof CompoundLikelihood) {
                            message += ": " + ((CompoundLikelihood) likelihood).getDiagnosis(2);
                        } else {
                            message += "!";
                        }
                        throw new IllegalArgumentException(message);
                    }

                    if (!xo.getAttribute(SPAWN, true))
                        mcmc.setSpawnable(false);

                    return mcmc;
                }

                //************************************************************************
                // AbstractXMLObjectParser implementation
                //************************************************************************

                public String getParserDescription() {
                    return "This element returns an MCMC chain and runs the chain as a side effect.";
                }

                public Class<MCMC> getReturnType() {
                    return MCMC.class;
                }

                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                private final XMLSyntaxRule[] rules = {
                        AttributeRule.newLongIntegerRule(CHAIN_LENGTH),
                        AttributeRule.newBooleanRule(COERCION, true),
                        AttributeRule.newIntegerRule(COERCION_DELAY, true),
                        AttributeRule.newIntegerRule(PRE_BURNIN, true),
                        AttributeRule.newDoubleRule(TEMPERATURE, true),
                        AttributeRule.newIntegerRule(FULL_EVALUATION, true),
                        AttributeRule.newIntegerRule(MIN_OPS_EVALUATIONS, true),
                        AttributeRule.newDoubleRule(EVALUATION_THRESHOLD, true),
                        AttributeRule.newBooleanRule(SPAWN, true),
                        AttributeRule.newStringRule(NAME, true),
                        AttributeRule.newStringRule(OPERATOR_ANALYSIS, true),
                        AttributeRule.newIntegerRule(STORE_EVERY, true),
                        new ElementRule(OperatorSchedule.class),
                        new ElementRule(Likelihood.class),
                        new ElementRule(Logger.class, 1, Integer.MAX_VALUE),
                        new ElementRule(MarkovChainDelegate.class, 0, Integer.MAX_VALUE)
                };

                public static final String COERCION = "autoOptimize";
                public static final String NAME = "name";
                public static final String PRE_BURNIN = "preBurnin";
                public static final String COERCION_DELAY = "autoOptimizeDelay";
                public static final String MCMC = "mcmc";
                public static final String CHAIN_LENGTH = "chainLength";
                public static final String FULL_EVALUATION = "fullEvaluation";
                public static final String EVALUATION_THRESHOLD  = "evaluationThreshold";
                public static final String MIN_OPS_EVALUATIONS = "minOpsFullEvaluations";
                public static final String WEIGHT = "weight";
                public static final String TEMPERATURE = "temperature";
                public static final String SPAWN = "spawn";
                public static final String OPERATOR_ANALYSIS = "operatorAnalysis";
                public static final String STORE_EVERY = "storeEvery";
            };
}

