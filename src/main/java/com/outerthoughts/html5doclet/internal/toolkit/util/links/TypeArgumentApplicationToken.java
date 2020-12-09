/*
 * Copyright (C) 2020, Thalia Nero
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  This particular file is
 * designated as subject to the "Classpath" exception as provided in
 * the LICENSE file that accompanies this code.
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
 */
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
