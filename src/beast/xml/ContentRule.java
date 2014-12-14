/*
 * ContentRule.java
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

import java.util.Collections;
import java.util.Set;

/**
 * A syntax rule to ensure that allows one to document arbitrary content.
 */
public class ContentRule implements XMLSyntaxRule {

	/**
	 * Creates a optional element rule.
	 */
	public ContentRule(String htmlDescription) {
		this.htmlDescription = htmlDescription;
	}

	/**
	 * @return true
	 */
	public boolean isSatisfied(XMLObject xo) { return true; }

    public boolean containsAttribute(String name) {
        return false;
    }

    /**
	 * @return a string describing the rule.
	 */
	public String ruleString() { return htmlDescription; }

	/**
	 * @return a string describing the rule.
	 */
	public String htmlRuleString(XMLDocumentationHandler handler) {
		return htmlDescription;
	}

	/**
	 * @return a string describing the rule.
	 */
	public String wikiRuleString(XMLDocumentationHandler handler, String prefix) {
		return prefix + ":" + htmlDescription;
	}

	/**
	 * @return a string describing the rule.
	 */
	public String ruleString(XMLObject xo) { return null; }

	/**
	 * @return a set containing the optional types of this rule.
	 */
	public Set<Class> getRequiredTypes() { return Collections.EMPTY_SET; }

    public boolean isLegalElementName(String elementName) {
        return true;
    }

    public boolean isLegalSubelementName(String elementName) {
        return true;
    }

    public boolean isLegalElementClass(Class c) {
        return true;
    }

    public boolean isAttributeRule() { return false; }

	private final String htmlDescription;
}
