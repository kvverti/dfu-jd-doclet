package com.outerthoughts.html5doclet.internal.toolkit.util.links;

import com.outerthoughts.html5doclet.internal.toolkit.Content;
import com.sun.javadoc.Type;

public class ClassLinkToken extends TypeShapeToken {

    final String value;

    ClassLinkToken(String value) {
        this.value = value;
    }

    @Override
    Content getContent(LinkFactory factory, LinkInfo linkInfo, Content[] outerParams, Type[] params) {
        Content c = factory.newContent();
        boolean excludeTypes = linkInfo.excludeTypeParameterLinks;
        linkInfo.excludeTypeParameterLinks = true;
        c.addContent(factory.getClassLink(linkInfo, this.value));
        linkInfo.excludeTypeParameterLinks = excludeTypes;
        return c;
    }
}
