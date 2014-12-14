/*
 * StrictClockBranchRates.java
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

package beast.evomodel.branchratemodel;

import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Variable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.logging.Logger;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: StrictClockBranchRates.java,v 1.3 2006/01/09 17:44:30 rambaut Exp $
 */
public class StrictClockBranchRates extends AbstractBranchRateModel {

    public static final String STRICT_CLOCK_BRANCH_RATES = "strictClockBranchRates";

    private final Parameter rateParameter;

    public StrictClockBranchRates(Parameter rateParameter) {

        super(STRICT_CLOCK_BRANCH_RATES);

        this.rateParameter = rateParameter;

        addVariable(rateParameter);
    }

    public void handleModelChangedEvent(Model model, Object object, int index) {
        // nothing to do
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        fireModelChanged();
    }

    protected void storeState() {
        // nothing to do
    }

    protected void restoreState() {
        // nothing to do
    }

    protected void acceptState() {
        // nothing to do
    }

    public double getBranchRate(final Tree tree, final NodeRef node) {
        return rateParameter.getParameterValue(0);
    }

    public static final XMLObjectParser<StrictClockBranchRates> PARSER = new AbstractXMLObjectParser<StrictClockBranchRates>() {
        public static final String RATE = "rate";

        public String getParserName() {
            return STRICT_CLOCK_BRANCH_RATES;
        }

        public StrictClockBranchRates parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter rateParameter = (Parameter) xo.getElementFirstChild(RATE);

            Logger.getLogger("beast.evomodel").info("Using strict molecular clock model.");

            return new StrictClockBranchRates(rateParameter);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return
                    "This element provides a strict clock model. " +
                            "All branches have the same rate of molecular evolution.";
        }

        public Class getReturnType() {
            return StrictClockBranchRates.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(RATE, Parameter.class, "The molecular evolutionary rate parameter", false),
        };
    };

}