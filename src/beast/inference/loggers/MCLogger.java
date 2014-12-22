/*
 * MCLogger.java
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

package beast.inference.loggers;

import beast.app.beast.BeastVersion;
import beast.math.MathUtils;
import beast.util.FileHelpers;
import beast.util.Identifiable;
import beast.util.Property;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.OrRule;
import beast.xml.StringAttributeRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLParser;
import beast.xml.XMLSyntaxRule;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A class for a general purpose logger.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: MCLogger.java,v 1.18 2005/05/24 20:25:59 rambaut Exp $
 */
public class MCLogger implements Logger {

    /**
     * Output performance stats in this log
     */
    private final boolean performanceReport;
    private final int performanceReportDelay;

    /**
     * Constructor. Will log every logEvery.
     *
     * @param formatter the formatter of this logger
     * @param logEvery  logging frequency
     */
    public MCLogger(LogFormatter formatter, int logEvery, boolean performanceReport, int performanceReportDelay) {

        addFormatter(formatter);
        this.logEvery = logEvery;
        this.performanceReport = performanceReport;
        this.performanceReportDelay = performanceReportDelay;
    }

    /**
     * Constructor. Will log every logEvery.
     *
     * @param formatter the formatter of this logger
     * @param logEvery  logging frequency
     */
    public MCLogger(LogFormatter formatter, int logEvery, boolean performanceReport) {
        this(formatter, logEvery, performanceReport, 0);
    }

    /**
     * Constructor. Will log every logEvery.
     *
     * @param logEvery logging frequency
     */
    public MCLogger(String fileName, int logEvery, boolean performanceReport, int performanceReportDelay) throws IOException {
        this(new TabDelimitedFormatter(new PrintWriter(new FileWriter(fileName))), logEvery, performanceReport, performanceReportDelay);
    }

    /**
     * Constructor. Will log every logEvery.
     *
     * @param logEvery logging frequency
     */
    public MCLogger(int logEvery) {
        this(new TabDelimitedFormatter(System.out), logEvery, true, 0);
    }

    public final void setTitle(String title) {
        this.title = title;
    }

    public final String getTitle() {
        return title;
    }

    public int getLogEvery() {
        return logEvery;
    }

    public void setLogEvery(int logEvery) {
        this.logEvery = logEvery;
    }

    public final void addFormatter(LogFormatter formatter) {

        formatters.add(formatter);
    }

    public final void add(Loggable loggable) {

        LogColumn[] columns = loggable.getColumns();

        for (LogColumn column : columns) {
            addColumn(column);
        }
    }

    public final void addColumn(LogColumn column) {

        columns.add(column);
    }

    public final void addColumns(LogColumn[] columns) {

        for (LogColumn column : columns) {
            addColumn(column);
        }
    }

    public final int getColumnCount() {
        return columns.size();
    }

    public final LogColumn getColumn(int index) {

        return columns.get(index);
    }

    public final String getColumnLabel(int index) {

        return columns.get(index).getLabel();
    }

    public final String getColumnFormatted(int index) {

        return columns.get(index).getFormatted();
    }

    protected void logHeading(String heading) {
        for (LogFormatter formatter : formatters) {
            formatter.logHeading(heading);
        }
    }

    protected void logLine(String line) {
        for (LogFormatter formatter : formatters) {
            formatter.logLine(line);
        }
    }

    protected void logLabels(String[] labels) {
        for (LogFormatter formatter : formatters) {
            formatter.logLabels(labels);
        }
    }

    protected void logValues(String[] values) {
        for (LogFormatter formatter : formatters) {
            formatter.logValues(values);
        }
    }

    public void startLogging() {

        for (LogFormatter formatter : formatters) {
            formatter.startLogging(title);
        }

        if (title != null) {
            logHeading(title);
        }

        if (logEvery > 0) {
            final int columnCount = getColumnCount();
            String[] labels = new String[columnCount + 1];

            labels[0] = "state";

            for (int i = 0; i < columnCount; i++) {
                labels[i + 1] = getColumnLabel(i);
            }

            logLabels(labels);
        }
    }

    public final void log(int state) {
        // just to prevent overriding of the old 32 bit signature
    }

    public void log(long state) {

        if (performanceReport && !performanceReportStarted && state >= performanceReportDelay) {
            startTime = System.currentTimeMillis();
            startState = state;
            formatter.setMaximumFractionDigits(2);
        }

        if (logEvery > 0 && (state % logEvery == 0)) {

            final int columnCount = getColumnCount();

            String[] values = new String[columnCount + (performanceReport ? 2 : 1)];

            values[0] = Long.toString(state);

            for (int i = 0; i < columnCount; i++) {
                values[i + 1] = getColumnFormatted(i);
            }

            if (performanceReport) {
                if (performanceReportStarted) {

                    long time = System.currentTimeMillis();

                    double hoursPerMillionStates = (double) (time - startTime) / (3.6 * (double) (state - startState));

                    String hpm = formatter.format(hoursPerMillionStates);
                    if (hpm.equals("0")) {
                        // test cases can run fast :)
                        hpm = formatter.format(1000 * hoursPerMillionStates);
                        values[columnCount + 1] = hpm + " hours/billion states";
                    } else {
                        values[columnCount + 1] = hpm + " hours/million states";
                    }

                } else {
                    values[columnCount + 1] = "-";
                }
            }

            logValues(values);
        }

        if (performanceReport && !performanceReportStarted && state >= performanceReportDelay) {
            performanceReportStarted = true;
        }

    }

    public void stopLogging() {

        for (LogFormatter formatter : formatters) {
            formatter.stopLogging();
        }
    }

    private String title = null;

    private ArrayList<LogColumn> columns = new ArrayList<LogColumn>();

    protected int logEvery = 0;

    public List<LogFormatter> getFormatters() {
        return formatters;
    }

    public void setFormatters(List<LogFormatter> formatters) {
        this.formatters = formatters;
    }

    protected List<LogFormatter> formatters = new ArrayList<LogFormatter>();

    private boolean performanceReportStarted = false;
    private long startTime;
    private long startState;

    private final NumberFormat formatter = NumberFormat.getNumberInstance();

    public static final XMLObjectParser<MCLogger> PARSER = new AbstractXMLObjectParser<MCLogger>() {

        public static final String LOG = "log";
        public static final String ECHO = "echo";
        public static final String ECHO_EVERY = "echoEvery";
        public static final String TITLE = "title";
        public static final String HEADER = "header";
        public static final String FILE_NAME = FileHelpers.FILE_NAME;
        public static final String FORMAT = "format";
        public static final String TAB = "tab";
        public static final String HTML = "html";
        public static final String PRETTY = "pretty";
        public static final String LOG_EVERY = "logEvery";
        public static final String ALLOW_OVERWRITE_LOG = "overwrite";

        public static final String COLUMNS = "columns";
        public static final String COLUMN = "column";
        public static final String LABEL = "label";
        public static final String SIGNIFICANT_FIGURES = "sf";
        public static final String DECIMAL_PLACES = "dp";
        public static final String WIDTH = "width";

        public String getParserName() {
            return LOG;
        }

        /**
         * @return an object based on the XML element it was passed.
         */
        public MCLogger parseXMLObject(XMLObject xo) throws XMLParseException {

            // You must say how often you want to log
            final int logEvery = xo.getIntegerAttribute(LOG_EVERY);

            final PrintWriter pw = getLogFile(xo, getParserName());

            final LogFormatter formatter = new TabDelimitedFormatter(pw);

            boolean performanceReport = false;

            if (!xo.hasAttribute(FILE_NAME)) {
                // is a screen log
                performanceReport = true;
            }

            // added a performance measurement delay to avoid the full evaluation period.
            final MCLogger logger = new MCLogger(formatter, logEvery, performanceReport, 10000);

            String title = null;
            if (xo.hasAttribute(TITLE)) {
                title = xo.getStringAttribute(TITLE);
            }

            String header = null;
            if (xo.hasAttribute(HEADER)) {
                header = xo.getStringAttribute(HEADER);
            }

            if (title == null) {
                final BeastVersion version = new BeastVersion();

                title = "BEAST " + version.getVersionString() +
                        " " + version.getBuildString() + "\n" +
                        (header != null ? header + "\n" : "") +
                        "Generated " + (new Date()).toString() + " [seed=" + MathUtils.getSeed() + "]";
            } else {
                if (header != null) {
                    title += "\n" + header;
                }
            }

            logger.setTitle(title);

            for (int i = 0; i < xo.getChildCount(); i++) {

                final Object child = xo.getChild(i);

                if (child instanceof Columns) {

                    logger.addColumns(((Columns) child).getColumns());

                } else if (child instanceof Loggable) {

                    logger.add((Loggable) child);

                } else if (child instanceof Identifiable) {

                    logger.addColumn(new LogColumn.Default(((Identifiable) child).getId(), child));

                } else if (child instanceof Property) {
                    logger.addColumn(new LogColumn.Default(((Property) child).getAttributeName(), child));
                } else {

                    logger.addColumn(new LogColumn.Default(child.getClass().toString(), child));
                }
            }

            return logger;
        }

        private PrintWriter getLogFile(XMLObject xo, String parserName) throws XMLParseException {
            return XMLParser.getFilePrintWriter(xo, parserName);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newIntegerRule(LOG_EVERY),
                AttributeRule.newBooleanRule(ALLOW_OVERWRITE_LOG, true),
                new StringAttributeRule(FILE_NAME,
                        "The name of the file to send log output to. " +
                                "If no file name is specified then log is sent to standard output", true),
                new StringAttributeRule(TITLE,
                        "The title of the log", true),
                new StringAttributeRule(HEADER,
                        "The subtitle of the log", true),
                new OrRule(
                        new XMLSyntaxRule[]{
                                new ElementRule(Columns.class, 1, Integer.MAX_VALUE),
                                new ElementRule(Loggable.class, 1, Integer.MAX_VALUE),
                                new ElementRule(Object.class, 1, Integer.MAX_VALUE)
                        }
                )
        };

        public String getParserDescription() {
            return "Logs one or more items at a given frequency to the screen or to a file";
        }

        public Class<MCLogger> getReturnType() {
            return MCLogger.class;
        }
    };

}
