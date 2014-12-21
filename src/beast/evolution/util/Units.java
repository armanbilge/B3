/*
 * Units.java
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

package beast.evolution.util;

import beast.xml.SimpleXMLObjectParser;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.io.Serializable;

/**
 * interface holding unit constants
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Units.java,v 1.17 2005/05/24 20:25:57 rambaut Exp $
 */
public interface Units extends Serializable {

    public static final String UNITS = "units";

    public static final XMLSyntaxRule UNITS_RULE = new StringAttributeRule("units", "the units", Type.values(), false);

    public enum Type {
        SUBSTITUTIONS, GENERATIONS, DAYS, MONTHS, YEARS;
        public String toString() {
            return super.toString().toLowerCase();
        }

        static {
            SimpleXMLObjectParser.registerXMLComponentFactory(new SimpleXMLObjectParser.XMLComponentFactory<UnitsAttribute>(UnitsAttribute.class) {
                @Override
                public Class getParsedType() {
                    return Type.class;
                }
                @Override
                public SimpleXMLObjectParser.XMLComponent<Type> createXMLComponent(Class parameterType, UnitsAttribute annotation) {
                    return new SimpleXMLObjectParser.XMLComponent<Type>() {
                        @Override
                        public Type parse(XMLObject xo) throws XMLParseException {
                            return parseUnitsAttribute(xo);
                        }
                        @Override
                        public XMLSyntaxRule getSyntaxRule() {
                            return UNITS_RULE;
                        }
                    };
                }
            });
        }
    }

    /**
     * @return the units for this object.
     */
    public Type getUnits();

    /**
     * Sets the units for this object.
     *
     * @param units to use
     */
    public void setUnits(Type units);

    public static Type parseUnitsAttribute(XMLObject xo) throws XMLParseException {
        Type units = Type.GENERATIONS;
        if (xo.hasAttribute(UNITS)) {
            String unitsAttr = (String) xo.getAttribute(UNITS);
            units = Type.valueOf(Type.class, unitsAttr.toUpperCase());
        }
        return units;
    }

    public @interface UnitsAttribute {}

}