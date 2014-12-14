/*
 * Attribute.java
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

package beast.util;


import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.io.Serializable;

/**
 * An immutable attribute has a name and value.
 *
 * @author Alexei Drummond
 * @version $Id: Attribute.java,v 1.24 2005/05/24 20:26:01 rambaut Exp $
 */


public interface Attribute<T> extends Serializable {

    public final static String ATTRIBUTE = "att";
    public final static String NAME = "name";
    public final static String VALUE = "value";

    String getAttributeName();

    T getAttributeValue();

    public class Default<T> implements Attribute<T> {


        public Default(String name, T value) {
            this.name = name;
            this.value = value;
        }

        public String getAttributeName() {
            return name;
        }

        public T getAttributeValue() {
            return value;
        }

        public String toString() {
            return name + ": " + value;
        }

        private final String name;
        private final T value;
    }

    public static final XMLObjectParser<Attribute> ATTRIBUTE_PARSER =
            new AbstractXMLObjectParser<Attribute>() {
                public final static String ATTRIBUTE = "attr";
                public final static String NAME = "name";
                public final static String VALUE = "value";

                public String getParserName() { return ATTRIBUTE; }

                public Attribute parseXMLObject(XMLObject xo) throws XMLParseException {

                    final String name = xo.getStringAttribute(NAME);
                    if( xo.hasAttribute(VALUE) ) {
                        return new Attribute.Default<Object>(name, xo.getAttribute(VALUE));
                    }
                    final Object value = xo.getChild(0);

                    return new Attribute.Default<Object>(name, value);
                }

                //************************************************************************
                // AbstractXMLObjectParser implementation
                //************************************************************************

                public String getParserDescription() {
                    return "This element represents a name/value pair.";
                }

                public Class<Attribute> getReturnType() { return Attribute.class; }

                public XMLSyntaxRule[] getSyntaxRules() { return rules; }

                private final XMLSyntaxRule[] rules = {
                        new StringAttributeRule("name", "The name to give to this attribute"),
                        new ElementRule(Object.class )
                };
            };

    public static final XMLObjectParser<Attribute[]> ATTRIBUTES_PARSER =
            new AbstractXMLObjectParser<Attribute[]>() {
                public final static String ATTRIBUTES = "attributes";
                public final static String NAMES = "names";
                public final static String VALUES = "values";

                public String getParserName() { return ATTRIBUTES; }

                public Attribute[] parseXMLObject(XMLObject xo) throws XMLParseException {

                    String[] names = ((XMLObject)xo.getChild(NAMES)).getStringArrayChild(0);
                    String[] values =((XMLObject)xo.getChild(VALUES)).getStringArrayChild(0);

                    if (names.length != values.length) {
                        throw new XMLParseException("The number of names and values must match.");
                    }

                    Attribute[] attributes = new Attribute[names.length];
                    for (int i =0; i < attributes.length; i++) {
                        attributes[i] = new Attribute.Default(names[i], values[i]);
                    }

                    return attributes;
                }

                //************************************************************************
                // AbstractXMLObjectParser implementation
                //************************************************************************

                public String getParserDescription() {
                    return "This element represents an array of name/value pairs.";
                }

                public Class<Attribute[]> getReturnType() { return Attribute[].class; }

                public XMLSyntaxRule[] getSyntaxRules() { return rules; }

                private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                        AttributeRule.newStringArrayRule("names"),
                        AttributeRule.newStringArrayRule("values" )
                };
            };
}

