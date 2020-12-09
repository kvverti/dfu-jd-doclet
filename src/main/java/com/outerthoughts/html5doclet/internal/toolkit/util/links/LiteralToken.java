package com.outerthoughts.html5doclet.internal.toolkit.util.links;

import com.outerthoughts.html5doclet.internal.toolkit.Content;
import com.sun.javadoc.Type;

public class LiteralToken extends TypeShapeToken {

    final String value;

    LiteralToken(String value) {
        this.value = value;
    }

    @Override
    Content getContent(LinkFactory factory, LinkInfo linkInfo, Content[] outerParams, Type[] params) {
        Content c = factory.newContent();
        c.addContent(this.value);
        return c;
    }
}
