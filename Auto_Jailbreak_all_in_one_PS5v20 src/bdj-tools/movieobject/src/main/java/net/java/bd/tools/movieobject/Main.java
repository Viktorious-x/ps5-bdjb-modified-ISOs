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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;

/**
 * This tool translates MovieObject.bdmv to an xml format and back.
 * See BD-ROM Part 3-1 10.3 MovieObject.bdmv structure for the format.
 */
public class Main {
    
   public static void main(String[] args) throws Exception {
       
       if (args.length < 2) {
           System.out.println("Missing input and output arguments");
           usage();
       }
       
       String input = args[0];
       String output = args[1];
      
       if (!((input.toLowerCase().endsWith(".xml")  || input.toLowerCase().endsWith(".bdmv")) &&
             (output.toLowerCase().endsWith(".xml") || output.toLowerCase().endsWith(".bdmv")))) {
           System.out.println("Input and output can only have xml or bdmv extension.");
           usage();
       }
       
       if (!new File(input).exists()) {
           System.out.println("File " + input + " not found.");
           usage();
       }
       
       MovieObjectFile idObject = null;
       FileInputStream fin = new FileInputStream(input);
       DataInputStream din = new DataInputStream(new BufferedInputStream(fin));
       if (input.toLowerCase().endsWith("xml")) {
          idObject = new MovieObjectReader().readXml(din); 
       } else {
          idObject = new MovieObjectReader().readBinary(din);
       }
       din.close();

       FileOutputStream fout = new FileOutputStream(output);
       DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(fout));       
       if (output.toLowerCase().endsWith("xml")) {
          new MovieObjectWriter().writeXml(idObject, dout);           
       } else {
          new MovieObjectWriter().writeBinary(idObject, dout);           
       }
       dout.close();
   }
   
   public static void usage() {
       System.out.println("\n\nThis is a tool to convert MovieObject.bdmv to an xml format and back.\n\n");
       System.out.println("Usage:");
       System.out.println("\n" + Main.class.getName() + " Input Output \n");
       System.out.println("where Input can be one of");
       System.out.println("   location of MovieObject.bdmv");
       System.out.println("   location of MovieObject.xml");
       System.out.println("and the Output can be one of");
       System.out.println("   MovieObject.bdmv");
       System.out.println("   MovieObject.xml");       
       
       System.exit(1);
   }
}
