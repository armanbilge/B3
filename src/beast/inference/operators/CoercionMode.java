/*
 * CoercionMode.java
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

package beast.inference.operators;

import beast.xml.AttributeRule;
import beast.xml.SimpleXMLObjectParser;
import beast.xml.XMLObject;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Alexei Drummond
 */
public enum CoercionMode {
    DEFAULT, COERCION_ON, COERCION_OFF;

    public static CoercionMode parseMode(XMLObject xo) throws XMLParseException {
        CoercionMode mode = CoercionMode.DEFAULT;
        if (xo.hasAttribute(CoercableMCMCOperator.AUTO_OPTIMIZE)) {
            if (xo.getBooleanAttribute(CoercableMCMCOperator.AUTO_OPTIMIZE)) {
                mode = CoercionMode.COERCION_ON;
            } else {
                mode = CoercionMode.COERCION_OFF;
            }
        }
        return mode;
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface CoercionModeAttribute {}
    public static final SimpleXMLObjectParser.XMLComponentFactory<CoercionModeAttribute> FACTORY =
            new SimpleXMLObjectParser.XMLComponentFactory<CoercionModeAttribute>(CoercionModeAttribute.class) {
                @Override
                public Class getParsedType() {
                    return CoercionMode.class;
                }
                @Override
                public SimpleXMLObjectParser.XMLComponent<CoercionMode> createXMLComponent(Class parameterType, CoercionModeAttribute annotation) {
                    return new SimpleXMLObjectParser.XMLComponent<CoercionMode>() {
                        @Override
                        public CoercionMode parse(XMLObject xo) throws XMLParseException {
                            return parseMode(xo);
                        }
                        @Override
                        public XMLSyntaxRule getSyntaxRule() {
                            return AttributeRule.newBooleanRule(CoercableMCMCOperator.AUTO_OPTIMIZE, true);
                        }
                    };
                }
            };

}
