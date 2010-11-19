/*
 * {{{ header & license
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.xhtmlrenderer.layout.nlb;

/**
 * A class that lists the type for the content.
 */
public final class ContentType {
    /**
     * In preformatted content, a single line of content. In non-preformatted
     * content, a consecutive series of non-whitespace characters. The
     * non-breaking space is not consider a whitespace character.
     */
    public static final ContentType CONTENT = new ContentType("CONTENT");
    
    /**
     * Whitespace-only content. Only found in non-preformatted content.
     */
    public static final ContentType WHITESPACE = new ContentType("WHITESPACE");
    
    private final String name;
    
    private ContentType(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }
}
