/*
 * Statistic.java
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

package beast.inference.model;

import beast.inference.loggers.LogColumn;
import beast.inference.loggers.Loggable;
import beast.inference.loggers.NumberColumn;
import beast.util.Attribute;
import beast.util.Identifiable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ElementRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

/**
 * @author Alexei Drummond
 * @author Andrew Rambaut
 * @version $Id: Statistic.java,v 1.8 2005/05/24 20:26:00 rambaut Exp $
 */
public interface Statistic extends Attribute<double[]>, Identifiable, Loggable {

    public static final String NAME = "name";

    /**
     * @return the name of this statistic
     */
    String getStatisticName();

    /**
     * @param dim the dimension to return name of
     * @return the statistic's name for a given dimension
     */
    String getDimensionName(int dim);

    /**
     * Set the names of the dimensions (optional, by default they are named after the statistic).
     * @param names
     */
    void setDimensionNames(String[] names) ;

    /**
     * @return the number of dimensions that this statistic has.
     */
    int getDimension();

    /**
     * @param dim the dimension to return value of
     * @return the statistic's scalar value in the given dimension
     */
    double getStatisticValue(int dim);


    /**
     * Abstract base class for Statistics
     */
    public abstract class Abstract implements Statistic {

        private String name = null;

        public Abstract() {
            this.name = null;
        }

        public Abstract(String name) {
            this.name = name;
        }

        public String getStatisticName() {
            if (name != null) {
                return name;
            } else if (id != null) {
                return id;
            } else {
                return getClass().toString();
            }
        }

        public String getDimensionName(int dim) {
            if (getDimension() == 1) {
                return getStatisticName();
            } else {
                return getStatisticName() + Integer.toString(dim + 1);
            }
        }

        public void setDimensionNames(String[] names) {
            // do nothing
        }

      public String toString() {
            StringBuffer buffer = new StringBuffer(String.valueOf(getStatisticValue(0)));

            for (int i = 1; i < getDimension(); i++) {
                buffer.append(", ").append(String.valueOf(getStatisticValue(i)));
            }
            return buffer.toString();
        }

        // **************************************************************
        // Attribute IMPLEMENTATION
        // **************************************************************

        public final String getAttributeName() {
            return getStatisticName();
        }

        public double[] getAttributeValue() {
            double[] stats = new double[getDimension()];
            for (int i = 0; i < stats.length; i++) {
                stats[i] = getStatisticValue(i);
            }

            return stats;
        }

        // **************************************************************
        // Identifiable IMPLEMENTATION
        // **************************************************************

        protected String id = null;

        /**
         * @return the id.
         */
        public String getId() {
            return id;
        }

        /**
         * Sets the id.
         */
        public void setId(String id) {
            this.id = id;
        }

        // **************************************************************
        // Loggable IMPLEMENTATION
        // **************************************************************

        /**
         * @return the log columns.
         */
        public LogColumn[] getColumns() {
            LogColumn[] columns = new LogColumn[getDimension()];
            for (int i = 0; i < getDimension(); i++) {
                columns[i] = new StatisticColumn(getDimensionName(i), i);
            }
            return columns;
        }

        private class StatisticColumn extends NumberColumn {
            private final int dim;

            public StatisticColumn(String label, int dim) {
                super(label);
                this.dim = dim;
            }

            public double getDoubleValue() {
                return getStatisticValue(dim); }
		}
	}

    public static final XMLObjectParser<Statistic> PARSER =
            new AbstractXMLObjectParser<Statistic>() {

                public final static String STATISTIC = "statistic";

                public String getParserName() {
                    return STATISTIC;
                }

                public Statistic parseXMLObject(XMLObject xo) throws XMLParseException {

                    final StatisticList statList = (StatisticList) xo.getChild(StatisticList.class);
                    final String name = xo.getStringAttribute("name");
                    final Statistic stat = statList.getStatistic(name);
                    if (stat == null) {
                        StringBuffer buffer = new StringBuffer("Unknown statistic name, " + name + "\n");
                        buffer.append("Valid statistics are:");
                        for (int i = 0; i < statList.getStatisticCount(); i++) {
                            buffer.append("\n  ").append(statList.getStatistic(i).getStatisticName());
                        }
                        throw new XMLParseException(buffer.toString());
                    }

                    return stat;
                }

                //************************************************************************
                // AbstractXMLObjectParser implementation
                //************************************************************************

                public String getParserDescription() {
                    return "A statistic of a given name from the specified object.  ";
                }

                public Class<Statistic> getReturnType() {
                    return Statistic.class;
                }

                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                private final XMLSyntaxRule[] rules = {
                        new StringAttributeRule("name", "The name of the statistic you wish to extract from the given object"),
                        new ElementRule(StatisticList.class)
                };

            };
}
