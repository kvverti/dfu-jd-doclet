package com.outerthoughts.html5doclet.internal.toolkit.util.links;

import com.outerthoughts.html5doclet.internal.toolkit.Content;
import com.sun.javadoc.Type;

public class TypeArgumentToken extends TypeShapeToken {

    final int index;

    TypeArgumentToken(int index) {
        this.index = index;
    }

    @Override
    Content getContent(LinkFactory factory, LinkInfo linkInfo, Content[] outerParams, Type[] params) {
        try {
            return factory.getTypeParameterLink(linkInfo, params[this.index]);
        } catch(IndexOutOfBoundsException e) {
            Content c = factory.newContent();
            c.addContent(Integer.toString(this.index));
            return c;
        }
    }
}
