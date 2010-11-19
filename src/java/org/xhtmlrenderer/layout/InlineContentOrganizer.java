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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.constants.IdentValue;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.layout.LayoutToken.Type;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.render.InlineBox;
import org.xhtmlrenderer.render.InlineLayoutBox;
import org.xhtmlrenderer.render.LineBox;
import org.xhtmlrenderer.render.MarkerData;

public class InlineContentOrganizer {
    protected static class Mark {
        private final int inlineContentIndex;
        private final InlineLayoutBox currentILB;
        private final int contentTokenIndex;
        private final LayoutToken breakToken;
        
        /**
         * @param inlineContentIndex
         * @param contentTokenIndex
         * @param breakToken
         */
        public Mark(int inlineContentIndex, InlineLayoutBox currentILB,  int contentTokenIndex, LayoutToken breakToken) {
            this.inlineContentIndex = inlineContentIndex;
            this.currentILB = currentILB;
            this.contentTokenIndex = contentTokenIndex;
            this.breakToken = breakToken;
        }

        public int getInlineContentIndex() {
            return inlineContentIndex;
        }
        
        public InlineLayoutBox getCurrentILB() {
            return currentILB;
        }

        public int getContentTokenIndex() {
            return contentTokenIndex;
        }
        
        public LayoutToken getBreakToken() {
            return breakToken;
        }
    }
    
    private static InlineContentOrganizer instance = new InlineContentOrganizer();
    
    protected InlineContentOrganizer() {
        //does nothing
    }
    
    public static synchronized InlineContentOrganizer getInstance() {
        return instance;
    }
    
    public static synchronized void setInstance(InlineContentOrganizer organizer) {
        instance = organizer;
    }
    
    public void layout(LayoutContext c, BlockBox box, int initialY, int breakAtLine) {
        final int maxAvailableWidth = box.getContentWidth();
        LineBox currentLine = newLine(c, initialY, box);
        
        int remainingWidth = maxAvailableWidth;
        int contentStart = 0;

        remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, currentLine, remainingWidth);

        CalculatedStyle parentStyle = box.getStyle();
        int minimumLineHeight = (int) parentStyle.getLineHeight(c);
        int indent = (int) parentStyle.getFloatPropertyProportionalWidth(CSSName.TEXT_INDENT, maxAvailableWidth, c);
        remainingWidth -= indent;
        contentStart += indent;
        
        MarkerData markerData = c.getCurrentMarkerData();
        if (markerData != null && box.getStyle().isListMarkerInside()) {
            remainingWidth -= markerData.getLayoutWidth();
            contentStart += markerData.getLayoutWidth();
        }
        c.setCurrentMarkerData(null);

        List pendingFloats = new ArrayList();
        int pendingLeftMBP = 0;
        int pendingRightMBP = 0;
        
        boolean firstLetterComplete = !c.getFirstLettersTracker().hasStyles();
        boolean firstLineComplete = !c.getFirstLinesTracker().hasStyles();
        Stack inlineLayoutBoxes = new Stack();
        Mark mark = null;
        
        for (ListIterator i = box.getInlineContent().listIterator(); i.hasNext(); ) {
            Styleable node = (Styleable) i.next();
            
            CalculatedStyle style = node.getStyle();
            
            if (!firstLetterComplete) {
                style = c.getFirstLettersTracker().deriveAll(style);
            } else if (!firstLineComplete) {
                style = c.getFirstLinesTracker().deriveAll(style);
            }
            
            if (node.getStyle().isInline()) {
                InlineBox ib = (InlineBox) node;
                InlineLayoutBox ilb = new InlineLayoutBox(c, ib.getElement(), style, maxAvailableWidth);
                
                for (ListIterator ci = ib.getContentTokens(); ci.hasNext();) {
                    LayoutToken token = (LayoutToken) ci.next();
                    
                    if (token.getType() == Type.WHITESPACE) {
                        if (currentLine.getChildCount() == 0) {
                            continue;
                        } else {
                            mark = new Mark(i.previousIndex(), ilb, ci.previousIndex(), token);
                        }
                    }
                    
                    if (!firstLetterComplete) {
                        int index = 0;
                        
                        if (token.getType() != Type.WHITESPACE) {
                            index = getFirstLetterEnd(token.getValue());
                            
                            LayoutToken first = new InlineTextLayoutToken(ilb, token.getType(), token.getValue().substring(0, index));
                            first.updateWidth(c);
                            remainingWidth -= first.getWidth();
                            
                            ilb.addInlineChild(c, first);
                            
                            if (index == token.getValue().length()) {
                                continue;
                            }
                        }
                        
                        firstLetterComplete = true;
                        
                        if (!firstLineComplete) {
                            style = c.getFirstLinesTracker().deriveAll(node.getStyle());
                            ilb = new InlineLayoutBox(c, ib.getElement(), style, maxAvailableWidth);
                            token = new InlineTextLayoutToken(ilb, token.getType(), token.getValue().substring(index));
                        } else {
                            style = node.getStyle();
                            ilb = new InlineLayoutBox(c, ib.getElement(), style, maxAvailableWidth);
                            token = new InlineTextLayoutToken(ib, token.getType(), token.getValue().substring(index));
                        }
                        
                        if (style.getWordWrap() == IdentValue.BREAK_WORD) {
                            //this ensures that we always keep the "first letter" together
                            LayoutToken breakingToken = new BreakToken(ilb);
                            mark = new Mark(i.previousIndex(), ilb, ci.previousIndex(), breakingToken);
                            ilb.addInlineChild(c, breakingToken);
                        }
                    }
                    
                    token.updateWidth(c);
                    
                    if (token.getWidth() > remainingWidth) {
                        if (mark == null) {
                            if (token.getType() == Type.WHITESPACE) {
                                if (inlineLayoutBoxes.isEmpty()) {
                                    currentLine.addChildForLayout(c, ilb);
                                } else {
                                    ((InlineLayoutBox) inlineLayoutBoxes.peek()).addInlineChild(c, ilb);
                                    inlineLayoutBoxes.push(ilb);
                                }
                                
                                box.addChildForLayout(c, currentLine);
                                currentLine = newLine(c, currentLine.getX() + currentLine.getHeight(), box);
                                remainingWidth = maxAvailableWidth;
                                
                                if (!firstLineComplete) {
                                    firstLineComplete = true;
                                    style = node.getStyle();
                                }

                                continue;
                            } else if (style.getWordWrap() == IdentValue.BREAK_WORD) {
                                int start = 0;
                                
                                //TODO this is a naive implementation that does not take graphemes into account
                                for (int index = 1; index <= token.getValue().length(); index++) {
                                    String s = token.getValue().substring(start, index);
                                    
                                    FSFont font = style.getFSFont(c);
                                    int width = c.getTextRenderer().getWidth(c.getFontContext(), font, s);
                                    
                                    if (width > remainingWidth) {
                                        if (currentLine.getChildCount() > 0) {
                                            index--;
                                            
                                            LayoutToken lt = new InlineTextLayoutToken(ilb, token.getType(), token.getValue().substring(start, index));
                                            ilb.addInlineChild(c, lt);
                                        }
                                        
                                        if (inlineLayoutBoxes.isEmpty()) {
                                            currentLine.addChildForLayout(c, ilb);
                                        } else {
                                            ((InlineLayoutBox) inlineLayoutBoxes.peek()).addInlineChild(c, ilb);
                                            inlineLayoutBoxes.push(ilb);
                                        }
                                        box.addChildForLayout(c, currentLine);
                                        currentLine = newLine(c, currentLine.getX() + currentLine.getHeight(), box);
                                        
                                        if (!firstLineComplete) {
                                            firstLineComplete = true;
                                            style = node.getStyle();
                                        }
                                        
                                        start = index;
                                    }
                                }
                            } else {
                                ilb.addInlineChild(c, token);
                                remainingWidth -= token.getWidth();
                            }
                        } else if (!firstLineComplete && token instanceof BreakToken) {
                            if (style.getWordWrap() == IdentValue.BREAK_WORD) {
                                int start = 0;
                                
                                //TODO this is a naive implementation that does not take graphemes into account
                                for (int index = 1; index <= token.getValue().length(); index++) {
                                    String s = token.getValue().substring(start, index);
                                    
                                    FSFont font = style.getFSFont(c);
                                    int width = c.getTextRenderer().getWidth(c.getFontContext(), font, s);
                                    
                                    if (width > remainingWidth) {
                                        if (currentLine.getChildCount() > 0) {
                                            index--;
                                            
                                            LayoutToken lt = new InlineTextLayoutToken(ilb, token.getType(), token.getValue().substring(start, index));
                                            ilb.addInlineChild(c, lt);
                                        }
                                        
                                        if (inlineLayoutBoxes.isEmpty()) {
                                            currentLine.addChildForLayout(c, ilb);
                                        } else {
                                            ((InlineLayoutBox) inlineLayoutBoxes.peek()).addInlineChild(c, ilb);
                                            inlineLayoutBoxes.push(ilb);
                                        }
                                        box.addChildForLayout(c, currentLine);
                                        currentLine = newLine(c, currentLine.getX() + currentLine.getHeight(), box);
                                        
                                        if (!firstLineComplete) {
                                            firstLineComplete = true;
                                            style = node.getStyle();
                                        }
                                        
                                        start = index;
                                    }
                                }
                            } else {
                                ilb.addInlineChild(c, token);
                                remainingWidth -= token.getWidth();
                            }
                        } else {
                            if (inlineLayoutBoxes.isEmpty()) {
                                currentLine.addChildForLayout(c, ilb);
                            } else {
                                ((InlineLayoutBox) inlineLayoutBoxes.peek()).addInlineChild(c, ilb);
                                inlineLayoutBoxes.push(ilb);
                            }
                            
                            for (ListIterator li = currentLine.getChildren().listIterator(currentLine.getChildCount()); li.hasPrevious();) {
                                Object o = li.previous();
                                
                                if (o == mark.getCurrentILB()) {
                                    InlineLayoutBox markedILB = mark.getCurrentILB();
                                    
                                    for (ListIterator oli = markedILB.getInlineChildren().listIterator(markedILB.getInlineChildCount()); oli.hasPrevious();) {
                                        boolean stop = oli.previous() == mark.getBreakToken();
                                        oli.remove();

                                        if (stop) {
                                            break;
                                        }
                                    }
                                    
                                    break;
                                } else {
                                    li.remove();
                                }
                            }
                            
                            box.addChildForLayout(c, currentLine);
                            currentLine = newLine(c, currentLine.getX() + currentLine.getHeight(), box);
                            
                            if (!firstLineComplete) {
                                firstLineComplete = true;
                                style = node.getStyle();
                            }
                            
                            i = box.getInlineContent().listIterator(mark.getInlineContentIndex());
                            ib = (InlineBox) i.next();
                            ci = ib.getContentTokens(mark.getContentTokenIndex());
                            
                            ilb = new InlineLayoutBox(c, ib.getElement(), style, maxAvailableWidth);
                            mark = null;
                            remainingWidth = maxAvailableWidth;
                        }
                    } else {
                        ilb.addInlineChild(c, token);
                        remainingWidth -= token.getWidth();
                    }
                }
                
                if (inlineLayoutBoxes.isEmpty()) {
                    currentLine.addChildForLayout(c, ilb);
                } else {
                    ((InlineLayoutBox) inlineLayoutBoxes.peek()).addInlineChild(c, ilb);
                    inlineLayoutBoxes.push(ilb);
                }
            } else {
                BlockBox child = (BlockBox)node;
                
                if (child.getStyle().isNonFlowContent()) {
                    //TODO handle
                } else if (child.getStyle().isInlineBlock() || child.getStyle().isInlineTable()) {
                    child.setContainingBlock(box);
                    child.setContainingLayer(c.getLayer());
                    child.initStaticPos(c, box, initialY);
                    child.calcCanvasLocation();
                    child.layout(c);


                    if (child.getWidth() > remainingWidth) {
                        if (mark == null || (!firstLineComplete && mark.getBreakToken() instanceof BreakToken)) {
                            if (style.getWordWrap() == IdentValue.BREAK_WORD) {
                                markerData = null;
                                contentStart = 0;
                                box.addChildForLayout(c, currentLine);
                                currentLine = newLine(c, currentLine.getX() + currentLine.getHeight(), box);
                                firstLineComplete = true;

                                remainingWidth = maxAvailableWidth;
                                remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, currentLine, remainingWidth);
                                
                                child.reset(c);
                                child.setContainingBlock(box);
                                child.setContainingLayer(c.getLayer());
                                child.initStaticPos(c, box, initialY);
                                child.calcCanvasLocation();
                                child.layout(c);
                            } else {
                                currentLine.addChildForLayout(c, child);
                                currentLine.setContainsBlockLevelContent(true);
                                
                                remainingWidth -= child.getWidth();
                            }
                        } else {
                            markerData = null;
                            contentStart = 0;
                            box.addChildForLayout(c, currentLine);
                            currentLine = newLine(c, currentLine.getX() + currentLine.getHeight(), box);
                            firstLineComplete = true;
                            
                            remainingWidth = maxAvailableWidth;
                            remainingWidth -= c.getBlockFormattingContext().getFloatDistance(c, currentLine, remainingWidth);
                            
                            child.reset(c);
                            child.setContainingBlock(box);
                            child.setContainingLayer(c.getLayer());
                            child.initStaticPos(c, box, initialY);
                            child.calcCanvasLocation();
                            child.layout(c);
                        }
                    } else {
                        currentLine.addChildForLayout(c, child);
                        currentLine.setContainsBlockLevelContent(true);
                        
                        remainingWidth -= child.getWidth();
                    }
                    
                    firstLetterComplete = true;
                } else {
                    assert false: "how did we get here?";
                }
            }
        }
        
        box.addChildForLayout(c, currentLine);
    }
    
    private static LineBox newLine(LayoutContext c, int y, Box box) {
        LineBox result = new LineBox(box);
        
        result.initContainingLayer(c);
        result.setY(y);
        result.calcCanvasLocation();

        return result;
    }
    
    private static int getFirstLetterEnd(String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            int type = Character.getType(c);
            if (type == Character.START_PUNCTUATION || 
                    type == Character.END_PUNCTUATION ||
                    type == Character.INITIAL_QUOTE_PUNCTUATION ||
                    type == Character.FINAL_QUOTE_PUNCTUATION ||
                    type == Character.OTHER_PUNCTUATION) {
                i++;
            } else {
                break;
            }
        }
        if (i < text.length()) {
            i++;
        }
        return i;
    }    
}
