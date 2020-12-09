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
 *
 * Modified 2020, Thalia Nero to add custom type shape support.
 */

package com.outerthoughts.html5doclet.internal.toolkit.util.links;

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
                        addCustomShapedType(linkInfo, link, dfuShapeTemplate, new Content[0]);
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

    protected void addAppliedType(LinkInfo linkInfo, Content link, Type ctor, Content[] params) {
        // turn App<F<A..>, B..> into F<A..,B..> or F A..,B..
        boolean customShape = false;
        if (ctor.asTypeVariable() == null && ctor.asClassDoc() != null) {
            LinkInfo innerInfo = makeLink(linkInfo, ctor);
            innerInfo.classDoc = ctor.asClassDoc();
            String innerTemplate = getShapeTemplate(innerInfo);
            if (innerTemplate != null) {
                addCustomShapedType(innerInfo, link, innerTemplate, params);
                customShape = true;
            }
        }
        if (!customShape) {
            link.addContent(getTypeParameterLink(linkInfo, ctor));
            link.addContent("<");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    link.addContent(",");
                }
                link.addContent(params[i]);
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

    private void addCustomShapedType(LinkInfo linkInfo, Content link, String template, Content[] outerParams) {
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
            link.addContent(token.getContent(this, linkInfo, outerParams, params));
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
