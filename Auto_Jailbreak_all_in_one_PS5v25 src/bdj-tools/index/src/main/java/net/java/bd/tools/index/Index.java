/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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

package net.java.bd.tools.index;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * BD-ROM Part 3-1 5.2 Index table file.
 */
@XmlRootElement
public class Index {

    String type;
    String version;
    AppInfoBDMV appInfo = new AppInfoBDMV();
    Indexes indexes = new Indexes();
    ExtensionData extensionData = new ExtensionData();
    int paddingN1, paddingN2, paddingN3;
    short[] n1, n2, n3;

    public Index() {}
    
    public void setVersion(String s) {
        this.version = s;
    }

    public String getVersion() {
        return version;
    }

    public void setAppInfo(AppInfoBDMV appInfo) {
        this.appInfo = appInfo;
    }
  
    public AppInfoBDMV getAppInfo() {
        return appInfo;
    }
    
    public void setIndexes(Indexes indexes) {
        this.indexes = indexes;
    }
  
    public Indexes getIndexes() {
        return indexes;
    }
    
    public void setExtensionData(ExtensionData extensionData) {
        this.extensionData = extensionData;
    }
  
    public ExtensionData getExtensionData() {
        return extensionData;
    }   
    
    public void setPaddingN1(int i) {
        paddingN1 = i;
    }
  
    public int getPaddingN1() {
        return paddingN1;
    }  
    
    public void setPaddingN1Data(short[] n1) {
        this.n1 = n1;
    }
    
    public short[] getPaddingN1Data() {
        return n1;
    }
    
    public void setPaddingN2(int i) {
        paddingN2 = i;
    }
  
    public int getPaddingN2() {
        return paddingN2;
    }    
    
    public void setPaddingN2Data(short[] n2) {
        this.n2 = n2;
    }
    
    public short[] getPaddingN2Data() {
        return n2;
    }
       
    public void setPaddingN3(int i) {
        paddingN3 = i;
    }
  
    public int getPaddingN3() {
        return paddingN3;
    }    
    
    public void setPaddingN3Data(short[] n3) {
        this.n3 = n3;
    }
    
    public short[] getPaddingN3Data() {
        return n3;
    } 
    
    public void readObject(DataInputStream din) throws IOException {
        // 8*4 bit type indicator
        // 8*4 bit version number
        // 32 bit Indexes_start_address
        // 32 bit ExtensionData_start_address
        // 192 reserved
        // AppInfoBDMV
        // Padding N1 * 16 bits
        // Indexes
        // Padding N2 * 16 bits
        // ExtensionData
        // padding N3 * 16 bits
        
        String typeIndicator = StringIOHelper.readISO646String(din, 4);
        String versionNumber = StringIOHelper.readISO646String(din, 4);
        int indexesAddress = din.readInt();
        int extensionAddress = din.readInt();
        
        if (!"INDX".equals(typeIndicator)) {
            throw new RuntimeException("TypeIndicator error " + typeIndicator);
        }
        setVersion(versionNumber);
        din.skipBytes(24);        
        
        int appInfoLength = indexesAddress - (4*4 + 24);
        byte[] appInfoBytes = new byte[appInfoLength];
        din.read(appInfoBytes);
        DataInputStream substream = 
                new DataInputStream(new ByteArrayInputStream(appInfoBytes));
        appInfo.readObject(substream);     
        n1 = seekPaddings(substream);
        setPaddingN1(n1.length);
        substream.close();
        
        if (extensionAddress == 0) {
            // no extension data, just read indexes and be done.
            indexes.readObject(din);
            n2 = seekPaddings(din);
            setPaddingN2(n2.length);
            setPaddingN3(0);
            return;
        } 
            
        // read indexes and extensions
        int indexesLength = extensionAddress - indexesAddress;
        byte[] indexesBytes = new byte[indexesLength];
        din.read(indexesBytes);
        DataInputStream substream2 
                = new DataInputStream(new ByteArrayInputStream(indexesBytes));
        indexes.readObject(substream2);
        n2 = seekPaddings(substream2);
        setPaddingN2(n2.length);        
        substream2.close();
            
        extensionData = new ExtensionData();
        extensionData.readObject(din);   
        n3 = seekPaddings(din);
        setPaddingN3(n3.length);
    }
    
    public void writeObject(DataOutputStream out) throws IOException {

        int indexDataStartAddr;
        int extensionDataStartAddr;
        byte[] reserved = new byte[24];
        
        // First, write appInfoBDMV and Indexes to the byte array block
        // find out how much byte they'd use.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream appInfoStream = new DataOutputStream(baos);
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        DataOutputStream indexDataStream = new DataOutputStream(baos2); 
        
        appInfo.writeObject(appInfoStream);
        if (paddingN1 != 0) {
            for (int i = 0; i < paddingN1; i++) {
                appInfoStream.writeShort(n1[i]);
            }
        }
        indexes.writeObject(indexDataStream);
        if (paddingN2 != 0) {
            for (int i = 0; i < paddingN2; i++) {
                appInfoStream.write(n2[i]);
            }
        }        
        appInfoStream.flush();
        indexDataStream.flush();
        
        indexDataStartAddr = ((4*4 + 24) + appInfoStream.size());
        if (extensionData.getData() == null) {
            extensionDataStartAddr = 0;
        } else {
            extensionDataStartAddr = indexDataStartAddr + indexDataStream.size();
        }
        
        // Now write out the entire dataset to the file.
        out.write(StringIOHelper.getISO646Bytes("INDX"));
        out.write(StringIOHelper.getISO646Bytes(getVersion()));
        out.writeInt(indexDataStartAddr);
        out.writeInt(extensionDataStartAddr);
        out.write(reserved);
        out.write(baos.toByteArray());  // appInfoBDMV
        out.write(baos2.toByteArray()); // Indexes
        if (extensionData.getData() != null) {
            extensionData.writeObject(out);  // extensionsDat       
        }
        if (paddingN3 != 0) {
            for (int i = 0; i < paddingN3; i ++) {
                appInfoStream.writeShort(n3[i]);
            }
        }        
        appInfoStream.close();
        indexDataStream.close();
    }
     
    // Find out how many 16-bit paddings are at the end of the stream.
    private short[] seekPaddings(DataInputStream in) 
            throws IOException {
        int count = 0;
        short[] s = new short[1024];
        try {
            s[count] = (short) in.readUnsignedShort();
            count++;
        } catch (EOFException e) {
        }
        
        return Arrays.copyOf(s, count);
    }
}
