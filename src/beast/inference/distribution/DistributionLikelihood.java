/*
 * DistributionLikelihood.java
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

import beast.inference.model.Statistic;
import beast.math.distributions.Distribution;
import beast.util.Attribute;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class that returns the log likelihood of a set of data (statistics)
 * being distributed according to the given parametric distribution.
 *
 * @author Alexei Drummond
 * @version $Id: DistributionLikelihood.java,v 1.11 2005/05/25 09:35:28 rambaut Exp $
 */

public class DistributionLikelihood extends AbstractDistributionLikelihood {

    public static final String DISTRIBUTION_LIKELIHOOD = "distributionLikelihood";

    private int from = -1;
    private int to = Integer.MAX_VALUE;
    private final boolean evaluateEarly;

    public DistributionLikelihood(Distribution distribution) {
        this(distribution, 0.0, false);
    }

    public DistributionLikelihood(Distribution distribution, double offset) {
        this(distribution, offset, offset > 0.0);
    }

    public DistributionLikelihood(Distribution distribution, boolean evaluateEarly) {
        this(distribution, 0.0, evaluateEarly);
    }

    public DistributionLikelihood(Distribution distribution, double offset, boolean evaluateEarly) {
        super(null);
        this.distribution = distribution;
        this.offset = offset;
        this.evaluateEarly = evaluateEarly;
    }

    public DistributionLikelihood(ParametricDistributionModel distributionModel) {
        super(distributionModel);
        this.distribution = distributionModel;
        this.offset = 0.0;
        this.evaluateEarly = false;
    }

    public Distribution getDistribution() {
        return distribution;
    }

    public void setRange(int from, int to) {
        this.from = from;
        this.to = to;
    }

    // **************************************************************
    // Likelihood IMPLEMENTATION
    // **************************************************************

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    public double calculateLogLikelihood() {

        double logL = 0.0;

        for( Attribute<double[]> data : dataList ) {

            // Using this in the loop is incredibly wasteful, especially in the loop condition to get the length
            final double[] attributeValue = data.getAttributeValue();

            for (int j = Math.max(0, from); j < Math.min(attributeValue.length, to); j++) {

                final double value = attributeValue[j] - offset;

                if (offset > 0.0 && value < 0.0) {
                    // fixes a problem with the offset on exponential distributions not
                    // actually bounding the distribution. This only performs this check
                    // if a non-zero offset is actually given otherwise it assumes the
                    // parameter is either legitimately allowed to go negative or is bounded
                    // at zero anyway.
                    return Double.NEGATIVE_INFINITY;
                }

                logL += distribution.logPdf(value);
            }

        }
        return logL;
    }

    @Override
    public boolean evaluateEarly() {
        return evaluateEarly;
    }

    // **************************************************************
    // XMLElement IMPLEMENTATION
    // **************************************************************

    public Element createElement(Document d) {
        throw new RuntimeException("Not implemented yet!");
    }

    @Override
    public String prettyName() {
        String s = distribution.getClass().getName();
        String[] parts = s.split("\\.");
        s = parts[parts.length - 1];
        if( s.endsWith("Distribution") ) {
            s = s.substring(0, s.length() - "Distribution".length());
        }
        s = s + '(';
        for( Attribute<double[]> data : dataList ) {
            String name = data.getAttributeName();
            if( name == null ) {
                name = "?";
            }
                s = s + name + ',';
        }
        s = s.substring(0,s.length()-1) + ')';

        return s;
    }

    protected Distribution distribution;
    private final double offset;

    public static final XMLObjectParser<DistributionLikelihood> PARSER = new AbstractXMLObjectParser<DistributionLikelihood>() {

        public static final String DISTRIBUTION = "distribution";
        public static final String DATA = "data";
        public static final String FROM = "from";
        public static final String TO = "to";


        public String getParserName() {
            return DistributionLikelihood.DISTRIBUTION_LIKELIHOOD;
        }

        public DistributionLikelihood parseXMLObject(XMLObject xo) throws XMLParseException {

            final XMLObject cxo = xo.getChild(DISTRIBUTION);
            ParametricDistributionModel model = (ParametricDistributionModel) cxo.getChild(ParametricDistributionModel.class);

            DistributionLikelihood likelihood = new DistributionLikelihood(model);

            XMLObject cxo1 = xo.getChild(DATA);
            final int from = cxo1.getAttribute(FROM, -1);
            int to = cxo1.getAttribute(TO, -1);
            if (from >= 0 || to >= 0) {
                if (to < 0) {
                    to = Integer.MAX_VALUE;
                }
                if (!(from >= 0 && to >= 0 && from < to)) {
                    throw new XMLParseException("ill formed from-to");
                }
                likelihood.setRange(from, to);
            }

            for (int j = 0; j < cxo1.getChildCount(); j++) {
                if (cxo1.getChild(j) instanceof Statistic) {

                    likelihood.addData((Statistic) cxo1.getChild(j));
                } else {
                    throw new XMLParseException("illegal element in " + cxo1.getName() + " element");
                }
            }

            return likelihood;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(DISTRIBUTION,
                        new XMLSyntaxRule[]{new ElementRule(ParametricDistributionModel.class)}),
                new ElementRule(DATA, new XMLSyntaxRule[]{
                        AttributeRule.newIntegerRule(FROM, true),
                        AttributeRule.newIntegerRule(TO, true),
                        new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)
                })
        };

        public String getParserDescription() {
            return "Calculates the likelihood of some data given some parametric or empirical distribution.";
        }

        public Class getReturnType() {
            return DistributionLikelihood.class;
        }
    };
}

