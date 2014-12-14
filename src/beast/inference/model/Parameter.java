/*
 * Parameter.java
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

import beast.math.MathUtils;
import beast.math.distributions.Distribution;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.Reference;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLParser;
import beast.xml.XMLSyntaxRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a multi-dimensional continuous parameter.
 *
 * @author Alexei Drummond
 * @version $Id: Parameter.java,v 1.22 2005/06/08 11:23:25 alexei Exp $
 */
public interface Parameter extends Statistic, Variable<Double> {

    /**
     * @param dim the index of the parameter dimension of interest
     * @return the parameter's scalar value in the given dimension
     */
    double getParameterValue(int dim);

    /**
     * @return the parameter's values  (may be modified, as this is a copy)
     */
    double[] getParameterValues();

    /**
     * sets the scalar value in the given dimension of this parameter
     *
     * @param dim   the index of the dimension to set
     * @param value the value to set
     */
    void setParameterValue(int dim, double value);

    /**
     * sets the scalar value in the given dimensin of this parameter to val, without firing any events
     *
     * @param dim   the index of the dimension to set
     * @param value the value to set
     */
    void setParameterValueQuietly(int dim, double value);


    /**
     * sets the scalar value in the given dimensin of this parameter to val,
     * and notifies that values in all dimension have been changed
     *
     * @param dim   the index of the dimension to set
     * @param value the value to set
     */
    void setParameterValueNotifyChangedAll(int dim, double value);

    /**
     * @return the name of this parameter
     */
    String getParameterName();

    /**
     * adds a parameter listener that is notified when this parameter changes.
     *
     * @param listener the listener
     */
    void addParameterListener(VariableListener listener);

    /**
     * removes a parameter listener.
     *
     * @param listener the listener
     */
    void removeParameterListener(VariableListener listener);

    /**
     * stores the state of this parameter for subsquent restore
     */
    void storeParameterValues();

    /**
     * restores the stored state of this parameter
     */
    void restoreParameterValues();

    /**
     * accepts the stored state of this parameter
     */
    void acceptParameterValues();

    /**
     * adopt the state of the source parameter
     *
     * @param source the parameter to adopt values from
     */
    void adoptParameterValues(Parameter source);

    /**
     * @return true if values in all dimensions are within their bounds
     */
    boolean isWithinBounds();

    /**
     * Can be called before store is called. If it results in new
     * dimensions, then the value of the first dimension is copied into the new dimensions.
     *
     * @param dim new dimension
     */
    void setDimension(int dim);

    /**
     * Adds new bounds to this parameter
     *
     * @param bounds to add
     */
    void addBounds(Bounds<Double> bounds);

    /**
     * @return the intersection of all bounds added to this parameter
     */
    Bounds<Double> getBounds();

    /**
     * Adds an extra dimension at the given index
     *
     * @param index Index of the dimension to add
     * @param value value to save at end of new array
     */
    public void addDimension(int index, double value);

    /**
     * Removes the specified dimension from parameter
     *
     * @param index Index of dimension to lose
     * @return the value of the dimension removed
     */
    public double removeDimension(int index);

    public void fireParameterChangedEvent();

    boolean isUsed();

    /**
     * Abstract base class for parameters
     */
    public abstract class Abstract extends Statistic.Abstract implements Parameter {

        protected Abstract() {
        }

        protected Abstract(final String name) {
            super(name);

        }

        // **************************************************************
        // MPI IMPLEMENTATION
        // **************************************************************


//        public void sendState(int toRank) {
//            double[] value = getParameterValues();
//            MPIServices.sendDoubleArray(value, toRank);
//        }
//
//
//        public void receiveState(int fromRank) {
//            final int length = getDimension();
//            double[] values = MPIServices.receiveDoubleArray(fromRank, length);
//            for (int i = 0; i < length; i++)
//                setParameterValueQuietly(i, values[i]);
//            this.fireParameterChangedEvent();
//        }

        public int getDimension() {
            return 1;
        }

        /**
         * Fired when all dimensions of the parameter have changed
         */
        public void fireParameterChangedEvent() {
            fireParameterChangedEvent(-1, Parameter.ChangeType.VALUE_CHANGED);
        }

        /**
         * Fired when a single dimension of the parameter has changed
         *
         * @param index which dimension changed
         * @param type  the type of parameter change event
         */
        public void fireParameterChangedEvent(int index, Parameter.ChangeType type) {
            if (listeners != null) {
                for (VariableListener listener : listeners) {
                    listener.variableChangedEvent(this, index, type);
                }
            }
        }

        public final void addParameterListener(VariableListener listener) {
            if (listeners == null) {
                listeners = new ArrayList<VariableListener>();
            }
            listeners.add(listener);
        }

        public final void removeParameterListener(VariableListener listener) {
            if (listeners != null) {
                listeners.remove(listener);
            }
        }

        public final String getStatisticName() {
            return getParameterName();
        }

        public final double getStatisticValue(int dim) {
            return getParameterValue(dim);
        }

        @Override
        public String getDimensionName(int dim) {
            if (dimensionNames == null) {
                return super.getDimensionName(dim);
            }
            return dimensionNames[dim];
        }

        public final void setDimensionNames(String[] names) {
            if (names != null && names.length != getDimension()) {
                throw new IllegalArgumentException("Length of dimension name array doesn't match the number of dimensions");
            }
            dimensionNames = names;
        }

        public void setDimension(int dim) {
            throw new UnsupportedOperationException();
        }

        /**
         * Defensively returns copy of parameter array.
         *
         * @return a copy of the parameter values
         */
        public double[] getParameterValues() {

            double[] copyOfValues = new double[getDimension()];
            for (int i = 0; i < copyOfValues.length; i++) {
                copyOfValues[i] = getParameterValue(i);
            }
            return copyOfValues;
        }


        public final void storeParameterValues() {
            if (isValid) {
                storeValues();

                isValid = false;
            }
        }

        public final void restoreParameterValues() {
            if (!isValid) {
                restoreValues();

                isValid = true;
            }
        }

        public final void acceptParameterValues() {
            if (!isValid) {
                acceptValues();

                isValid = true;
            }
        }

        public final void adoptParameterValues(Parameter source) {

            adoptValues(source);

            isValid = true;
        }

        public boolean isWithinBounds() {
            Bounds<Double> bounds = getBounds();
            for (int i = 0; i < getDimension(); i++) {
                final double value = getParameterValue(i);
                if (value < bounds.getLowerLimit(i) || value > bounds.getUpperLimit(i)) {
                    return false;
                }
            }
            return true;
        }

        // --------------------------------------------------------------------
        // IMPLEMENT VARIABLE
        // --------------------------------------------------------------------

        /**
         * @return the name of this variable.
         */
        public final String getVariableName() {
            return getParameterName();
        }

        public final Double getValue(int index) {
            return getParameterValue(index);
        }

        public final void setValue(int index, Double value) {
            setParameterValue(index, value);
        }

        public Double[] getValues() {
            Double[] copyOfValues = new Double[getDimension()];
            for (int i = 0; i < getDimension(); i++) {
                copyOfValues[i] = getValue(i);
            }
            return copyOfValues;
        }

        /**
         * @return the size of this variable - i.e. the length of the vector
         */
        public int getSize() {
            return getDimension();
        }

        /**
         * adds a parameter listener that is notified when this parameter changes.
         *
         * @param listener the listener
         */
        public final void addVariableListener(VariableListener listener) {
            addParameterListener(listener);
        }

        /**
         * removes a parameter listener.
         *
         * @param listener the listener
         */
        public final void removeVariableListener(VariableListener listener) {
            removeParameterListener(listener);
        }

        /**
         * stores the state of this parameter for subsquent restore
         */
        public void storeVariableValues() {
            storeParameterValues();
        }

        /**
         * restores the stored state of this parameter
         */
        public void restoreVariableValues() {
            restoreParameterValues();
        }

        /**
         * accepts the stored state of this parameter
         */
        public void acceptVariableValues() {
            acceptParameterValues();
        }

        public boolean isUsed() {
            return listeners != null && listeners.size() > 0;
        }

// --------------------------------------------------------------------

        protected abstract void storeValues();

        protected abstract void restoreValues();

        protected abstract void acceptValues();

        protected abstract void adoptValues(Parameter source);

        public String toString() {
            StringBuffer buffer = new StringBuffer(String.valueOf(getParameterValue(0)));
            Bounds bounds = null;
            try {
                bounds = getBounds();
            } catch (NullPointerException e) {
                //
            }
            final String id = getId();
            if (id != null) buffer.append(id);
            if (bounds != null) {
                buffer.append("=[").append(String.valueOf(bounds.getLowerLimit(0)));
                buffer.append(",").append(String.valueOf(bounds.getUpperLimit(0))).append("]");
            }

            for (int i = 1; i < getDimension(); i++) {
                buffer.append(", ").append(String.valueOf(getParameterValue(i)));
                if (bounds != null) {
                    buffer.append("[").append(String.valueOf(bounds.getLowerLimit(i)));
                    buffer.append(",").append(String.valueOf(bounds.getUpperLimit(i))).append("]");
                }
            }
            return buffer.toString();
        }

        public Element createElement(Document document) {
            throw new IllegalArgumentException();
        }

        private boolean isValid = true;

        private ArrayList<VariableListener> listeners;

        private String[] dimensionNames = null;
    }


    /**
     * A class that implements the Parameter interface.
     */
    class Default extends Abstract {

        public Default(String id, int dimension) {
            this(dimension);
            setId(id);
        }

        public Default(String id) {
            this(1); // dimension
            setId(id);
        }

        public Default(int dimension) {
            this(dimension, 1.0);
        }

        public Default(double initialValue) {
            values = new double[1];
            values[0] = initialValue;
            this.bounds = null;
        }

        /**
         * @param id           a unique id for this parameter
         * @param initialValue the initial value for this parameter
         * @param lower        the lower bound on this parameter
         * @param upper        the upper bound on this parameter
         */
        public Default(String id, double initialValue, double lower, double upper) {
            this(initialValue);
            setId(id);
            addBounds(new DefaultBounds(upper, lower, 1));
        }

        public Default(int dimension, double initialValue) {
            values = new double[dimension];
            for (int i = 0; i < dimension; i++) {
                values[i] = initialValue;
            }
            this.bounds = null;
        }

        public Default(String id, double[] values) {
            this(values);
            setId(id);
        }

        public Default(double[] values) {
            this.values = new double[values.length];
            System.arraycopy(values, 0, this.values, 0, values.length);
        }

        public Default(String id, int dimension, double initialValue) {
            this(dimension, initialValue);
            setId(id);
        }

        public void addBounds(Bounds<Double> boundary) {
            if (bounds == null) {
                bounds = boundary;
            } else {
                if (!(bounds instanceof IntersectionBounds)) {
                    IntersectionBounds newBounds = new IntersectionBounds(getDimension());
                    newBounds.addBounds(bounds);
                    bounds = newBounds;
                }

                ((IntersectionBounds) bounds).addBounds(boundary);
            }

            // can't change dimension after bounds are added!
            //hasBeenStored = true;
        }

        //********************************************************************
        // GETTERS
        //********************************************************************

        public final int getDimension() {
            return values.length;
        }

        public final int getSize() {
            return getDimension();
        }

        public final double getParameterValue(int i) {
            return values[i];
        }

        /**
         * Defensively returns copy of parameter array.
         *
         * @return a copy of the parameter values
         */
        public final double[] getParameterValues() {

            double[] copyOfValues = new double[values.length];
            System.arraycopy(values, 0, copyOfValues, 0, copyOfValues.length);
            return copyOfValues;
        }

        /**
         * Do not write to the returned array directly!!
         *
         * @return the parameter values
         */
        public final double[] inspectParameterValues() {
            return values;
        }

        public Bounds<Double> getBounds() {
            if (bounds == null) {
                throw new NullPointerException(getParameterName() + " parameter: Bounds not set");
            }
            return bounds;
        }

        public String getParameterName() {
            return getId();
        }

        //********************************************************************
        // SETTERS
        //********************************************************************

        /**
         * Can only be called before store is called. If it results in new
         * dimensions, then the value of the first dimension is copied into the new dimensions.
         */
        public void setDimension(int dim) {
            final int oldDim = getDimension();
            if (oldDim == dim) {
                return;
            }

            assert storedValues == null :
                    "Can't change dimension after store has been called! storedValues=" +
                            Arrays.toString(storedValues) + " bounds=" + bounds;


            double[] newValues = new double[dim];
            // copy over new values, min in case new dim is smaller
            System.arraycopy(values, 0, newValues, 0, Math.min(oldDim, dim));
            // fill new values with first item
            for (int i = oldDim; i < dim; i++) {
                newValues[i] = values[0];
            }
            values = newValues;

            if (bounds != null) {
                //assert oldDim < dim :  "Can't decrease dimension when bounds are set";
                for (int k = 1; k < oldDim; ++k) {
                    assert ((double) bounds.getLowerLimit(k) == bounds.getLowerLimit(0)) &&
                            ((double) bounds.getUpperLimit(k) == bounds.getUpperLimit(0)) :
                            "Can't change dimension when bounds are not all equal";
                }
                final double low = bounds.getLowerLimit(0);
                final double high = bounds.getUpperLimit(0);
                bounds = null;
                addBounds(low, high);
            }
        }

        /**
         * Adds an extra dimension to the end of values
         *
         * @param value value to save at end of new array
         */
        public void addDimension(int index, double value) {
            assert bounds == null;

            final int n = values.length;
            double[] newValues = new double[n + 1];
            System.arraycopy(values, 0, newValues, 0, index);
            newValues[index] = value;
            System.arraycopy(values, index, newValues, index + 1, n - index);
            values = newValues;
            fireParameterChangedEvent(index, Parameter.ChangeType.ADDED);
        }


        /**
         * Removes a single dimension from value array
         *
         * @param index Index of dimension to lose
         */
        public double removeDimension(int index) {
            assert bounds == null;

            final int n = values.length;
            final double value = values[index];

            final double[] newValues = new double[n - 1];
            System.arraycopy(values, 0, newValues, 0, index);
            System.arraycopy(values, index, newValues, index - 1, n - index);
            values = newValues;
            fireParameterChangedEvent(index, Parameter.ChangeType.REMOVED);
            return value;
        }


        public void setParameterValue(int i, double val) {
            values[i] = val;
            fireParameterChangedEvent(i, Parameter.ChangeType.VALUE_CHANGED);
        }

        /**
         * Sets the value of the parameter without firing a changed event.
         *
         * @param dim   the index of the parameter dimension
         * @param value the value to set
         */
        public void setParameterValueQuietly(int dim, double value) {
            values[dim] = value;
        }


        /**
         * Sets the values of the parameter and notify that all values of the parameter have changed.
         *
         * @param i   index of the value
         * @param val to value to set
         */
        public void setParameterValueNotifyChangedAll(int i, double val) {
            values[i] = val;
            fireParameterChangedEvent(i, Parameter.ChangeType.ALL_VALUES_CHANGED);
        }

        protected final void storeValues() {
            // no need to pay a price in a very common call for one-time rare usage
            //hasBeenStored = true;
            if (storedValues == null || storedValues.length != values.length) {
                storedValues = new double[values.length];
            }
            System.arraycopy(values, 0, storedValues, 0, storedValues.length);
        }

        protected final void restoreValues() {

            //swap the arrays
            double[] temp = storedValues;
            storedValues = values;
            values = temp;

            //if (storedValues != null) {
            //	System.arraycopy(storedValues, 0, values, 0, values.length);
            //} else throw new RuntimeException("restore called before store!");
        }

        /**
         * Nothing to do
         */
        protected final void acceptValues() {
        }

        protected final void adoptValues(Parameter source) {
            // todo bug ? bounds not adopted?

            if (getDimension() != source.getDimension()) {
                throw new RuntimeException("The two parameters don't have the same number of dimensions");
            }

            for (int i = 0, n = getDimension(); i < n; i++) {
                values[i] = source.getParameterValue(i);
            }
        }

        private double[] values;

        private double[] storedValues;

        // same as !storedValues && !bounds
        //private boolean hasBeenStored = false;
        private Bounds<Double> bounds = null;

        public void addBounds(double lower, double upper) {
            addBounds(new DefaultBounds(upper, lower, getDimension()));
        }
    }

    class DefaultBounds implements Bounds<Double> {

        public DefaultBounds(double upper, double lower, int dimension) {

            this.uppers = new double[dimension];
            this.lowers = new double[dimension];
            for (int i = 0; i < dimension; i++) {
                uppers[i] = upper;
                lowers[i] = lower;
            }
        }
//
//		public DefaultBounds(ArrayList<java.lang.Double> upperList, ArrayList<java.lang.Double> lowerList) {
//
//            final int length = upperList.size();
//            if (length != lowerList.size()) {
//				throw new IllegalArgumentException("upper and lower limits must be defined on the same number of dimensions.");
//			}
//			uppers = new double[length];
//			lowers = new double[length];
//			for (int i = 0; i < uppers.length; i++) {
//				uppers[i] = upperList.get(i);
//				lowers[i] = lowerList.get(i);
//			}
//		}

        public DefaultBounds(double[] uppers, double[] lowers) {

            if (uppers.length != lowers.length) {
                throw new IllegalArgumentException("upper and lower limits must be defined on the same number of dimensions.");
            }
            this.uppers = uppers;
            this.lowers = lowers;
        }

        public Double getUpperLimit(int i) {
            return uppers[i];
        }

        public Double getLowerLimit(int i) {
            return lowers[i];
        }

        public int getBoundsDimension() {
            return uppers.length;
        }

        public boolean isConstant() {
            return true;
        }

        private final double[] uppers, lowers;
    }

    final static Set<Parameter> FULL_PARAMETER_SET = new HashSet<Parameter>();

    public static final String VALUE = "value";
    public static final String UPPER = "upper";
    public static final String LOWER = "lower";

    public static final XMLObjectParser<Parameter> PARSER =
            new AbstractXMLObjectParser<Parameter>() {
                public static final String DIMENSION = "dimension";
                public static final String PARAMETER = "parameter";
                public static final String RANDOMIZE = "randomize";
                public static final String FILENAME = "fileName";
                public static final String BURNIN = "burnin";
                public static final String PARAMETERCOLUMN = "parameterColumn";

                public String getParserName() {
                    return PARAMETER;
                }

                public Parameter parseXMLObject(XMLObject xo) throws XMLParseException {

                    double[] values = null;
                    double[] uppers;
                    double[] lowers;

                    if( xo.hasAttribute(DIMENSION) ) {
                        values = new double[xo.getIntegerAttribute(DIMENSION)];
                    }

                    if ( xo.hasAttribute(FILENAME)) {
                        //read samples from a file (used for a reference prior) and calculate the mean
                        // TODO
                        throw new RuntimeException("No support for " + FILENAME + "attribute yet.");
//                        String fileName = xo.getStringAttribute(FILENAME);
//                        File file = new File(fileName);
//                        fileName = file.getName();
//                        String parent = file.getParent();
//                        if (!file.isAbsolute()) {
//                            parent = System.getProperty("user.dir");
//                        }
//                        file = new File(parent, fileName);
//                        fileName = file.getAbsolutePath();
//                        String columnName = "";
//                        if ( xo.hasAttribute(PARAMETERCOLUMN)) {
//                            columnName = xo.getStringAttribute(PARAMETERCOLUMN);
//                        } else {
//                            throw new XMLParseException("when providing a file name you must provide a parameter column as well");
//                        }
//                        //if a number for the burnin is not given, use 0 ...
//                        int burnin;
//                        if ( xo.hasAttribute(BURNIN)) {
//                            burnin = xo.getIntegerAttribute(BURNIN);
//                        } else {
//                            burnin = 0;
//                        }
//                        LogFileTraces traces = new LogFileTraces(fileName, file);
//                        List parameterSamples = null;
//                        try {
//                            traces.loadTraces();
//                            traces.setBurnIn(burnin);
//                            int traceIndexParameter = -1;
//                            for (int i = 0; i < traces.getTraceCount(); i++) {
//                                String traceName = traces.getTraceName(i);
//                                if (traceName.trim().equals(columnName)) {
//                                    traceIndexParameter = i;
//                                }
//                            }
//                            if (traceIndexParameter == -1) {
//                                throw new XMLParseException("Column '" + columnName + "' can not be found for " + getParserName() + " element.");
//                            }
//                            parameterSamples = traces.getValues(traceIndexParameter);
//                        } catch (TraceException e) {
//                            throw new XMLParseException(e.getMessage());
//                        } catch (IOException ioe) {
//                            throw new XMLParseException(ioe.getMessage());
//                        }
//                        values = new double[1];
//                        for (int i = 0, stop = parameterSamples.size(); i < stop; i++) {
//                            values[0] += ((Double)parameterSamples.get(i))/((double)stop);
//                        }
//                        System.out.println("Number of samples: " + parameterSamples.size());
//                        System.out.println("Parameter mean: " + values[0]);
                    } else if( xo.hasAttribute(VALUE) ) {
                        if( values == null ) {
                            values = xo.getDoubleArrayAttribute(VALUE);
                        } else {
                            double[] v = xo.getDoubleArrayAttribute(VALUE);
                            if( v.length == values.length ) {
                                System.arraycopy(v, 0, values, 0, v.length);
                            } else if( v.length == 1 ) {
                                for(int i = 0; i < values.length; i++) {
                                    values[i] = v[0];
                                }
                            } else {
                                throw new XMLParseException("value string must have 1 value or dimension values");
                            }
                        }
                    } else {
                        if( xo.hasAttribute(DIMENSION) ) {
                            values = new double[xo.getIntegerAttribute(DIMENSION)];
                        } else {
                            // parameter dimension will get set correctly by TreeModel presumably.
                            if (!xo.hasChildNamed(RANDOMIZE)) {
                                return new Parameter.Default(1);
                            }
                            values = new double[1];
                            values[0] = 1.0;
                        }
                    }

                    uppers = new double[values.length];
                    for(int i = 0; i < values.length; i++) {
                        uppers[i] = Double.POSITIVE_INFINITY;
                    }

                    lowers = new double[values.length];
                    for(int i = 0; i < values.length; i++) {
                        lowers[i] = Double.NEGATIVE_INFINITY;
                    }

                    if( xo.hasAttribute(UPPER) ) {
                        double[] v = xo.getDoubleArrayAttribute(UPPER);
                        if( v.length == uppers.length ) {
                            System.arraycopy(v, 0, uppers, 0, v.length);
                        } else if( v.length == 1 ) {
                            for(int i = 0; i < uppers.length; i++) {
                                uppers[i] = v[0];
                            }
                        } else {
                            throw new XMLParseException("uppers string must have 1 value or dimension values");
                        }
                    }

                    if( xo.hasAttribute(LOWER) ) {
                        double[] v = xo.getDoubleArrayAttribute(LOWER);
                        if( v.length == lowers.length ) {
                            System.arraycopy(v, 0, lowers, 0, v.length);
                        } else if( v.length == 1 ) {
                            for(int i = 0; i < lowers.length; i++) {
                                lowers[i] = v[0];
                            }
                        } else {
                            throw new XMLParseException("lowers string must have 1 value or dimension values");
                        }
                    }

                    //  assert uppers != null && lowers != null;

                    if( (uppers.length != values.length) ) {
                        throw new XMLParseException("value and upper limit strings have different dimension, in parameter");
                    }

                    if( (lowers.length != values.length) ) {
                        throw new XMLParseException("value and lower limit strings have different dimension, in parameter");
                    }

                    // check if uppers and lowers are consistent
                    for(int i = 0; i < values.length; i++) {
                        if( uppers[i] < lowers[i] ) {
                            throw new XMLParseException("upper is lower than lower, in parameter");
                        }
                    }

                    if (xo.hasChildNamed(RANDOMIZE)) {

                        Distribution distribution = (Distribution) xo.getChild(RANDOMIZE).getChild(Distribution.class);
                        for (int i = 0; i < values.length; i++) {
                            do {
                                // Not an efficient way to draw random variables, but this is currently the only general interface
                                values[i] = distribution.quantile(MathUtils.nextDouble());
                            } while (values[i] < lowers[i] || values[i] > uppers[i]);
                        }

                    } else {

                        // make values consistent with bounds
                        for(int i = 0; i < values.length; i++) {
                            if( uppers[i] < values[i] ) values[i] = uppers[i];
                        }

                        for(int i = 0; i < values.length; i++) {
                            if (lowers[i] > values[i]) values[i] = lowers[i];
                        }
                    }

                    Parameter param = new Parameter.Default(values.length);
                    for(int i = 0; i < values.length; i++) {
                        param.setParameterValue(i, values[i]);
                    }
                    param.addBounds(new Parameter.DefaultBounds(uppers, lowers));
                    return param;
                }

                public XMLSyntaxRule[] getSyntaxRules() {
                    return rules;
                }

                private final XMLSyntaxRule[] rules = {
                        AttributeRule.newDoubleArrayRule(VALUE, true),
                        AttributeRule.newIntegerRule(DIMENSION, true),
                        AttributeRule.newStringRule(FILENAME, true),
                        AttributeRule.newStringRule(PARAMETERCOLUMN, true),
                        AttributeRule.newIntegerRule(BURNIN, true),
                        AttributeRule.newDoubleArrayRule(UPPER, true),
                        AttributeRule.newDoubleArrayRule(LOWER, true),
                        new ElementRule(RANDOMIZE, new XMLSyntaxRule[] {
                                new ElementRule(Distribution.class),
                        },true),
                };


                public String getParserDescription() {
                    return "A real-valued parameter of one or more dimensions.";
                }

                public Class<Parameter> getReturnType() {
                    return Parameter.class;
                }

            };

    static public void replaceParameter(XMLObject xo, Parameter newParam) throws XMLParseException {

        for (int i = 0; i < xo.getChildCount(); i++) {

            if (xo.getChild(i) instanceof Parameter) {

                XMLObject rxo;
                Object obj = xo.getRawChild(i);

                if (obj instanceof Reference ) {
                    rxo = ((Reference) obj).getReferenceObject();
                } else if (obj instanceof XMLObject) {
                    rxo = (XMLObject) obj;
                } else {
                    throw new XMLParseException("object reference not available");
                }

                if (rxo.getChildCount() > 0) {
                    throw new XMLParseException("No child elements allowed in parameter element.");
                }

                if (rxo.hasAttribute(XMLParser.IDREF)) {
                    throw new XMLParseException("References to " + xo.getName() + " parameters are not allowed in treeModel.");
                }

                if (rxo.hasAttribute(VALUE)) {
                    throw new XMLParseException("Parameters in " + xo.getName() + " have values set automatically.");
                }

                if (rxo.hasAttribute(UPPER)) {
                    throw new XMLParseException("Parameters in " + xo.getName() + " have bounds set automatically.");
                }

                if (rxo.hasAttribute(LOWER)) {
                    throw new XMLParseException("Parameters in " + xo.getName() + " have bounds set automatically.");
                }

                if (rxo.hasAttribute(XMLParser.ID)) {
                    newParam.setId(rxo.getStringAttribute(XMLParser.ID));
                }

                rxo.setNativeObject(newParam);

                return;
            }
        }
    }

}
