/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Modified 2021 Thalia Nero: add hidden method type
 */

package com.outerthoughts.html5doclet.internal.toolkit.util;

/**
 * Enum representing method types.
 *
 * @author Bhavesh Patel
 */
public enum MethodTypes {
    ALL(0xffff, "All Methods", "t0", true),
    STATIC(0x1, "Static Methods", "t1", false),
    INSTANCE(0x2, "Instance Methods", "t2", false),
    ABSTRACT(0x4, "Abstract Methods", "t3", false),
    CONCRETE(0x8, "Concrete Methods", "t4", false),
    DEFAULT(0x10, "Default Methods", "t5", false),
    DEPRECATED(0x20, "Deprecated Methods", "t6", false),
    HIDDEN(0x1_0000, "Hidden Methods", "t7", false);

    private final int value;
    private final String text;
    private final String tabId;
    private final boolean isDefaultTab;

    MethodTypes(int v, String t, String id, boolean dt) {
        this.value = v;
        this.text = t;
        this.tabId = id;
        this.isDefaultTab = dt;
    }

    public int value() {
        return value;
    }

    public String text() {
        return text;
    }

    public String tabId() {
        return tabId;
    }

    public boolean isDefaultTab() {
        return isDefaultTab;
    }
}
