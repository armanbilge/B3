/*
 * XMLSyntaxRule.java
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

package beast.xml;

import java.util.Set;

public interface XMLSyntaxRule {

	/**
	 * Returns true if the rule is satisfied for the given XML object.
	 */
	public boolean isSatisfied(XMLObject object);

    /**
     * Check if rule contains attribute of that name
     *
     * @param name attribute name
     * @return   true if contains attribute
     */
    public boolean containsAttribute(String name);

    /**
	 * Describes the rule in general.
	 */
	public String ruleString();

	/**
	 * Describes the rule in html.
	 */
	public String htmlRuleString(XMLDocumentationHandler handler);

	/**
	 * Describes the rule in wiki.
	 */
	public String wikiRuleString(XMLDocumentationHandler handler, String prefix);
	
	/**
	 * Describes the rule as pertains to the given object.
	 * In particular if object does not satisfy the rule then how.
	 */
	public String ruleString(XMLObject object);

	/**
	 * @return the classes potentially optional by this rule.
	 */
	public Set<Class> getRequiredTypes();

    /**
     *  Check for possible elements: catch typos, old syntax and elements with identical names to global
     *  xml element parsers.
     * @param elementName
     * @return true if rule allows a element with that name
     */
    boolean isLegalElementName(String elementName);

    /**
     *
     * @param c  class type
     * @return true if rule accepts an element which, after parsing, is represented as a class of type 'c'
     */
    boolean isLegalElementClass(Class c);

    /**
     *
     * @param elementName
     * @return true if rule allows a sub-element with that name
     */
    boolean isLegalSubelementName(String elementName);
}
