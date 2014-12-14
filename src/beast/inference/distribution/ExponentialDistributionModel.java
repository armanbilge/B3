/*
 * ExponentialDistributionModel.java
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
import beast.math.distributions.ExponentialDistribution;
import beast.xml.XMLObjectParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class that acts as a model for exponentially distributed data.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: ExponentialDistributionModel.java,v 1.12 2005/05/24 20:25:59 rambaut Exp $
 */

public class ExponentialDistributionModel extends AbstractModel implements ParametricDistributionModel {

    public static final String EXPONENTIAL_DISTRIBUTION_MODEL = "exponentialDistributionModel";

    /**
     * Constructor.
     */
    public ExponentialDistributionModel(Variable<Double> mean) {

        this(mean, 0.0);
    }


    /**
     * Constructor.
     */
    public ExponentialDistributionModel(Variable<Double> mean, double offset) {

        super(EXPONENTIAL_DISTRIBUTION_MODEL);

        this.mean = mean;
        this.offset = offset;

        addVariable(mean);
        mean.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
    }

    // *****************************************************************
    // Interface Distribution
    // *****************************************************************

    public double pdf(double x) {
        if (x < offset) return 0.0;
        return ExponentialDistribution.pdf(x - offset, 1.0 / getMean());
    }

    public double logPdf(double x) {
        if (x < offset) return Double.NEGATIVE_INFINITY;
        return ExponentialDistribution.logPdf(x - offset, 1.0 / getMean());
    }

    public double cdf(double x) {
        if (x < offset) return 0.0;
        return ExponentialDistribution.cdf(x - offset, 1.0 / getMean());
    }

    public double quantile(double y) {
        return ExponentialDistribution.quantile(y, 1.0 / getMean()) + offset;
    }

    public double mean() {
        return ExponentialDistribution.mean(1.0 / getMean()) + offset;
    }

    public double variance() {
        return ExponentialDistribution.variance(1.0 / getMean());
    }

    public final UnivariateFunction getProbabilityDensityFunction() {
        return pdfFunction;
    }

    private final UnivariateFunction pdfFunction = new UnivariateFunction() {
        public final double evaluate(double x) {
            return pdf(x);
        }

        public final double getLowerBound() {
            return offset;
        }

        public final double getUpperBound() {
            return Double.POSITIVE_INFINITY;
        }
    };

    // *****************************************************************
    // Interface Model
    // *****************************************************************

    public void handleModelChangedEvent(Model model, Object object, int index) {
        // no intermediates need to be recalculated...
    }

    public void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        // no intermediates need to be recalculated...
    }

    protected void storeState() {
    } // no additional state needs storing

    protected void restoreState() {
    } // no additional state needs restoring

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

    private double getMean() {
        return mean.getValue(0);
    }

    // **************************************************************
    // Private instance variables
    // **************************************************************

    private Variable<Double> mean = null;
    private double offset = 0.0;

    public static final XMLObjectParser<ExponentialDistributionModel> PARSER = new DistributionModelParser<ExponentialDistributionModel>() {
        public String getParserName() {
            return ExponentialDistributionModel.EXPONENTIAL_DISTRIBUTION_MODEL;
        }

        ExponentialDistributionModel parseDistributionModel(Parameter[] parameters, double offset) {
            return new ExponentialDistributionModel(parameters[0], offset);
        }

        public String[] getParameterNames() {
            return new String[]{MEAN};
        }

        public String getParserDescription() {
            return "A model of an exponential distribution.";
        }

        public boolean allowOffset() {
            return true;
        }

        public Class getReturnType() {
            return ExponentialDistributionModel.class;
        }

    };

}

