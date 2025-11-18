
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

import java.io.UnsupportedEncodingException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * xxxxx.bdjo syntax - section 10.2.1.2
 *
 * @author A. Sundararajan
 */
@XmlRootElement
@XmlType(propOrder={})
public class BDJO {
    public static final int FILE_MAX = 99999;
    public static final String type = "BDJO";
    
    // section 10.2.3.3
    public static final String LANGUAGE_CODE_WILDCARD = "*.*";
    public static final String LANGUAGE_CODE_FALLBACK = "%%%";
    public static final String LANGUAGE_CODE_ENGLISH = "eng";
    
    // 10.2.8.2 directory_paths_length is specified in 16 bits
    public static final int NUM_FILEACCESSINFO_BITS = 16;
    public static final int MAX_FILEACCESSINFO_LENGTH = (1 << NUM_FILEACCESSINFO_BITS);
    
    private Version version = Version.V_0200;
    private TerminalInfo terminalInfo;
    private AppCacheInfo appCacheInfo;
    private TableOfAccessiblePlayLists tableOfAccessiblePlayLists;
    private ApplicationManagementTable applicationManagementTable;
     // this should OR of constants in KeyInterestTable class
    private int keyInterestTable;
    private String fileAccessInfo;
    
    public static void checkFileName(String fileName) {        
        if (fileName.length() != 5) {
            throw new IllegalArgumentException("invalid file : " + fileName);
        }
        for (int i = 0; i < fileName.length(); i++) {
            char ch = fileName.charAt(i);
            if (! Character.isDigit(ch)) {
                throw new IllegalArgumentException("invalid file : " + fileName);
            }
        }
    }
    
    public static void checkLanguage(String name) {
        if (name.length() != 3) {
            throw new IllegalArgumentException("language code length != 3");
        }
    }
    
    public static int utf8Length(String str) {
        try {
            return str.getBytes("UTF-8").length;
        } catch (UnsupportedEncodingException uee) {
            // ignore, can not happen
            return -1;
        }
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public TerminalInfo getTerminalInfo() {
        return terminalInfo;
    }

    public void setTerminalInfo(TerminalInfo terminalInfo) {
        this.terminalInfo = terminalInfo;
    }
    
    public AppCacheInfo getAppCacheInfo() {
        return appCacheInfo;
    }

    public void setAppCacheInfo(AppCacheInfo appCacheInfo) {
        this.appCacheInfo = appCacheInfo;
    }
    
    public TableOfAccessiblePlayLists getTableOfAccessiblePlayLists() {
        return tableOfAccessiblePlayLists;
    }

    public void setTableOfAccessiblePlayLists(TableOfAccessiblePlayLists tableOfAccessiblePlayLists) {
        this.tableOfAccessiblePlayLists = tableOfAccessiblePlayLists;
    }
    
    public ApplicationManagementTable getApplicationManagementTable() {
        return applicationManagementTable;
    }

    public void setApplicationManagementTable(ApplicationManagementTable applicationManagementTable) {
        this.applicationManagementTable = applicationManagementTable;
    }
    
    @XmlJavaTypeAdapter(HexStringIntegerAdapter.class)
    public Integer getKeyInterestTable() {
        return keyInterestTable;
    }

    public void setKeyInterestTable(Integer keyInterestTable) {
        this.keyInterestTable = keyInterestTable;
    }
    
    public String getFileAccessInfo() {
        return fileAccessInfo;
    }

    public void setFileAccessInfo(String fileAccessInfo) {
        if (fileAccessInfo != null) {
            int len = fileAccessInfo.length();
            if (len >= MAX_FILEACCESSINFO_LENGTH) {
                throw new IllegalArgumentException("fileAccessInfo length exceeded : " + len);
            }
        }
        this.fileAccessInfo = fileAccessInfo;
    }
}
