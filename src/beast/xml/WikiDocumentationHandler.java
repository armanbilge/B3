/*
 * WikiDocumentationHandler.java
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

import beast.app.beast.BeastParser;
import beast.app.tools.BeastParserDoc;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * A subclass of XMLDocumentationHandler that creates output in MediaWiki format
 */
public class WikiDocumentationHandler extends XMLDocumentationHandler {

    private final static String WIKILINK = "[[Main Page|BEAST Documentation]]-> BEAST v1.5.x XML Reference";

    public WikiDocumentationHandler(BeastParser parser) {
        super(parser);
    }

    public void outputElements(PrintWriter writer) {

        printDocWikiTitle(writer);

        Iterator iterator = parser.getParsers();
        while (iterator.hasNext()) {
            XMLObjectParser xmlParser = (XMLObjectParser)iterator.next();
            writer.println(xmlParser.toWiki(this));

            // convert the html into wiki...

            System.out.println("  outputting Wiki for element " + xmlParser.getParserName());
        }

    }

    private void printDocWikiTitle(PrintWriter writer) {

        Calendar date = Calendar.getInstance();
        SimpleDateFormat dateformatter = new SimpleDateFormat("'updated on' d MMMM yyyy zzz");

        writer.println(WIKILINK + "\n");
        writer.println("==" + BeastParserDoc.TITTLE + "==\n");

        if (parser.parsers != null) {
            if (parser.parsers.equalsIgnoreCase(BeastParser.RELEASE)) {
                writer.println("Release Version (" + dateformatter.format(date.getTime()) + ")");
                System.out.println("Release Version");
            } else if (parser.parsers.equalsIgnoreCase(BeastParser.DEV)) {
                writer.println("Development Version (" + dateformatter.format(date.getTime()) + ")");
                System.out.println("Development Version");
            }
        }

        writer.println("");
        writer.println("The following is a list of valid elements in a beast file.");
        writer.println("");

    }

    /**
     * Outputs all types that appear as optional attributes or elements in an HTML table to the given writer.
     * @param writer PrintWriter
     */
    public void outputTypes(PrintWriter writer) {

        writer.println("==BEAST types==");
        writer.println("");
        writer.println("The following is a list of generic types that elements represent in a beast file.");
        writer.println("");


        // iterate through the types
        //Iterator iterator = requiredTypes.iterator();
        for (Class requiredType : requiredTypes) {
            if (requiredType != Object.class) {

                String name = ClassComparator.getName(requiredType);

                System.out.println("  outputting Wiki for generic type " + name);


                TreeSet<String> matchingParserNames = new TreeSet<String>();

                // find all parsers that match this optional type
                Iterator i = parser.getParsers();
                while (i.hasNext()) {
                    XMLObjectParser xmlParser = (XMLObjectParser) i.next();
                    Class returnType = xmlParser.getReturnType();
                    if (requiredType.isAssignableFrom(returnType)) {
                        matchingParserNames.add(xmlParser.getParserName());
                    }
                }

                if (!(matchingParserNames.size() == 1 && matchingParserNames.iterator().next().equals(name))) {
                    
                    // output table row containing the type and the matching parser names
                    writer.println("===" + name + "===");
                    writer.println();
                    writer.println("Elements of this type include:");
                    writer.println();
                    i = matchingParserNames.iterator();
                    while (i.hasNext()) {
                        String parserName = (String) i.next();
                        writer.println(":*" + getWikiLink(parserName));
                    }
                    writer.println();
                }
            }

        }
    }

    public String getHTMLForClass(Class c) {
        return getWikiLink(ClassComparator.getName(c));
    }

    public String getWikiLink(String name) {
        if (Character.isUpperCase(name.charAt(0))) {
            // linking to a 'type'
            return "[[#" + name + "|" + name + "]]";
        } else {
            // linking to an 'element'
            return "[[#&lt;" + name + "&gt; element|" + name + "]]";
        }
    }
}