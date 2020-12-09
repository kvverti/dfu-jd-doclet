package com.outerthoughts.html5doclet.internal.toolkit.util.links;

import java.util.List;

import com.outerthoughts.html5doclet.internal.toolkit.Content;
import com.sun.javadoc.Type;

public class TypeArgumentApplicationToken extends TypeShapeToken {

    final List<TypeShapeToken> subTokens;

    TypeArgumentApplicationToken(List<TypeShapeToken> subTokens) {
        this.subTokens = subTokens;
    }

    @Override
    Content getContent(LinkFactory factory, LinkInfo linkInfo, Content[] outerParams, Type[] params) {
        try {
            List<TypeShapeToken> subTokens = this.subTokens;
            Content[] innerParams = new Content[subTokens.size() - 1];
            Type ctor = params[subTokens.get(0).asTypeArgument().index];
            for (int i = 1; i < subTokens.size(); i++) {
                innerParams[i - 1] = subTokens.get(i).getContent(factory, linkInfo, outerParams, params);
            }
            Content c = factory.newContent();
            factory.addAppliedType(linkInfo, c, ctor, innerParams);
            return c;
        } catch(IndexOutOfBoundsException e) {
            Content c = factory.newContent();
            c.addContent(Integer.toString(subTokens.get(0).asTypeArgument().index));
            return c;
        }
    }
}
