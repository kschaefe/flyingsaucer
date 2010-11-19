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
package org.xhtmlrenderer.render;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.xhtmlrenderer.layout.InlineTextLayoutToken;
import org.xhtmlrenderer.layout.LayoutToken;

class ContentTokens {
    private static class ContentIterator implements ListIterator {
        private InlineBox box;
        private String text;
        
        private List parsed;
        private ListIterator delegate;
        
        private int start = 0;
        private int end = 0;
        
        private LayoutToken token;
        
        public ContentIterator(InlineBox box) {
            this.box = box;
            this.text = box.getText();
            this.parsed = new ArrayList(text.length() / 6);
            this.delegate = parsed.listIterator();
        }
        
        void resetIterator(int index) {
            delegate = parsed.listIterator(index);
        }
        
        void clear() {
            parsed.clear();
        }

        public boolean hasNext() {
            return delegate.hasNext() || end < text.length();
        }
        
        public int nextIndex() {
            return delegate.nextIndex();
        }
        
        public Object next() {
            if (!delegate.hasNext()) {
                if (end >= text.length()) {
                    throw new NoSuchElementException();
                }
                
                //TODO preformatted text should always be content followed by breaktoken
                //TODO how do we handle new lines
                //TODO this is naive only working on spaces
                end = text.indexOf(' ', start);
                
                if (end == start) {
                    // we tokenize spaces as a group
                    while (++end != text.length() && text.charAt(end) == ' ');
                } else if (end == -1) {
                    end = text.length();
                }
                
                String s = text.substring(start, end);
                token = new InlineTextLayoutToken(box, s);
                start = end;
                
                delegate.add(token);
                
                return token;
            }

            return delegate.next();
        }
        
        public boolean hasPrevious() {
            return delegate.hasPrevious();
        }
        
        public int previousIndex() {
            return delegate.previousIndex();
        }
        
        public Object previous() {
            return delegate.previous();
        }

        public void add(Object element) {
            throw new UnsupportedOperationException();
        }
        
        public void set(Object element) {
            throw new UnsupportedOperationException();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    private ContentIterator iterator;
    
    public ContentTokens(InlineBox box) {
        iterator = new ContentIterator(box);
    }
    
    public ListIterator/*<ContentToken>*/ iterator() {
        return iterator(0);
    }
    
    public ListIterator/*<ContentToken>*/ iterator(int index) {
        iterator.resetIterator(index);
        
        return iterator;
    }
    
    public void clear() {
        iterator.clear();
    }
}
