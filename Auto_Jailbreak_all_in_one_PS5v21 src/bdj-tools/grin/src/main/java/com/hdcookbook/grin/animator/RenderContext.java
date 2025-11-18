
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

import java.awt.Rectangle;

/**
 * This class represents a context for tracking the updates that will be
 * needed to render the next frame of animation.  It tracks areas that will
 * need to be displayed, and areas that will need to be erased and then
 * displayed.  The animation framework collapses these areas into an optimized
 * set.
 * <p>
 * Internally, two sets of "render area" targets are maintained, one set for
 * erasing, and one set for drawing.  Each
 * target computes its bounding rectangle.  Before erasing and before
 * painting, the animation
 * framework attempts to collapse these targets.
 **/

abstract public class RenderContext {

    /**
     * Add the given area to this render area, so that the animation manager
     * knows about it.  The framework will erase any part of the area that
     * needs it in this frame, cause drawing to any part of the area that
     * needs it in this frame, and arrange to erase any part of the area
     * that needs it in the next frame.
     * <p>
     * A given DrawRecord instance can't be added to a RenderContext more
     * than once.
     **/
    abstract public void addArea(DrawRecord r);

    /**
     * Guarantee that the given area will have all of its pixels filled
     * (e.g. by Src mode drawing, or by drawing with fully opaque pixels).
     * This information can be used to help optimize the areas that need
     * to be erased - the current RenderArea to be erased can have
     * this area removed from it.
     * <p>
     * If the fill-guarantee area doesn't result in the rectangular
     * bounds of the erase area being reduced, it probably won't help.
     * At least in the first implementation, only removal of a complete slice
     * of the bounding rectangle of a RenderArea is effective.
     * <p>
     * It's OK to call this method for an area that hasn't changed since
     * the last frame of drawing.  The area in question will only be
     * drawn to if requested by a call to one of the other methods.
     * <p>
     * A given DrawRecord instance can't be added to a RenderContext more
     * than once.  It also can't be used for both addArea() and
     * guaranteeAreaFilled().
     **/
    abstract public void guaranteeAreaFilled(DrawRecord r);

    /**
     * Set the render area target for this context to direct its
     * calls for adding areas.  Callers of this method should restore
     * the target to the old value when done, to put the RenderContext
     * back in a consistent state.
     * <p>
     * A typical scene should usually have a small number of targets, like
     * between one and maybe four.  See the discussion in 
     * AnimationClient.addDisplayAreas(RenderContext) for a more
     * in-depth discussion of this.
     *
     * @param newTarget The draw target number, obtained from mapDrawTargets
     *
     * @return the old target number
     *
     * @see AnimationClient#addDisplayAreas(RenderContext)
     * @see AnimationClient#mapDrawTargets(java.util.Hashtable)
     **/
    abstract public int setTarget(int newTarget);


}
