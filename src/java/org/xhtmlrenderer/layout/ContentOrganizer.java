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
package org.xhtmlrenderer.layout;

import org.xhtmlrenderer.render.BlockBox;

/**
 * ContentOrganizer creates a pluggable layer that allows layout mechanisms to
 * be switched. By design the same layout mechanism is used for all XHTMLPanel
 * instances.
 * <p>
 * By default, ContentOrganizer defers to the original static layout methods.
 * 
 * @see InlineBoxing
 */
public class ContentOrganizer {
    private static ContentOrganizer instance = new ContentOrganizer();
    
    protected ContentOrganizer() {
        //does nothing
    }
    
    public synchronized static ContentOrganizer getIntance() {
        return instance;
    }
    
    public synchronized static void setInstance(ContentOrganizer instance) {
        ContentOrganizer.instance = instance;
    }
    
    public void layoutContent(LayoutContext c, BlockBox box, int initialY, int breakAtLine) {
        InlineBoxing.layoutContent(c, box, initialY, breakAtLine);
    }
}
