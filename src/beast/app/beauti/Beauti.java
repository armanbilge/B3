/*
 * Beauti.java
 *
 * BEAST: Bayesian Evolutionary Analysis by Sampling Trees
 * Copyright (C) 2015 BEAST Developers
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

package beast.app.beauti;

import beast.app.beast.BeastParser;
import beast.inference.mcmc.MCMC;
import beast.xml.Reference;
import beast.xml.XMLObject;
import beast.xml.XMLObjectParser;
import beast.xml.XMLParser;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Arman Bilge
 */
public final class Beauti {

    private final XMLParser parser;
    private final Document document;
    private final Map<Class<?>, XMLObjectParser<?>> class2Parser;
    private final Map<XMLObjectParser<?>,Integer> instanceCounts;

    {
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        class2Parser = new HashMap<>();
        instanceCounts = new HashMap<>();
    }

    public Beauti() {
        parser = new BeastParser(new String[0], Collections.EMPTY_LIST, true, true, true);
        for (final Iterator<XMLObjectParser<?>> iter = this.parser.getParsers(); iter.hasNext();) {
            final XMLObjectParser<?> parser = iter.next();
            class2Parser.put(parser.getReturnType(), parser);
        }
        final ObjectFrame frame = new ObjectFrame<>(new XMLObject(document, MCMC.PARSER.getParserName()), this, MCMC.PARSER);
        final XMLObject xo = frame.getXMLObject();
        document.appendChild(xo.getWrappedElement());
        try {
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(System.out));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public Beauti(final XMLParser parser) {
        this.parser = parser;
    }

    public Document getDocument() {
        return document;
    }

    public Set<XMLObjectParser> getParsers(Class type) {
        final Set<XMLObjectParser> parsers = new HashSet<>();
        for (final Map.Entry<Class<?>,XMLObjectParser<?>> e : class2Parser.entrySet())
            if (type.isAssignableFrom(e.getKey())) parsers.add(e.getValue());
        return parsers;
    }

    public Set<Reference> getReferences(Class type) {
        final Set<Reference> references = new HashSet<>();
        for (final Object o : parser.getObjectStore().getObjects()) {
            final XMLObject xo = (XMLObject) o;
            if (type.isAssignableFrom(parser.getParser(xo.getName()).getReturnType()))
                references.add(new Reference(xo));
        }
        return references;
    }

    public void store(String id, Object o) {
        if (!parser.getObjectStore().getObjects().contains(o))
            parser.storeObject(id, o);
    }

    public int getInstanceCount(XMLObjectParser<?> p) {
        if (!instanceCounts.containsKey(p))
            instanceCounts.put(p, 0);
        final int c = instanceCounts.get(p);
        instanceCounts.put(p, c+1);
        return c;
    }

    public static void main(final String[] args) {

        new Beauti();

    }

}
