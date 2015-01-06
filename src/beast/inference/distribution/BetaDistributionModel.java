/*
 * BetaDistributionModel.java
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

package beast.inference.distribution;

import beast.inference.model.AbstractModel;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.math.UnivariateFunction;
import beast.math.distributions.BetaDistribution;
import beast.xml.XMLObjectParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class that acts as a model for beta distributed data.
 *
 * @author Marc A. Suchard
 */

public class BetaDistributionModel extends AbstractModel implements ParametricDistributionModel {

    public static final String BETA_DISTRIBUTION_MODEL = "betaDistributionModel";

    public BetaDistributionModel(Variable<Double> alpha, Variable<Double> beta) {
        this(alpha, beta, 0.0, 1.0);
    }


    /**
     * Constructor.
     */
    public BetaDistributionModel(Variable<Double> alpha, Variable<Double> beta, double offset, double length) {

        super(BETA_DISTRIBUTION_MODEL);

        this.alpha = alpha;
        this.beta = beta;
        this.length = length;
        this.offset = offset;

        addVariable(alpha);
        alpha.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        addVariable(beta);
        beta.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        recomputeBetaDistribution();
    }

    // *****************************************************************
    // Interface Distribution
    // *****************************************************************

    public double pdf(double x) {
        double xScaled = getXScaled(x);
        if (xScaled < 0.0 || xScaled > 1.0) return 0.0;

        return betaDistribution.pdf(xScaled);
    }

    public double logPdf(double x) {
        double xScaled = getXScaled(x);
        if (xScaled < 0.0 || xScaled > 1.0) return Double.NEGATIVE_INFINITY;

        return betaDistribution.logPdf(xScaled);
    }

    public double differentiateLogPdf(double x) {
        double xScaled = getXScaled(x);
        if (xScaled < 0.0 || xScaled > 1.0) return 0.0;

        return betaDistribution.differentiateLogPdf(xScaled);
    }

    public double cdf(double x) {
        if (x < offset) return 0.0;
        return betaDistribution.cdf(getXScaled(x));
    }

    public double quantile(double y) {
        return betaDistribution.quantile(getXScaled(y)) * length + offset;
    }

    public double mean() {
        return betaDistribution.mean() * length + offset;
    }

    public double variance() {
        return betaDistribution.variance() * length * length;
    }

    public final UnivariateFunction getProbabilityDensityFunction() {
        return pdfFunction;
    }

    private final UnivariateFunction pdfFunction = new UnivariateFunction() {
        public final double evaluate(double x) {
            double xScale = (x - offset) / length;
            return pdf(xScale);
        }

        public final double getLowerBound() {
            return offset;
        }

        public final double getUpperBound() {
            return offset + length;
        }
    };

    // *****************************************************************
    // Interface Model
    // *****************************************************************

    public void handleModelChangedEvent(Model model, Object object, int index) {
        // no intermediates need to be recalculated...
    }

    public void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        recomputeBetaDistribution();
    }

    protected void storeState() {
        storedBetaDistribution = betaDistribution;
    }

    protected void restoreState() {
        betaDistribution = storedBetaDistribution;
    }

    protected void acceptState() {
    } // no additional state needs accepting

    // **************************************************************
    // XMLElement IMPLEMENTATION
    // **************************************************************

    public Element createElement(Document document) {
        throw new RuntimeException("Not implemented!");
    }

    // **************************************************************
    // Private methods
    // **************************************************************

    private void recomputeBetaDistribution() {
        betaDistribution = new BetaDistribution(alpha.getValue(0), beta.getValue(0));
    }

    private double getXScaled(double x) {
        return (x - offset) / length;
    }

    // **************************************************************
    // Private instance variables
    // **************************************************************

    private Variable<Double> alpha = null;
    private Variable<Double> beta = null;
    private double offset = 0.0;
    private double length = 0.0;

    private BetaDistribution betaDistribution = null;
    private BetaDistribution storedBetaDistribution = null;

    public static final XMLObjectParser<BetaDistributionModel> PARSER = new DistributionModelParser<BetaDistributionModel>() {
        public static final String ALPHA = "alpha";
        public static final String BETA = "beta";

        public String getParserName() {
            return BetaDistributionModel.BETA_DISTRIBUTION_MODEL;
        }

        BetaDistributionModel parseDistributionModel(Parameter[] parameters, double offset) {
            return new BetaDistributionModel(parameters[0], parameters[1]);
        }

        public String[] getParameterNames() {
            return new String[]{ALPHA, BETA};
        }

        public String getParserDescription() {
            return "A model of a beta distribution.";
        }

        public boolean allowOffset() {
            return false;
        }

        public Class getReturnType() {
            return BetaDistributionModel.class;
        }
    };

}

