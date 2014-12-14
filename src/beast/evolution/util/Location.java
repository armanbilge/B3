/*
 * Location.java
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

import beast.util.Identifiable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class Location implements Identifiable {

    public static final String LOCATION = "location";

    private Location(final String id, final int index, final String description, final double longitude, final double latitude) {
        this.id = id;
        this.index = index;
        this.description = description;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
//        this.id = id;
        // ignore setting...
    }

    public String getDescription() {
        return description;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return getId();
    }

    private final String id;
    private final int index;
    private final String description;

    private final double longitude;
    private final double latitude;

    // STATIC MEMBERS

    public static Location newLocation(String id, String description, double longitude, double latitude) {
        if (getLocation(id) != null) {
            throw new IllegalArgumentException("Location with id, " + id + ", already exists");
        }
        int index = getLocationCount();
        Location location =  new Location(id, index, description, longitude, latitude);
        locations.put(index, location);
        return location;
    }

    public static Location getLocation(String id) {
        for (Location location : locations.values()) {
            if (location.getId().equals(id)) {
                return location;
            }
        }
        return null;
    }

    public static int getLocationCount() {
        return locations.keySet().size();
    }

    public static Location getLocation(int index) {
        return locations.get(index);
    }

    static private Map<Integer, Location> locations = new TreeMap<Integer, Location>();

    public static final XMLObjectParser<Location> PARSER = new AbstractXMLObjectParser<Location>() {

        public static final String DESCRIPTION = "description";
        public static final String LONGITUDE = "longitude";
        public static final String LATITUDE = "latitude";

        public String getParserName() { return Location.LOCATION; }

        public Location parseXMLObject(XMLObject xo) throws XMLParseException {

            if (xo.getChildCount() > 0) {
                throw new XMLParseException("No child elements allowed in location element.");
            }

            String description = xo.getAttribute(DESCRIPTION, "");

            double longitude = parseLongLat(xo.getAttribute(LONGITUDE, ""));
            double latitude = parseLongLat(xo.getAttribute(LATITUDE, ""));

            return Location.newLocation(xo.getId(), description, longitude, latitude);
        }

        private double parseLongLat(final String value) throws XMLParseException {
            double d = 0.0;

            if (value != null && value.length() > 0) {
                try {
                    d = Double.parseDouble(value);
                } catch (NumberFormatException nfe) {
                    // @todo - parse degrees minutes and seconds
                }
            }

            return d;
        }

        public String getParserDescription() {
            return "Specifies a location with an optional longitude and latitude";
        }

        public XMLSyntaxRule[] getSyntaxRules() { return rules; }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                AttributeRule.newStringRule(DESCRIPTION, true,
                        "A description of this location"),
                AttributeRule.newStringRule(LONGITUDE, true,
                        "The longitude in degrees, minutes, seconds or decimal degrees"),
                AttributeRule.newStringRule(LATITUDE, true,
                        "The latitude in degrees, minutes, seconds or decimal degrees"),
        };

        public Class getReturnType() { return Location.class; }
    };
}
