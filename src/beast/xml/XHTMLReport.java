/*
 * XHTMLReport.java
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

import beast.util.XHTMLable;


/**
 * A generates an XHTML report from the elements within it.
 *
 * @version $Id: XHTMLReport.java,v 1.5 2005/05/24 20:26:01 rambaut Exp $
 *
 * @author Andrew Rambaut
 */
public class XHTMLReport extends Report {
		
	public void createReport() {
		System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
		System.out.print("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ");
		System.out.println("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
		System.out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
		System.out.println("<head>");
		System.out.println("<title>");
		System.out.println(getTitle());
		System.out.println("</title>");
		System.out.println("</head>");
		System.out.println("<body>");
		
		for (int i = 0; i < objects.size(); i++) {
			if (objects.get(i) instanceof XHTMLable)
				System.out.println(((XHTMLable)objects.get(i)).toXHTML());
			else {
				System.out.print("<p>");
				System.out.print(objects.get(i).toString());
				System.out.println("</p>");
			}
		}
		System.out.println("</body>");
		System.out.println("</html>");
	}
}
