/*
 * Model.java
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

import beast.util.Identifiable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * An interface that describes a model of some data.
 *
 * @version $Id: Model.java,v 1.6 2005/05/24 20:26:00 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */

public abstract class Model implements Identifiable, ModelListener, VariableListener, StatisticList {

	protected final String name;
	private final ArrayList<Model> models = new ArrayList<Model>();
	private final ArrayList<Variable> variables = new ArrayList<Variable>();
	private final ArrayList<Statistic> statistics = new ArrayList<Statistic>();
	protected ListenerHelper listenerHelper = new ListenerHelper();
	boolean isValidState = true;
	private String id = null;

	public Model(String name) {
		this.name = name;
	}

	public final void addVariable(Variable variable) {
        if (!variables.contains(variable)) {
            variables.add(variable);
            variable.addVariableListener(this);
        }

        // parameters are also statistics
        if (variable instanceof Statistic) addStatistic((Statistic) variable);
    }

	public final void removeVariable(Variable variable) {
        variables.remove(variable);
        variable.removeVariableListener(this);

        // parameters are also statistics
        if (variable instanceof Statistic) removeStatistic((Statistic) variable);
    }

	/**
     * @param parameter
     * @return true of the given parameter is contained in this model
     */
    public final boolean hasVariable(Variable parameter) {
        return variables.contains(parameter);
    }

	/**
     * Adds a model listener.
     */
    public void addModelListener(ModelListener listener) {
        listenerHelper.addModelListener(listener);
    }

	/**
     * remove a model listener.
     */
    public void removeModelListener(ModelListener listener) {
        listenerHelper.removeModelListener(listener);
    }

	public final void modelChangedEvent(Model model, Object object, int index) {

//		String message = "  model: " + getModelName() + "/" + getId() + "  component: " + model.getModelName();
//		if (object != null) {
//			message += " object: " + object;
//		}
//		if (index != -1) {
//			message += " index: " + index;
//		}
//		System.out.println(message);

        handleModelChangedEvent(model, object, index);
    }

	// do nothing by default
    public void modelRestored(Model model) {
    }

	abstract protected void handleModelChangedEvent(Model model, Object object, int index);

	public final void variableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        handleVariableChangedEvent(variable, index, type);
        listenerHelper.fireModelChanged(this, variable, index);
    }

	/**
     * This method is called whenever a parameter is changed.
     * <p/>
     * It is strongly recommended that the model component sets a "dirty" flag and does no
     * further calculations. Recalculation is typically done when the model component is asked for
     * some information that requires them. This mechanism is 'lazy' so that this method
     * can be safely called multiple times with minimal computational cost.
     */
    protected abstract void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type);

	public final void storeModelState() {
        if (isValidState) {
            //System.out.println("STORE MODEL: " + getModelName() + "/" + getId());

            for (Model m : models) {
                m.storeModelState();
            }

            for (Variable variable : variables) {
                variable.storeVariableValues();
            }

            storeState();
            isValidState = false;
        }
    }

	public final void restoreModelState() {
        if (!isValidState) {
            //System.out.println("RESTORE MODEL: " + getModelName() + "/" + getId());

            for (Variable variable : variables) {
                variable.restoreVariableValues();
            }
            for (Model m : models) {
                m.restoreModelState();
            }

            restoreState();
            isValidState = true;

            listenerHelper.fireModelRestored(this);
        }
    }

	public final void acceptModelState() {
        if (!isValidState) {
            //System.out.println("ACCEPT MODEL: " + getModelName() + "/" + getId());

            for (Variable variable : variables) {
                variable.acceptVariableValues();
            }

            for (Model m : models) {
                m.acceptModelState();
            }

            acceptState();

            isValidState = true;
        }
    }

	public boolean isValidState() {
        return isValidState;
    }

	/**
     * Adds a sub-model to this model. If the model is already in the
     * list then it does nothing.
     */
    public void addModel(Model model) {

        if (!models.contains(model)) {
            models.add(model);
            model.addModelListener(this);
        }
    }

	public void removeModel(Model model) {
        models.remove(model);
        model.removeModelListener(this);
    }

	public int getModelCount() {
        return models.size();
    }

	public final Model getModel(int i) {
        return models.get(i);
    }

	/**
     * Fires a model changed event.
     */
    public void fireModelChanged() {
        listenerHelper.fireModelChanged(this, this, -1);
    }

	public void fireModelChanged(Object object) {
        listenerHelper.fireModelChanged(this, object, -1);
    }

	public void fireModelChanged(Object object, int index) {
        listenerHelper.fireModelChanged(this, object, index);
    }

	public final int getVariableCount() {
        return variables.size();
    }

	public final Variable getVariable(int i) {
        return variables.get(i);
    }

	/**
	 * @return the variable of the component with a given name
	 */
	//Parameter getParameter(String name);

	public final String getModelName() {
        return name;
    }

	public void addModelRestoreListener(ModelListener listener) {
        listenerHelper.addModelRestoreListener(listener);
    }

	public boolean isUsed() {
        return listenerHelper.getListenerCount() > 0;
    }

	/**
     * Additional state information, outside of the sub-model is stored by this call.
     */
    protected abstract void storeState();

	/**
     * After this call the model is guaranteed to have returned its extra state information to
     * the values coinciding with the last storeState call.
     * Sub-models are handled automatically and do not need to be considered in this method.
     */
    protected abstract void restoreState();

	/**
     * This call specifies that the current state is accept. Most models will not need to do anything.
     * Sub-models are handled automatically and do not need to be considered in this method.
     */
    protected abstract void acceptState();

	public final void addStatistic(Statistic statistic) {
        if (!statistics.contains(statistic)) {
            statistics.add(statistic);
        }
    }

	public final void removeStatistic(Statistic statistic) {

        statistics.remove(statistic);
    }

	/**
     * @return the number of statistics of this component.
     */
    public int getStatisticCount() {

        return statistics.size();
    }

	/**
     * @return the ith statistic of the component
     */
    public Statistic getStatistic(int i) {

        return statistics.get(i);
    }

	public final Statistic getStatistic(String name) {

        for (int i = 0; i < getStatisticCount(); i++) {
            Statistic statistic = getStatistic(i);
            if (name.equals(statistic.getStatisticName())) {
                return statistic;
            }
        }

        return null;
    }

	public void setId(String id) {
        this.id = id;
    }

	public String getId() {
        return id;
    }

	public String toString() {
        if (id != null) {
            return id;
        } else if (name != null) {
            return name;
        }
        return super.toString();
    }

	public Element createElement(Document d) {
        throw new RuntimeException("Not implemented!");
    }

	/**
	 * A helper class for storing listeners and firing events.
	 */
	public class ListenerHelper {

		public void fireModelChanged(Model model) {
			fireModelChanged(model, model, -1);
		}

		public void fireModelChanged(Model model, Object object) {
			fireModelChanged(model, object, -1);
		}

		public void fireModelChanged(Model model, Object object, int index) {
			if (listeners != null) {
                for (ModelListener listener : listeners) {
                    listener.modelChangedEvent(model, object, index);
                }
            }
		}

		public void addModelListener(ModelListener listener) {
			if (listeners == null) {
				listeners = new java.util.ArrayList<ModelListener>();
			}
			listeners.add(listener);
		}

		public void removeModelListener(ModelListener listener) {
			if (listeners != null) {
				listeners.remove(listener);
			}
		}

        public void addModelRestoreListener(ModelListener listener) {
            if (restoreListeners == null) {
                restoreListeners = new java.util.ArrayList<ModelListener>();
            }
            restoreListeners.add(listener);
        }

        public void fireModelRestored(Model model) {
            if (restoreListeners != null) {
                for (ModelListener listener : restoreListeners ) {
                    listener.modelRestored(model);
                }
            }
        }

        public int getListenerCount() {
            return listeners != null ? listeners.size() : 0;
        }

        private ArrayList<ModelListener> listeners = null;

        private ArrayList<ModelListener> restoreListeners = null;
    }


    // set to store all created models
    public final static Set<Model> FULL_MODEL_SET = new HashSet<>();

}

