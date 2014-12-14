/*
 * ParametricDistributionModel.java
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

import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.math.distributions.Distribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

/**
 * A class that describes a parametric distribution
 *
 * @author Alexei Drummond
 * @version $Id: ParametricDistributionModel.java,v 1.4 2005/05/24 20:25:59 rambaut Exp $
 */

public interface ParametricDistributionModel extends Distribution, Model {

    public static abstract class DistributionModelParser<D extends ParametricDistributionModel> extends AbstractXMLObjectParser<D> {

        public static final String OFFSET = "offset";
        public static final String MEAN = "mean";
        public static final String SHAPE = "shape";
        public static final String SCALE = "scale";

        /**
         * @param parameters an array of the parsed parameters, in order of the getParameterNames() array.
         * @param offset     the parsed offset
         * @return a distribution model constructed from provided parameters and offset
         */
        abstract D parseDistributionModel(Parameter[] parameters, double offset);

        /**
         * @return a list of xml element names for parameters of this distribution.
         */
        abstract String[] getParameterNames();

        abstract boolean allowOffset();

        public final D parseXMLObject(XMLObject xo) throws XMLParseException {

            double offset = xo.getAttribute(OFFSET, 0.0);

            String[] names = getParameterNames();
            Parameter[] parameters = new Parameter[names.length];
            for (int i = 0; i < names.length; i++) {
                parameters[i] = getParameter(xo, names[i]);
            }

            return parseDistributionModel(parameters, offset);
        }

        private Parameter getParameter(XMLObject xo, String parameterName) throws XMLParseException {
            final XMLObject cxo = xo.getChild(parameterName);
            return cxo.getChild(0) instanceof Parameter ?
                    (Parameter) cxo.getChild(Parameter.class) : new Parameter.Default(cxo.getDoubleChild(0));
        }

        public XMLSyntaxRule[] getSyntaxRules() {

            String[] names = getParameterNames();

            XMLSyntaxRule[] rules = new XMLSyntaxRule[names.length + (allowOffset() ? 1 : 0)];
            for (int i = 0; i < names.length; i++) {
                rules[i] = new XORRule(
                        new ElementRule(names[i], Double.class),
                        new ElementRule(names[i], Parameter.class)
                );
            }
            if (allowOffset()) {
                rules[rules.length - 1] = AttributeRule.newDoubleRule(OFFSET, true);
            }

            return rules;
        }

    }


}