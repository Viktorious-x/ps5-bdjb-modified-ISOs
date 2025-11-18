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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import java.math.BigInteger;

import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CertPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import javax.security.auth.x500.X500Principal;

import javax.xml.parsers.DocumentBuilderFactory;

import net.java.bd.tools.security.CredentialUtil.Files;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Base64;
import sun.security.util.HexDumpEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import static net.java.bd.tools.security.CredentialUtil.*;

/**
 * This class contails static methods to verify the credentials; it's useful for
 * catching obvious errors in the encoding of the credentials.
 * The verify() method takes: the path to the signed jarfile, the location
 * of the permission request file within the jar file, and the path to the
 * grantee root certificate. 
 * 
 * @author Jaya Hangal  
 */
class CredentialVerifier {

    public static void verify(String jarfile,
            String permReqFileName,
            String rootCert,
            boolean isBudaCredential)
            throws Exception {
        JarFile jf = new JarFile(jarfile);
        ZipEntry je = jf.getEntry(permReqFileName);
        if (je == null) {
            verifyError("Jar Entry:" + permReqFileName + " not found.");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(jf.getInputStream(je));
        Element e = doc.getDocumentElement();
        Node credNode;
        if (isBudaCredential) {
            credNode = getNodeWithTag(e, BUDA_CRED_TAG);
        } else {
            credNode = getNodeWithTag(e, FILE_CRED_TAG);
        }
        Node grantorNode = getNodeWithTag(credNode, GRANTOR_ID_TAG);
        String grantorOrg = ((Element) grantorNode).getAttribute("id");
        System.out.println("*************** Verifying Credentials ***********");
        System.out.println("Grantor's organization Id:" + grantorOrg);

        // remove 0x suffix from the orgId field of the permission request file
        grantorOrg = grantorOrg.substring(2);
        long grantorId = Long.parseLong(grantorOrg, 16);
        List<X509Certificate> grantorCerts = verifyCertChainFileId(credNode,
                grantorId, jarfile);

        // Lets reorder the certificates to form the trusted path to the root
        grantorCerts = getCertPath(grantorCerts);
        System.out.println("Found the grantor chain length:" + grantorCerts.size());
        printCerts(grantorCerts);
        if (grantorCerts.size() < 1) {
            verifyError("Unable to find grantor certificates");
        }
        System.out.println("####### <certchainfileid> Verification PASSED #######");
        verifySignature(e, grantorId, jarfile, rootCert, grantorCerts, isBudaCredential);
        System.out.println("*************** Verification Done ***********");
    }

    static private void verifyError(String errMsg) {
        System.out.println("===========================");
        System.out.println("VERFICATION FAILED:" + errMsg);
        System.out.println("===========================");
        System.exit(1);
    }

    // Assertion: Build a certpath to ensure that the certificate chain
    // forms a trusted certificate chain to the root.
    static List<X509Certificate> getCertPath(List<X509Certificate> certs)
            throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath cp = cf.generateCertPath(certs);
        return (List<X509Certificate>) cp.getCertificates();
    }

    static void printCerts(Collection certs) {
        Iterator i = certs.iterator();
        int count = 1;
        while (i.hasNext()) {
            X509Certificate cert = (X509Certificate) i.next();
            System.out.println(count + ". " + cert.getSubjectX500Principal());
            count++;
        }
    }

    static private List<X509Certificate> verifyCertChainFileId(Node credNode,
            long grantorId, String jarFileName) throws Exception {
        Node fileIdNode = getNodeWithTag(credNode, FILE_ID_TAG);
        if (fileIdNode == null) {
            verifyError("No elements in the permission request file with tag: " +
                    FILE_ID_TAG);
        }
        String base64Data = fileIdNode.getTextContent();
        byte[] derData = Base64.getDecoder().decode(base64Data);

        // issuerAndSerialNumber
        DerInputStream derin = new DerInputStream(derData);
        DerValue[] issuerAndSerialNumber = derin.getSequence(2);
        byte[] issuerBytes = issuerAndSerialNumber[0].toByteArray();

        X500Principal issuerName = new X500Principal(issuerBytes);
        // 3-2 s. 12.1.10:  "Issuer that matches the issuer of the
        //                   leaf certificate used for authentication"
        BigInteger certificateSerialNumber = issuerAndSerialNumber[1].getBigInteger();

        System.out.println("Looking for the certificate with issuerName:" +
                issuerName + " and cert serial no:" + Integer.toHexString(
                certificateSerialNumber.intValue()));

        // The return array below is for grantor's root cert and the
        // grantor cert.
        ArrayList<X509Certificate> returnCerts = new ArrayList<X509Certificate>();

        // retrieve the cert from the jarfile
        Collection certs = retrieveCerts(jarFileName);
        Iterator i = certs.iterator();
        X500Principal certName = null;

        // Find the grantor certificate chain
        // step 1. Locate the grantor certificate
        // step 2. Use the certificate found in step 1 to get to the root certificate
        boolean foundGrantorCert = false;
        while ((i.hasNext()) && !foundGrantorCert) {
            X509Certificate cert = (X509Certificate) i.next();
            if (issuerName.equals(cert.getIssuerX500Principal())) {
                String orgValue = getOrgValue(issuerName.toString());
                int indexOfOrgId = orgValue.lastIndexOf(".");
                String orgId = null;
                if (indexOfOrgId != -1) {
                    orgId = orgValue.substring(indexOfOrgId + 1);
                } else {
                    System.out.println("Could not retrieve the orgId from the" +
                            " grantor certificate");
                    continue;
                }
                long certOrgId = Long.parseLong(orgId, 16);
                if (grantorId != certOrgId) {
                    System.out.println(
                            "grantor org Id:" + grantorId +
                            " and the one in the certificate:" + certOrgId +
                            " did not match");
                    continue;
                }
                if (certificateSerialNumber.equals(cert.getSerialNumber())) {
                    System.out.println("Found the grantor's certificate:" +
                            cert.getSubjectX500Principal());
                    certName = issuerName;
                    returnCerts.add(cert);
                    foundGrantorCert = true;
                }
            }
        }

        // Grantor is found; step 2. Now look for grantor's root
        boolean rootCertFound = false;
        while (!rootCertFound) {
            X509Certificate matchedCert = getCert(certs, certName);
            if (matchedCert != null) {
                returnCerts.add(matchedCert);
                if (certName.equals(matchedCert.getIssuerX500Principal())) {
                    // Self signed certificate must be the root.
                    rootCertFound = true;
                } else {
                    certName = matchedCert.getIssuerX500Principal();
                }
            } else {
                verifyError("Could not find root certificate for the grantor chain");
            }
        }
        return returnCerts;
    }

    static X509Certificate getCert(Collection certs, X500Principal certName) {
        Iterator i = certs.iterator();
        while (i.hasNext()) {
            X509Certificate cert = (X509Certificate) i.next();
            if (certName.equals(cert.getSubjectX500Principal())) {
                return cert;
            }
        }
        return null;
    }

    private static void verifySignature(Element e, long grantorId, String jarfile,
            String granteeRootCertName,
            List<X509Certificate> grantorCerts,
            boolean isBudaCredential) throws Exception {
        byte credentialUsage = 0x00;
        if (isBudaCredential) {
            credentialUsage = 0x01;     // cf. 3-2 s. 12.1.10 table 12-4
        }
        String geOrgId = e.getAttribute("orgid");
        geOrgId = geOrgId.substring(2);
        long granteeOrgId = Long.parseLong(geOrgId, 16);
        String geAppId = e.getAttribute("appid");
        geAppId = geAppId.substring(2);
        int granteeAppId = Integer.parseInt(geAppId, 16);

        // compute grantee root cert digest
        FileInputStream fis = new FileInputStream(granteeRootCertName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate granteeRootCert = (X509Certificate) cf.generateCertificate(bis);
        byte[] granteeRootCertDigest = getCertDigest(granteeRootCert);
        byte[] grantorRootCertDigest = getCertDigest(grantorCerts.get(grantorCerts.size() - 1));

        Node credNode;
        if (isBudaCredential) {
            credNode = getNodeWithTag(e, BUDA_CRED_TAG);
        } else {
            credNode = getNodeWithTag(e, FILE_CRED_TAG);
        }
        NodeList credAttrs = credNode.getChildNodes();
        String expDate = null;
        ArrayList<Files> fileList = new ArrayList<Files>();
        for (int i = 0; i < credAttrs.getLength(); i++) {
            Node cNode = credAttrs.item(i);
            if (cNode.getNodeName().equals(EXP_DATE_TAG)) {
                expDate = ((Element) cNode).getAttribute("date");
            } else if (cNode.getNodeName().equals(FILE_NAME_TAG)) {
                String filePath = cNode.getTextContent();
                NamedNodeMap fileAttrs = cNode.getAttributes();
                String read = fileAttrs.getNamedItem("read").getNodeValue();
                String write = fileAttrs.getNamedItem("write").getNodeValue();
                Files f = new Files(read, write, filePath);
                fileList.add(f);
            }
        }
        byte[] expiryDate = getAscii(expDate);

        // binary concatenation of the fields to be signed
        ByteArrayOutputStream baos = new ByteArrayOutputStream(450);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(credentialUsage);
        dos.writeInt((int) granteeOrgId);
        dos.writeShort(granteeAppId);
        dos.write(granteeRootCertDigest, 0, granteeRootCertDigest.length);
        dos.writeInt((int) grantorId);
        dos.write(grantorRootCertDigest, 0, grantorRootCertDigest.length);
        dos.write(expiryDate, 0, expiryDate.length);

        // file related attributes
        for (Files f : fileList) {
            byte[] readPerm = getAscii(f.read);
            byte[] writePerm = getAscii(f.write);
            byte[] filepath = getAscii(f.filepath);
            short fileLength = (short) filepath.length;
            dos.write(readPerm, 0, readPerm.length);
            dos.write(writePerm, 0, writePerm.length);
            dos.writeShort(fileLength);
            dos.write(filepath, 0, filepath.length);
        }
        dos.close();
        byte[] data = baos.toByteArray();
        System.out.println("Data gathered for signature verification:");
        HexDumpEncoder hexDump = new HexDumpEncoder();
        System.out.println(hexDump.encodeBuffer(data));

        Node signNode = getNodeWithTag(credNode, SIGNATURE_TAG);
        if (signNode == null) {
            verifyError("No elements in the permission request file with tag: " +
                    SIGNATURE_TAG);
        }
        String base64Data = signNode.getTextContent();
        byte[] signature = Base64.getDecoder().decode(base64Data);
        Signature verifier = Signature.getInstance(SIG_ALGO);
        verifier.initVerify(grantorCerts.get(0));
        verifier.update(data);
        boolean verified = verifier.verify(signature);
        if (verified) {
            System.out.println("####### Credentials signature verification PASSED ######");
        } else {
            verifyError("Credentials signature verification FAILED");
        }
    }

    private static Collection retrieveCerts(String jarFileName) throws Exception {
        JarFile jf = new JarFile(jarFileName);
        JarEntry jarEntry = (JarEntry) jf.getEntry("META-INF/SIG-BD00.RSA");
        InputStream in = jf.getInputStream(jarEntry);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        Collection certs = cf.generateCertificates(in);
        System.out.println("# of certs in the signed Jar File:" + certs.size());
        jf.close();
        return certs;
    }

    private static String getOrgValue(String name) throws Exception {
        LdapName dn = new LdapName(name);
        List<Rdn> rdns = dn.getRdns();
        for (Rdn rdn : rdns) {
            String type = rdn.getType();
            if (type.equalsIgnoreCase("O")) {
                return (String) rdn.getValue();
            }
        }
        return null;
    }
}
