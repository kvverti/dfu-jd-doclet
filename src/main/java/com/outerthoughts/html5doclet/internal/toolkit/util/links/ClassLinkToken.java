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
