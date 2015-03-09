/*
 * DummyModel.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
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

import beast.inference.loggers.LogColumn;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * @author Marc Suchard
 */
public class DummyModel extends AbstractModelLikelihood {

    public static final String DUMMY_MODEL = "dummyModel";

    final double likelihood;

    public DummyModel() {
        this(0.0);
    }

    public DummyModel(double logL) {
        super(DUMMY_MODEL);
        likelihood = logL;
    }

    public DummyModel(String str) {
        this(str, 0.0);
    }

    public DummyModel(String str, double logL) {
        super(str);
        likelihood = logL;
    }

    public DummyModel(Parameter parameter) {
        this(parameter, 0.0);
    }

    public DummyModel(Parameter parameter, double logL) {
        super(DUMMY_MODEL);
        addVariable(parameter);
        likelihood = logL;
    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {

    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {

    }

    protected void storeState() {

    }

    protected void restoreState() {

    }

    protected void acceptState() {

    }

    public Model getModel() {
        return this;
    }

    public double getLogLikelihood() {
        return likelihood;
    }

    public double differentiate(Variable<Double> var, int index) {
        return 0;
    }

    public void makeDirty() {

    }

    public LogColumn[] getColumns() {
        return new LogColumn[0];
    }

    public static final XMLObjectParser<DummyModel> PARSER = new AbstractXMLObjectParser<DummyModel>() {

        static final String LOG_L = "logL";

        public String getParserName() {
            return DUMMY_MODEL;
        }

        public DummyModel parseXMLObject(XMLObject xo) throws XMLParseException {

            DummyModel likelihood = new DummyModel(xo.getAttribute(LOG_L, 0.0));

            for (int i = 0; i < xo.getChildCount(); i++) {
                Parameter parameter = (Parameter) xo.getChild(i);
                likelihood.addVariable(parameter);
            }

            return likelihood;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A function wraps a component model that would otherwise not be registered with the MCMC. Always returns a log likelihood of zero.";
        }

        public Class getReturnType() {
            return DummyModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(Parameter.class, 1, Integer.MAX_VALUE),
                AttributeRule.newDoubleRule(LOG_L, true)
        };
    };

}


