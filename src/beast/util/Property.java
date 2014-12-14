/*
 * Property.java
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
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.lang.reflect.Method;


/**
 * Gets a property of another object using introspection.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: Property.java,v 1.19 2005/05/24 20:26:01 rambaut Exp $
 */
public class Property implements Attribute {

    private Object object = null;
    private Method getter = null;
    private Object argument = null;
    private String name = null;

    public Property(Object object, String name) {
        this(object, name, null);
    }

    public Property(Object object, String name, Object argument) {

        this.name = name;
        this.argument = argument;

        this.object = object;

        StringBuffer getterName = new StringBuffer("get");
        getterName.append(name.substring(0, 1).toUpperCase());
        getterName.append(name.substring(1));
        Class c = object.getClass();

        //System.out.println(getterName + "(" + argument + ")");

        try {
            if (argument != null)
                getter = c.getMethod(getterName.toString(), new Class[]{argument.getClass()});
            else
                getter = c.getMethod(getterName.toString(), (Class[]) null);
        } catch (NoSuchMethodException e) {
        }

    }

    public Method getGetter() {
        return getter;
    }

    //public Object getObject() { return object; }

    public String getAttributeName() {
        if (argument == null) return name;
        return name + "." + argument;
    }

    public Object getAttributeValue() {
        if (object == null || getter == null)
            return null;

        Object result = null;
        Object[] args = null;
        if (argument != null)
            args = new Object[]{argument};

        try {
            result = getter.invoke(object, args);
        } catch (Exception e) {

            e.printStackTrace(System.out);

            throw new RuntimeException(e.getMessage());
        }

        return result;
    }

    public String getPropertyName() {
        return name;
    }

    public String toString() {
        return getAttributeValue().toString();
    }

    public static final XMLObjectParser<Property> PARSER =
            new AbstractXMLObjectParser<Property>() {
                public String getParserName() {
                    return "property";
                }

                public Property parseXMLObject(XMLObject xo) throws XMLParseException {

                    Object object = xo.getChild(0);
                    String name = xo.getStringAttribute("name");

                    Property property;

                    if (xo.hasAttribute("index")) {
                        int index = xo.getIntegerAttribute("index");
                        property = new Property(object, name, index);
                    } else if (xo.hasAttribute("label")) {
                        String label = xo.getStringAttribute("label");
                        property = new Property(object, name, label);
                    } else {
                        property = new Property(object, name);
                    }

                    if (property.getGetter() == null)
                        throw new XMLParseException("unknown property, " + name + ", for object, " + object + ", in property element");

                    return property;
                }

                //************************************************************************
                // AbstractXMLObjectParser implementation
                //************************************************************************

                public String getParserDescription() {
                    return "This element returns an object representing the named property of the given child object.";
                }

                public Class<Property> getReturnType() {
                    return Property.class;
                }

                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
                        new StringAttributeRule("name", "name of the property", "length"),
                        new ElementRule(Object.class)
                };

            };

}
