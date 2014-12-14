/*
 * ClassComparator.java
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

import java.util.Comparator;

public class ClassComparator implements Comparator<Class> {

	public int compare(Class c1, Class c2) {
	
		String name1 = getName(c1);
		String name2 = getName(c2);
		
		return name1.compareTo(name2);
	}
	
	protected static String getName(Class c1) {
		String name = c1.getName();
		return name.substring(name.lastIndexOf('.')+1);
	}
	
	public static final ClassComparator INSTANCE = new ClassComparator();
}
