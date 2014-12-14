/*
 * GeneralDataType.java
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

package beast.evolution.datatype;

import beast.util.Identifiable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.ContentRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Implements a general DataType for any number of states
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: GeneralDataType.java,v 1.11 2005/05/24 20:25:56 rambaut Exp $
 */
public class GeneralDataType extends DataType implements Identifiable {

    public static final String GENERAL_DATA_TYPE = "generalDataType";
    public static final String DESCRIPTION = GENERAL_DATA_TYPE;
    public static final Type TYPE = Type.GENERAL;
    public static final GeneralDataType INSTANCE = new GeneralDataType();

    // for BEAUti trait PartitionSubstitutionModel
    public GeneralDataType() {}
    /**
     * Unlike the other standard data types, this general one has a public
     * constructor so multiple instances can be created.
     *
     * @param stateCodes the codes of the states
     */
    public GeneralDataType(final String[] stateCodes) {
        for (int i = 0; i < stateCodes.length; i++) {
            State state = new State(i, stateCodes[i]);
            states.add(state);
            stateMap.put(stateCodes[i], state);
        }
        stateCount = states.size();

        this.ambiguousStateCount = 0;

    }

    /**
     * Unlike the other standard data types, this general one has a public
     * constructor so multiple instances can be created.
     *
     * @param stateCodes the codes of the states
     */
    public GeneralDataType(final Collection<String> stateCodes) {
        int i = 0;
        for (String code : stateCodes) {
            State state = new State(i, code);
            states.add(state);
            stateMap.put(code, state);
            i++;
        }
        stateCount = states.size();

        this.ambiguousStateCount = 0;
    }

    /**
     * Add an alias (a state code that represents a particular state).
     * Note that all this does is put an extra entry in the stateNumbers
     * array.
     *
     * @param alias a string that represents the state
     * @param code the state number
     */
    public void addAlias(String alias, String code) {
        State state =stateMap.get(code);
        if (state == null) {
            throw new IllegalArgumentException("DataType doesn't contain the state, " + code);
        }
        stateMap.put(alias, state);
    }

    /**
     * Add an ambiguity (a state code that represents multiple states).
     *
     * @param code            a string that represents the state
     * @param ambiguousStates the set of states that this code refers to.
     */
    public void addAmbiguity(String code, String[] ambiguousStates) {

        int n = ambiguousStateCount + stateCount;

        int[] indices = new int[ambiguousStates.length];
        int i = 0;
        for (String stateCode : ambiguousStates) {
            State state =stateMap.get(stateCode);
            if (state == null) {
                throw new IllegalArgumentException("DataType doesn't contain the state, " + stateCode);
            }
            indices[i] = state.number;
            i++;
        }
        State state = new State(n, code, indices);
        states.add(state);
        ambiguousStateCount++;

        stateMap.put(code, state);
    }

    @Override
    public char[] getValidChars() {
        return null;
    }

    /**
     * Get state corresponding to a code
     *
     * @param code string code
     * @return state
     */
    public int getState(String code) {
        if (code.equals("?")) {
            return getUnknownState();
        }
        if (!stateMap.containsKey(code)) {
            return -1;
        }
        return stateMap.get(code).number;
    }

    /**
     * Override this function to cast to string codes...
     * @param c character
     *
     * @return
     */
    public int getState(char c) {
        return getState(String.valueOf(c));
    }
    /**
     * Get state corresponding to an unknown
     *
     * @return state
     */
    public int getUnknownState() {
        return stateCount + ambiguousStateCount;
    }

    /**
     * Get state corresponding to a gap
     *
     * @return state
     */
    public int getGapState() {
        return getUnknownState();
    }

    /**
     * Get character corresponding to a given state
     *
     * @param state state
     * @return corresponding code
     */
    public String getCode(int state) {
        return states.get(state).code;
    }

    /**
     * returns an array containing the non-ambiguous states
     * that this state represents.
     */
    public int[] getStates(int state) {

        return states.get(state).ambiguities;
    }

    /**
     * returns an array containing the non-ambiguous states that this state represents.
     */
    public boolean[] getStateSet(int state) {

        if (state >= states.size()) {
            throw new IllegalArgumentException("invalid state index");
        }
        State s = states.get(state);

        boolean[] stateSet = new boolean[stateCount];
        for (int i = 0; i < stateCount; i++)
            stateSet[i] = false;

        for (int i = 0, n = s.ambiguities.length; i < n; i++) {
            stateSet[s.ambiguities[i]] = true;
        }

        return stateSet;
    }

    /**
     * description of data type
     *
     * @return string describing the data type
     */
    public String getDescription() {
        if (id != null) {
            return id;
        } else {
            return DESCRIPTION;
        }
    }

    /**
     * type of data type
     *
     * @return integer code for the data type
     */
    public Type getType() {
        return TYPE;
    }

    // **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

    private String id = null;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    private List<State> states = new ArrayList<State>();
    private Map<String, State> stateMap = new TreeMap<String, State>();

    private class State {
        int number;
        String code;

        int[] ambiguities;

        State(int number, String code) {
            this.number = number;
            this.code = code;
            this.ambiguities = new int[]{number};
        }

        State(int number, String code, int[] ambiguities) {
            this.number = number;
			this.code = code;
			this.ambiguities = ambiguities;
		}
	}

    public static final XMLObjectParser<GeneralDataType> PARSER = new AbstractXMLObjectParser<GeneralDataType>() {
        public static final String GENERAL_DATA_TYPE = "generalDataType";
        public static final String STATE = "state";
        public static final String STATES = "states";
        public static final String ALIAS = "alias";
        public static final String AMBIGUITY = "ambiguity";
        public static final String CODE = "code";

        public String getParserName() { return GENERAL_DATA_TYPE; }

        public GeneralDataType parseXMLObject(XMLObject xo) throws XMLParseException {

            List<String> states = new ArrayList<String>();

            for (int i =0; i < xo.getChildCount(); i++) {
                if (xo.getChild(i) instanceof XMLObject) {
                    XMLObject cxo = (XMLObject)xo.getChild(i);

                    if (cxo.getName().equals(STATE)) {
                        states.add(cxo.getStringAttribute(CODE));
                    } else if (cxo.getName().equals(ALIAS)) {
                        // Do nothing just now
                    } else if (cxo.getName().equals(AMBIGUITY)) {
                        // Do nothing just now
                    } else {
                        throw new XMLParseException("illegal element, " + cxo.getName() + ", in " + getParserName() + " element");
                    }
                } else if (xo.getChild(i) instanceof Identifiable)  {
                    states.add(((Identifiable)xo.getChild(i)).getId());
                } else {
                    throw new XMLParseException("illegal element in " + getParserName() + " element");
                }
            }

            if (states.size() == 0) {
                throw new XMLParseException("No state elements defined in " + getParserName() + " element");
            } else if (states.size() < 2 ) {
                throw new XMLParseException("Less than two state elements defined in " + getParserName() + " element");
            }

            GeneralDataType dataType = new GeneralDataType(states);

            for (int i =0; i < xo.getChildCount(); i++) {
                if (xo.getChild(i) instanceof XMLObject) {
                    XMLObject cxo = (XMLObject)xo.getChild(i);
                    if (cxo.getName().equals(ALIAS)) {

                        String alias = cxo.getStringAttribute(CODE);
//                    if (alias.length() != 1) {
//                        throw new XMLParseException("State alias codes in " + getParserName() + " element must be exactly one character");
//                    }

                        String state = cxo.getStringAttribute(STATE);
//                    if (state.length() != 1) {
//                        throw new XMLParseException("State codes in " + getParserName() + " element must be exactly one character");
//                    }

                        try {
                            dataType.addAlias(alias, state);
                        } catch (IllegalArgumentException iae) {
                            throw new XMLParseException(iae.getMessage() + "in " + getParserName() + " element");
                        }

                    } else if (cxo.getName().equals(AMBIGUITY)) {

                        String code = cxo.getStringAttribute(CODE);
//                    if (code.length() != 1) {
//                        throw new XMLParseException("State ambiguity codes in " + getParserName() + " element must be exactly one character");
//                    }

                        String[] ambiguities = cxo.getStringArrayAttribute(STATES);
                        if (ambiguities.length == 1) {
                            String codes = ambiguities[0];
                            if (codes.length() < 2) {
                                throw new XMLParseException("States for ambiguity code in " + getParserName() + " element are not ambiguous");
                            }
                            ambiguities = new String[codes.length()];
                            for (int j = 0; j < codes.length(); j++) {
                                ambiguities[j] = String.valueOf(codes.charAt(j));
                            }
                        }

                        try {
                            dataType.addAmbiguity(code, ambiguities);
                        } catch (IllegalArgumentException iae) {
                            throw new XMLParseException(iae.getMessage() + "in " + getParserName() + " element");
                        }

                    }
                }
            }

            return dataType;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "Defines a general DataType for any number of states";
        }

        public String getExample() {
            return "<!-- The XML for a nucleotide data type under this scheme would be -->\n"+
                    "<generalDataType id=\"nucleotides\">\n"+
                    "	<state code=\"A\"/>\n"+
                    "	<state code=\"C\"/>\n"+
                    "	<state code=\"G\"/>\n"+
                    "	<state code=\"T\"/>\n"+
                    "	<alias code=\"U\" state=\"T\"/>\n"+
                    "	<ambiguity code=\"R\" states=\"AG\"/>\n"+
                    "	<ambiguity code=\"Y\" states=\"CT\"/>\n"+
                    "	<ambiguity code=\"?\" states=\"ACGT\"/>\n"+
                    "	<ambiguity code=\"-\" states=\"ACGT\"/>\n"+
                    "</generalDataType>\n";
        }

        public Class getReturnType() { return GeneralDataType.class; }

        public XMLSyntaxRule[] getSyntaxRules() { return rules; }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                new ElementRule(Identifiable.class, 0, Integer.MAX_VALUE),
                new ContentRule("<state code=\"X\"/>"),
                new ContentRule("<alias code=\"Y\" state=\"X\"/>"),
                new ContentRule("<ambiguity code=\"Z\" states=\"XY\"/>")
        };
    };

}
