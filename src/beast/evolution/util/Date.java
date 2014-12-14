/*
 * Date.java
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

import beast.util.Attribute;
import beast.util.NumberFormatter;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * A data class.
 *
 * @version $Id: Date.java,v 1.26 2005/05/24 20:25:57 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */
public class Date extends TimeScale implements Attribute { 

    public static final String DATE = "date";
    private double precision = 0.0;

    /**
     * Constructor for relative to origin
     * @param time the time in units relative to origin
     * @param units the units of the given time
     * @param backwards true if the time is earlier than the origin
     * @param origin the absolute origin at a Date.
     */
    public Date(double time, Type units, boolean backwards, java.util.Date origin) {
        super(units, backwards, origin);
        this.time = time;
    }

    /**
     * Constructor for an absolute date.
     * @param date the date
     */
    public Date(java.util.Date date) {
        super(Units.Type.YEARS, false);
        origin = -1970.0;
        initUsingDate(date);
    }

    /**
     * Constructor for an absolute date.
     * @param date the date
     * @param units the units
     */
    public Date(java.util.Date date, Type units) {
        super(units, false);
        initUsingDate(date);
    }

    /**
	 * Constructor for an absolute date with origin specified
     * @param date the date
     * @param units the units
     * @param origin the origin as a date
	 */
	public Date(java.util.Date date, Type units, java.util.Date origin) {
		super(units, false, origin);
		initUsingDate(date);
	}

	/**
     * Constructor of a relative age
     * @param time the time relative to arbitrary zero point.
     * @param units the units the time is measured in
     * @param backwards true of the time is earlier than the zero point.
     */
	public Date(double time, Type units, boolean backwards) {
		super(units, backwards);
		this.time = time;
	}
	
	/**
	 * Constructor for time a relative to origin
	 * @param origin the origin in given units from Jan 1st 1970
	 */
	private Date(double time, Type units, boolean backwards, double origin) {
		super(units, backwards, origin);
		this.time = time;
	}

	//************************************************************************
	// Factory methods
	//************************************************************************
	
	/**
	 * Create an age representing the given age (time ago) in the given units
	 */
	public static Date createRelativeAge(double age, Type units) {
		return new Date(age, units, true);
	}
	
	/**
	 * Create an age representing the given age (time ago) in the given units
	 * with an origin of the given date.
	 * The age represents the number units back in time from the origin.
	 */
	public static Date createTimeAgoFromOrigin(double age, Type units, java.util.Date origin) {
		return new Date(age, units, true, origin);
	}
	
	/**
	 * Create an age representing the given age (time ago) in the given units
	 * with an origin as the given number of units since 1970.
	 * The age represents the number units back in time from the origin.
	 */
	public static Date createTimeAgoFromOrigin(double age, Type units, double origin) {
		return new Date(age, units, true, origin);
	}
	
	/**
	 * Create an age representing the given age (time since) in the given units
	 * with an origin of the given date.
	 * The age represents the number units back in time from the origin.
	 */
	public static Date createTimeSinceOrigin(double age, Type units, java.util.Date origin) {
		return new Date(age, units, false, origin);
	}
	
	/**
	 * Create an age representing the given age (time since) in the given units
	 * with an origin as the given number of units since 1970.
	 * The age represents the number units forwards in time from the origin.
	 */
	public static Date createTimeSinceOrigin(double age, Type units, double origin) {
		return new Date(age, units, false, origin);
	}
		
	/**
	 * Create a date an sets units to Units.YEARS
	 */
	public static Date createDate(java.util.Date date) {
		return new Date(date, Units.Type.YEARS);
	}
	
	//************************************************************************
	// Private methods
	//************************************************************************
	
	private void initUsingDate(java.util.Date date) {

        // get the number of milliseconds this date is after the 1st January 1970
        long millisAhead = date.getTime();


		double daysAhead = ((double)millisAhead)/MILLIS_PER_DAY;

		switch (units) {
			case DAYS: time = daysAhead;
                break;
			case MONTHS: time = daysAhead / DAYS_PER_MONTH;
                break;
			case YEARS:
                //time = daysAhead / DAYS_PER_YEAR;
                // more precise (so 1st Jan 2013 is 2013.0)

                // to avoid timezone specific differences in date calculations, all dates and calendars are
                // set to GMT.
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                cal.setTime(date);

                int year = cal.get(Calendar.YEAR);
                long millis1 = cal.getTimeInMillis();

                cal.set(year, Calendar.JANUARY, 1, 0, 0);
                long millis2 = cal.getTimeInMillis();

                cal.set(year + 1, Calendar.JANUARY, 1, 0, 0);
                long millis3 = cal.getTimeInMillis();
                double fractionalYear = ((double)(millis1 - millis2)) / (millis3 - millis2);

                time = fractionalYear + year - 1970;
                break;
			default: throw new IllegalArgumentException();
		}


		if (time < getOrigin()) {
			time = getOrigin() - time;
			backwards = true;
		} else {
			time = time - getOrigin();
			backwards = false;
		}

        
	}
	
	/**
	 * Returns the time value that is relative to the origin
	 */
	public double getTimeValue() { 
		return time; 
	}
	
	/**
	 * Returns the absolute time value (i.e., relative to zero).
	 */
	public double getAbsoluteTimeValue() { 
		if (isBackwards()) {
			return getOrigin() - getTimeValue();
		}
		return getOrigin() + getTimeValue();
	}
	
	public boolean before(Date date) {
		double newTime = convertTime(date.getTimeValue(), date);
		if (isBackwards()) {
			return getTimeValue() > newTime;
		}
		return getTimeValue() < newTime; 
	}

	public boolean after(Date date) {
		double newTime = convertTime(date.getTimeValue(), date);
		if (isBackwards()) {
			return getTimeValue() < newTime;
		}
		return getTimeValue() > newTime; 
	}

	public boolean equals(Date date) {
		double newTime = convertTime(date.getTimeValue(), date);
		return getTimeValue() == newTime; 
	}

	public String getAttributeName() { return DATE; }
	
	public Object getAttributeValue() { return this; }
	
	public String toString() {
		if (isBackwards()) {
			return formatter.format(time).trim() + " " + unitString(time) + " ago";
		} else {
			return formatter.format(time).trim() + " " + unitString(time);
		}	
	}
	
	private double time;

	private NumberFormatter formatter = new NumberFormatter(5);

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getPrecision() {
        return precision;
    }

	public enum DateUnitsType {
		YEARS("units", "Years"), //
		MONTHS("units", "Months"),
		DAYS("days", "Days"), //
		FORWARDS("forwards", "Since some time in the past"), //
		BACKWARDS("backwards", "Before the present"); //

		DateUnitsType(String attr, String name) {
			this.attr = attr;
			this.name = name;
		}

		public String toString() {
			return name;
		}

		public String getAttribute() {
			return attr;
		}

		private final String attr;
		private final String name;
	}

	public static final XMLObjectParser<Date> PARSER = new AbstractXMLObjectParser<Date>() {

		public static final String VALUE = "value";
		public static final String UNITS = "units";
		public static final String ORIGIN = "origin";
		public static final String DIRECTION = "direction";

		public final String FORWARDS = DateUnitsType.FORWARDS.getAttribute(); //"forwards";
		public final String BACKWARDS = DateUnitsType.BACKWARDS.getAttribute(); //"backwards";

		public final String YEARS = DateUnitsType.YEARS.getAttribute(); //"units";
		public final String MONTHS = DateUnitsType.MONTHS.getAttribute(); //"units";
		public final String DAYS = DateUnitsType.DAYS.getAttribute(); //"days";

		public static final String PRECISION = "precision";

		public String getParserName() {
			return Date.DATE;
		}

		public Date parseXMLObject(XMLObject xo) throws XMLParseException {

			java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, java.util.Locale.UK);
			dateFormat.setLenient(true);

			if (xo.getChildCount() > 0) {
				throw new XMLParseException("No child elements allowed in date element.");
			}

			double value = 0.0;
			java.util.Date dateValue = null;

			if (xo.hasAttribute(VALUE)) {
				try {
					value = xo.getDoubleAttribute(VALUE);
				} catch (XMLParseException e) {
					String dateString = xo.getStringAttribute(VALUE);

					try {

						dateValue = dateFormat.parse(dateString);

					} catch (Exception ex) {
						throw new XMLParseException("value=" + dateString + " not recognised as a date, use DD/MM/YYYY");
					}
				}
			} else {
				throw new XMLParseException("Value attribute missing from date element.");
			}

			boolean backwards = false;

			if (xo.hasAttribute(DIRECTION)) {
				String direction = (String) xo.getAttribute(DIRECTION);
				if (direction.equals(BACKWARDS)) {
					backwards = true;
				}
			}

			Units.Type units = Units.parseUnitsAttribute(xo);

			Date date;

			if (xo.hasAttribute(ORIGIN)) {

				String originString = (String) xo.getAttribute(ORIGIN);
				java.util.Date origin;

				try {
					origin = dateFormat.parse(originString);
				} catch (Exception e) {
					throw new XMLParseException("origin=" + originString + " not recognised as a date, use DD/MM/YYYY");
				}

				if (dateValue != null) {
					date = new Date(dateValue, units, origin);
				} else {
					date = new Date(value, units, backwards, origin);
				}

			} else {

				// No origin specified so use default (1st Jan 1970)
				if (dateValue != null) {
					date = new Date(dateValue, units);
				} else {
					date = new Date(value, units, backwards);
				}
			}

			if (xo.hasAttribute(PRECISION)) {
				double precision = (Double)xo.getDoubleAttribute(PRECISION);
				date.setPrecision(precision);
			}


			return date;
		}

		public String getParserDescription() {
			return "Specifies a date on a given timescale";
		}

		public String getExample() {
			return
					"<!-- a date representing 10 years in the past                                 -->\n" +
							"<date value=\"10.0\" units=\"years\" direction=\"backwards\"/>\n" +
							"\n" +
							"<!-- a date representing 300 days after Jan 1st 1989                          -->\n" +
							"<date value=\"300.0\" origin=\"01/01/89\" units=\"days\" direction=\"forwards\"/>\n";
		}

		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
				new StringAttributeRule(VALUE,
						"The value of this date"),
				new StringAttributeRule(ORIGIN,
						"The origin of this time scale, which must be a valid calendar date", "01/01/01", true),
				new StringAttributeRule(UNITS, "The units of the timescale", new String[]{YEARS, MONTHS, DAYS}, true),
				new StringAttributeRule(DIRECTION, "The direction of the timescale", new String[]{FORWARDS, BACKWARDS}, true),
				AttributeRule.newDoubleRule(PRECISION, true, "The precision to which the date is specified"),
		};

		public Class getReturnType() {
			return Date.class;
		}
	};
}
