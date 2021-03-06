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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Arman Bilge
 */
public final class SimpleXMLObjectParser<T> extends AbstractXMLObjectParser<T> {

    private final String name;
    private final String description;
    private final Class<T> parsedType;
    private final Map<Constructor<T>, XMLComponent[]> constructorsToComponents;
    private final Map<XMLSyntaxRule,Constructor<T>> rulesToConstructors;
    private final XMLSyntaxRule[] rules;

    private static String camelCase(final String s) {
        final StringBuilder cc = new StringBuilder(s);
        for (int i = 0; i+1 < cc.length() && Character.isUpperCase(cc.charAt(i+1)); ++i)
            cc.setCharAt(i, Character.toLowerCase(cc.charAt(i)));
        return Introspector.decapitalize(cc.toString());
    }
    
    public SimpleXMLObjectParser(final Class<T> parsedType) throws ParserCreationException {
        this.name = camelCase(parsedType.getSimpleName());
        this.parsedType = parsedType;
        if (parsedType.isAnnotationPresent(Description.class))
            this.description = parsedType.getAnnotation(Description.class).value();
        else
            this.description = "";
        constructorsToComponents = new HashMap<>();
        rulesToConstructors = new HashMap<>();
        final List<XMLSyntaxRule> rules = new ArrayList<>();
        for (Constructor constructor : parsedType.getConstructors()) {
            if (constructor.isAnnotationPresent(Parseable.class)) {
                final XMLComponent[] components = new XMLComponent[constructor.getParameterCount()];
                final XMLSyntaxRule[] constructorRules = new XMLSyntaxRule[components.length];
                for (int i = 0; i < components.length; ++i) {
                    for (final Annotation a : constructor.getParameterAnnotations()[i]) {
                        if (isParseableComponent(a)) {
                            components[i] = createXMLComponent(constructor.getParameterTypes()[i], a);
                            break;
                        }
                    }
                    if (components[i] == null)
                        throw new ParserCreationException(parsedType, "Parameter missing parseable annotation.");
                    constructorRules[i] = components[i].getSyntaxRule();
                }
                constructorsToComponents.put(constructor, components);
                final AndRule rule = new AndRule(constructorRules);
                rulesToConstructors.put(rule, constructor);
                rules.add(rule);
            }
        }
        if (constructorsToComponents.size() == 0) throw new ParserCreationException(parsedType, "No @Parseable constructors found!");
        this.rules = new XMLSyntaxRule[]{new XORRule(rules.toArray(new XMLSyntaxRule[rules.size()]))};
    }

    public String getParserName() {
        return name;
    }

    public String getParserDescription() {
        return description;
    }

    public Class<T> getReturnType() {
        return parsedType;
    }

    public T parseXMLObject(final XMLObject xo) throws XMLParseException {

        final Constructor<T> constructor = rulesToConstructors.entrySet().stream().filter(e -> e.getKey().isSatisfied(xo)).findFirst().get().getValue();
        final XMLComponent[] components = constructorsToComponents.get(constructor);

        final Object[] parameters = new Object[components.length];
        for (int i = 0; i < parameters.length; ++i)
            parameters[i] = components[i].parse(xo);

        try {
            return constructor.newInstance(parameters);
        } catch (final InvocationTargetException ex) {
            throw new XMLParseException(ex.getTargetException().getMessage());
        } catch (final InstantiationException|IllegalAccessException ex) {
            throw new XMLParseException(ex.getMessage());
        }
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private <X> XMLComponent<X> createXMLComponent(final Class<X> parameterType, Annotation annotation) throws ParserCreationException {
        final XMLComponentFactory factory = PARSEABLE_ANNOTATIONS.get(annotation.annotationType());
        if (!factory.validate(parameterType))
            throw new ParserCreationException(parsedType, factory.getAnnotationType().getSimpleName() + " annotation must be associated with " + factory.getParsedType().getSimpleName() + " parameter.");
        return factory.createXMLComponent(parameterType, annotation);
    }

    public interface XMLComponent<T> {
        T parse(XMLObject xo) throws XMLParseException;
        XMLSyntaxRule getSyntaxRule();
    }

    public static abstract class XMLComponentFactory<A extends Annotation> {
        private final Class<A> annotationType;
        public XMLComponentFactory(final Class<A> annotationType) {
            this.annotationType = annotationType;
        }
        public final Class<A> getAnnotationType() {
            return annotationType;
        }
        public abstract Class getParsedType();
        public boolean validate(final Class<?> c) {
            return c.isAssignableFrom(getParsedType());
        }
        public abstract XMLComponent createXMLComponent(Class parameterType, A annotation);
    }

    private static final Map<Class<? extends Annotation>, XMLComponentFactory<?>> PARSEABLE_ANNOTATIONS = new HashMap<>();

    private static final boolean isParseableComponent(final Annotation annotation) {
        return PARSEABLE_ANNOTATIONS.keySet().contains(annotation.annotationType());
    }

    public static final void registerXMLComponentFactory(final XMLComponentFactory<?> factory) {
        PARSEABLE_ANNOTATIONS.put(factory.getAnnotationType(), factory);
    }

    static {
        registerXMLComponentFactory(new XMLComponentFactory<BooleanAttribute>(BooleanAttribute.class) {
            @Override
            public Class getParsedType() {
                return boolean.class;
            }
            @Override
            public XMLComponent<Boolean> createXMLComponent(final Class parameterType, final BooleanAttribute ba) {
                return new XMLComponent<Boolean>() {
                    @Override
                    public Boolean parse(XMLObject xo) throws XMLParseException {
                        return xo.getAttribute(ba.name(), ba.defaultValue());
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return AttributeRule.newBooleanRule(ba.name(), ba.optional(), ba.description());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<DoubleArrayAttribute>(DoubleArrayAttribute.class) {
            @Override
            public Class getParsedType() {
                return double[].class;
            }
            @Override
            public XMLComponent<double[]> createXMLComponent(final Class parameterType, final DoubleArrayAttribute daa) {
                return new XMLComponent<double[]>() {
                    @Override
                    public double[] parse(XMLObject xo) throws XMLParseException {
                        final String name = daa.name();
                        return xo.hasAttribute(name) ? xo.getDoubleArrayAttribute(name) : null;
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return AttributeRule.newDoubleArrayRule(daa.name(), daa.optional(), daa.description());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<DoubleAttribute>(DoubleAttribute.class) {
            @Override
            public Class getParsedType() {
                return double.class;
            }
            @Override
            public XMLComponent<Double> createXMLComponent(final Class parameterType, final DoubleAttribute da) {
                return new XMLComponent<Double>() {
                    @Override
                    public Double parse(XMLObject xo) throws XMLParseException {
                        return xo.getAttribute(da.name(), da.defaultValue());
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return AttributeRule.newDoubleRule(da.name(), da.optional(), da.description());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<EnumAttribute>(EnumAttribute.class) {
            @Override
            public Class getParsedType() {
                return Enum.class;
            }
            @Override
            public boolean validate(Class c) {
                return c.isEnum();
            }
            @Override
            public XMLComponent<Enum> createXMLComponent(Class parameterType, EnumAttribute ea) {
                return new XMLComponent<Enum>() {
                    @Override
                    public Enum parse(XMLObject xo) throws XMLParseException {
                        if (!xo.hasAttribute(ea.name())) return null;
                        final String sa = xo.getStringAttribute(ea.name());
                        return Arrays.stream(((Enum[]) parameterType.getEnumConstants())).filter(e -> e.toString().equalsIgnoreCase(sa)).findFirst().get();
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return new StringAttributeRule(ea.name(), ea.description(), (Enum[]) parameterType.getEnumConstants(), ea.optional());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<IntegerArrayAttribute>(IntegerArrayAttribute.class) {
            @Override
            public Class getParsedType() {
                return int[].class;
            }
            @Override
            public XMLComponent<int[]> createXMLComponent(final Class parameterType, final IntegerArrayAttribute iaa) {
                return new XMLComponent<int[]>() {
                    @Override
                    public int[] parse(XMLObject xo) throws XMLParseException {
                        final String name = iaa.name();
                        return xo.hasAttribute(name) ? xo.getIntegerArrayAttribute(name) : null;
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return AttributeRule.newIntegerArrayRule(iaa.name(), iaa.optional(), iaa.description());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<IntegerAttribute>(IntegerAttribute.class) {
            @Override
            public Class getParsedType() {
                return int.class;
            }
            @Override
            public XMLComponent<Integer> createXMLComponent(final Class parameterType, final IntegerAttribute ia) {
                return new XMLComponent<Integer>() {
                    @Override
                    public Integer parse(XMLObject xo) throws XMLParseException {
                        return xo.getAttribute(ia.name(), ia.defaultValue());
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return AttributeRule.newIntegerRule(ia.name(), ia.optional(), ia.description());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<LongAttribute>(LongAttribute.class) {
            @Override
            public Class getParsedType() {
                return long.class;
            }
            @Override
            public XMLComponent<Long> createXMLComponent(final Class parameterType, final LongAttribute la) {
                return new XMLComponent<Long>() {
                    @Override
                    public Long parse(XMLObject xo) throws XMLParseException {
                        return xo.getAttribute(la.name(), la.defaultValue());
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return AttributeRule.newLongIntegerRule(la.name(), la.optional(), la.description());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<ObjectArrayElement>(ObjectArrayElement.class) {
            @Override
            public Class getParsedType() {
                return Object[].class;
            }
            @Override
            public boolean validate(Class c) {
                return c.isArray();
            }
            @Override
            public XMLComponent<Object[]> createXMLComponent(final Class parameterType, final ObjectArrayElement oae) {
                return new XMLComponent<Object[]>() {
                    @Override
                    public Object[] parse(XMLObject xo) throws XMLParseException {
                        final ArrayList arrayList = new ArrayList();
                        final String name = oae.name();
                        final Class componentType = parameterType.getComponentType();
                        if (xo.hasChildNamed(name)) {
                            final XMLObject cxo = xo.getChild(name);
                            for (int i = 0; i < cxo.getChildCount(); ++i) {
                                final Object o = cxo.getChild(i);
                                if (componentType.isInstance(o))
                                    arrayList.add(o);
                            }
                        }
                        try {
                            return arrayList.toArray((Object[]) Array.newInstance(componentType, arrayList.size()));
                        } catch (final NegativeArraySizeException ex) {
                            throw new XMLParseException(ex.getMessage());
                        }
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return new ElementRule(oae.name(), new XMLSyntaxRule[]{new ElementRule(parameterType.getComponentType(), oae.description(), oae.min(), oae.max())}, oae.min() == 0);
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<ObjectElement>(ObjectElement.class) {
            @Override
            public Class getParsedType() {
                return Object.class;
            }
            @Override
            public boolean validate(Class c) {
                return true;
            }
            @Override
            public XMLComponent<Object> createXMLComponent(final Class parameterType, final ObjectElement oe) {
                return new XMLComponent<Object>() {
                    @Override
                    public Object parse(XMLObject xo) throws XMLParseException {
                        final String name = oe.name();
                        return xo.hasChildNamed(name) ? xo.getChild(name).getChild(parameterType) : null;
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return new ElementRule(oe.name(), parameterType, oe.description(), oe.optional());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<StringArrayAttribute>(StringArrayAttribute.class) {
            @Override
            public Class getParsedType() {
                return String[].class;
            }
            @Override
            public XMLComponent<String[]> createXMLComponent(final Class parameterType, final StringArrayAttribute saa) {
                return new XMLComponent<String[]>() {
                    @Override
                    public String[] parse(XMLObject xo) throws XMLParseException {
                        final String name = saa.name();
                        return xo.hasAttribute(name) ? xo.getStringArrayAttribute(name) : null;
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return AttributeRule.newStringArrayRule(saa.name(), saa.optional(), saa.description());
                    }
                };
            }
        });
        registerXMLComponentFactory(new XMLComponentFactory<StringAttribute>(StringAttribute.class) {
            @Override
            public Class getParsedType() {
                return String.class;
            }
            @Override
            public XMLComponent<String> createXMLComponent(final Class parameterType, final StringAttribute sa) {
                return new XMLComponent<String>() {
                    @Override
                    public String parse(XMLObject xo) throws XMLParseException {
                        return xo.getAttribute(sa.name(), sa.defaultValue());
                    }
                    @Override
                    public XMLSyntaxRule getSyntaxRule() {
                        return AttributeRule.newStringRule(sa.name(), sa.optional(), sa.description());
                    }
                };
            }
        });
    }

    public static class ParserCreationException extends Exception {
        public ParserCreationException(final Class parsedType, final String msg) {
            super("Failed to create parser for class " + parsedType.getSimpleName() + ": "  + msg);
        }
    }
}
