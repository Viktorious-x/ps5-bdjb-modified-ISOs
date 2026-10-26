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
/**
 * This tool is a credential generator tool.
 * Step 1)
 * Input: Permission Request File, Output: Permission Request File with credentials
 * This tool takes in a premission request file that has all other fields of persistent
 * credentials but the <signature> and the <certchainfileid> fields/elements of the XML.
 * This tool generates the credentials using the keystores of the grantor and 
 * the grantee. And adds these fields to the permission request file.
 * For example consider the following input permission request file:
 * <?xml version="1.0" encoding="UTF-8" standalone="no"?>
 * <n:permissionrequestfile xmlns:n="urn:BDA:bdmv;PRF" appid="0x4001" orgid="0x02">
 *   <file value="true"/>
 *   <applifecyclecontrol value="true"/>
 *   <servicesel value="true"/>
 *   <userpreferences read="true" write="false"/>
 *  <persistentfilecredential>
 *      <grantoridentifier id="0x01"/>
 *      <expirationdate date="10/12/2010"/>
 *      <filename read="true" write="true">01/4000/tmp.txt</filename>
 *  </persistentfilecredential>
 * </n:permissionrequestfile>
 * 
 * The output permission request file looks like below:
 *      ......
 *     <persistentfilecredential>
 *       <grantoridentifier id="0x01"/>
 *       <expirationdate date="10/12/2010"/>
 *       <filename read="true" write="true">01/4000/tmp.txt</filename>
 *       <signature>KSrmmBCGY9RkOCug6HRWjBLC29VkCOKBoPAVbbxv+q7Ed4iVv6tzerrkXudjs1rez
 * CYtrGysX0VK&#13;
 * qKE/GlqQy2ICTWl8RVdWHFR/1KobWcsghIqtXeyR89pKrUWw8Z52o00pQsV351MrYAb7wZUzRozO&#13
 * ;
 * 1VWAViCRoKkjHbxw/pI=</signature><certchainfileid>MGIwXTEPMA0GA1UEAwwGU3R1ZGlvMR8
 * wHQYDVQQLDBZDb2Rlc2lnbmluZyBEZXBhcnRtZW50MRww&#13;
 * GgYDVQQKDBNCREpDb21wYW55LjAwMDAwMDAxMQswCQYDVQQGDAJVUwIBAQ==</certchainfileid>
 * </persistentfilecredential>
 * </n:permissionrequestfile>
 * ...
 * 
 * Step 2):
 * Input: signed jarfile, Output: signed jar file with updated certificates that
 * includes the certchain to establish the trust cert chain of the grantor.
 */
package net.java.bd.tools.security;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.math.BigInteger;

import java.security.MessageDigest;
import java.security.Signature;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import sun.security.pkcs.PKCS7;
import java.util.Base64;
import sun.tools.jar.Main;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sun.security.util.HexDumpEncoder;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

class CredentialUtil {

    // Default values used.
    static final String DEFAULT_GRANTOR_STORE = "grantor.store";
    static final String DEFAULT_GRANTOR_STOREPASS = "keystorepassword";
    static final String DEFAULT_GRANTOR_ALIAS = "appcert";
    static final String DEFAULT_GRANTOR_KEYPASS = "appcertpassword";
    static final String DEFAULT_GRANTEE_STORE = "keystore.store";
    static final String DEFAULT_GRANTEE_STOREPASS = "keystorepassword";
    static final String DEFAULT_GRANTEE_ROOT_ALIAS = "rootcert";
    // Options initialized by the Builder
    String grantorKeyStore;
    String grantorStorePass;
    String grantorAlias;
    String grantorPassword;
    String granteeKeyStore;
    String granteeStorePass;
    String granteeRootAlias;
    String grantorCertFile;
    String jarFileName;
    boolean debug;
    String permReqFile;
    String discRootFile;
    boolean isBudaCredential;
    List<? extends Certificate> grantorCerts;
    // Constants used by this class
    static final String SIG_BLOCK_FILE = "META-INF/SIG-BD00.RSA";
    static ObjectIdentifier digestAlgorithmID;
    static final String SHA1OID = "1.3.14.3.2.26";
    static final String SIG_ALGO = "SHA1withRSA";
    static final String ENCR_ALGO = "RSA";
    static final String PERM_REQ_FILE_TAG = "permissionrequestfile";
    static final String FILE_CRED_TAG = "persistentfilecredential";
    static final String BUDA_CRED_TAG = "bd-bindingunitareacredential";
    static final String GRANTOR_ID_TAG = "grantoridentifier";
    static final String EXP_DATE_TAG = "expirationdate";
    static final String FILE_NAME_TAG = "filename";
    static final String SIGNATURE_TAG = "signature";
    static final String FILE_ID_TAG = "certchainfileid";
    static final String GRANTOR_CERT_FILE = "grantorchain.crt";

    private CredentialUtil(Builder b) {
        this.grantorKeyStore = b.grantorKeyStore;
        this.grantorStorePass = b.grantorStorePass;
        this.grantorAlias = b.grantorAlias;
        this.grantorPassword = b.grantorPassword;
        this.granteeKeyStore = b.granteeKeyStore;
        this.granteeStorePass = b.granteeStorePass;
        this.granteeRootAlias = b.granteeRootAlias;
        this.grantorCertFile = b.grantorCertFile;
        this.jarFileName = b.jarFileName;
        this.debug = b.debug;
        this.isBudaCredential = b.isBudaCredential;
        this.permReqFile = b.permReqFile;
        this.discRootFile = b.discRootFile;
        if (debug) {
            printDebugMsg("grantor keystore:" + grantorKeyStore +
                    ", grantor alias: " + grantorAlias +
                    ", grantee keystore:" + granteeKeyStore +
                    ", grantee root alias:" + granteeRootAlias +
                    ", permReqFile:" + permReqFile +
                    ", jarFile    :" + jarFileName);
        }
    }

    public static class Builder {
        // Initialize with default values

        String grantorKeyStore = DEFAULT_GRANTOR_STORE;
        String grantorStorePass = DEFAULT_GRANTOR_STOREPASS;
        String grantorAlias = DEFAULT_GRANTOR_ALIAS;
        String grantorPassword = DEFAULT_GRANTOR_KEYPASS;
        String granteeKeyStore = DEFAULT_GRANTEE_STORE;
        String granteeStorePass = DEFAULT_GRANTEE_STOREPASS;
        String granteeRootAlias = DEFAULT_GRANTEE_ROOT_ALIAS;
        String grantorCertFile;
        String jarFileName;
        String permReqFile;
        String discRootFile;
        boolean debug = false;
        boolean isBudaCredential = false;

        public Builder() {
        }

        public Builder grantorKeyStore(String storefile) {
            this.grantorKeyStore = storefile;
            return this;
        }

        public Builder grantorStorePass(String storepass) {
            this.grantorStorePass = storepass;
            return this;
        }

        public Builder grantorAlias(String alias) {
            this.grantorAlias = alias;
            return this;
        }

        public Builder grantorPassword(String password) {
            this.grantorPassword = password;
            return this;
        }

        public Builder granteeRootCert(String filename) {
            this.discRootFile = filename;
            return this;
        }

        public Builder granteeKeyStore(String storefile) {
            this.granteeKeyStore = storefile;
            return this;
        }

        public Builder granteeStorePass(String storepass) {
            this.granteeStorePass = storepass;
            return this;
        }

        public Builder granteeRootAlias(String alias) {
            this.granteeRootAlias = alias;
            return this;
        }

        public Builder grantorCertFile(String certFile) {
            this.grantorCertFile = certFile;
            return this;
        }

        public Builder permReqFile(String file) {
            this.permReqFile = file;
            return this;
        }

        public Builder jarFile(String file) {
            this.jarFileName = file;
            return this;
        }

        public Builder debug() {
            this.debug = true;
            return this;
        }

        public Builder budaCredential() {
            this.isBudaCredential = true;
            return this;
        }

        public CredentialUtil build() {
            return new CredentialUtil(this);
        }
    }

    static void printDebugMsg(String msg) {
        System.out.println("[debug]:" + msg);
    }

    public void genCredentials() throws Exception {

        // Read the permission request file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new File(permReqFile));
        Element e = doc.getDocumentElement();
        String orgId = e.getAttribute("orgid");
        String appId = e.getAttribute("appid");
        Node credNode;
        if (isBudaCredential) {
            credNode = getNodeWithTag(e, BUDA_CRED_TAG);
        } else {
            credNode = getNodeWithTag(e, FILE_CRED_TAG);
        }
        ArrayList<Files> fileList = new ArrayList<Files>();
        NodeList cns = credNode.getChildNodes();
        String gaOrgId = null;
        String expDate = null;
        for (int i = 0; i < cns.getLength(); i++) {
            Node cNode = cns.item(i);
            if (cNode.getNodeName().equals(GRANTOR_ID_TAG)) {
                gaOrgId = ((Element) cNode).getAttribute("id");
            } else if (cNode.getNodeName().equals(EXP_DATE_TAG)) {
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
        String fileId = genCertChainFileId();
        byte credentialUsage = 0;
        if (isBudaCredential) {
            credentialUsage = 0x01;     // 3-2 s. 12.1.10 table 12-4
        }
        String signature = genCredSignature(credentialUsage, orgId, appId,
                gaOrgId, expDate, fileList);
        Element se;
        if ((se = (Element) getNodeWithTag(credNode, SIGNATURE_TAG)) == null) {
            se = doc.createElement(SIGNATURE_TAG);
        }
        se.setTextContent(signature);
        Element fe;
        if ((fe = (Element) getNodeWithTag(credNode, FILE_ID_TAG)) == null) {
            fe = doc.createElement(FILE_ID_TAG);
        }
        fe.setTextContent(fileId);
        credNode.appendChild(se);
        credNode.appendChild(doc.createTextNode("\n        "));
        credNode.appendChild(fe);
        credNode.appendChild(doc.createTextNode("\n        "));
        Source domSource = new DOMSource(doc);
        Result fileResult = new StreamResult(new File(permReqFile));
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();
        transformer.transform(domSource, fileResult);
        removeEntityReference(permReqFile);

        // export the grantor's certificate chain
        exportGrantorCert();
    }

    static Node getNodeWithTag(Node node, String tag) throws Exception {
        NodeList nl = node.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals(tag)) {
                    return n;
                }
            }
        }
        printDebugMsg("No elements with tag:" + tag);
        return null;
    }

    private String genCertChainFileId() throws Exception {
        Certificate[] certs =
                getCerts(grantorKeyStore, grantorStorePass, grantorAlias);
        grantorCerts = getCertPath(certs);
        X509Certificate leafCert = (X509Certificate) grantorCerts.get(0);
        if (debug) {
            printDebugMsg("Using the grantor cert for generating <certchainfileid> :" + leafCert);
        }

        // read the issuer name
        byte[] issuerName = leafCert.getIssuerX500Principal().getEncoded();

        // read the serial number
        BigInteger certificateSerialNumber = leafCert.getSerialNumber();

        // lets do the DER encoding of above fields
        DerOutputStream seq = new DerOutputStream();
        DerOutputStream issuerAndSerialNumber = new DerOutputStream();
        issuerAndSerialNumber.write(issuerName, 0, issuerName.length);
        issuerAndSerialNumber.putInteger(certificateSerialNumber);
        seq.write(DerValue.tag_Sequence, issuerAndSerialNumber);

        return base64Encode(seq.toByteArray());
    }

    void exportGrantorCert() throws Exception {
        Certificate[] grantorCerts = getCerts(grantorKeyStore, grantorStorePass, grantorAlias);
        BufferedOutputStream bos = new BufferedOutputStream(
                                   new FileOutputStream(GRANTOR_CERT_FILE));
        for (int i = 0; i < grantorCerts.length; i++) {
           byte[] certBytes = grantorCerts[i].getEncoded();
           bos.write(certBytes);
        }
        bos.flush();
        bos.close();
    }

    // Lets build a certpath to ensure that the certificate chain 
    // forms a trusted certificate chain to the root.
    private List<? extends Certificate> getCertPath(Certificate[] certs)
            throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        List certList = Arrays.asList(certs);
        CertPath cp = cf.generateCertPath(certList);
        return cp.getCertificates();
    }

    private Certificate[] getCerts(String keystore, String storepass, String alias)
            throws Exception {
        File f = new File(keystore);
        if (!f.exists()) {
            exitWithErrorMessage("The keystore file:\"" + keystore +
                    "\" does not exists, please provide a keystore");
        }
        KeyStore ks = KeyStore.getInstance("JKS");

        // load the contents of the KeyStore
        ks.load(new FileInputStream(f), storepass.toCharArray());

        // fetch certificate chain stored with the given alias
        return ks.getCertificateChain(alias);
    }

    private void exitWithErrorMessage(String message) {
        System.err.println("Error: " + message);
        System.exit(1);
    }

    private PrivateKey getGrantorKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(grantorKeyStore), grantorStorePass.toCharArray());
        PrivateKey grantorKey = (PrivateKey) ks.getKey(grantorAlias,
                grantorPassword.toCharArray());
        return grantorKey;
    }

    private Certificate[] readCertsFromFile(String fileName)
            throws Exception {
        FileInputStream fis = new FileInputStream(fileName);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> c = cf.generateCertificates(fis);
        return c.toArray(new Certificate[0]);
    }

    private String genCredSignature(byte credentialUsage,
            String granteeOrgIdStr,
            String granteeAppIdStr,
            String grantorOrgIdStr,
            String expDateStr,
            List<Files> fileList)
            throws Exception {
        if (debug) {
            printDebugMsg("Generating the signature using- granteeId:" + granteeOrgIdStr +
                    ", granteeAppId:" + granteeAppIdStr +
                    ", grantorOrgId:" + grantorOrgIdStr +
                    ", expDate:" + expDateStr +
                    ", file permissions:" + fileList);
        }
        // get the grantee org_id, 32 bits, assuming the grantonIdStr begins with "0x"
        long granteeOrgId = Long.parseLong(granteeOrgIdStr.substring(2), 16);

        // get the grantee app_id 16 bits
        short granteeAppId = (short) Integer.parseInt(
                granteeAppIdStr.substring(2), 16);

        // get grantee certificate digest.
        Certificate[] granteeCerts;
        if (discRootFile != null) {
            granteeCerts = readCertsFromFile(discRootFile);
        } else {
            granteeCerts = getCerts(granteeKeyStore,
                    granteeStorePass, granteeRootAlias);
        }
        byte[] granteeCertDigest = getCertDigest(granteeCerts[0]);
        HexDumpEncoder hexDump = null;
        if (debug) {
            printDebugMsg("GranteeCertDigest:");
            hexDump = new HexDumpEncoder();
            System.out.println(hexDump.encodeBuffer(granteeCertDigest));
        }
        long grantorOrgId = Long.parseLong(grantorOrgIdStr.substring(2), 16);
        byte[] grantorRootCertDigest = getCertDigest(grantorCerts.get(grantorCerts.size() - 1));
        if (debug) {
            printDebugMsg("GrantorRootCertDigest:");
            System.out.println(hexDump.encodeBuffer(grantorRootCertDigest));
        }
        byte[] expiryDate = getAscii(expDateStr);

        // binary concatenation of the fields to be signed
        ByteArrayOutputStream baos = new ByteArrayOutputStream(450);
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(credentialUsage);
        dos.writeInt((int) granteeOrgId);
        dos.writeShort(granteeAppId);
        dos.write(granteeCertDigest, 0, granteeCertDigest.length);
        dos.writeInt((int) grantorOrgId);
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

        if (debug) {
            printDebugMsg("Concatenated binary data to be signed (length: " +
                    data.length + " bytes) is:");
            System.out.println(hexDump.encodeBuffer(data));
        }
        PrivateKey grantorKey = getGrantorKey();
        Signature sig = Signature.getInstance(SIG_ALGO);
        sig.initSign(grantorKey);
        sig.update(data);
        byte[] signature = sig.sign();

        return base64Encode(signature);
    }

    private static String base64Encode(byte[] src) throws Exception {
        // encode with base64
        return Base64.getEncoder().encodeToString(src).replace("\r", "").replace("\n", "");
    // Having whitespace in the base64 encoding makes the
    // credentials fail, at least on some players.  MHP references
    // IETF RFC 2045 as the specification for Base64.  Section 6.8
    // says that there are supposed to be newlines at least every
    // 76 characters in base-64 encoding, so stripping out the
    // newlines is actually non-compliant to the spec.  However,
    // encoders are required to ignore newlines, so this non-compliance
    // won't cause problems on correct Base64 decoder implementations.
    // See http://tools.ietf.org/html/rfc2045#section-6.8 .
    //
    // After diligent searching in the BD spec, I (Bill) was unable
    // to find anything that removes the requirement to put newlines
    // in.  However, in testing performed in October 2009 on several
    // players, *no* players were identified that worked if the
    // base 64 encoding used in a BUDA credential had a newline.
    // Removing the newlines fixed it.
    //
    // BDA spec clarification on this matter will be sought, but for
    // credential generation, the safe course of action is to never
    // put a newline in the base64 encoding of these values.
    }

    static void printHex(byte[] value) {
        int count = 0;
        for (int i = 0; i < value.length; i++, count++) {
            System.out.format("%2x ", value[i]);
            if (count == 20) {
                count = 0;
                System.out.println();
            }
        }
        System.out.println();
    }

    static class Files {

        String read;
        String write;
        String filepath;

        Files(String read, String write, String filepath) {
            this.read = read;
            this.write = write;
            this.filepath = filepath;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("read:" + read);
            sb.append(", write:" + write);
            sb.append(", filepath:" + filepath);
            return sb.toString();
        }
    };

    public void updateCerts() throws Exception {
        JarFile jf = new JarFile(jarFileName);
        JarEntry je = (JarEntry) jf.getJarEntry(SIG_BLOCK_FILE);
        if (je == null) {
            System.out.println("No entry found:" + SIG_BLOCK_FILE);
        }
        InputStream pkcs7Is = jf.getInputStream(je);
        PKCS7 signBlockFile = new PKCS7(pkcs7Is);
        pkcs7Is.close();
        jf.close();

        X509Certificate[] certs = (X509Certificate[]) signBlockFile.getCertificates();
        Certificate[] grantorCerts;
        if (grantorCertFile != null) {
            grantorCerts = readCertsFromFile(grantorCertFile);
        } else {
            grantorCerts = getCerts(grantorKeyStore, grantorStorePass, grantorAlias);
        }
        X509Certificate[] addedCerts =
                new X509Certificate[certs.length + grantorCerts.length];
        int len = 0;
        for (; len < certs.length; len++) {
            addedCerts[len] = (X509Certificate) certs[len];
        }
        for (int i = 0; i < grantorCerts.length; len++, i++) {
            addedCerts[len] = (X509Certificate) grantorCerts[i];
        }
        if (debug) {
            printDebugMsg("Updated Certs:");
            for (Certificate cert : addedCerts) {
                System.out.println("CERT:" + ((X509Certificate) cert).getSubjectX500Principal());
            }
        }

        // generate the updated PKCS7 Block including grantor certs
        PKCS7 newSignBlockFile = new PKCS7(
                signBlockFile.getDigestAlgorithmIds(),
                signBlockFile.getContentInfo(),
                addedCerts,
                signBlockFile.getSignerInfos());
        File mif = new File("META-INF");
        File sbf;
        if (!mif.isDirectory()) {
            if (!mif.mkdir()) {
                System.err.println("Could not create a META-INF directory");
            }
            return;
        }
        sbf = new File(mif, "SIG-BD00.RSA");
        FileOutputStream fos = new FileOutputStream(sbf);
        newSignBlockFile.encodeSignedData(fos);
        fos.close();
        String[] jarArgs = {"-uvf", jarFileName, SIG_BLOCK_FILE};
        Main jar = new Main(System.out, System.err, "jar");
        jar.run(jarArgs);
    }

    static byte[] getAscii(String str) throws Exception {
        return str.getBytes("US-ASCII");
    }

    static byte[] getCertDigest(Certificate cert) throws Exception {
        byte[] encCertInfo = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        return md.digest(encCertInfo);
    }

    /**
     * This method explicitly removes the character references from the PRF file
     * that were generated after updating the PRF file with credentials. The XML
     * APIs automatically generate character references when the XML document
     * is written to a file. According to MHP Specifcation section 14.3
     * character or entity references are not allowed in XML structure.
     * @param fileName
     * @throws java.lang.Exception
     */
    void removeEntityReference(String fileName) throws Exception {
        BufferedReader br = new BufferedReader(
                new FileReader(fileName));
        int ch;
        CharArrayWriter caw = new CharArrayWriter(2000);
        while ((ch = br.read()) != -1) {
            if (ch == '&') {
                if (br.skip(4) != 4) {
                    System.out.println("ERROR removing character references" +
                            " from PRF file; Could not skip 4 chars");
                    br.close();
                    System.exit(1);
                }
            } else {
                caw.write(ch);
            }
        }
        br.close();
        FileWriter fw = new FileWriter(fileName);
        fw.write(caw.toCharArray());
        fw.close();
    }
}
