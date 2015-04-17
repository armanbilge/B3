/*
 * CompoundModel.java
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

import beast.inference.model.Variable.ChangeType;

import java.util.ArrayList;
import java.util.List;

/**
 * An interface that describes a model of some data.
 *
 * @version $Id: CompoundModel.java,v 1.8 2005/05/24 20:25:59 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */

public class CompoundModel extends Model {

	public static final String COMPOUND_MODEL = "compoundModel";

	public CompoundModel(String name) {
		super(name);
	}

	@Override
	protected void storeState() {
		// Nothing to do
	}

	@Override
	protected void restoreState() {
		// Nothing to do
	}

	@Override
	protected void acceptState() {
		// Nothing to do
	}

	@Override
	protected void handleModelChangedEvent(Model model, Object object, int index) {
		// Nothing to do
	}

	@Override
	protected void handleVariableChangedEvent(Variable variable, int index, ChangeType type) {
		// Nothing to do
	}

	public boolean isValidState() {

		for (int i = 0; i < models.size(); i++) {
			if (!getModel(i).isValidState()) {
				return false;
			}
		}

		return true;
	}

	// **************************************************************
    // Identifiable IMPLEMENTATION
    // **************************************************************

	private String id = null;

	public void setId(String id) { this.id = id; }

	public String getId() { return id; }

	/* AER - do we need a parser?
	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() { return COMPOUND_MODEL; }

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			CompoundModel compoundModel = new CompoundModel("model");

			int childCount = xo.getChildCount();
			for (int i = 0; i < childCount; i++) {
				Object xoc = xo.getChild(i);
				if (xoc instanceof Model) {
					compoundModel.addModel((Model)xoc);
				}
			}
			return compoundModel;
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "This element represents a combination of models.";
		}

		public Class getReturnType() { return CompoundModel.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			new OneOrMoreRule(Model.class)
		};
	};*/

	private final ArrayList<Model> models = new ArrayList<Model>();
    private final List<ModelListener> listeners = new ArrayList<ModelListener>();
}

