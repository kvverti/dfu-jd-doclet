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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.outerthoughts.html5doclet.internal.toolkit.Content;
import com.sun.javadoc.Type;

abstract class TypeShapeToken {

    protected TypeShapeToken() {
    }

    TypeArgumentToken asTypeArgument() {
        return (TypeArgumentToken) this;
    }

    abstract Content getContent(LinkFactory factory, LinkInfo linkInfo, Content[] outerParams, Type[] params);

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
            tokens.add(new LiteralToken(template.substring(bgn, end)));
        }
        // do tokenizing loop
        while (end < len) {
            assert template.charAt(end) == '%';
            end++;
            if (end < len) {
                end = parseNextToken(template, tokens, len, end);
                bgn = end;
                // get as large of a literal as possible
                while (end < len && template.charAt(end) != '%') {
                    end++;
                }
                // if nonempty, save it in a literal token
                if (bgn != end) {
                    tokens.add(new LiteralToken(template.substring(bgn, end)));
                }
            }
        }
        return tokens;
    }

    private static int parseNextToken(String template, List<TypeShapeToken> tokens, int len, int end) {
        int bgn;
        if (template.charAt(end) == '.') { // class link
            bgn = end + 1;
            do {
                end++;
            } while (end < len && template.charAt(end) != '.');
            if (bgn != end) {
                // .ClassLink. -> ClassLink
                tokens.add(new ClassLinkToken(template.substring(bgn, end)));
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
                tokens.add(new TypePlaceholderToken(Integer.parseInt(template.substring(bgn, end))));
            }
        } else if (template.charAt(end) == '(') {
            // type argument application. Applies an argument to another argument (no placeholders).
            end++;
            bgn = end;
            List<TypeShapeToken> subTokens = new ArrayList<>();
            // parse the initial type argument index
            end = parseTypeArgumentIndex(template, bgn);
            if (bgn != end) {
                subTokens.add(new TypeArgumentToken(Integer.parseInt(template.substring(bgn, end))));
                do {
                    // skip the ","
                    end++;
                    bgn = end;
                    // add the next token (possible recursive)
                    if (end < len) {
                        end = parseNextToken(template, subTokens, len, end);
                    }
                } while (end < len && template.charAt(end) != ')');
                if (bgn != end) {
                    // a123,213; -> 123,213
                    tokens.add(new TypeArgumentApplicationToken(subTokens));
                    end++;
                }
            }
        } else if (template.charAt(end) == '\'') {
            // quoted literal
            end++;
            bgn = end;
            while(end < len && template.charAt(end) != '\'') {
                end++;
            }
            if(bgn != end) {
                tokens.add(new LiteralToken(template.substring(bgn, end)));
            }
            // skip closing quote
            end++;
        } else {
            // type argument
            bgn = end;
            end = parseTypeArgumentIndex(template, bgn);
            if (bgn != end) {
                tokens.add(new TypeArgumentToken(Integer.parseInt(template.substring(bgn, end))));
            }
        }
        return end;
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
}
