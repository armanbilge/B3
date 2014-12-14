/*
 * TreeModel.java
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

package beast.evomodel.tree;

import beast.evolution.tree.FlexibleTree;
import beast.evolution.tree.MultivariateTraitTree;
import beast.evolution.tree.MutableTree;
import beast.evolution.tree.MutableTreeListener;
import beast.evolution.tree.NodeRef;
import beast.evolution.tree.Tree;
import beast.evolution.util.MutableTaxonListListener;
import beast.evolution.util.Taxon;
import beast.evolution.util.TaxonList;
import beast.inference.model.AbstractModel;
import beast.inference.model.Bounds;
import beast.inference.model.CompoundParameter;
import beast.inference.model.Model;
import beast.inference.model.Parameter;
import beast.inference.model.Statistic;
import beast.inference.model.Variable;
import beast.util.Attributable;
import beast.xml.AbstractXMLObjectParser;
import beast.xml.AttributeRule;
import beast.xml.ElementRule;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParseException;
import beast.xml.XMLSyntaxRule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A model component for trees.
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: TreeModel.java,v 1.129 2006/01/05 17:55:47 rambaut Exp $
 */
public class TreeModel extends AbstractModel implements MultivariateTraitTree {

    //
    // Public stuff
    //

    public static final String TREE_MODEL = "treeModel";

    private static final boolean TEST_NODE_BOUNDS = false;

    public TreeModel(String name) {
        super(name);
        nodeCount = 0;
        externalNodeCount = 0;
        internalNodeCount = 0;
    }

    public TreeModel(Tree tree) {
        this(TREE_MODEL, tree, false);
    }

    public TreeModel(String id, Tree tree) {

        this(TREE_MODEL, tree, false);
        setId(id);
    }

    /* New constructor that copies the attributes of Tree tree into the new TreeModel
      * Useful for constructing a TreeModel from a NEXUS file entry
      */

    public TreeModel(String name, Tree tree, boolean copyAttributes) {

        super(name);

        // get a rooted version of the tree to clone
        FlexibleTree binaryTree = new FlexibleTree(tree, copyAttributes);
        binaryTree.resolveTree();

        // adjust the heights to be compatible with the tip dates and perturb
        // any zero branches.
        MutableTree.Utils.correctHeightsForTips(binaryTree);

        // clone the node structure (this will create the individual parameters)
        Node node = new Node(binaryTree, binaryTree.getRoot());

        internalNodeCount = binaryTree.getInternalNodeCount();
        externalNodeCount = binaryTree.getExternalNodeCount();

        nodeCount = internalNodeCount + externalNodeCount;

        nodes = new Node[nodeCount];
        storedNodes = new Node[nodeCount];

        int i = 0;
        int j = externalNodeCount;

        root = node;

        do {
            node = (Node) Tree.Utils.postorderSuccessor(this, node);

            if (node.isExternal()) {
                node.number = i;

                nodes[i] = node;
                storedNodes[i] = new Node();
                storedNodes[i].taxon = node.taxon;
                storedNodes[i].number = i;

                i++;
            } else {
                node.number = j;

                nodes[j] = node;
                storedNodes[j] = new Node();
                storedNodes[j].number = j;

                j++;
            }
        } while (node != root);

        // must be done here to allow programmatic running of BEAST
        setupHeightBounds();
    }


    boolean heightBoundsSetup = false;

    public void setupHeightBounds() {

        if (heightBoundsSetup) {
            throw new IllegalArgumentException("Node height bounds set up twice");
        }

        for (int i = 0; i < nodeCount; i++) {
            nodes[i].setupHeightBounds();
        }

        heightBoundsSetup = true;
    }

    /**
     * Push a tree changed event into the event stack.
     */
    public void pushTreeChangedEvent() {
        pushTreeChangedEvent(new TreeChangedEvent());
    }

    /**
     * Push a tree changed event into the event stack.
     */
    public void pushTreeChangedEvent(NodeRef nodeRef) {
        pushTreeChangedEvent(new TreeChangedEvent((Node) nodeRef));
    }

    /**
     * Push a tree changed event into the event stack.
     */
    public void pushTreeChangedEvent(Node node, Parameter parameter, int index) {
        pushTreeChangedEvent(new TreeChangedEvent(node, parameter, index));
    }

    /**
     * Push a tree changed event into the event stack.
     */
    public void pushTreeChangedEvent(TreeChangedEvent event) {
        if (inEdit) {
            treeChangedEvents.add(event);
        } else {
            listenerHelper.fireModelChanged(this, event);
        }
    }


    protected void handleModelChangedEvent(Model model, Object object, int index) {
        // no submodels so nothing to do
    }

    /**
     * Called when a parameter changes.
     */
    public void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        final Node node = getNodeOfParameter((Parameter) variable);
        if (type == Parameter.ChangeType.ALL_VALUES_CHANGED) {
            //this signals events where values in all dimensions of a parameter is changed.
            pushTreeChangedEvent(new TreeChangedEvent(node, (Parameter) variable, TreeChangedEvent.CHANGE_IN_ALL_INTERNAL_NODES));
        } else {
            pushTreeChangedEvent(node, (Parameter) variable, index);
        }
    }


    private final List<TreeChangedEvent> treeChangedEvents = new ArrayList<TreeChangedEvent>();

    public boolean hasRates() {
        return hasRates;
    }

    public boolean inTreeEdit() {
        return inEdit;
    }

    public class TreeChangedEvent {
        static final int CHANGE_IN_ALL_INTERNAL_NODES = -2;

        final Node node;
        final Parameter parameter;
        final int index;

        public TreeChangedEvent() {
            this(null, null, -1);
        }

        public TreeChangedEvent(Node node) {
            this(node, null, -1);
        }

        public TreeChangedEvent(Node node, Parameter parameter, int index) {
            this.node = node;
            this.parameter = parameter;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public Node getNode() {
            return node;
        }

        public Parameter getParameter() {
            return parameter;
        }

        public boolean isTreeChanged() {
            return parameter == null;
        }

        public boolean isNodeChanged() {
            return node != null;
        }

        public boolean isNodeParameterChanged() {
            return parameter != null;
        }

        public boolean isHeightChanged() {
            return parameter == node.heightParameter;
        }

        public boolean isRateChanged() {
            return parameter == node.rateParameter;
        }

        public boolean isTraitChanged(String name) {
            return parameter == node.traitParameters.get(name);
        }

        public boolean areAllInternalHeightsChanged() {
            if (parameter != null) {
                return parameter == node.heightParameter && index == CHANGE_IN_ALL_INTERNAL_NODES;
            }
            return false;
        }

    }

    // *****************************************************************
    // Interface Tree
    // *****************************************************************

    /**
     * Return the units that this tree is expressed in.
     */
    public Type getUnits() {
        return units;
    }

    /**
     * Sets the units that this tree is expressed in.
     */
    public void setUnits(Type units) {
        this.units = units;
    }

    /**
     * @return a count of the number of nodes (internal + external) in this
     *         tree.
     */
    public int getNodeCount() {
        return nodeCount;
    }

    public boolean hasNodeHeights() {
        return true;
    }

    public double getNodeHeight(NodeRef node) {
        return ((Node) node).getHeight();
    }

    public final double getNodeHeightUpper(NodeRef node) {
        return ((Node) node).heightParameter.getBounds().getUpperLimit(0);
    }

    public final double getNodeHeightLower(NodeRef node) {
        return ((Node) node).heightParameter.getBounds().getLowerLimit(0);
    }


    /**
     * @param node
     * @return the rate parameter associated with this node.
     */
    public double getNodeRate(NodeRef node) {
        if (!hasRates) {
            return 1.0;
        }
        return ((Node) node).getRate();
    }

    public Object getNodeAttribute(NodeRef node, String name) {

        if (name.equals("rate")) {
            return getNodeRate(node);
        }

        return null;
    }

    public Iterator getNodeAttributeNames(NodeRef node) {
        return new Iterator() {

            int i = 0;
            String[] attributes = {"rate"};

            public boolean hasNext() {
                return i < attributes.length;
            }

            public Object next() {
                return attributes[i++];
            }

            public void remove() {
                throw new UnsupportedOperationException("can't remove from this iterator!");
            }
        };
    }

    public boolean hasNodeTraits() {
        return hasTraits;
    }

    public Map<String, Parameter> getTraitMap(NodeRef node) {
        if (!hasTraits) throw new IllegalArgumentException("Trait parameters have not been created");
        return ((Node) node).getTraitMap();
    }

    public double getNodeTrait(NodeRef node, String name) {
        if (!hasTraits) throw new IllegalArgumentException("Trait parameters have not been created");
        return ((Node) node).getTrait(name);
    }

    public Parameter getNodeTraitParameter(NodeRef node, String name) {
        if (!hasTraits) throw new IllegalArgumentException("Trait parameters have not been created");
        return ((Node) node).getTraitParameter(name);
    }

    public double[] getMultivariateNodeTrait(NodeRef node, String name) {
        if (!hasTraits) throw new IllegalArgumentException("Trait parameters have not been created");
        return ((Node) node).getMultivariateTrait(name);
    }

    public final void swapAllTraits(NodeRef node1, NodeRef node2) {
        if (!hasTraits) throw new IllegalArgumentException("Trait parameters have not been created");
        swapAllTraits((Node) node1, (Node) node2);
    }

    public Taxon getNodeTaxon(NodeRef node) {
        return ((Node) node).taxon;
    }

    public void setNodeTaxon(NodeRef node, Taxon taxon) {
        ((Node) node).taxon = taxon;
    }

    public boolean isExternal(NodeRef node) {
        return ((Node) node).isExternal();
    }

    public boolean isRoot(NodeRef node) {
        return (node == root);
    }

    public int getChildCount(NodeRef node) {
        return ((Node) node).getChildCount();
    }

    public NodeRef getChild(NodeRef node, int i) {
        return ((Node) node).getChild(i);
    }

    public NodeRef getParent(NodeRef node) {
        return ((Node) node).parent;
    }

    public boolean hasBranchLengths() {
        return true;
    }

    public double getBranchLength(NodeRef node) {
        NodeRef parent = getParent(node);
        if (parent == null) {
            return 0.0;
        }

        return getNodeHeight(parent) - getNodeHeight(node);
    }

    public NodeRef getExternalNode(int i) {
        return nodes[i];
    }

    public NodeRef getInternalNode(int i) {
        return nodes[i + externalNodeCount];
    }

    public NodeRef getNode(int i) {
        return nodes[i];
    }

    public NodeRef[] getNodes() {
        return nodes;
    }

    /**
     * Returns the number of external nodes.
     */
    public int getExternalNodeCount() {
        return externalNodeCount;
    }

    /**
     * Returns the ith internal node.
     */
    public int getInternalNodeCount() {
        return internalNodeCount;
    }

    /**
     * Returns the root node of this tree.
     */
    public NodeRef getRoot() {
        return root;
    }

    // *****************************************************************
    // Interface MutableTree
    // *****************************************************************

    /**
     * Set a new node as root node.
     */
    public final void setRoot(NodeRef newRoot) {

        if (!inEdit) throw new RuntimeException("Must be in edit transaction to call this method!");

        root = (Node) newRoot;

        // We shouldn't need this because the addChild will already have fired appropriate events.
        pushTreeChangedEvent(root);
    }

    public void addChild(NodeRef p, NodeRef c) {

        if (!inEdit) throw new RuntimeException("Must be in edit transaction to call this method!");

        Node parent = (Node) p;
        Node child = (Node) c;
        if (parent.hasChild(child)) throw new IllegalArgumentException("Child already exists in parent");

        parent.addChild(child);
        pushTreeChangedEvent(parent);
    }

    public void removeChild(NodeRef p, NodeRef c) {

        if (!inEdit) throw new RuntimeException("Must be in edit transaction to call this method!");

        Node parent = (Node) p;
        Node child = (Node) c;

        parent.removeChild(child);
    }

    public void replaceChild(NodeRef node, NodeRef child, NodeRef newChild) {
        throw new RuntimeException("Unimplemented");
    }

    private Node oldRoot;

    public boolean beginTreeEdit() {
        if (inEdit) throw new RuntimeException("Alreading in edit transaction mode!");

        oldRoot = root;

        inEdit = true;

        return false;
    }

    public void endTreeEdit() {
        if (!inEdit) throw new RuntimeException("Not in edit transaction mode!");

        inEdit = false;

        if (root != oldRoot) {
            swapParameterObjects(oldRoot, root);
        }

        if (TEST_NODE_BOUNDS) {
            try {
                checkTreeIsValid();
            } catch (InvalidTreeException ite) {
                throw new RuntimeException(ite.getMessage());
            }
        }

        for (TreeChangedEvent treeChangedEvent : treeChangedEvents) {
            listenerHelper.fireModelChanged(this, treeChangedEvent);
        }
        treeChangedEvents.clear();
    }

    public void checkTreeIsValid() throws MutableTree.InvalidTreeException {
        for (Node node : nodes) {
            if (!node.heightParameter.isWithinBounds()) {
                throw new InvalidTreeException("height parameter out of bounds");
            }
        }
    }

    public void setNodeHeight(NodeRef n, double height) {
        ((Node) n).setHeight(height);
    }


    public void setNodeRate(NodeRef n, double rate) {
        if (!hasRates) throw new IllegalArgumentException("Rate parameters have not been created");
        ((Node) n).setRate(rate);

    }

    public void setNodeTrait(NodeRef n, String name, double value) {
        if (!hasTraits) throw new IllegalArgumentException("Trait parameters have not been created");
        ((Node) n).setTrait(name, value);
    }

    public void setMultivariateTrait(NodeRef n, String name, double[] value) {
        if (!hasTraits) throw new IllegalArgumentException("Trait parameters have not been created");
        ((Node) n).setMultivariateTrait(name, value);
    }

    public void setBranchLength(NodeRef node, double length) {
        throw new UnsupportedOperationException("TreeModel cannot have branch lengths set");
    }

    public void setNodeAttribute(NodeRef node, String name, Object value) {
        throw new UnsupportedOperationException("TreeModel does not use NodeAttributes");
    }

    // *****************************************************************
    // Interface ModelComponent
    // *****************************************************************

    /**
     * Store current state
     */
    protected void storeState() {

        copyNodeStructure(storedNodes);
        storedRootNumber = root.getNumber();

    }

    /**
     * Restore the stored state
     */
    protected void restoreState() {

        Node[] tmp = storedNodes;
        storedNodes = nodes;
        nodes = tmp;

        root = nodes[storedRootNumber];
    }

    /**
     * accept the stored state
     */
    protected void acceptState() {
    } // nothing to do

    /**
     * Copies the node connections from this TreeModel's nodes array to the
     * destination array. Basically it connects up the nodes in destination
     * in the same way as this TreeModel is set up. This method is package
     * private.
     */
    void copyNodeStructure(Node[] destination) {

        if (nodes.length != destination.length) {
            throw new IllegalArgumentException("Node arrays are of different lengths");
        }

        for (int i = 0, n = nodes.length; i < n; i++) {
            Node node0 = nodes[i];
            Node node1 = destination[i];

            // the parameter values are automatically stored and restored
            // just need to keep the links
            node1.heightParameter = node0.heightParameter;
            node1.rateParameter = node0.rateParameter;
            node1.traitParameters = node0.traitParameters;

            if (node0.parent != null) {
                node1.parent = storedNodes[node0.parent.getNumber()];
            } else {
                node1.parent = null;
            }

            if (node0.leftChild != null) {
                node1.leftChild = storedNodes[node0.leftChild.getNumber()];
            } else {
                node1.leftChild = null;
            }

            if (node0.rightChild != null) {
                node1.rightChild = storedNodes[node0.rightChild.getNumber()];
            } else {
                node1.rightChild = null;
            }
        }
    }

    /**
     * Copies a different tree into the current treeModel. Needs to reconnect
     * the existing internal and external nodes, taking into account that the
     * node numbers of the external nodes may differ between the two trees.
     */
    public void adoptTreeStructure(Tree donor) {

        /*System.err.println("internalNodeCount: " + this.internalNodeCount);
          System.err.println("externalNodeCount: " + this.externalNodeCount);
          for (int i = 0; i < this.nodeCount; i++) {
              System.err.println(nodes[i]);
          }*/

        //first remove all the child nodes of the internal nodes
        for (int i = this.externalNodeCount; i < this.nodeCount; i++) {
            int childCount = nodes[i].getChildCount();
            for (int j = 0; j < childCount; j++) {
                nodes[i].removeChild(j);
            }
        }

        // set-up nodes in this.nodes[] to mirror connectedness in donor via a simple recursion on donor.getRoot()
        addNodeStructure(donor, donor.getRoot());

        //Tree donor has no rates nor traits, only heights

    }

    /**
     * Recursive algorithm to copy a proposed tree structure into the current treeModel.
     */
    private void addNodeStructure(Tree donorTree, NodeRef donorNode) {

        NodeRef acceptorNode = null;
        if (donorTree.isExternal(donorNode)) {
            //external nodes can have different numbers between both trees
            acceptorNode = this.nodes[this.getTaxonIndex(donorTree.getTaxonId(donorNode.getNumber()))];
        } else {
            //not really important for internal nodes
            acceptorNode = this.nodes[donorNode.getNumber()];
        }

        setNodeHeight(acceptorNode, donorTree.getNodeHeight(donorNode));

        //removing all child nodes up front currently works
        //((Node)acceptorNode).leftChild = null;
        //((Node)acceptorNode).rightChild = null;
        /*int nrChildren = getChildCount(acceptorNode);
          for (int i = 0; i < nrChildren; i++) {
              this.removeChild(acceptorNode, this.getChild(acceptorNode, i));
          }*/

        for (int i = 0; i < donorTree.getChildCount(donorNode); i++) {
            //add a check when the added child is an external node
            if (donorTree.isExternal(donorTree.getChild(donorNode, i))) {
                addChild(acceptorNode, this.nodes[this.getTaxonIndex(donorTree.getTaxonId(donorTree.getChild(donorNode, i).getNumber()))]);
            } else {
                addChild(acceptorNode, this.nodes[donorTree.getChild(donorNode, i).getNumber()]);
            }
        }

        pushTreeChangedEvent(acceptorNode);

        if (!donorTree.isExternal(donorNode)) {
            for (int i = 0; i < donorTree.getChildCount(donorNode); i++) {
                addNodeStructure(donorTree, donorTree.getChild(donorNode, i));
            }
        }

    }

    /**
     * @return the number of statistics of this component.
     */
    public int getStatisticCount() {
        return super.getStatisticCount() + 1;
    }

    /**
     * @return the ith statistic of the component
     */
    public Statistic getStatistic(int i) {
        if (i == super.getStatisticCount()) return root.heightParameter;
        return super.getStatistic(i);
    }

//    public String getModelComponentName() {
//        return TREE_MODEL;
//    }

    // **************************************************************
    // TaxonList IMPLEMENTATION
    // **************************************************************

    /**
     * @return a count of the number of taxa in the list.
     */
    public int getTaxonCount() {
        return getExternalNodeCount();
    }

    /**
     * @return the ith taxon in the list.
     */
    public Taxon getTaxon(int taxonIndex) {
        return ((Node) getExternalNode(taxonIndex)).taxon;
    }

    /**
     * @return the ID of the taxon of the ith external node. If it doesn't have
     *         a taxon, returns the ID of the node itself.
     */
    public String getTaxonId(int taxonIndex) {
        Taxon taxon = getTaxon(taxonIndex);
        if (taxon != null) {
            return taxon.getId();
        } else {
            return null;
        }
    }

    /**
     * returns the index of the taxon with the given id.
     */
    public int getTaxonIndex(String id) {
        for (int i = 0, n = getTaxonCount(); i < n; i++) {
            if (getTaxonId(i).equals(id)) return i;
        }
        return -1;
    }

    /**
     * returns the index of the given taxon.
     */
    public int getTaxonIndex(Taxon taxon) {
        for (int i = 0, n = getTaxonCount(); i < n; i++) {
            if (getTaxon(i) == taxon) return i;
        }
        return -1;
    }

    public List<Taxon> asList() {
        List<Taxon> taxa = new ArrayList<Taxon>();
        for (int i = 0, n = getTaxonCount(); i < n; i++) {
            taxa.add(getTaxon(i));
        }
        return taxa;
    }

    public Iterator<Taxon> iterator() {
        return new Iterator<Taxon>() {
            private int index = -1;

            public boolean hasNext() {
                return index < getTaxonCount() - 1;
            }

            public Taxon next() {
                index++;
                return getTaxon(index);
            }

            public void remove() { /* do nothing */ }
        };
    }

    /**
     * @param taxonIndex the index of the taxon whose attribute is being fetched.
     * @param name       the name of the attribute of interest.
     * @return an object representing the named attributed for the taxon of the given
     *         external node. If the node doesn't have a taxon then the nodes own attribute
     *         is returned.
     */
    public Object getTaxonAttribute(int taxonIndex, String name) {
        Taxon taxon = getTaxon(taxonIndex);
        if (taxon != null) {
            return taxon.getAttribute(name);
        }
        return null;
    }

    // **************************************************************
    // MutableTaxonList IMPLEMENTATION
    // **************************************************************

    public int addTaxon(Taxon taxon) {
        throw new IllegalArgumentException("Cannot add taxon to a TreeModel");
    }

    public boolean removeTaxon(Taxon taxon) {
        throw new IllegalArgumentException("Cannot add taxon to a TreeModel");
    }

    public void setTaxonId(int taxonIndex, String id) {
        throw new IllegalArgumentException("Cannot set taxon id in a TreeModel");
    }

    public void setTaxonAttribute(int taxonIndex, String name, Object value) {
        throw new IllegalArgumentException("Cannot set taxon attribute in a TreeModel");
    }

    public void addMutableTreeListener(MutableTreeListener listener) {
    } // Do nothing at the moment

    public void addMutableTaxonListListener(MutableTaxonListListener listener) {
    } // Do nothing at the moment

    // **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

    private String id = null;

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
    // Attributable IMPLEMENTATION
    // **************************************************************

    private Attributable.AttributeHelper treeAttributes = null;

    /**
     * Sets an named attribute for this object.
     *
     * @param name  the name of the attribute.
     * @param value the new value of the attribute.
     */
    public void setAttribute(String name, Object value) {
        if (treeAttributes == null)
            treeAttributes = new Attributable.AttributeHelper();
        treeAttributes.setAttribute(name, value);
    }

    /**
     * @param name the name of the attribute of interest.
     * @return an object representing the named attributed for this object.
     */
    public Object getAttribute(String name) {
        if (treeAttributes == null)
            return null;
        else
            return treeAttributes.getAttribute(name);
    }

    /**
     * @return an iterator of the attributes that this object has.
     */
    public Iterator<String> getAttributeNames() {
        if (treeAttributes == null)
            return null;
        else
            return treeAttributes.getAttributeNames();
    }

    /**
     * @return a string containing a newick representation of the tree
     */
    public final String getNewick() {
        return Tree.Utils.newick(this);
    }

    /**
     * @return a string containing a newick representation of the tree
     */
    public String toString() {
        return getNewick();
    }

    public Tree getCopy() {
        throw new UnsupportedOperationException("please don't call this function");
    }

    // **************************************************************
    // XMLElement IMPLEMENTATION
    // **************************************************************

    public Element createElement(Document document) {
        throw new RuntimeException("Not implemented yet");
    }

    // ***********************************************************************
    // Private methods
    // ***********************************************************************

    /**
     * @return the node that this parameter is a member of
     */
    public Node getNodeOfParameter(Parameter parameter) {

        if (parameter == null) throw new IllegalArgumentException("Parameter is null!");

        for (Node node : nodes) {
            if (node.heightParameter == parameter) {
                return node;
            }
        }

        if (hasRates) {
            for (Node node : nodes) {
                if (node.rateParameter == parameter) {
                    return node;
                }
            }
        }
        if (hasTraits) {
            for (Node node : nodes) {
                if (node.traitParameters.containsValue(parameter)) {
                    return node;
                }
            }
        }
        throw new RuntimeException("Parameter not found in any nodes:" + parameter.getId() + " " + parameter.hashCode());
        // assume it is a trait parameter and return null
//		return null;
    }

    /**
     * Get the root height parameter. Is private because it can only be called by the XMLParser
     */
    public Parameter getRootHeightParameter() {

        return root.heightParameter;
    }

    /**
     * @return the relevant node height parameter. Is private because it can only be called by the XMLParser
     */
    public Parameter createNodeHeightsParameter(boolean rootNode, boolean internalNodes, boolean leafNodes) {

        if (!rootNode && !internalNodes && !leafNodes) {
            throw new IllegalArgumentException("At least one of rootNode, internalNodes or leafNodes must be true");
        }

        CompoundParameter parameter = new CompoundParameter("nodeHeights(" + getId() + ")");

        for (int i = externalNodeCount; i < nodeCount; i++) {
            if ((rootNode && nodes[i] == root) || (internalNodes && nodes[i] != root)) {
                parameter.addParameter(nodes[i].heightParameter);
            }
        }

        if (leafNodes) {
            for (int i = 0; i < externalNodeCount; i++) {
                parameter.addParameter(nodes[i].heightParameter);
            }
        }

        return parameter;
    }

    public Parameter getLeafHeightParameter(NodeRef node) {

        if (!isExternal(node)) {
            throw new RuntimeException("only leaves can be used with getLeafHeightParameter");
        }

        return nodes[node.getNumber()].heightParameter;
    }

    /**
     * @return the relevant node rate parameter. Is private because it can only be called by the XMLParser
     */
    public Parameter createNodeRatesParameter(double[] initialValues, boolean rootNode, boolean internalNodes, boolean leafNodes) {

        if (!rootNode && !internalNodes && !leafNodes) {
            throw new IllegalArgumentException("At least one of rootNode, internalNodes or leafNodes must be true");
        }

        CompoundParameter parameter = new CompoundParameter("nodeRates(" + getId() + ")");

        hasRates = true;

        for (int i = externalNodeCount; i < nodeCount; i++) {
            nodes[i].createRateParameter(initialValues);
            if ((rootNode && nodes[i] == root) || (internalNodes && nodes[i] != root)) {
                parameter.addParameter(nodes[i].rateParameter);
            }
        }

        for (int i = 0; i < externalNodeCount; i++) {
            nodes[i].createRateParameter(initialValues);
            if (leafNodes) {
                parameter.addParameter(nodes[i].rateParameter);
            }
        }

        return parameter;
    }

    public Parameter createNodeTraitsParameter(String name, double[] initialValues) {
        return createNodeTraitsParameter(name, initialValues.length,
                initialValues, true, true, true, true);
    }

    /**
     * Create a node traits parameter. Is private because it can only be called by the XMLParser
     */
    public Parameter createNodeTraitsParameter(String name, int dim, double[] initialValues,
                                               boolean rootNode, boolean internalNodes,
                                               boolean leafNodes, boolean firesTreeEvents) {

        if (!rootNode && !internalNodes && !leafNodes) {
            throw new IllegalArgumentException("At least one of rootNode, internalNodes or leafNodes must be true");
        }

        CompoundParameter parameter = new CompoundParameter(name);

        hasTraits = true;

        for (int i = externalNodeCount; i < nodeCount; i++) {
            nodes[i].createTraitParameter(name, dim, initialValues, firesTreeEvents);
            if ((rootNode && nodes[i] == root) || (internalNodes && nodes[i] != root)) {
                parameter.addParameter(nodes[i].getTraitParameter(name));
            }
        }

        for (int i = 0; i < externalNodeCount; i++) {
            nodes[i].createTraitParameter(name, dim, initialValues, firesTreeEvents);
            if (leafNodes) {
                parameter.addParameter(nodes[i].getTraitParameter(name));
            }
        }

        return parameter;
    }

    private void swapAllTraits(Node n1, Node n2) {

        for (Map.Entry<String, Parameter> entry : n1.traitParameters.entrySet()) {
            Parameter p1 = n1.traitParameters.get(entry.getKey());
            Parameter p2 = n2.traitParameters.get(entry.getKey());
            final int dim = p1.getDimension();
            for (int i = 0; i < dim; i++) {
                double transfer = p1.getParameterValue(i);
                p1.setParameterValue(i, p2.getParameterValue(i));
                p2.setParameterValue(i, transfer);
            }

        }

    }

    /**
     * This method swaps the parameter objects of the two nodes
     * but maintains the values in each node.
     * This method is used to ensure that root node of the tree
     * always has the same parameter object.
     */
    private void swapParameterObjects(Node n1, Node n2) {

        double height1 = n1.getHeight();
        double height2 = n2.getHeight();

        double rate1 = 1.0, rate2 = 1.0;

        if (hasRates) {
            rate1 = n1.getRate();
            rate2 = n2.getRate();
        }

        // swap all trait parameters

        if (hasTraits) {
            Map<String, Parameter> traits1 = new HashMap<String, Parameter>();
            Map<String, Parameter> traits2 = new HashMap<String, Parameter>();

            traits1.putAll(n1.traitParameters);
            traits2.putAll(n2.traitParameters);

            Map<String, Parameter> temp = n1.traitParameters;
            n1.traitParameters = n2.traitParameters;
            n2.traitParameters = temp;

            for (Map.Entry<String, Parameter> entry : traits1.entrySet()) {
                n1.traitParameters.get(entry.getKey()).setParameterValueQuietly(0, entry.getValue().getParameterValue(0));
            }
            for (Map.Entry<String, Parameter> entry : traits2.entrySet()) {
                n2.traitParameters.get(entry.getKey()).setParameterValueQuietly(0, entry.getValue().getParameterValue(0));
            }
        }

        Parameter temp = n1.heightParameter;
        n1.heightParameter = n2.heightParameter;
        n2.heightParameter = temp;

        if (hasRates) {
            temp = n1.rateParameter;
            n1.rateParameter = n2.rateParameter;
            n2.rateParameter = temp;
        }

        n1.heightParameter.setParameterValueQuietly(0, height1);
        n2.heightParameter.setParameterValueQuietly(0, height2);

        if (hasRates) {
            n1.rateParameter.setParameterValueQuietly(0, rate1);
            n2.rateParameter.setParameterValueQuietly(0, rate2);
        }
    }

    // **************************************************************
    // Private inner classes
    // **************************************************************

    public class Node implements NodeRef {

        public Node parent;
        public Node leftChild, rightChild;
        private int number;
        public Parameter heightParameter;
        public Parameter rateParameter = null;
        //public Parameter traitParameter = null;
        public Taxon taxon = null;

        Map<String, Parameter> traitParameters = new HashMap<String, Parameter>();

        public Node() {
            parent = null;
            leftChild = rightChild = null;
            heightParameter = null;
            number = 0;
            taxon = null;
        }

        /**
         * constructor used to clone a node and all children
         */
        public Node(Tree tree, NodeRef node) {
            parent = null;
            leftChild = rightChild = null;

            heightParameter = new Parameter.Default(tree.getNodeHeight(node));
            addVariable(heightParameter);

            number = node.getNumber();
            taxon = tree.getNodeTaxon(node);
            heightParameter.setId("" + number);
            for (int i = 0; i < tree.getChildCount(node); i++) {
                addChild(new Node(tree, tree.getChild(node, i)));
            }
        }

        public final void setupHeightBounds() {
            heightParameter.addBounds(new NodeHeightBounds(heightParameter));
        }

        public final void createRateParameter(double[] initialValues) {
            if (rateParameter == null) {
                if (initialValues != null) {
                    rateParameter = new Parameter.Default(initialValues[0]);
                } else {
                    rateParameter = new Parameter.Default(1.0);
                }
                if (isRoot()) {
                    rateParameter.setId("root.rate");
                } else if (isExternal()) {
                    rateParameter.setId(getTaxonId(getNumber()) + ".rate");
                } else {
                    rateParameter.setId("node" + getNumber() + ".rate");
                }
                rateParameter.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
                addVariable(rateParameter);
            }
        }

        public final void createTraitParameter(String name, double[] initialValues, boolean firesTreeEvents) {
            createTraitParameter(name, initialValues.length, initialValues, firesTreeEvents);
        }

        public final void createTraitParameter(String name, int dim, double[] initialValues, boolean firesTreeEvents) {

            if (!traitParameters.containsKey(name)) {

                Parameter trait = new Parameter.Default(dim);
                if (isRoot()) {
                    trait.setId("root." + name);
                } else if (isExternal()) {
                    trait.setId(getTaxonId(getNumber()) + "." + name);
                } else {
                    trait.setId("node" + getNumber() + "." + name);
                }
                trait.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, dim));

                if (initialValues != null && initialValues.length > 0) {
                    for (int i = 0; i < dim; i++) {
                        if (initialValues.length == dim) {
                            trait.setParameterValue(i, initialValues[i]);
                        } else {
                            trait.setParameterValue(i, initialValues[0]);
                        }
                    }
                }

                traitParameters.put(name, trait);

                if (firesTreeEvents) {
                    addVariable(trait);
                }
            }
        }

        public final double getHeight() {
            return heightParameter.getParameterValue(0);
        }

        public final double getRate() {
            return rateParameter.getParameterValue(0);
        }

        public final double getTrait(String name) {
            return traitParameters.get(name).getParameterValue(0);
        }

        public final double[] getMultivariateTrait(String name) {
            return traitParameters.get(name).getParameterValues();
        }

        public final Map<String, Parameter> getTraitMap() {
            return traitParameters;
        }

        public final void setHeight(double height) {
            heightParameter.setParameterValue(0, height);
        }

        public final void setRate(double rate) {
            //System.out.println("Rate set for parameter " + rateParameter.getParameterName());
            rateParameter.setParameterValue(0, rate);
        }

        public final void setTrait(String name, double trait) {
            //System.out.println("Trait set for parameter " + traitParameter.getParameterName());
            traitParameters.get(name).setParameterValue(0, trait);
        }

        public final void setMultivariateTrait(String name, double[] trait) {
            int dim = trait.length;
            for (int i = 0; i < dim; i++)
                traitParameters.get(name).setParameterValue(i, trait[i]);
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int n) {
            number = n;
        }

        /**
         * Returns the number of children this node has.
         */
        public final int getChildCount() {
            int n = 0;
            if (leftChild != null) n++;
            if (rightChild != null) n++;
            return n;
        }

        public Node getChild(int n) {
            if (n == 0) return leftChild;
            if (n == 1) return rightChild;
            throw new IllegalArgumentException("TreeModel.Nodes can only have 2 children");
        }

        public boolean hasChild(Node node) {
            return (leftChild == node || rightChild == node);
        }

        /**
         * add new child node
         *
         * @param node new child node
         */
        public void addChild(Node node) {
            if (leftChild == null) {
                leftChild = node;
            } else if (rightChild == null) {
                rightChild = node;
            } else {
                throw new IllegalArgumentException("TreeModel.Nodes can only have 2 children");
            }
            node.parent = this;
        }

        /**
         * remove child
         *
         * @param node child to be removed
         */
        public Node removeChild(Node node) {
            if (leftChild == node) {
                leftChild = null;
            } else if (rightChild == node) {
                rightChild = null;
            } else {
                throw new IllegalArgumentException("Unknown child node");
            }
            node.parent = null;
            return node;
        }

        /**
         * remove child
         *
         * @param n number of child to be removed
         */
        public Node removeChild(int n) {
            Node node;
            if (n == 0) {
                node = leftChild;
                leftChild = null;
            } else if (n == 1) {
                node = rightChild;
                rightChild = null;
            } else {
                throw new IllegalArgumentException("TreeModel.Nodes can only have 2 children");
            }
            node.parent = null;
            return node;
        }

        public boolean hasNoChildren() {
            return (leftChild == null && rightChild == null);
        }

        public boolean isExternal() {
            return hasNoChildren();
        }

        public boolean isRoot() {
            return (parent == null);
        }

        public String toString() {
            return "node " + number + ", height=" + getHeight() + (taxon != null ? ": " + taxon.getId() : "");
        }

        public Parameter getTraitParameter(String name) {
            return traitParameters.get(name);
        }
    }

    /**
     * This class provides bounds for parameters that represent a node height
     * in this tree model.
     */
    private class NodeHeightBounds implements Bounds<Double> {

        public NodeHeightBounds(Parameter parameter) {
            nodeHeightParameter = parameter;
        }

        public Double getUpperLimit(int i) {

            Node node = getNodeOfParameter(nodeHeightParameter);
            if (node.isRoot()) {
                return Double.POSITIVE_INFINITY;
            } else {
                return node.parent.getHeight();
            }
        }

        public Double getLowerLimit(int i) {

            Node node = getNodeOfParameter(nodeHeightParameter);
            if (node.isExternal()) {
                return 0.0;
            } else {
                return Math.max(node.leftChild.getHeight(), node.rightChild.getHeight());
            }
        }

        public int getBoundsDimension() {
            return 1;
        }


        private Parameter nodeHeightParameter = null;
    }

    // ***********************************************************************
    // Private members
    // ***********************************************************************


    /**
     * root node
     */
    private Node root = null;
    private int storedRootNumber;

    /**
     * list of internal nodes (including root)
     */
    private Node[] nodes = null;
    private Node[] storedNodes = null;

    /**
     * number of nodes (including root and tips)
     */
    private final int nodeCount;

    /**
     * number of external nodes
     */
    private final int externalNodeCount;

    /**
     * number of internal nodes (including root)
     */
    private final int internalNodeCount;

    /**
     * holds the units of the trees branches.
     */
    private Type units = Type.SUBSTITUTIONS;

    private boolean inEdit = false;

    private boolean hasRates = false;
    private boolean hasTraits = false;

    public static final XMLObjectParser<TreeModel> PARSER = new AbstractXMLObjectParser<TreeModel>() {

        public static final String ROOT_HEIGHT = "rootHeight";
        public static final String LEAF_HEIGHT = "leafHeight";
        public static final String LEAF_TRAIT = "leafTrait";

        public static final String NODE_HEIGHTS = "nodeHeights";
        public static final String NODE_RATES = "nodeRates";
        public static final String NODE_TRAITS = "nodeTraits";
        public static final String MULTIVARIATE_TRAIT = "traitDimension";
        public static final String INITIAL_VALUE = "initialValue";

        public static final String ROOT_NODE = "rootNode";
        public static final String INTERNAL_NODES = "internalNodes";
        public static final String LEAF_NODES = "leafNodes";

        public static final String LEAF_HEIGHTS = "leafHeights";

        public static final String FIRE_TREE_EVENTS = "fireTreeEvents";

        public static final String TAXON = "taxon";
        public static final String NAME = "name";

        {
            rules = new XMLSyntaxRule[]{
                    new ElementRule(Tree.class),
                    new ElementRule(ROOT_HEIGHT, Parameter.class, "A parameter definition with id only (cannot be a reference!)", false),
                    new ElementRule(NODE_HEIGHTS,
                            new XMLSyntaxRule[]{
                                    AttributeRule.newBooleanRule(ROOT_NODE, true, "If true the root height is included in the parameter"),
                                    AttributeRule.newBooleanRule(INTERNAL_NODES, true, "If true the internal node heights (minus the root) are included in the parameter"),
                                    new ElementRule(Parameter.class, "A parameter definition with id only (cannot be a reference!)")
                            }, 1, Integer.MAX_VALUE),
                    new ElementRule(LEAF_HEIGHT,
                            new XMLSyntaxRule[]{
                                    AttributeRule.newStringRule(TAXON, false, "The name of the taxon for the leaf"),
                                    new ElementRule(Parameter.class, "A parameter definition with id only (cannot be a reference!)")
                            }, 0, Integer.MAX_VALUE),
                    new ElementRule(NODE_TRAITS,
                            new XMLSyntaxRule[]{
                                    AttributeRule.newStringRule(NAME, false, "The name of the trait attribute in the taxa"),
                                    AttributeRule.newBooleanRule(ROOT_NODE, true, "If true the root trait is included in the parameter"),
                                    AttributeRule.newBooleanRule(INTERNAL_NODES, true, "If true the internal node traits (minus the root) are included in the parameter"),
                                    AttributeRule.newBooleanRule(LEAF_NODES, true, "If true the leaf node traits are included in the parameter"),
                                    AttributeRule.newIntegerRule(MULTIVARIATE_TRAIT, true, "The number of dimensions (if multivariate)"),
                                    AttributeRule.newDoubleRule(INITIAL_VALUE, true, "The initial value(s)"),
                                    AttributeRule.newBooleanRule(FIRE_TREE_EVENTS, true, "Whether to fire tree events if the traits change"),
                                    new ElementRule(Parameter.class, "A parameter definition with id only (cannot be a reference!)")
                            }, 0, Integer.MAX_VALUE),
                    new ElementRule(NODE_RATES,
                            new XMLSyntaxRule[]{
                                    AttributeRule.newBooleanRule(ROOT_NODE, true, "If true the root rate is included in the parameter"),
                                    AttributeRule.newBooleanRule(INTERNAL_NODES, true, "If true the internal node rate (minus the root) are included in the parameter"),
                                    AttributeRule.newBooleanRule(LEAF_NODES, true, "If true the leaf node rate are included in the parameter"),
                                    AttributeRule.newDoubleRule(INITIAL_VALUE, true, "The initial value(s)"),
                                    new ElementRule(Parameter.class, "A parameter definition with id only (cannot be a reference!)")
                            }, 0, Integer.MAX_VALUE),
                    new ElementRule(LEAF_TRAIT,
                            new XMLSyntaxRule[]{
                                    AttributeRule.newStringRule(TAXON, false, "The name of the taxon for the leaf"),
                                    AttributeRule.newStringRule(NAME, false, "The name of the trait attribute in the taxa"),
                                    new ElementRule(Parameter.class, "A parameter definition with id only (cannot be a reference!)")
                            }, 0, Integer.MAX_VALUE),
                    new ElementRule(LEAF_HEIGHTS,
                            new XMLSyntaxRule[]{
                                    new ElementRule(TaxonList.class, "A set of taxa for which leaf heights are required"),
                                    new ElementRule(Parameter.class, "A compound parameter containing the leaf heights")
                            }, true)
            };
        }

        public String getParserName() {
            return TREE_MODEL;
        }

        /**
         * @return a tree object based on the XML element it was passed.
         */
        public TreeModel parseXMLObject(XMLObject xo) throws XMLParseException {

            Tree tree = (Tree) xo.getChild(Tree.class);
            TreeModel treeModel = new TreeModel(xo.getId(), tree);

            Logger.getLogger("beast.evomodel").info("Creating the tree model, '" + xo.getId() + "'");

            for (int i = 0; i < xo.getChildCount(); i++) {
                if (xo.getChild(i) instanceof XMLObject) {

                    XMLObject cxo = (XMLObject) xo.getChild(i);

                    if (cxo.getName().equals(ROOT_HEIGHT)) {

                        Parameter.replaceParameter(cxo, treeModel.getRootHeightParameter());

                    } else if (cxo.getName().equals(LEAF_HEIGHT)) {

                        String taxonName;
                        if (cxo.hasAttribute(TAXON)) {
                            taxonName = cxo.getStringAttribute(TAXON);
                        } else {
                            throw new XMLParseException("taxa element missing from leafHeight element in treeModel element");
                        }

                        int index = treeModel.getTaxonIndex(taxonName);
                        if (index == -1) {
                            throw new XMLParseException("taxon " + taxonName + " not found for leafHeight element in treeModel element");
                        }
                        NodeRef node = treeModel.getExternalNode(index);

                        Parameter newParameter = treeModel.getLeafHeightParameter(node);

                        Parameter.replaceParameter(cxo, newParameter);

                        Taxon taxon = treeModel.getTaxon(index);

                        setPrecisionBounds(newParameter, taxon);

                    } else if (cxo.getName().equals(LEAF_HEIGHTS)) {
                        // get a set of leaf height parameters out as a compound parameter...

                        TaxonList taxa = (TaxonList)cxo.getChild(TaxonList.class);
                        Parameter offsetParameter = (Parameter)cxo.getChild(Parameter.class);

                        CompoundParameter leafHeights = new CompoundParameter("leafHeights");
                        for (Taxon taxon : taxa) {
                            int index = treeModel.getTaxonIndex(taxon);
                            if (index == -1) {
                                throw new XMLParseException("taxon " + taxon.getId() + " not found for leafHeight element in treeModel element");
                            }
                            NodeRef node = treeModel.getExternalNode(index);

                            Parameter newParameter = treeModel.getLeafHeightParameter(node);

                            leafHeights.addParameter(newParameter);

                            setPrecisionBounds(newParameter, taxon);
                        }

                        Parameter.replaceParameter(cxo, leafHeights);

                    } else if (cxo.getName().equals(NODE_HEIGHTS)) {

                        boolean rootNode = cxo.getAttribute(ROOT_NODE, false);
                        boolean internalNodes = cxo.getAttribute(INTERNAL_NODES, false);
                        boolean leafNodes = cxo.getAttribute(LEAF_NODES, false);

                        if (!rootNode && !internalNodes && !leafNodes) {
                            throw new XMLParseException("one or more of root, internal or leaf nodes must be selected for the nodeHeights element");
                        }

                        Parameter.replaceParameter(cxo, treeModel.createNodeHeightsParameter(rootNode, internalNodes, leafNodes));

                    } else if (cxo.getName().equals(NODE_RATES)) {

                        boolean rootNode = cxo.getAttribute(ROOT_NODE, false);
                        boolean internalNodes = cxo.getAttribute(INTERNAL_NODES, false);
                        boolean leafNodes = cxo.getAttribute(LEAF_NODES, false);
                        double[] initialValues = null;

                        if (cxo.hasAttribute(INITIAL_VALUE)) {
                            initialValues = cxo.getDoubleArrayAttribute(INITIAL_VALUE);
                        }

                        if (!rootNode && !internalNodes && !leafNodes) {
                            throw new XMLParseException("one or more of root, internal or leaf nodes must be selected for the nodeRates element");
                        }

                        Parameter.replaceParameter(cxo, treeModel.createNodeRatesParameter(initialValues, rootNode, internalNodes, leafNodes));

                    } else if (cxo.getName().equals(NODE_TRAITS)) {

                        boolean rootNode = cxo.getAttribute(ROOT_NODE, false);
                        boolean internalNodes = cxo.getAttribute(INTERNAL_NODES, false);
                        boolean leafNodes = cxo.getAttribute(LEAF_NODES, false);
                        boolean fireTreeEvents = cxo.getAttribute(FIRE_TREE_EVENTS, false);
                        String name = cxo.getAttribute(NAME, "trait");
                        int dim = cxo.getAttribute(MULTIVARIATE_TRAIT, 1);

                        double[] initialValues = null;
                        if (cxo.hasAttribute(INITIAL_VALUE)) {
                            initialValues = cxo.getDoubleArrayAttribute(INITIAL_VALUE);
                        }

                        if (!rootNode && !internalNodes && !leafNodes) {
                            throw new XMLParseException("one or more of root, internal or leaf nodes must be selected for the nodeTraits element");
                        }

                        Parameter.replaceParameter(cxo, treeModel.createNodeTraitsParameter(name, dim, initialValues, rootNode, internalNodes, leafNodes, fireTreeEvents));

                    } else if (cxo.getName().equals(LEAF_TRAIT)) {

                        String name = cxo.getAttribute(NAME, "trait");

                        String taxonName;
                        if (cxo.hasAttribute(TAXON)) {
                            taxonName = cxo.getStringAttribute(TAXON);
                        } else {
                            throw new XMLParseException("taxa element missing from leafTrait element in treeModel element");
                        }

                        int index = treeModel.getTaxonIndex(taxonName);
                        if (index == -1) {
                            throw new XMLParseException("taxon '" + taxonName + "' not found for leafTrait element in treeModel element");
                        }
                        NodeRef node = treeModel.getExternalNode(index);

                        Parameter parameter = treeModel.getNodeTraitParameter(node, name);

                        if (parameter == null)
                            throw new XMLParseException("trait '" + name + "' not found for leafTrait (taxon, " + taxonName + ") element in treeModel element");

                        Parameter.replaceParameter(cxo, parameter);

                    } else {
                        throw new XMLParseException("illegal child element in " + getParserName() + ": " + cxo.getName());
                    }

                } else if (xo.getChild(i) instanceof Tree) {
                    // do nothing - already handled
                } else {
                    throw new XMLParseException("illegal child element in  " + getParserName() + ": " + xo.getChildName(i) + " " + xo.getChild(i));
                }
            }

            // AR this is doubling up the number of bounds on each node.
//        treeModel.setupHeightBounds();
            //System.err.println("done constructing treeModel");

            Logger.getLogger("beast.evomodel").info("  initial tree topology = " + Tree.Utils.uniqueNewick(treeModel, treeModel.getRoot()));
            Logger.getLogger("beast.evomodel").info("  tree height = " + treeModel.getNodeHeight(treeModel.getRoot()));
            return treeModel;
        }

        private void setPrecisionBounds(Parameter newParameter, Taxon taxon) {
            beast.evolution.util.Date date = taxon.getDate();
            if (date != null) {
                double precision = date.getPrecision();
                if (precision > 0.0) {
                    // taxon date not specified to exact value so add appropriate bounds
                    double upper = Taxon.getHeightFromDate(date);
                    double lower = Taxon.getHeightFromDate(date);
                    if (date.isBackwards()) {
                        upper += precision;
                    } else {
                        lower -= precision;
                    }

                    // set the bounds for the given precision
                    newParameter.addBounds(new Parameter.DefaultBounds(upper, lower, 1));

                    // set the initial value to be mid-point
                    newParameter.setParameterValue(0, (upper + lower) / 2);
                }
            }
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents a model of the tree. The tree model includes and attributes of the nodes " +
                    "including the age (or <i>height</i>) and the rate of evolution at each node in the tree.";
        }

        public String getExample() {
            return
                    "<!-- the tree model as special sockets for attaching parameters to various aspects of the tree     -->\n" +
                            "<!-- The treeModel below shows the standard setup with a parameter associated with the root height -->\n" +
                            "<!-- a parameter associated with the internal node heights (minus the root height) and             -->\n" +
                            "<!-- a parameter associates with all the internal node heights                                     -->\n" +
                            "<!-- Notice that these parameters are overlapping                                                  -->\n" +
                            "<!-- The parameters are subsequently used in operators to propose changes to the tree node heights -->\n" +
                            "<treeModel id=\"treeModel1\">\n" +
                            "	<tree idref=\"startingTree\"/>\n" +
                            "	<rootHeight>\n" +
                            "		<parameter id=\"treeModel1.rootHeight\"/>\n" +
                            "	</rootHeight>\n" +
                            "	<nodeHeights internalNodes=\"true\" rootNode=\"false\">\n" +
                            "		<parameter id=\"treeModel1.internalNodeHeights\"/>\n" +
                            "	</nodeHeights>\n" +
                            "	<nodeHeights internalNodes=\"true\" rootNode=\"true\">\n" +
                            "		<parameter id=\"treeModel1.allInternalNodeHeights\"/>\n" +
                            "	</nodeHeights>\n" +
                            "</treeModel>";

        }

        public Class getReturnType() {
            return TreeModel.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules;
    };
}
