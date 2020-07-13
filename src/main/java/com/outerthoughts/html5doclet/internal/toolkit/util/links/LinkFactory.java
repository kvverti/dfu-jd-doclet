/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.outerthoughts.html5doclet.internal.toolkit.util.links;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.outerthoughts.html5doclet.formats.html.LinkInfoImpl;
import com.outerthoughts.html5doclet.internal.toolkit.Content;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.javadoc.WildcardType;

/**
 * A factory that constructs links from given link information.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 *
 * @author Jamie Ho, Thalia Nero
 * @since 1.5
 */
public abstract class LinkFactory {

    /**
     * Return an empty instance of a content object.
     *
     * @return an empty instance of a content object.
     */
    protected abstract Content newContent();

    private static class TypeShapeToken {

        final Kind kind;
        final String value;

        private TypeShapeToken(Kind kind, String value) {
            this.kind = kind;
            this.value = value;
        }

        public static List<TypeShapeToken> of(String template) {
            if (template.isEmpty() || template.equals("\"\"")) {
                return Collections.emptyList();
            }
            // turns [ ] into generic brackets < >
            template = template.replaceAll("\\[", "<").replaceAll("]", ">");
            // get rid of quoted templates
            if (template.charAt(0) == '\"' && template.charAt(template.length() - 1) == '\"') {
                template = template.substring(1, template.length() - 1);
            }
            List<TypeShapeToken> tokens = new ArrayList<>();
            int len = template.length();
            int bgn = 0;
            int end = 0;
            // get as large of a literal as possible
            while (end < len && template.charAt(end) != '%') {
                end++;
            }
            // if nonempty, save it in a literal token
            if (bgn != end) {
                tokens.add(new TypeShapeToken(Kind.LITERAL, template.substring(bgn, end)));
            }
            // do tokenizing loop
            while (end < len) {
                assert template.charAt(end) == '%';
                end++;
                if (end < len) {
                    if (template.charAt(end) == '.') {// class link
                        bgn = end + 1;
                        do {
                            end++;
                        } while (end < len && template.charAt(end) != '.');
                        if (bgn != end) {
                            // .ClassLink. -> ClassLink
                            tokens.add(new TypeShapeToken(Kind.CLASS_LINK, template.substring(bgn, end)));
                            end++;
                        }
                    } else if (template.charAt(end) == '^') {
                        // type placeholder
                        // increment to first numeric char
                        end++;
                        bgn = end;
                        end = parseTypeArgumentIndex(template, bgn);
                        if (bgn != end) {
                            // ^123 -> 123
                            tokens.add(new TypeShapeToken(Kind.TYPE_PLACEHOLDER, template.substring(bgn, end)));
                        }
                    } else if (template.charAt(end) == 'a') {
                        // type argument application. Applies an argument to another argument (no placeholders).
                        end++;
                        bgn = end;
                        do {
                            end++;
                        } while (end < len && template.charAt(end) != ';');
                        if (bgn != end) {
                            tokens.add(new TypeShapeToken(Kind.TYPE_ARGUMENT_APPLICATION, template.substring(bgn, end)));
                            end++;
                        }
                    } else {
                        // type argument
                        bgn = end;
                        end = parseTypeArgumentIndex(template, bgn);
                        if (bgn != end) {
                            // a123,213; -> 123,213
                            tokens.add(new TypeShapeToken(Kind.TYPE_ARGUMENT, template.substring(bgn, end)));
                        }
                    }
                    bgn = end;
                    // get as large of a literal as possible
                    while (end < len && template.charAt(end) != '%') {
                        end++;
                    }
                    // if nonempty, save it in a literal token
                    if (bgn != end) {
                        tokens.add(new TypeShapeToken(Kind.LITERAL, template.substring(bgn, end)));
                    }
                }
            }
            return tokens;
        }

        private static int parseTypeArgumentIndex(String template, int bgn) {
            String digits = "0123456789";
            // get three digits max (maximum number of type params is 255)
            int end = bgn;
            int len = template.length();
            while (end < len && digits.indexOf(template.charAt(end)) >= 0 && (end - bgn) < 3) {
                end++;
            }
            return end;
        }

        enum Kind {
            CLASS_LINK,
            TYPE_ARGUMENT,
            TYPE_PLACEHOLDER,
            TYPE_ARGUMENT_APPLICATION,
            LITERAL
        }
    }

    protected abstract LinkInfo makeLink(LinkInfo linkInfo, Type type);

    /**
     * Constructs a link from the given link information.
     *
     * @param linkInfo the information about the link.
     * @return the output of the link.
     */
    public Content getLink(LinkInfo linkInfo) {
        if (linkInfo.type != null) {
            Type type = linkInfo.type;
            Content link = newContent();
            if (type.isPrimitive()) {
                //Just a primitive.
                link.addContent(type.typeName());
            } else if (type.asAnnotatedType() != null && type.dimension().length() == 0) {
                link.addContent(getTypeAnnotationLinks(linkInfo));
                linkInfo.type = type.asAnnotatedType().underlyingType();
                link.addContent(getLink(linkInfo));
                return link;
            } else if (type.asWildcardType() != null) {
                //Wildcard type.
                linkInfo.isTypeBound = true;
                link.addContent("?");
                WildcardType wildcardType = type.asWildcardType();
                Type[] extendsBounds = wildcardType.extendsBounds();
                for (int i = 0; i < extendsBounds.length; i++) {
                    link.addContent(i > 0 ? ", " : " extends ");
                    setBoundsLinkInfo(linkInfo, extendsBounds[i]);
                    link.addContent(getLink(linkInfo));
                }
                Type[] superBounds = wildcardType.superBounds();
                for (int i = 0; i < superBounds.length; i++) {
                    link.addContent(i > 0 ? ", " : " super ");
                    setBoundsLinkInfo(linkInfo, superBounds[i]);
                    link.addContent(getLink(linkInfo));
                }
            } else if (type.asTypeVariable() != null) {
                link.addContent(getTypeAnnotationLinks(linkInfo));
                linkInfo.isTypeBound = true;
                //A type variable.
                Doc owner = type.asTypeVariable().owner();
                if ((!linkInfo.excludeTypeParameterLinks) &&
                    owner instanceof ClassDoc) {
                    linkInfo.classDoc = (ClassDoc) owner;
                    Content label = newContent();
                    label.addContent(type.typeName());
                    linkInfo.label = label;
                    link.addContent(getClassLink(linkInfo, "Type"));
                } else {
                    //No need to link method type parameters.
                    link.addContent(type.typeName());
                }

                Type[] bounds = type.asTypeVariable().bounds();
                if (!linkInfo.excludeTypeBounds) {
                    linkInfo.excludeTypeBounds = true;
                    for (int i = 0; i < bounds.length; i++) {
                        link.addContent(i > 0 ? " & " : " extends ");
                        setBoundsLinkInfo(linkInfo, bounds[i]);
                        link.addContent(getLink(linkInfo));
                    }
                }
            } else if (type.asClassDoc() != null) {
                //A class type.
                if (linkInfo.isTypeBound &&
                    linkInfo.excludeTypeBoundsLinks) {
                    //Since we are excluding type parameter links, we should not
                    //be linking to the type bound.
                    link.addContent(type.typeName());
                    link.addContent(getTypeParameterLinks(linkInfo));
                    return link;
                } else {
                    linkInfo.classDoc = type.asClassDoc();
                    link = newContent();
                    String dfuShapeTemplate = getShapeTemplate(linkInfo);
                    if (dfuShapeTemplate != null) {
                        // special case for @dfu.render applied
                        if (dfuShapeTemplate.equals("applied")) {
                            Type[] params = linkInfo.type.asParameterizedType().typeArguments();
                            addAppliedType(linkInfo, link, params);
                        } else {
                            addCustomShapedType(linkInfo, link, dfuShapeTemplate, new Type[0]);
                        }
                    } else {
                        link.addContent(getClassLink(linkInfo, "Type"));
                        if (linkInfo.includeTypeAsSepLink) {
                            link.addContent(getTypeParameterLinks(linkInfo, false));
                        }
                    }
                }
            }

            if (linkInfo.isVarArg) {
                if (type.dimension().length() > 2) {
                    //Javadoc returns var args as array.
                    //Strip out the first [] from the var arg.
                    link.addContent(type.dimension().substring(2));
                }
                link.addContent("...");
            } else {
                while (type != null && type.dimension().length() > 0) {
                    if (type.asAnnotatedType() != null) {
                        linkInfo.type = type;
                        link.addContent(" ");
                        link.addContent(getTypeAnnotationLinks(linkInfo));
                        link.addContent("[]");
                        type = type.asAnnotatedType().underlyingType().getElementType();
                    } else {
                        link.addContent("[]");
                        type = type.getElementType();
                    }
                }
                linkInfo.type = type;
                Content newLink = newContent();
                newLink.addContent(getTypeAnnotationLinks(linkInfo));
                newLink.addContent(link);
                link = newLink;
            }
            return link;
        } else if (linkInfo.classDoc != null) {
            //Just a class link
            Content link = newContent();
            link.addContent(getClassLink(linkInfo, "Type"));
            if (linkInfo.includeTypeAsSepLink) {
                link.addContent(getTypeParameterLinks(linkInfo, false));
            }
            return link;
        } else {
            return null;
        }
    }

    private void addAppliedType(LinkInfo linkInfo, Content link, Type[] params) {
        // turn App<F<A..>, B..> into F<A..,B..> or F A..,B..
        boolean customShape = false;
        if (params[0].asTypeVariable() == null && params[0].asClassDoc() != null) {
            LinkInfo innerInfo = makeLink(linkInfo, params[0]);
            innerInfo.classDoc = params[0].asClassDoc();
            String innerTemplate = getShapeTemplate(innerInfo);
            if (innerTemplate != null) {
                addCustomShapedType(innerInfo, link, innerTemplate, params);
                customShape = true;
            }
        }
        if (!customShape) {
            link.addContent(getTypeParameterLink(linkInfo, params[0]));
            link.addContent("<");
            for (int i = 1; i < params.length; i++) {
                if (i > 1) {
                    link.addContent(",");
                }
                link.addContent(getTypeParameterLink(linkInfo, params[i]));
            }
            link.addContent(">");
        }
    }

    private String getShapeTemplate(LinkInfo linkInfo) {
        String dfuShapeTemplate;
        String overrideTemplate = ((LinkInfoImpl) linkInfo).configuration.templateStringsByClass.get(linkInfo.classDoc.qualifiedName());
        if (overrideTemplate != null) {
            dfuShapeTemplate = overrideTemplate;
        } else {
            Tag[] dfuRendering = linkInfo.classDoc.tags("dfu.shape");
            if (dfuRendering.length != 0) {
                dfuShapeTemplate = dfuRendering[0].text();
            } else {
                dfuShapeTemplate = null;
            }
        }
        return dfuShapeTemplate;
    }

    private static final Constructor<Type> PRIMITIVE_TYPE_CONSTRUCTOR;

    static {
        try {
            @SuppressWarnings("unchecked")
            Constructor<Type> ctor = (Constructor<Type>) Class.forName("com.sun.tools.javadoc.PrimitiveType")
                .getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            PRIMITIVE_TYPE_CONSTRUCTOR = ctor;
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private void addCustomShapedType(LinkInfo linkInfo, Content link, String template, Type[] outerParams) {
        // all other type shapes
        // e.g. DataResult<_>, Const<C,_>, Functor F, (T) -> R
        // special support for the names "Mu" and "Type"
        List<TypeShapeToken> tokens = TypeShapeToken.of(template);
        Type[] params;
        if (linkInfo.type.asParameterizedType() != null) {
            params = linkInfo.type.asParameterizedType().typeArguments();
        } else {
            params = new Type[0];
        }
        for (TypeShapeToken token : tokens) {
            switch (token.kind) {
            case LITERAL:
                link.addContent(token.value);
                break;
            case CLASS_LINK:
                addRawTypeName(linkInfo, link, () -> getClassLink(linkInfo, token.value));
                break;
            case TYPE_ARGUMENT:
                try {
                    int i = Integer.parseInt(token.value);
                    link.addContent(getTypeParameterLink(linkInfo, params[i]));
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    link.addContent(token.value);
                }
                break;
            case TYPE_PLACEHOLDER:
                try {
                    int i = Integer.parseInt(token.value);
                    if (i < outerParams.length) {
                        link.addContent(getTypeParameterLink(linkInfo, outerParams[i]));
                    } else {
                        link.addContent("_");
                    }
                } catch (NumberFormatException e) {
                    link.addContent(token.value);
                }
                break;
            case TYPE_ARGUMENT_APPLICATION:
                String[] indices = token.value.split(",");
                if (indices.length < 2) {
                    link.addContent(token.value);
                } else {
                    try {
                        Type[] innerParams = new Type[indices.length];
                        int typeCtorIdx = Integer.parseInt(indices[0]);
                        innerParams[0] = params[typeCtorIdx];
                        for (int i = 1; i < innerParams.length; i++) {
                            try {
                                String t = indices[i];
                                if (!t.isEmpty() && t.charAt(0) == '^') {
                                    t = t.substring(1);
                                    int idx = Integer.parseInt(t);
                                    innerParams[i] = outerParams[idx];
                                } else {
                                    int idx = Integer.parseInt(t);
                                    innerParams[i] = params[idx];
                                }
                            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                innerParams[i] = PRIMITIVE_TYPE_CONSTRUCTOR.newInstance(indices[i]);
                            }
                        }
                        addAppliedType(linkInfo, link, innerParams);
                    } catch (NumberFormatException | IndexOutOfBoundsException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        link.addContent(token.value);
                    }
                }
            }
        }
    }

    private void addRawTypeName(LinkInfo linkInfo, Content link, Supplier<Content> typeParameterLink) {
        boolean excludeTypes = linkInfo.excludeTypeParameterLinks;
        linkInfo.excludeTypeParameterLinks = true;
        link.addContent(typeParameterLink.get());
        linkInfo.excludeTypeParameterLinks = excludeTypes;
    }

    private void setBoundsLinkInfo(LinkInfo linkInfo, Type bound) {
        linkInfo.classDoc = null;
        linkInfo.label = null;
        linkInfo.type = bound;
    }

    /**
     * Return the link to the given class.
     *
     * @param linkInfo      the information about the link to construct.
     * @param labelOverride
     * @return the link for the given class.
     */
    protected abstract Content getClassLink(LinkInfo linkInfo, String labelOverride);

    /**
     * Return the link to the given type parameter.
     *
     * @param linkInfo  the information about the link to construct.
     * @param typeParam the type parameter to link to.
     */
    protected abstract Content getTypeParameterLink(LinkInfo linkInfo, Type typeParam);

    protected abstract Content getTypeAnnotationLink(LinkInfo linkInfo,
                                                     AnnotationDesc annotation);

    /**
     * Return the links to the type parameters.
     *
     * @param linkInfo the information about the link to construct.
     * @return the links to the type parameters.
     */
    public Content getTypeParameterLinks(LinkInfo linkInfo) {
        return getTypeParameterLinks(linkInfo, true);
    }

    /**
     * Return the links to the type parameters.
     *
     * @param linkInfo     the information about the link to construct.
     * @param isClassLabel true if this is a class label.  False if it is
     *                     the type parameters portion of the link.
     * @return the links to the type parameters.
     */
    public Content getTypeParameterLinks(LinkInfo linkInfo, boolean isClassLabel) {
        Content links = newContent();
        Type[] vars;
        if (linkInfo.executableMemberDoc != null) {
            vars = linkInfo.executableMemberDoc.typeParameters();
        } else if (linkInfo.type != null &&
            linkInfo.type.asParameterizedType() != null) {
            vars = linkInfo.type.asParameterizedType().typeArguments();
        } else if (linkInfo.classDoc != null) {
            vars = linkInfo.classDoc.typeParameters();
        } else {
            //Nothing to document.
            return links;
        }
        if (((linkInfo.includeTypeInClassLinkLabel && isClassLabel) ||
            (linkInfo.includeTypeAsSepLink && !isClassLabel)
        )
            && vars.length > 0) {
            links.addContent("<");
            for (int i = 0; i < vars.length; i++) {
                if (i > 0) {
                    links.addContent(",");
                }
                links.addContent(getTypeParameterLink(linkInfo, vars[i]));
            }
            links.addContent(">");
        }
        return links;
    }

    public Content getTypeAnnotationLinks(LinkInfo linkInfo) {
        Content links = newContent();
        if (linkInfo.type.asAnnotatedType() == null) {
            return links;
        }
        AnnotationDesc[] annotations = linkInfo.type.asAnnotatedType().annotations();
        for (int i = 0; i < annotations.length; i++) {
            if (i > 0) {
                links.addContent(" ");
            }
            links.addContent(getTypeAnnotationLink(linkInfo, annotations[i]));
        }

        links.addContent(" ");
        return links;
    }
}
