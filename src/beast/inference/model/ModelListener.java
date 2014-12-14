/*
 * ModelListener.java
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

/**
 * An interface that provides a listener on a model.
 *
 * @version $Id: ModelListener.java,v 1.2 2005/05/24 20:26:00 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */

public interface ModelListener {

	/**
	 * The model has changed. The model firing the event can optionally
	 * supply a reference to an object and an index if appropriate. Use
	 * of this extra information will be contingent on recognising what
	 * model it was that fired the event.
	 */
	void modelChangedEvent(Model model, Object object, int index);

    /**
     * The model has been restored.
     * Required only for notification of non-models (say pure likelihoods) which depend on
     * models.
     * @param model
     */
    void modelRestored(Model model);
}
