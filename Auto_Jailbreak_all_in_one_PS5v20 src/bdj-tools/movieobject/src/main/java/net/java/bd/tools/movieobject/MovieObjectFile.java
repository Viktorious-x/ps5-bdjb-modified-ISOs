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


package net.java.bd.tools.movieobject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder={"version", "movieObjects", "extensionData", 
                    "paddingsN1", "paddingsN2"})
public class MovieObjectFile {
    
    static final String FILETYPE = "MOBJ";
    String version;
    int padding_N1;
    int padding_N2;
    MovieObjects movieObjects = new MovieObjects();
    ExtensionData extensionData = new ExtensionData();
    
    public MovieObjects getMovieObjects() {
        return movieObjects;
    }
    public void setMovieObjects(MovieObjects mObject) {
        this.movieObjects = mObject;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public ExtensionData getExtensionData() {
        return extensionData;
    }
    public void setExtensionData(ExtensionData data) {
        this.extensionData = data;
    }
    public void setPaddingsN1(int padding) {
        this.padding_N1 = padding;
    }
    public int getPaddingsN1() {
        return padding_N1;
    }
    public void setPaddingsN2(int padding) {
        this.padding_N2 = padding;
    }
    public int getPaddingsN2() {
        return padding_N2;
    }
    public void readObject(DataInputStream din) 
            throws IOException {
        // 8*4 bit type indicator
        // 8*4 bit version number
        // 32 bit extension_start_address
        // 224 bit reserved
        // MovieObjects
        // N1 * short paddings
        // ExtensionData
        // N2 * short paddings
        
        String type = StringIOHelper.readISO646String(din, 4);
        String version = StringIOHelper.readISO646String(din, 4);
        if (!FILETYPE.equals(type)) {
            throw new IOException("Type indicator mismatch, read " + type);
        }
        if ("0100".equals(version) || "0200".equals(version)) {
           setVersion(version);
        } else {
            throw new IOException("Unexpected version number read, " + version);            
        }
        
        int extensionStartAddr = din.readInt();
        din.skipBytes(28); // reserved
       
        if (extensionStartAddr == 0) { 
            // no extension data, just read off of the main stream
            movieObjects.readObject(din);
            int paddings = seekPaddings(din);
            setPaddingsN1(paddings);
            setPaddingsN2(0);
        } else {
            byte[] bytes = new byte[extensionStartAddr - 40];
            din.read(bytes);
            DataInputStream substream = new DataInputStream(
                    new ByteArrayInputStream(bytes));
            movieObjects.readObject(substream);
            int paddings = seekPaddings(substream);
            setPaddingsN1(paddings);
            substream.close();
            
            extensionData.readObject(din);
            paddings = seekPaddings(din);
            setPaddingsN2(paddings);
        }
        
    }
    public void writeObject(DataOutputStream dout) throws IOException {
        // 8*4 bit type indicator
        // 8*4 bit version number
        // 32 bit extension_start_address
        // 224 bit reserved
        // MovieObjects
        // N1 * short paddings
        // ExtensionData
        // N2 * short paddings
        
        int extensionStartAddr;
        byte[] reserved = new byte[28];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream moStream = new DataOutputStream(baos);
        
        dout.write(StringIOHelper.getISO646Bytes(FILETYPE));
        dout.write(StringIOHelper.getISO646Bytes(getVersion()));

        movieObjects.writeObject(moStream);
        for (int i = 0; i < getPaddingsN1(); i++) {
            moStream.writeShort(0);
        }
        moStream.flush();
        moStream.close();
        byte[] moData = baos.toByteArray();
        
        if (extensionData.getData() == null) { // no extension data
            extensionStartAddr = 0;
        } else {
            extensionStartAddr = moData.length + 40;
        }
        
        dout.writeInt(extensionStartAddr);
        dout.write(reserved);
        dout.write(moData);
        
        if (extensionStartAddr != 0) {
            extensionData.writeObject(dout);
            for (int i = 0; i < getPaddingsN2(); i++) {
                dout.writeShort(0);
            }
        }
    }
    
    // Find out how many 16-bit paddings are at the end of the stream.
    private int seekPaddings(DataInputStream in) throws IOException {
        int i = 0;
        try {
            while (true) {
                in.readUnsignedShort();
                i++;
            }
        } catch (EOFException e) {
        }
        
        return i;
    }    
}
