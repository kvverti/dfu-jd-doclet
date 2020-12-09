package com.outerthoughts.html5doclet.internal.toolkit.util.links;

import com.outerthoughts.html5doclet.internal.toolkit.Content;
import com.sun.javadoc.Type;

public class TypePlaceholderToken extends TypeShapeToken {

    final int index;

    TypePlaceholderToken(int index) {
        this.index = index;
    }

    @Override
    Content getContent(LinkFactory factory, LinkInfo linkInfo, Content[] outerParams, Type[] params) {
        try {
            return outerParams[this.index - 1];
        } catch(IndexOutOfBoundsException e) {
            Content c = factory.newContent();
            c.addContent("_");
            return c;
        }
    }
}
