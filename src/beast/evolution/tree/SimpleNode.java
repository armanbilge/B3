/*
 * SimpleNode.java
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

package beast.evolution.tree;


import beast.evolution.util.Date;
import beast.evolution.util.Taxon;
import beast.util.Attributable;
import beast.util.Attribute;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import beast.xml.XORRule;

import java.util.Iterator;

/**
 * A simple implementation of the Node interface.
 *
 * @version $Id: SimpleNode.java,v 1.40 2005/08/16 16:03:17 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public class SimpleNode implements NodeRef, Attributable {

	/** parent node */
	private SimpleNode parent;

	/** number of node */
	private int nodeNumber;

	/** height of this node */
	private double height;

	/** instantaneous rate at this node */
	private double rate;

	/** the taxon if node is external */
	private Taxon taxon = null;

	//
	// Private stuff
	//

	private SimpleNode[] child;

	/** constructor default node */
	public SimpleNode()
	{
		parent = null;
		child =null;
		height = 0.0;
		rate = 1.0;

		nodeNumber = 0;
	}
	
	public SimpleNode(SimpleNode node) {
		parent = null;

		setHeight(node.getHeight());
		setRate(node.getRate());
		setId(node.getId());		
		setNumber(node.getNumber());
		setTaxon(node.getTaxon());
		
		child = null;

		for (int i = 0; i < node.getChildCount(); i++) {
			addChild(new SimpleNode(node.getChild(i)));
		}
	}
	
	/** constructor used to clone a node and all children */
	public SimpleNode(Tree tree, NodeRef node)
	{
		parent = null;
		setHeight(tree.getNodeHeight(node));
		setRate(tree.getNodeRate(node));
        final int nodeNumber = node.getNumber();
        setId(tree.getTaxonId(nodeNumber));
		setNumber(nodeNumber);
		setTaxon(tree.getNodeTaxon(node));
		
		child = null;

		for (int i = 0; i < tree.getChildCount(node); i++) {
			addChild(new SimpleNode(tree, tree.getChild(node, i)));
		}
	}
	
	public SimpleNode getDeepCopy() {
        return new SimpleNode(this);
	}

	/**
	 * Returns the parent node of this node.
	 */
	public final SimpleNode getParent() {
		return parent;
	}
	
	/** Set the parent node of this node. */
	public void setParent(SimpleNode node) { parent = node; }

	/**
	 * Get the height of this node.
	 */
	public final double getHeight() {
		return height;
	}

	/**
	 * Set the height of this node.
	 */
	public final void setHeight(double value) {
		height = value;
	}

	/**
	 * Get the rate at this node.
	 */
	public final double getRate() {
		return rate;
	}

	/**
	 * Set the rate at this node.
	 */
	public final void setRate(double value) {
		rate = value;
	}

	public void setNumber(int n) {
		nodeNumber = n;
	}

	public int getNumber() {
		return nodeNumber;
	}

	public void setTaxon(Taxon taxon) {
		this.taxon = taxon;
	}

	public Taxon getTaxon() {
		return taxon;
	}

	/**
	 * get child node
	 *
	 * @param n number of child
	 *
	 * @return child node
	 */
	public SimpleNode getChild(int n)
	{
        assert 0 <= n && n < child.length;
		
		return child[n];
	}
	
	public boolean hasChild(SimpleNode node) { 
	
		for (int i = 0, n = getChildCount(); i < n; i++) {
			if (node == child[i]) return true;
		}
		return false;
	}
	
	/**
	 * add new child node
	 *
	 * @param n new child node
	 */
	public void addChild(SimpleNode n)
	{
		insertChild(n, getChildCount());
	}

	/**
	 * add new child node (insertion at a specific position)
	 *
	 * @param n new child node
	 + @param pos position
	 */
	public void insertChild(SimpleNode n, int pos)
	{
		int numChildren = getChildCount();

		SimpleNode[] newChild = new SimpleNode[numChildren + 1];

		for (int i = 0; i < pos; i++)
		{
			newChild[i] = child[i];
		}
		newChild[pos] = n;
		for (int i = pos; i < numChildren; i++)
		{
			newChild[i+1] = child[i];
		}

		child = newChild;

		n.setParent(this);
	}
	
	/**
	 * remove child
	 *
	 * @param n child to be removed
	 */
	public SimpleNode removeChild(SimpleNode n)
	{
		int numChildren = getChildCount();
		SimpleNode[] newChild = new SimpleNode[numChildren-1];

		int j = 0;
		boolean found = false;
		for (int i = 0; i < numChildren; i++) {
			if (child[i] != n) {
				newChild[j] = child[i];
				j++;
			} else 
				found = true;
		}
		
		if (!found)
			throw new IllegalArgumentException("Nonexistent child");
			
		//remove parent link from removed child!
		n.setParent(null);

		child = newChild;
		
		return n;
	}
	
	/**
	 * remove child
	 *
	 * @param n number of child to be removed
	 */
	public SimpleNode removeChild(int n)
	{
		int numChildren = getChildCount();

		if (n >= numChildren)
		{
			throw new IllegalArgumentException("Nonexistent child");
		}

		return removeChild(child[n]);
	}


    public void replaceChild(SimpleNode childNode, SimpleNode replacment) {
         for(int nc = 0; nc < child.length; ++nc) {
             if( child[nc] == childNode ) {
                 replacment.setParent(this);
                 child[nc] = replacment;
                 break;
             }
         }
    }

	/**
	 * check whether this node has any children
	 *
	 * @return result (true or false)
	 */
	public boolean hasChildren()
	{
		return (getChildCount() != 0);
	}

	/**
	 * check whether this node is an external node
	 *
	 * @return result (true or false)
	 */
	public boolean isExternal()	{
		return !hasChildren();
	}

	/**
	 * check whether this node is a root node
	 *
	 * @return result (true or false)
	 */
	public boolean isRoot()
	{
		return (getParent() == null);
	}


	/**
	 * Returns the number of children this node has.
	 */
	public final int getChildCount() {
		if (child == null) return 0;
		return child.length;
	}

    // **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

	private String id = null;

	/**
	 * @return the id as a string.
	 */
	public String getId() {
		return id;
	}

	/**
	 * set the id as a string.
	 */
	public void setId(String id) {
		this.id = id;
	}
	
    // **************************************************************
    // Attributable IMPLEMENTATION
    // **************************************************************

	private Attributable.AttributeHelper attributes = null;

	/**
	 * Sets an named attribute for this object.
	 * @param name the name of the attribute.
	 * @param value the new value of the attribute.
	 */
	public void setAttribute(String name, Object value) {
		if (attributes == null)
			attributes = new Attributable.AttributeHelper();
		attributes.setAttribute(name, value);
	}

	/**
	 * @return an object representing the named attributed for this object.
	 * @param name the name of the attribute of interest.
	 */
	public Object getAttribute(String name) {
		if (attributes == null)
			return null;
		else
			return attributes.getAttribute(name);
	}

	/**
	 * @return an iterator of the attributes that this object has.
	 */
	public Iterator<String> getAttributeNames() {
		if (attributes == null)
			return null;
		else
			return attributes.getAttributeNames();
	}

	public static final XMLObjectParser<SimpleNode> PARSER = new AbstractXMLObjectParser<SimpleNode>() {
		public final static String NODE = "node";
		public final static String HEIGHT = "height";
		public final static String RATE = "rate";

		public String getParserName() { return NODE; }

		public SimpleNode parseXMLObject(XMLObject xo) throws XMLParseException {

			SimpleNode node = new SimpleNode();

			Taxon taxon = null;

			if (xo.hasAttribute(HEIGHT)) {
				node.setHeight(xo.getDoubleAttribute(HEIGHT));
			}

			if (xo.hasAttribute(RATE)) {
				node.setRate(xo.getDoubleAttribute(RATE));
			}

			for (int i = 0; i < xo.getChildCount(); i++) {
				Object child = xo.getChild(i);
				if (child instanceof SimpleNode) {
					node.addChild((SimpleNode)child);
				} else if (child instanceof Taxon) {
					taxon = (Taxon)child;
				} else if (child instanceof Date) {
					node.setAttribute("date", child);
				} else if (child instanceof Attribute) {
					Attribute attr = (Attribute)child;
					String name = attr.getAttributeName();
					Object value = attr.getAttributeValue();
					node.setAttribute(name, value);
				} else if (child instanceof Attribute[]) {
					Attribute[] attrs = (Attribute[])child;
					for (int j =0; j < attrs.length; j++) {
						String name = attrs[j].getAttributeName();
						Object value = attrs[j].getAttributeValue();
						node.setAttribute(name, value);
					}
				} else if (child instanceof XMLObject) {
					XMLObject xoc = (XMLObject)child;
					if (xoc.getName().equals(Attributable.ATTRIBUTE)) {
						node.setAttribute(xoc.getStringAttribute(Attributable.NAME), xoc.getAttribute(Attributable.VALUE));
					} else {
						throw new XMLParseException("Unrecognized element" + xoc.getName() + " found in node element!");
					}
				} else {
					throw new XMLParseException("Unrecognized element found in node element!");
				}
			}

			if (taxon != null) {
				node.setTaxon(taxon);
			}

			return node;
		}

		public String getParserDescription() {
			return "This element represents a node in a tree.";
		}

		public Class getReturnType() { return SimpleNode.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }

		private final XMLSyntaxRule[] rules = {
				AttributeRule.newDoubleRule(HEIGHT, true, "the age of the node"),
				AttributeRule.newDoubleRule(RATE, true, "the relative rate of evolution at this node - default is 1.0"),
				new XORRule(
						new ElementRule(Taxon.class, "The taxon of this leaf node"),
						new ElementRule(SimpleNode.class, "The children of this internal node", 2, 2))

		};
	};
}
