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

import java.util.Arrays;
import java.util.List;

import com.outerthoughts.html5doclet.internal.toolkit.Content;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
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
 * @author Jamie Ho
 * @since 1.5
 */
public abstract class LinkFactory {

    private static final List<String> typeClasses = Arrays.asList(
        "Applicative",
        "CartesianLike",
        "CocartesianLike",
        "Functor",
        "Kind1",
        "Kind2",
        "Representable",
        "Traversable"
    );

    public static final List<String> functionClasses = Arrays.asList(
        "Supplier",
        "Function",
        "BiFunction",
        "Function3",
        "Function4",
        "Function5",
        "Function6",
        "Function7",
        "Function8",
        "Function9",
        "Function10",
        "Function11",
        "Function12",
        "Function13",
        "Function14",
        "Function15",
        "Function16"
    );

    public static final List<String> consumerClasses = Arrays.asList(
        "Consumer",
        "BiConsumer"
    );

    public static final List<String> productClasses = Arrays.asList(
        "Products.P1",
        "Products.P2",
        "Products.P3",
        "Products.P4",
        "Products.P5",
        "Products.P6",
        "Products.P7",
        "Products.P8",
        "Products.P9",
        "Products.P10",
        "Products.P11",
        "Products.P12",
        "Products.P13",
        "Products.P14",
        "Products.P15",
        "Products.P16"
    );

    /**
     * Return an empty instance of a content object.
     *
     * @return an empty instance of a content object.
     */
    protected abstract Content newContent();

    /**
     * Constructs a link from the given link information.
     *
     * @param linkInfo   the information about the link.
     * @param muBrackets
     * @return the output of the link.
     */
    public Content getLink(LinkInfo linkInfo, boolean muBrackets) {
        if (linkInfo.type != null) {
            Type type = linkInfo.type;
            Content link = newContent();
            if (type.isPrimitive()) {
                //Just a primitive.
                link.addContent(type.typeName());
            } else if (type.asAnnotatedType() != null && type.dimension().length() == 0) {
                link.addContent(getTypeAnnotationLinks(linkInfo));
                linkInfo.type = type.asAnnotatedType().underlyingType();
                link.addContent(getLink(linkInfo, muBrackets));
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
                    link.addContent(getLink(linkInfo, muBrackets));
                }
                Type[] superBounds = wildcardType.superBounds();
                for (int i = 0; i < superBounds.length; i++) {
                    link.addContent(i > 0 ? ", " : " super ");
                    setBoundsLinkInfo(linkInfo, superBounds[i]);
                    link.addContent(getLink(linkInfo, muBrackets));
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
                    link.addContent(getClassLink(linkInfo, muBrackets));
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
                        link.addContent(getLink(linkInfo, muBrackets));
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
                    if ("App".equals(linkInfo.classDoc.name())) {
                        Type[] params = linkInfo.type.asParameterizedType().typeArguments();
                        link.addContent(getTypeParameterLink(linkInfo, params[0], false));
                        link.addContent("<");
                        link.addContent(getTypeParameterLink(linkInfo, params[1], muBrackets));
                        link.addContent(">");
                    } else if ("App2".equals(linkInfo.classDoc.name())) {
                        Type[] params = linkInfo.type.asParameterizedType().typeArguments();
                        link.addContent(getTypeParameterLink(linkInfo, params[0], false));
                        link.addContent("<");
                        link.addContent(getTypeParameterLink(linkInfo, params[1], muBrackets));
                        link.addContent(",");
                        link.addContent(getTypeParameterLink(linkInfo, params[2], muBrackets));
                        link.addContent(">");
                    } else if (typeClasses.contains(linkInfo.classDoc.name())) {
                        Type[] params = linkInfo.type.asParameterizedType().typeArguments();
                        boolean excludeTypes = linkInfo.excludeTypeParameterLinks;
                        linkInfo.excludeTypeParameterLinks = true;
                        link.addContent(getClassLink(linkInfo, muBrackets));
                        linkInfo.excludeTypeParameterLinks = excludeTypes;
                        link.addContent(" ");
                        link.addContent(getTypeParameterLink(linkInfo, params[0], false));
                    } else if (functionClasses.contains(linkInfo.classDoc.name())) {
                        Type[] params = linkInfo.type.asParameterizedType().typeArguments();
                        link.addContent("(");
                        for (int i = 0; i < params.length - 1; i++) {
                            if (i > 0) {
                                link.addContent(",");
                            }
                            link.addContent(getTypeParameterLink(linkInfo, params[i], muBrackets));
                        }
                        link.addContent(") ");
                        boolean excludeTypes = linkInfo.excludeTypeParameterLinks;
                        linkInfo.excludeTypeParameterLinks = true;
                        link.addContent(getClassLink(linkInfo, false));
                        linkInfo.excludeTypeParameterLinks = excludeTypes;
                        link.addContent(" ");
                        link.addContent(getTypeParameterLink(linkInfo, params[params.length - 1], muBrackets));
                    } else if (consumerClasses.contains(linkInfo.classDoc.name())) {
                        Type[] params = linkInfo.type.asParameterizedType().typeArguments();
                        link.addContent("(");
                        for (int i = 0; i < params.length; i++) {
                            if (i > 0) {
                                link.addContent(",");
                            }
                            link.addContent(getTypeParameterLink(linkInfo, params[i], muBrackets));
                        }
                        link.addContent(") ");
                        boolean excludeTypes = linkInfo.excludeTypeParameterLinks;
                        linkInfo.excludeTypeParameterLinks = true;
                        link.addContent(getClassLink(linkInfo, false));
                        linkInfo.excludeTypeParameterLinks = excludeTypes;
                        link.addContent(" void");
                    } else if (productClasses.contains(linkInfo.classDoc.name())) {
                        Type[] params = linkInfo.type.asParameterizedType().typeArguments();
                        link.addContent(getTypeParameterLink(linkInfo, params[0], false));
                        boolean excludeTypes = linkInfo.excludeTypeParameterLinks;
                        linkInfo.excludeTypeParameterLinks = true;
                        link.addContent(getClassLink(linkInfo, false));
                        linkInfo.excludeTypeParameterLinks = excludeTypes;
                        link.addContent("(");
                        for (int i = 1; i < params.length; i++) {
                            if (i > 1) {
                                link.addContent(",");
                            }
                            link.addContent(getTypeParameterLink(linkInfo, params[i], muBrackets));
                        }
                        link.addContent(")");
                    } else {
                        link.addContent(getClassLink(linkInfo, muBrackets));
                        if (linkInfo.includeTypeAsSepLink) {
                            link.addContent(getTypeParameterLinks(linkInfo, false, muBrackets));
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
            link.addContent(getClassLink(linkInfo, muBrackets));
            if (linkInfo.includeTypeAsSepLink) {
                link.addContent(getTypeParameterLinks(linkInfo, false, muBrackets));
            }
            return link;
        } else {
            return null;
        }
    }

    private void setBoundsLinkInfo(LinkInfo linkInfo, Type bound) {
        linkInfo.classDoc = null;
        linkInfo.label = null;
        linkInfo.type = bound;
    }

    /**
     * Return the link to the given class.
     *
     * @param linkInfo   the information about the link to construct.
     * @param muBrackets
     * @return the link for the given class.
     */
    protected abstract Content getClassLink(LinkInfo linkInfo, boolean muBrackets);

    /**
     * Return the link to the given type parameter.
     *
     * @param linkInfo   the information about the link to construct.
     * @param typeParam  the type parameter to link to.
     * @param muBrackets
     */
    protected abstract Content getTypeParameterLink(LinkInfo linkInfo, Type typeParam, boolean muBrackets);

    protected abstract Content getTypeAnnotationLink(LinkInfo linkInfo,
                                                     AnnotationDesc annotation);

    /**
     * Return the links to the type parameters.
     *
     * @param linkInfo the information about the link to construct.
     * @return the links to the type parameters.
     */
    public Content getTypeParameterLinks(LinkInfo linkInfo) {
        return getTypeParameterLinks(linkInfo, true, true);
    }

    /**
     * Return the links to the type parameters.
     *
     * @param linkInfo     the information about the link to construct.
     * @param isClassLabel true if this is a class label.  False if it is
     *                     the type parameters portion of the link.
     * @param muBrackets
     * @return the links to the type parameters.
     */
    public Content getTypeParameterLinks(LinkInfo linkInfo, boolean isClassLabel, boolean muBrackets) {
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
                links.addContent(getTypeParameterLink(linkInfo, vars[i], muBrackets));
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
