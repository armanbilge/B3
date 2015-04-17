/*
 * OneOnXPrior.java
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

package beast.inference.prior;

import beast.inference.model.Likelihood;
import beast.inference.model.Statistic;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.ArrayList;

/**
 * @author Alexei Drummond
 */

public class OneOnXPrior extends Likelihood {

    public OneOnXPrior() {
        super(null);
    }

    /**
     * Adds a statistic, this is the data for which the Prod_i (1/x_i) prior is calculated.
     *
     * @param data the statistic to compute density of
     */
    public void addData(Statistic data) {
        dataList.add(data);
    }


    protected ArrayList<Statistic> dataList = new ArrayList<Statistic>();

    @Override
    public void cacheCalculations() {
        // Nothing to do
    }

    @Override
    public void uncacheCalculations() {
        // Nothing to do
    }

    /**
     * Overridden to always return false.
     */
    protected boolean getLikelihoodKnown() {
        return false;
    }

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    public double calculateLogLikelihood() {

        double logL = 0.0;

        for (Statistic statistic : dataList) {
            for (int j = 0; j < statistic.getDimension(); j++) {
                logL -= Math.log(statistic.getStatisticValue(j));
            }
        }
        return logL;
    }


    public String prettyName() {
        String s = "OneOnX" + "(";
        for (Statistic statistic : dataList) {
            s = s + statistic.getStatisticName() + ",";
        }
        return s.substring(0, s.length() - 1) + ")";
    }

    public static final XMLObjectParser<OneOnXPrior> PARSER = new AbstractXMLObjectParser<OneOnXPrior>() {

        public static final String ONE_ONE_X_PRIOR = "oneOnXPrior";
        public static final String JEFFREYS_PRIOR = "jeffreysPrior";
        public static final String DATA = "data";

        public String getParserName() {
            return ONE_ONE_X_PRIOR;
        }

        public String[] getParserNames() {
            return new String[]{getParserName(), JEFFREYS_PRIOR};
        }

        public OneOnXPrior parseXMLObject(XMLObject xo) throws XMLParseException {

            OneOnXPrior likelihood = new OneOnXPrior();

            XMLObject cxo = xo;

            if (xo.hasChildNamed(DATA)) {
                cxo = xo.getChild(DATA);
            }

            for (int i = 0; i < cxo.getChildCount(); i++) {
                if (cxo.getChild(i) instanceof Statistic) {
                    likelihood.addData((Statistic) cxo.getChild(i));
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
                new XORRule(
                        new ElementRule(Statistic.class, 1, Integer.MAX_VALUE),
                        new ElementRule(DATA, new XMLSyntaxRule[]{new ElementRule(Statistic.class, 1, Integer.MAX_VALUE)})
                )
        };

        public String getParserDescription() {
            return "Calculates the (improper) prior proportional to Prod_i (1/x_i) for the given statistic x.";
        }

        public Class getReturnType() {
            return OneOnXPrior.class;
        }
    };
}

