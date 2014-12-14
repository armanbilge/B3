/*
 * SimpleXMLObjectParser.java
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

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by armanbilge on 11/25/14.
 */
public final class SimpleXMLObjectParser<T> extends AbstractXMLObjectParser<T> {

    private final String name;
    private final String description;
    private final Class<T> parsedClass;
    private final Map<Constructor<T>, XMLComponent[]> constructorsToComponents;
    private final Map<XMLSyntaxRule,Constructor<T>> rulesToConstructors;
    private final XMLSyntaxRule[] rules;

    public SimpleXMLObjectParser(final Class<T> parsedClass, final String description) {
        this(Introspector.decapitalize(parsedClass.getSimpleName()), parsedClass, description);
    }

    public SimpleXMLObjectParser(final String name, final Class<T> parsedClass, final String description) {
        this.name = name;
        this.parsedClass = parsedClass;
        this.description = description;
        constructorsToComponents = new HashMap<>();
        rulesToConstructors = new HashMap<>();
        final List<XMLSyntaxRule> rules = new ArrayList<>();
        for (Constructor constructor : parsedClass.getConstructors()) {
            if (constructor.isAccessible() && constructor.isAnnotationPresent(Parseable.class)) {
                final XMLComponent[] components = new XMLComponent[constructor.getParameterCount()];
                final XMLSyntaxRule[] constructorRules = new XMLSyntaxRule[components.length];
                for (int i = 0; i < components.length; ++i) {
                    for (final Annotation a : constructor.getParameterAnnotations()[i]) {
                        if (isParseableComponent(a)) {
                            components[i] = new XMLComponent(constructor.getParameterTypes()[i], a);
                            break;
                        }
                    }
                    if (components[i] == null)
                        throw new ParserCreationException(parsedClass, "Parameter missing parseable annotation.");
                    constructorRules[i] = components[i].getRule();
                }
                constructorsToComponents.put(constructor, components);
                final AndRule rule = new AndRule(constructorRules);
                rulesToConstructors.put(rule, constructor);
                rules.add(rule);
            }
        }
        if (constructorsToComponents.size() == 0) throw new RuntimeException("No @Parseable constructors found!");
        this.rules = new XMLSyntaxRule[]{new XORRule(rules.toArray(new XMLSyntaxRule[rules.size()]))};
    }

    public String getParserName() {
        return name;
    }

    public String getParserDescription() {
        return description;
    }

    public Class<T> getReturnType() {
        return parsedClass;
    }

    public T parseXMLObject(final XMLObject xo) throws XMLParseException {

        Constructor<T> constructor = null;
        for (final Map.Entry<XMLSyntaxRule,Constructor<T>> e : rulesToConstructors.entrySet()) {
            if (e.getKey().isSatisfied(xo)) {
                constructor = e.getValue();
                break;
            }
        }
        if (constructor == null) throw new XMLParseException();
        final XMLComponent[] components = constructorsToComponents.get(constructor);

        final Object[] parameters = new Object[components.length];
        for (int i = 0; i < parameters.length; ++i)
            parameters[i] = components[i].parse(xo);

        try {
            return constructor.newInstance(parameters);
        } catch (final Exception ex) {
            throw new XMLParseException(ex.getMessage());
        }
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final class XMLComponent<T> {
        private final XMLSyntaxRule rule;
        private final Parser<T> parser;

        public XMLComponent(final Class<T> c, final Annotation a) {
            if (a instanceof BooleanAttribute) {
                if (c != Boolean.class)
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "Boolean attribute must be associated with boolean parameter.");
                final BooleanAttribute ba = (BooleanAttribute) a;
                final String name = ba.name();
                rule = AttributeRule.newBooleanRule(name, ba.optional(), ba.description());
                parser = (xo) -> xo.getAttribute(name, (T) (Boolean) ba.defaultValue());
            } else if (a instanceof DoubleArrayAttribute) {
                if (c != Double[].class)
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "Double array attribute must be associated with double array parameter.");
                final DoubleArrayAttribute daa = (DoubleArrayAttribute) a;
                final String name = daa.name();
                rule = AttributeRule.newDoubleArrayRule(name, daa.optional(), daa.description());
                parser = (xo) -> (T) (xo.hasAttribute(name) ? xo.getDoubleArrayAttribute(name) : new double[0]);
            } else if (a instanceof DoubleAttribute) {
                if (c != Double.class)
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "Double attribute must be associated with double parameter.");
                final DoubleAttribute da = (DoubleAttribute) a;
                final String name = da.name();
                rule = AttributeRule.newDoubleRule(name, da.optional(), da.description());
                parser = (xo) -> xo.getAttribute(name, (T) (Double) da.defaultValue());
            } else if (a instanceof IntegerArrayAttribute) {
                if (c != Integer[].class)
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "Integer array attribute must be associated with integer array parameter.");
                final IntegerArrayAttribute iaa = (IntegerArrayAttribute) a;
                final String name = iaa.name();
                rule = AttributeRule.newIntegerArrayRule(name, iaa.optional(), iaa.description());
                parser = (xo) -> (T) (xo.hasAttribute(name) ? xo.getIntegerArrayAttribute(name) : new int[0]);
            } else if (a instanceof IntegerAttribute) {
                if (c != Integer.class)
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "Integer attribute must be associated with integer parameter.");
                final IntegerAttribute ia = (IntegerAttribute) a;
                final String name = ia.name();
                rule = AttributeRule.newIntegerRule(name, ia.optional(), ia.description());
                parser = (xo) -> xo.getAttribute(name, (T) (Integer) ia.defaultValue());
            } else if (a instanceof LongAttribute) {
                if (c != Long.class)
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "Long attribute must be associated with long parameter.");
                final LongAttribute la = (LongAttribute) a;
                final String name = la.name();
                rule = AttributeRule.newLongIntegerRule(name, la.optional(), la.description());
                parser = (xo) -> xo.getAttribute(name, (T) (Long) la.defaultValue());
            } else if (a instanceof StringArrayAttribute) {
                if (c != String[].class)
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "String array attribute must be associated with String array parameter.");
                final StringArrayAttribute saa = (StringArrayAttribute) a;
                final String name = saa.name();
                rule = AttributeRule.newStringArrayRule(name, saa.optional(), saa.description());
                parser = (xo) -> (T) (xo.hasAttribute(name) ? xo.getStringArrayAttribute(name) : new String[0]);
            } else if (a instanceof StringAttribute) {
                if (c != String.class)
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "String attribute must be associated with String parameter.");
                final StringAttribute sa = (StringAttribute) a;
                final String name = sa.name();
                rule = AttributeRule.newStringRule(name, sa.optional(), sa.description());
                parser = (xo) -> xo.getAttribute(name, (T) sa.defaultValue());
            } else if (a instanceof ObjectArrayElement) {
                if (!c.isArray())
                    throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "Object array element must be associated with object array parameter.");
                final ObjectArrayElement oae = (ObjectArrayElement) a;
                final String name = oae.name();
                final Class componentType = c.getComponentType();
                rule = new ElementRule(name, componentType, description, oae.min(), oae.max());
                parser = (xo) -> {
                    final ArrayList arrayList = new ArrayList();
                    if (xo.hasChildNamed(name)) {
                        final XMLObject cxo = xo.getChild(name);
                        for (int i = 0; i < cxo.getChildCount(); ++i) {
                            final Object o = cxo.getChild(i);
                            if (componentType.isInstance(o))
                                arrayList.add(o);
                        }
                    }
                    try {
                        return (T) arrayList.toArray((Object[]) Array.newInstance(componentType, arrayList.size()));
                    } catch (final NegativeArraySizeException ex) {
                        throw new XMLParseException(ex.getMessage());
                    }
                };
            } else if (a instanceof ObjectElement) {
                final ObjectElement oe = (ObjectElement) a;
                rule = new ElementRule(oe.name(), c, oe.description(), oe.optional());
                parser = (xo) -> xo.hasChildNamed(name) ? xo.getChild(name).getChild(c) : null;
            } else {
                throw new ParserCreationException(SimpleXMLObjectParser.this.getReturnType(), "Unknown annotation type.");
            }
        }

        public T parse(final XMLObject xo) throws XMLParseException {
            return parser.parse(xo);
        }

        public XMLSyntaxRule getRule() {
            return rule;
        }

    }

    private interface Parser<T> {
        T parse(XMLObject xo) throws XMLParseException;
    }

    private static final boolean isParseableComponent(final Annotation annotation) {
        for (final Class<? extends Annotation> c : PARSEABLE_ANNOTATIONS)
            if (c.isInstance(annotation)) return true;
        return false;
    }

    private static final Set<Class<? extends Annotation>> PARSEABLE_ANNOTATIONS = new HashSet<>();
    static {
        PARSEABLE_ANNOTATIONS.add(BooleanAttribute.class);
        PARSEABLE_ANNOTATIONS.add(DoubleArrayAttribute.class);
        PARSEABLE_ANNOTATIONS.add(DoubleAttribute.class);
        PARSEABLE_ANNOTATIONS.add(IntegerArrayAttribute.class);
        PARSEABLE_ANNOTATIONS.add(IntegerAttribute.class);
        PARSEABLE_ANNOTATIONS.add(LongAttribute.class);
        PARSEABLE_ANNOTATIONS.add(ObjectArrayElement.class);
        PARSEABLE_ANNOTATIONS.add(ObjectElement.class);
        PARSEABLE_ANNOTATIONS.add(StringArrayAttribute.class);
        PARSEABLE_ANNOTATIONS.add(StringAttribute.class);
    }

}
