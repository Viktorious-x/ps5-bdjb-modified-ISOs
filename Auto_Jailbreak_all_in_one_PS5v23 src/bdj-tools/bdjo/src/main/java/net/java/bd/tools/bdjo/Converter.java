
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.script.ScriptException;
import javax.xml.bind.JAXBException;

/**
 * Utility class to convert between different serializarion 
 * formats for BDJO.
 *
 * @author A. Sundararajan
 */
public final class Converter {
    // don't create me!
    private Converter() {}
    
    /**
     * Converts a .bdjo file to a JavaFX script.
     */
    public static void bdjoToFX(InputStream in, Writer out) 
            throws IOException {
        BDJO bdjo = BDJOReader.readBDJO(in);
        BDJOWriter.writeFX(bdjo, out);
    }
    
    /**
     * Converts a .bdjo file to an XML document using Java XML
     * binding (JAXB).
     */
    public static void bdjoToXML(InputStream in, Writer out)
            throws IOException, JAXBException {
        BDJO bdjo = BDJOReader.readBDJO(in);
        BDJOWriter.writeXML(bdjo, out);
    }
    
    /**
     * Converts an XML document to a JavaFX script.
     */
    public static void xmlToFX(Reader in, Writer out)
            throws IOException, JAXBException {
        BDJO bdjo = BDJOReader.readXML(in);
        BDJOWriter.writeFX(bdjo, out);
    }
    
    
    /**
     * Converts an XML document to a .bdjo file.
     */
    public static void xmlToBDJO(Reader in, OutputStream out) 
            throws IOException, JAXBException {
        BDJO bdjo = BDJOReader.readXML(in);
        BDJOWriter.writeBDJO(bdjo, out);
    }
    
    /**
     * Converts a JavaFX script to an XML document.
     */
    public static void fxToXML(Reader in, Writer out) 
            throws ScriptException, JAXBException {
        BDJO bdjo = BDJOReader.readFX(in);
        BDJOWriter.writeXML(bdjo, out);
    }

    /**
     * Converts a JavaFX document to a .bdjo file.
     */
    public static void fxToBDJO(Reader in, OutputStream out)
            throws IOException, ScriptException {
        BDJO bdjo = BDJOReader.readFX(in);
        BDJOWriter.writeBDJO(bdjo, out);
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.printf("Usage: %s <file-1> <file-2>", Converter.class.getName());
            System.exit(1);
        }
        String in = args[0];
        String out = args[1];
        if (in.endsWith(".fx")) {
            if (out.endsWith(".xml")) {
                Reader r = new BufferedReader(new FileReader(in));
                Writer o = new BufferedWriter(new FileWriter(out));
                fxToXML(r, o);
            } else if (out.endsWith(".bdjo")) {
                Reader r = new BufferedReader(new FileReader(in));
                OutputStream o = new BufferedOutputStream(new FileOutputStream(out));
                fxToBDJO(r, o);
            } else {
                System.err.println("not supported yet!");
            }
        } else if (in.endsWith(".xml")) {
            if (out.endsWith(".fx")) {
                Reader r = new BufferedReader(new FileReader(in));
                Writer o = new BufferedWriter(new FileWriter(out));
                xmlToFX(r, o);
            }  else if (out.endsWith(".bdjo")) {
                Reader r = new BufferedReader(new FileReader(in));
                OutputStream o = new BufferedOutputStream(new FileOutputStream(out));
                xmlToBDJO(r, o);
            } else {
                System.err.println("not supported yet!");
            }
        } else if (in.endsWith(".bdjo")) {
            if (out.endsWith(".fx")) {
                InputStream i = new BufferedInputStream(
                        new FileInputStream(in));
                Writer o = new BufferedWriter(new FileWriter(out));
                bdjoToFX(i, o);
            } else if (out.endsWith(".xml")) {
                InputStream i = new BufferedInputStream(
                        new FileInputStream(in));
                Writer o = new BufferedWriter(new FileWriter(out));
                bdjoToXML(i, o);
            } else {
                System.err.println("not supported yet!");
            }
        } else {
            System.err.println("Unsupported input format");
            System.exit(1);
        }
    }
}
