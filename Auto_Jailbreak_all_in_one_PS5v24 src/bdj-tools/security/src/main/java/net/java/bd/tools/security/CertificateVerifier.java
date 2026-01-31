
/*  
 * Copyright (c) 2009, Sun Microsystems, Inc.
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

package net.java.bd.tools.security;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;
import sun.security.x509.*;
import sun.security.util.*;

/* 
 * A poor man's certificate verifier for BD-J.
 * This tool checks for items below.  Pass in the application certificate, root certificate
 * or the binding unit certificate to the command line.
 * 
 * The certificate:
 * 1. Need to be in SHA1WithRSA signature format,
 * 2. Validity needs to be in GeneralizedTime format, MHP 12.5.5
 * 3. Subject and Issuer lines can only have UTF8String encoding, HMP 12.5.6,
 * 4. Need IssuerAlternative and SubjectAlternative extensions set, MHP 12.5.9,
 * 
 * For the root cert:
 * 5. Need KeyUsage extension, marked critical, KeyUsage and keyCertSign turned on.   MHP 12.5.9,
 * 6. Need BasicConstraints extension, marked critical, MHP 12.11.2.10,
 *
 * For the app cert:
 * 7. Need KeyUsage extension, marked critical, digitalCert turned on.  MHP 12.5.9.
 *
 * Note that item 2 and 3 needs to be checked against the certificate file's bytecode,
 * since this is about DER encoding of the file, and this encoding detail can be disregarded after java
 * reconstructs data from the byte array.  For example, sun.security.x509.CertificateValidity class
 * can be constructed from the GeneralizedTime format bytearray, but it will return the default 
 * UTCTime format bytearray if one asks for the bytearray respresentation of the CertificateValidity 
 * instance regardless of what the original bytearray was.
 * Hence, this tool does a twofold check: first, through public certificate APIs for general
 * format, and second, by parsing the byte array of a certificate file directly.
 *
**/
public class CertificateVerifier {
    
    public static void main(String[] args) throws CertificateException {
        if (args == null || args.length < 1) {
            printUsage();
            return;
        }
        String type = args[0];
        File certFile = new File(args[1]);
        
        if (!certFile.exists()) {
            System.out.println("File not found " + certFile.getAbsolutePath());
            printUsage();
            return;
        }   
        new CertificateVerifier().runTest(type, certFile);
    }
    
    private String errorString = null;
    
    /*
     * Check two certificate files according to the bd-j specification.
     * @return true if the check passes, false if either certificate check results in an error.
     */
    public boolean runTest(String type, File certFile) {
     
        boolean failed = false;
        System.out.println("Starting the verfication for certificate file:" + certFile);
        X509Certificate cert = null;
        
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate)factory.generateCertificate(new FileInputStream(certFile));   
        } catch (Exception e) {
            System.out.println("Error in creating certificate from a file");
            e.printStackTrace();
            failed = true;
        }
        
        if (!failed) {
            System.out.println("Checking the certiticate");
            try {
                checkCert(cert, type);
                doParsingChecks(certFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (errorString != null){
                System.out.println(errorString);
                failed = true;
            }
        }
        System.out.println("Done with the verification for the certificate file:" + certFile);
        return !failed;
    }   
    
    // Check for some DER encoding format by inspecting the bytearray level.
    private void doParsingChecks(File file) throws Exception {
  
        FileInputStream fin = new FileInputStream(file);
        
        // Parsing logic is taken from sun.security.x509.CertInfoImpl.parse() method.
        DerValue tmp = new DerValue(fin).getData().getDerValue();
  
        if (tmp.tag != DerValue.tag_Sequence) {
            throw new CertificateParsingException("signed fields invalid");
        }

        DerInputStream in = tmp.getData();

        // Version
        tmp = in.getDerValue();
        CertificateVersion version = null;
        if (tmp.isContextSpecific((byte)0)) {
            version = new CertificateVersion(tmp);
            tmp = in.getDerValue();
        }
  
        // Serial number ... an integer
        CertificateSerialNumber serialNum = new CertificateSerialNumber(tmp);

        // Algorithm Identifier
        CertificateAlgorithmId algId = new CertificateAlgorithmId(in);

        // Issuer name
        checkForUTF8String(in.toByteArray());
        tmp = in.getDerValue(); // advance to the next element
        //CertificateIssuerName issuer = new CertificateIssuerName(in);
        //X500Name issuerDN = (X500Name)issuer.get(CertificateIssuerName.DN_NAME);

        // validity:  SEQUENCE { start date, end date }
        checkForGeneralizedTime(in.toByteArray());
        tmp = in.getDerValue(); // advance to the next element
        //CertificateValidity interval = new CertificateValidity(in);


        // subject name
        checkForUTF8String(in.toByteArray());
        //CertificateSubjectName subject = new CertificateSubjectName(in);
        //X500Name subjectDN = (X500Name)subject.get(CertificateSubjectName.DN_NAME);
 
    }
    
    private void checkForGeneralizedTime(byte[] validity) throws IOException {

        DerInputStream stream = new DerInputStream(validity);
        DerValue[] values = stream.getSequence(2);
        
        for (int i=0; i < 2; i++) {
            if (values[i].tag != 0x18) {
                errorString+= "\nCheckFormat, validity not in GeneralizedTime format, tag = " + Integer.toHexString(values[i].tag);
            }
        }
    }
    
    private void checkForUTF8String(byte[] names) throws IOException {
        DerInputStream stream = new DerInputStream(names);
        DerValue[] values = stream.getSequence(5);  // SETS of Sequence
        
        for (int i = 0; i < values.length; i++) {
            stream = values[i].getData();
            DerValue[] sets = stream.getSequence(2); // Sequence of identifier & String 
            if (sets[1].tag != 0x0c) {
                errorString += "\nCheckUTF8String, name includes non-UTF8String encoding";
            }
        }
    }
    
    private void checkCert(X509Certificate cert, String type) 
       throws CertificateParsingException {
       
        boolean isRootCert = type.equalsIgnoreCase("root");
        boolean isBindingCert = type.equalsIgnoreCase("binding");
        
        // 1, check algorithm
        String certAlg = cert.getSigAlgName();
        if (!certAlg.equalsIgnoreCase("SHA1withRSA")) {
            errorString += "\nCheckCertAlgorithm, not SHA1withRSA";
        }
         
        // 2. check extensions
        Set criticalExtensions = cert.getCriticalExtensionOIDs();
        if (criticalExtensions == null) {
            errorString+= "\nCheckExtensions, no critical extensions found";
            return;
        }
           
        if (!criticalExtensions.contains("2.5.29.15")) {
            errorString+= "\nCheckExtensions, KeyUsage extension not marked critical";
            return;
        } 
        
        boolean[] keyUsage = cert.getKeyUsage();
        boolean[] expected;
        
        if (isRootCert) {
            // the 1st bit, digitalSignature, the 6th bit, keyCertSign should be set        
            expected = new boolean[]{true, false, false, false, false, true, false, false, false};
        } else {
            // the 1st bit, digitalSignature, should be set  
            expected = new boolean[]{true, false, false, false, false, false, false, false, false };            
        }
        
        for (int i =0; i < expected.length; i++) {
            if (keyUsage[i] != expected[i]) {
                errorString += "\nCheckKeyUsageExtension, wrong usage set " + i;
            }
        }
       
        Collection issuerAltName = cert.getIssuerAlternativeNames();
        if (issuerAltName == null || issuerAltName.isEmpty()) {
            errorString += "\nCheckExtensions, no IssuerAlternativeName set";
        }
        
        Collection subjectAltName = cert.getSubjectAlternativeNames();
        if (subjectAltName == null || subjectAltName.isEmpty()) {
            errorString += "\nCheckExtensions, no SubjectAlternativeName set";
        }

        if (isRootCert || isBindingCert) {
            int basicConstraints = cert.getBasicConstraints();
            if (basicConstraints == 1) {
                errorString += "\nCheckExtensions, root certificate missing or having non-critical BasicConstraints";
            }
        }
    }
    
    public static void printUsage() {
        System.out.println("Certificate Verifier <root|app|binding> <certificate>");
    }
}
