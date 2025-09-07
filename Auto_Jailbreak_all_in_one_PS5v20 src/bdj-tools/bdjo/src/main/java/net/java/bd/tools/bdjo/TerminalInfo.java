
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

package net.java.bd.tools.bdjo;

import javax.xml.bind.annotation.XmlType;

/**
 * TerminalInfo - sections 10.2.2.2 and 10.2.2.3
 * @author A. Sundararajan
 */
@XmlType(propOrder={})
public class TerminalInfo {
    // default_font_file_name
    private String defaultFontFile;
    // initial_HAVi_configuation_id
    private HaviDeviceConfig initialHaviConfig = HaviDeviceConfig.HD_1920_1080;
    // Table 10.2. - menu_call_mask flag
    private boolean menuCallMask;
    // Table 10.3 - title_search_mask flag
    private boolean titleSearchMask;
    private boolean mouseSupported;
    private boolean mouseInterest;
    private int initialOutputMode;
    private int initialFrameRate;
    
    public TerminalInfo() {
        this("*****", HaviDeviceConfig.HD_1920_1080, false, false, 
             false, false, 0, 0);
    }
    
    public TerminalInfo(String defaultFontFile, HaviDeviceConfig initialHaviConfig,
            boolean menuCallMask, boolean titleSearchMask,
            boolean mouseSupported, boolean mouseInterest,
            int initialOutputMode, int initialFrameRate) 
    {
        setDefaultFontFile(defaultFontFile);
        setInitialHaviConfig(initialHaviConfig);
        setMenuCallMask(menuCallMask);
        setTitleSearchMask(titleSearchMask);
        setMouseSupported(mouseSupported);
        setMouseInterest(mouseInterest);
        setInitialOutputMode(initialOutputMode);
        setInitialFrameRate(initialFrameRate);
    }
    
    public String getDefaultFontFile() {
        return defaultFontFile;
    }
    
    public void setDefaultFontFile(String defaultFontFile) {
        // default font file is either "*****" or NNNNN 
        // where N is a digit - see section 10.2.2.3 
        if (defaultFontFile != null) {
            if (! defaultFontFile.equals("*****")) {
                BDJO.checkFileName(defaultFontFile);
            }
        }
        this.defaultFontFile = defaultFontFile;
    }
    
    public HaviDeviceConfig getInitialHaviConfig() {
        return initialHaviConfig;
    }
    
    public void setInitialHaviConfig(HaviDeviceConfig initialHaviConfig) {
        this.initialHaviConfig = initialHaviConfig;
    }
    
    public boolean isMenuCallMask() {
        return menuCallMask;
    }
    
    public void setMenuCallMask(boolean menuCallMask) {
        this.menuCallMask = menuCallMask;
    }
    
    public boolean isTitleSearchMask() {
        return titleSearchMask;
    }
    
    public void setTitleSearchMask(boolean titleSearchMask) {
        this.titleSearchMask = titleSearchMask;
    }

    public boolean isMouseSupported() {
        return mouseSupported;
    }

    public void setMouseSupported(boolean mouseSupported) {
        this.mouseSupported = mouseSupported;
    }

    public boolean isMouseInterest() {
        return mouseInterest;
    }

    public void setMouseInterest(boolean mouseInterest) {
        this.mouseInterest = mouseInterest;
    }

    public int getInitialOutputMode() {
        return initialOutputMode;
    }

    public void setInitialOutputMode(int initialOutputMode) {
        this.initialOutputMode = initialOutputMode;
    }

    public int getInitialFrameRate() {
        return initialFrameRate;
    }

    public void setInitialFrameRate(int initialFrameRate) {
        this.initialFrameRate = initialFrameRate;
    }
}
