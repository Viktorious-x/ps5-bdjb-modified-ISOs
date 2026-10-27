
/*  
 * Copyright (c) 2007, Sun Microsystems, Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  Note:  In order to comply with the binary form redistribution 
 *         requirement in the above license, the licensee may include 
 *         a URL reference to a copy of the required copyright notice, 
 *         the list of conditions and the disclaimer in a human readable 
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a 
 *         Blu-ray disc containing the binary form of the code or could 
 *         be put in a JAR file that is broadcast via a digital television 
 *         broadcast medium.  In any event, you must include in any end 
 *         user licenses governing any code that includes the code subject 
 *         to the above license (in source and/or binary form) a disclaimer 
 *         that is at least as protective of Sun as the disclaimers in the 
 *         above license.
 * 
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at 
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 * 
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

package com.hdcookbook.grin.animator;

import com.hdcookbook.grin.util.Debug;

import java.awt.Rectangle;

/**
 * This class is the render context at the base of a stack of render contexts.
 * It's where the real data lives that's used to track drawing.
 **/
class RenderContextBase extends RenderContext {


    private int currTarget; 
        // The render area target for erasing and drawing

    Rectangle[] drawTargets;
        // Targets for drawing

    int numDrawTargets;
        // Number of drawTargets that need to be drawn to in the current
        // frame

    Rectangle[] eraseTargets;
        // Targets for erasing

    private Rectangle collapsed = new Rectangle(); 
        // see collapseTargets(Rectangle[])

    private DrawRecord thisFrameList = null;
        // A list of DrawRecord instances used to addArea() in this
        // frame of animation.  It's a singly-linked list kept as a
        // stack (that is, LIFO).

    private DrawRecord guaranteeList = null;
        // A list of DrawRecord instances used in guaranteeAreaFilled.
        // A singly-linked list, kept in order of insertion (that
        // is, FIFO).

    private DrawRecord guaranteeListLast = null;
        // The last record on guaranteeList.

    private DrawRecord lastFrameList = new DrawRecord();
        // A list of the DrawRecord instances used to addArea() in
        // the last frame of animation.  It's kept as a doubly-linked
        // list with a dummy node at the head.  It's in the same order 
        // as thisFrameList, and nodes are taken off of lastFrameList
        // as they are added to thisFrameList.

    private int collapseThreshold = 385*385;
        // We collapse two rectangles into one when doing so adds at most this
        // many pixels.  See setCollapseThreshold.

    private boolean targetsCanOverlap;

    RenderContextBase(int numTargets) {
        this.currTarget = 0;
        if (numTargets < 1) {
            numTargets = 1;
            // RenderContextBase piggybacks on drawTargets[0] to implement
            // setFullPaint(), so make sure there's at least one drawTarget,
            // e.g. to handle the case where there are no animation clients.
        }
        this.drawTargets = newRectArray(numTargets);
        this.eraseTargets = newRectArray(numTargets);
        lastFrameList.prev = lastFrameList;
        lastFrameList.next = lastFrameList;
    }

    private Rectangle[] newRectArray(int n) {
        Rectangle[] r = new Rectangle[n];
        for (int i = 0; i < n; i++) {
            r[i] = new Rectangle();
        }
        return r;
    }

    // 
    // Sets the threshold of the number of pixels of increased drawing the
    // engine is willing to tolerate in order to collapse two draw targets
    // into one.  It's more efficient to draw one slightly bigger area than
    // two smaller areas, but at some threshold it's better to leave them
    // divided.  By default, this threshold is set to 40,000 pixels
    // (e.g. a 200x200 area), but this is a guess.
    //
    // This value may be set to 0, or to -1.  Setting it to -1 can be valuable
    // to totally disable the collapse optimization, for clients that want a 
    // predictable and consistent render time.
    //
    void setCollapseThreshold(int collapseThreshold) {
        this.collapseThreshold = collapseThreshold;
    }

    void setTargetsCanOverlap(boolean v) {
        targetsCanOverlap = v;
    }

    /**
     * {@inheritDoc}
     **/
    public void addArea(DrawRecord r) {
        if (r.prev != null) {           // If on lastFrameList
                // remove from lastFrameList
            r.prev.next = r.next;
            r.next.prev = r.prev;
        } else {                        // Otherwise, it's newly activated
            r.resetPreviousFrame();
        }
        r.target = currTarget;
        r.addAreaTo(drawTargets[currTarget]);
        // Add to this frame's list
        r.next = thisFrameList;
        thisFrameList = r;
        r.prev = null;
    }

    /**
     * {@inheritDoc}
     **/
    public void guaranteeAreaFilled(DrawRecord filled) {
        filled.target = currTarget;

        // Remove from lastFrameList, if on it.
        if (filled.prev != null) {
            filled.prev.next = filled.next;
            filled.next.prev = filled.prev;
        }
        // add to guaranteeList
        if (guaranteeList == null) {
            guaranteeList = filled;
        } else {
            guaranteeListLast.next = filled;
        }
        guaranteeListLast = filled;
        filled.next = null;
        filled.prev = null;
    }

    /**
     * {@inheritDoc}
     **/
    public int setTarget(int newTarget) {
        int old = currTarget;
        currTarget = newTarget;
        return old;
    }

    //
    // Set the extent of this render context to empty.  This is done at the
    // beginning of each animation cycle.
    //
    void setEmpty() {
        for (int i = 0; i < drawTargets.length; i++) {
            drawTargets[i].width = 0;
            eraseTargets[i].width = 0;
        }
    }

    //
    // Sets the initial area that needs to be drawn.  This can be called
    // just after setEmpty(), but at no other time.
    //
    void setFullPaint(int x, int y, int width, int height) {
        drawTargets[0].setBounds(x, y, width, height);
    }

    //
    // Process the two draw record lists, lastFrameList and
    // thisFrameList.
    //
    // In lastFrameList, we process
    // any DrawRecord instances that were used in the previous
    // frame of animation, but that aren't used in this frame.  The
    // area of such a draw record needs to be erased.
    // 
    // In thisFrameList, we look for changes in z-order between this
    // frame of animation, and the last frame.  Any DrawRecord
    // that is in a different order represents an area that might have
    // to be re-drawn, because it might be involved in an overlay with
    // something else.  See
    // https://hdcookbook.dev.java.net/issues/show_bug.cgi?id=215
    //
    // This is done after AnimationClient.addDisplayAreas()
    //
    void processDrawRecordLists() {
        //
        // First, run through the list in reverse order, which is
        // the original order of calls to addArea().  On each node,
        // we add in the area of what now needs to be erased.
        //
        DrawRecord n = lastFrameList.prev;
        while (n != lastFrameList) {
            n.eraseLastFrame(drawTargets[n.target]);
                // This also does bookkeeping on DrawRecord.drawSequence
                // for us.
            DrawRecord tmp = n;
            n = n.prev;
            tmp.prev = null;
                // Nodes not on a list need to have a prev set to null,
                // since we use prev to know to take a node off lastFrameList
                // in other methods of this class.
            tmp.next = null;
                // We also null out the next pointer, in order to minimize
                // any problem with unintentional object retention, in case
                // our client retains some DrawRecord instances, but not 
                // others.
        }
        //
        // Now, set lastFrameList to thisFrameList, and set up the
        // backwards links, in preparation for the next frame.
        // thisFrameList becomes empty.
        //
        lastFrameList.next = thisFrameList;  // head is dummy node
        thisFrameList = null;
        n = lastFrameList.next;
        DrawRecord prev = lastFrameList;
        int drawSequence = 1;
        int lastDrawSequence = 0;
        while (n != null) {
            lastDrawSequence = n.finishedFrame(drawSequence, lastDrawSequence, 
                                               drawTargets[n.target]);
            drawSequence++;
            n.prev = prev;
            prev = n;
            n = n.next;
        }
        prev.next = lastFrameList;  // make it circular
        lastFrameList.prev = prev;
    }

    //
    // Empty out the lastFrameList.  This removes the DrawRecord
    // instances from lastFrameList, and re-initialize them.  This
    // needs to be called if a new RenderContext is created for use
    // with DrawTarget instances that have been previously used.
    //
    // This is called from AnimationEngine when a new list of
    // animation clients is given to a running animation engine
    // (see checkNewClients() and resetAnimationClients() in
    // AnimationEngine).  This method is called at the beginning of
    // a frame of animation, so only the last frame list has DrawRecord
    // instance on it -- thisFrameList is empty.
    //
    // This reset is needed because the RenderContext's list of DrawRecord
    // instances is held as an old-style linked list held in data members
    // of DrawRecord.
    // 
    void resetLastFrameList() {
        if (Debug.ASSERT && thisFrameList != null) {
            Debug.assertFail();
        }
        DrawRecord n = lastFrameList.next;
        while (n != lastFrameList) {
            n.resetPreviousFrame();
            DrawRecord tmp = n.next;
            n.next = null;
            n.prev = null;
            n = tmp;
        }
        lastFrameList.next = lastFrameList;
        lastFrameList.prev = lastFrameList;
    }
    
    static void setEmpty(Rectangle r) {
        r.width = 0;
        r.height = 0;
    }

    static boolean isEmpty(Rectangle r) {
        return r.width <= 0;
    }


    /**
     * Collapse the erase targets and the draw targets to an optimal
     * set.
     **/
    void collapseTargets() {

                // First, we try to optimally collapse the targets.
        numDrawTargets = collapseTargets(drawTargets);
    }

    //
    // Collapse the draw areas into an optimal set.  Return the number
    // of targets that need to be drawn; targets[0..n-1]
    // will need to be drawn .  If no drawing is needed, n will
    // be 0.
    //
    private int collapseTargets(Rectangle[] targets) {

        int n = purgeEmpty(targets, targets.length) - 1;

        // Now, targets[0..n] are non-empty

                // Next, figure out which areas should be collapsed.
                // As a SWAG, we collapse areas when combining them
                // at most adds collapseThreshold pixels to the area 
                // of the screen drawn to.
                //
                // This is an area where it would be worth measuring what
                // is optimal, and perhaps even using different heuristics
                // based on player.
                //
                // Note that this algorithm is O(n^3) on the number of
                // targets.

        if (Debug.ASSERT) {
            for (int i = 0; i < n; i++) {
                if (isEmpty(targets[i])) {
                    Debug.assertFail();
                }
            }
        }
    collapse: 
        for (;;) {
            for (int i = 0; i < n; i++) {
                for (int j = i+1; j <= n; j++) {
                    collapsed.setBounds(targets[i]);
                    collapsed.add(targets[j]);
                        // If there's a seperate erase step,
                        // we conservatively combine intersecting draw rects
                        // here, since it's not OK to draw an area twice
                        // in SrcOver mode.
                        //
                        // This could be a bit more efficient, in the
                        // case where the intersection is compeletely
                        // contained within one of the rectangles and
                        // all on one side of the other.  In this case,
                        // instead of collapsing, the other rectangle
                        // could be made smaller.
                    boolean combine = !targetsCanOverlap
                                      && targets[i].intersects(targets[j]);
                    if (!combine) {
                        int ac = collapsed.width * collapsed.height;
                        int a = targets[i].width * targets[i].height
                               + targets[j].width * targets[j].height;

                        combine = ac <= a + collapseThreshold;
                    }
                    if (combine) {
                        // combine them
                        targets[i].setBounds(collapsed);
                        if (j < n) {
                            Rectangle ra = targets[j];
                            targets[j] = targets[n];
                            targets[n] = ra;
                        }
                        setEmpty(targets[n]);  
                            // Not necessary, but fast and adds some robustness
                        n--;
                        continue collapse;   // yay goto!  :-)
                            // Transfers control to top of for(;;) loop
                            // labeled by collapse, see
                            // http://java.sun.com/docs/books/tutorial/java/nutsandbolts/branch.html
                    }
                }
            }
            break collapse;
                // Transfers control outside of the for(;;) loop 
                // labeled by collapse, see
                // http://java.sun.com/docs/books/tutorial/java/nutsandbolts/branch.html
        }

        // At this point, targets[0..n] represents an optimal set of
        // the areas we need to display and erase.  Add one to get the 
        // length of the list of targets.

        return n+1;
    }


    // 
    // Purge the empty targets from the given array considering
    // [0..num-1]
    //
    private int purgeEmpty(Rectangle[] targets, int num) {

        int n = num - 1;

        while (n >= 0 && isEmpty(targets[n])) {
            n--;
        }
        // Now, targets[n] is non-empty, or n is -1

        for (int i = 0; i < n; ) {
            if (isEmpty(targets[i])) {
                Rectangle a = targets[n];
                targets[n] = targets[i];
                targets[i] = a;
                n--;
            } else {
                i++;
            }
        }
        // Now, targets[0..n] are non-empty

        return n+1;
    }

    //
    // Called by the animation engine just after collapsing targets,
    // this processes the areas that are guaranteed to be painted, in an
    // effort to minimize erasing.
    //
    // The erase targets correspond 1:1 with the draw targets.  Some erase
    // targets might be empty due to erase guarantees 
    // (see guaranteeAreaFilled()), so the caller should check every
    // eraseTarget with RenderContextBase.isEmpty() before erasing.
    //
    void calculateEraseTargets() {
        for (int i = 0; i < numDrawTargets; i++) {
            eraseTargets[i].setBounds(drawTargets[i]);
        }
        while (guaranteeList != null) {
            for (int i = 0; i < numDrawTargets; i++) {
                Rectangle area = eraseTargets[i];
                if (!isEmpty(area)) {
                    guaranteeList.applyGuarantee(area);
                }
            }
            guaranteeList = guaranteeList.next;
        }
        guaranteeListLast = null;
    }

}
