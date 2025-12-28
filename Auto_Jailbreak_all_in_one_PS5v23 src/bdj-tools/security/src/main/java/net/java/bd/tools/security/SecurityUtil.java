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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.Signature;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.InvalidNameException;


import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import java.util.Base64;
import sun.tools.jar.Main;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.jce.PKCS10CertificationRequest;

/**
 * A generic security utility class that generates the certificates for the BD-J
 * applications and signs the jars or the BUMF.
 * This methods of this class wrap around jarsigner and keytool to perform bd-j
 * required signing.
 * 
 * Here are the 3 steps for signing jar{s}:
 * 1) a) Generate root certificate.
 *    b) Export the root certificate as "app.discroot.crt". 
 * 2) a) Generate app certificate.
 *    b) Generate a certificate signing request (CSR) from (a).
 *    c) Issue a certificate for the CSR from (a), using the alias for root certificate from (1a).
 *    d) Import back the certificate issued at (c) to the app certificate for (a).
 * 3) Sign the jar using the app certificate generated for (2).
 */
public class SecurityUtil {

    // default values
    static final String DEF_KEYSTORE_FILE = "keystore.store";
    static final String DEF_KEYSTORE_PASSWORD = "keystorepassword";
    static final String DEF_NEWKEY_PASSWORD = "appcertpassword";
    static final String DEF_ROOTKEY_PASSWORD = "rootcertpassword";
    static final String DEF_ONLINEKEY_PASSWORD = "onlinecertpassword";
    static final String DEF_APPCERT_ALIAS = "appcert";
    static final String DEF_ROOTCERT_ALIAS = "rootcert";
    static final String DEF_BUCERT_ALIAS = "bucert";
    static final String DEF_ONLINE_CERT_ALIAS = "onlinecert";
    static final String DEF_APP_ALT_NAME = "app@producer.com";
    static final String DEF_ROOT_ALT_NAME = "root@studio.com";
    static final String DEF_APP_CERT_DN = "CN=Producer, OU=Codesigning Department, O=BDJCompany, C=US";
    static final String DEF_ROOT_CERT_DN = "CN=Studio, OU=Codesigning Department, O=BDJCompany, C=US";
    static final String APP_DISC_ROOT_FILE = "app.discroot.crt";
    static final String BU_DISC_ROOT_FILE = "bu.discroot.crt";
    static final String ONLINE_SIG_FILE = "online.sig";
    static final String SIG_ALG = "SHA1WithRSA";
    static final String DIGEST_ALG = "SHA1";
    static final int KEY_LENGTH = 1024;

    // Intermediate files to create, will be deleted at the tool exit time;
    // XXX Make sure they are always deleted.
    static final String APPCSRFILE = "appcert.csr";
    static final String APPCERTFILE = "appcert.cer";

    // Optional fields initialized by the nested Builder class.
    String keystoreFile;
    String keystorePassword;
    String contentSignerAlias;  // Used by Signer
    String contentSignerPassword;
    String newCertAlias;        // Used by Cert Generator
    String newKeyPassword;
    String certSignerAlias;     // Used by Cert Generator
    String certSignerPassword;
    List<String> jarfiles;
    String orgId;
    String dn;
    String altName;
    String outputDiscrootFile;
    boolean writeDiscroot = true;
    String BUMFile;   // Binding Unit Manifest File
    String onlineDiscRootFile;
    boolean isRootCert = false;
    boolean isAppCert = false;
    boolean isBindingUnitCert = false;
    String onlinePvtKeyFile;
    String onlineCrtFile;
    boolean skipOnlineDiscroot = false;

    // This is a special flag indicating newly added
    // jar entries (not present in the signature file of an already
    // signed jar) should be excluded from signing. 
    boolean signOriginalOnly = false;
    boolean debug = false;

    // non-optional fields
    private KeyStore store;
    private BigInteger appCertSerNo;
    private boolean ksInitialized = false;

    /*
     * Using Builder pattern from Effective Java Reloaded. The arguments
     * for this constructor are way too many. The Builder Pattern makes
     * it easy to create an instance of this class.
     * reference:http://developers.sun.com/learning/javaoneonline/2007/pdf/TS-2689.pdf
     */
    private SecurityUtil(Builder b) throws Exception {
        // Optional parameters specified from the command line tools
        this.orgId = b.orgId;
        this.keystoreFile = b.keystoreFile;
        this.keystorePassword = b.keystorePassword;
        this.newCertAlias = b.newCertAlias;
        this.newKeyPassword = b.newKeyPassword;
        this.contentSignerPassword = b.contentSignerPassword;
        this.contentSignerAlias = b.contentSignerAlias;
        this.certSignerAlias = b.certSignerAlias;
        this.certSignerPassword = b.certSignerPassword;
        this.dn = b.dn;
        this.altName = b.altName;
        this.debug = b.debug;
        this.isAppCert = b.isAppCert;
        this.isRootCert = b.isRootCert;
        this.outputDiscrootFile = b.outputDiscrootFile;
        this.writeDiscroot = b.writeDiscroot;
        this.isBindingUnitCert = b.isBindingUnitCert;
        this.BUMFile = b.BUMFile;
        this.onlineDiscRootFile = b.onlineDiscRootFile;
        this.jarfiles = b.jarfiles;
        this.signOriginalOnly = b.signOriginalOnly;
        this.onlinePvtKeyFile = b.onlinePvtKeyFile;
        this.onlineCrtFile = b.onlineCrtFile;
        this.skipOnlineDiscroot = b.skipOnlineDiscroot;

        // Minor processing;append the orgid to the names
        dn = appendOrgId(dn);
    }

    public static class Builder {
        // Initialize with default values.

        String keystoreFile = DEF_KEYSTORE_FILE;
        String keystorePassword = DEF_KEYSTORE_PASSWORD;
        String newCertAlias;  // initialized based on root/app/binding cert
        String newKeyPassword;
        String contentSignerAlias; // initialized based on jar or bumf file
        String contentSignerPassword;
        String certSignerAlias;
        String certSignerPassword = DEF_ROOTKEY_PASSWORD;
        String outputDiscrootFile = APP_DISC_ROOT_FILE;
        boolean writeDiscroot = true;

        // Certificate data.
        String dn;
        String altName;
        List<String> jarfiles;
        String orgId;
        boolean debug = false;
        boolean isRootCert = false;
        boolean isAppCert = false;
        boolean isBindingUnitCert = false;
        boolean signOriginalOnly = false;
        String BUMFile;
        String onlineDiscRootFile;
        String onlinePvtKeyFile;
        String onlineCrtFile;
        boolean skipOnlineDiscroot;

        public Builder() {
        }

        public Builder orgId(String id) {
            this.orgId = id;
            return this;
        }

        public Builder keystoreFile(String file) {
            this.keystoreFile = file;
            return this;
        }

        public Builder storepass(String password) {
            this.keystorePassword = password;
            return this;
        }

        public Builder setRootCert() {
            this.isRootCert = true;
            setRootDefaults();
            return this;
        }

        private void setRootDefaults() {
            if (dn == null) {
                dn = DEF_ROOT_CERT_DN;
            }
            if (altName == null) {
                altName = DEF_ROOT_ALT_NAME;
            }
            if (newCertAlias == null) {
                if (isRootCert) {
                    newCertAlias = DEF_ROOTCERT_ALIAS;
                } else {
                    newCertAlias = DEF_BUCERT_ALIAS;
                }
            }
            if (newKeyPassword == null) {
                newKeyPassword = DEF_ROOTKEY_PASSWORD;
            }
            if (certSignerAlias == null) {
                if (isRootCert) {
                    certSignerAlias = DEF_ROOTCERT_ALIAS;
                } else {
                    certSignerAlias = DEF_BUCERT_ALIAS;
                }
            }
        }

        public Builder setAppCert() {
            this.isAppCert = true;
            setAppcertDefaults();
            return this;
        }

        void setAppcertDefaults() {
            if (dn == null) {
                dn = DEF_APP_CERT_DN;
            }
            if (altName == null) {
                altName = DEF_APP_ALT_NAME;
            }
            if (newCertAlias == null) {
                newCertAlias = DEF_APPCERT_ALIAS;
            }
            if (newKeyPassword == null) {
                newKeyPassword = DEF_NEWKEY_PASSWORD;
            }
            if (certSignerAlias == null) {
                certSignerAlias = DEF_ROOTCERT_ALIAS;
            }
        }

        public Builder setBindingUnitCert() {
            this.isBindingUnitCert = true;
            setRootDefaults();
            return this;
        }

        public Builder newCertAlias(String alias) {
            this.newCertAlias = alias;
            if (isRootCert || isBindingUnitCert) {
                certSignerAlias = newCertAlias; // self signed
            }
            return this;
        }

        public Builder certSignerAlias(String alias) {
            this.certSignerAlias = alias;
            return this;
        }

        public Builder contentSignerAlias(String alias) {
            this.contentSignerAlias = alias;
            return this;
        }

        public Builder newKeyPassword(String password) {
            this.newKeyPassword = password;
            if (isRootCert || isBindingUnitCert) {
                certSignerPassword = newKeyPassword; // self signed
            }
            return this;
        }

        public Builder certSignerPassword(String password) {
            this.certSignerPassword = password;
            return this;
        }

        public Builder contentSignerPassword(String password) {
            this.contentSignerPassword = password;
            return this;
        }

        public Builder dn(String name) {
            this.dn = name;
            return this;
        }

        public Builder altName(String name) {
            this.altName = name;
            return this;
        }

        public Builder outputDiscrootFile(String file) {
            this.outputDiscrootFile = file;
            return this;
        }

        public Builder dontWriteDiscroot() {
            this.writeDiscroot = false;
            return this;
        }

        public Builder originalOnly() {
            this.signOriginalOnly = true;
            return this;
        }

        public Builder debug() {
            this.debug = true;
            return this;
        }

        public Builder bumf(String file) {
            this.BUMFile = file;
            if (contentSignerAlias == null) {
                contentSignerAlias = DEF_BUCERT_ALIAS;
            }
            if (contentSignerPassword == null) {
                contentSignerPassword = DEF_ROOTKEY_PASSWORD;
            }
            return this;
        }

        public Builder onlineDiscRootFile(String file) {
            this.onlineDiscRootFile = file;
            initOnline();
            return this;
        }

        private void initOnline() {
            if (contentSignerAlias == null) {
                contentSignerAlias = DEF_ONLINE_CERT_ALIAS;
            }
            if (contentSignerPassword == null) {
                contentSignerPassword = DEF_ONLINEKEY_PASSWORD;
            }
        }

        public Builder onlinePvtKeyFile(String file) {
            this.onlinePvtKeyFile = file;
            return this;
        }

        public Builder onlineCrtFile(String file) {
            this.onlineCrtFile = file;
            return this;
        }

        public Builder skipOnlineDiscroot() {
            this.skipOnlineDiscroot = true;
            initOnline();
            return this;
        }

        public Builder jarfiles(List<String> files) {
            this.jarfiles = files;
            if (contentSignerAlias == null) {
                contentSignerAlias = DEF_APPCERT_ALIAS;
            }
            if (contentSignerPassword == null) {
                contentSignerPassword = DEF_NEWKEY_PASSWORD;
            }
            return this;
        }

        public SecurityUtil build() throws Exception {
            return new SecurityUtil(this);
        }
    }

    // append the orgId to the OrganizationName of the DN
    private String appendOrgId(String dn)
            throws InvalidNameException {
        if (dn == null) {
            return null;
        }
        if (orgId == null) {
            return dn;
        }
        LdapName name = new LdapName(dn);
        List<Rdn> rdns = name.getRdns();
        List<Rdn> newRdns = new ArrayList<Rdn>();
        for (Rdn rdn : rdns) {
            String type = rdn.getType();
            if (type.equalsIgnoreCase("O")) {
                String value = (String) rdn.getValue();
                String newValue = value + "." + orgId;
                newRdns.add(new Rdn(type, newValue));
            } else {
                newRdns.add(rdn);
            }
        }
        return (new LdapName(newRdns)).toString();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("orgID = ");
        sb.append(orgId);
        sb.append("\n");
        sb.append("keystoreFile = ");
        sb.append(keystoreFile);
        sb.append("\n");
        sb.append("keystorePassword = ");
        sb.append(keystorePassword);
        sb.append("\n");
        sb.append("newCertAlias = ");
        sb.append(newCertAlias);
        sb.append("\n");
        sb.append("newKeyPassword = ");
        sb.append(newKeyPassword);
        sb.append("\n");
        sb.append("contentSignerPassword = ");
        sb.append(contentSignerPassword);
        sb.append("\n");
        sb.append("contentSignerAlias = ");
        sb.append(contentSignerAlias);
        sb.append("\n");
        sb.append("certSignerAlias = ");
        sb.append(certSignerAlias);
        sb.append("\n");
        sb.append("certSignerPassword = ");
        sb.append(certSignerPassword);
        sb.append("\n");
        sb.append("dn = ");
        sb.append(dn);
        sb.append("\n");
        sb.append("altName = ");
        sb.append(altName);
        sb.append("\n");
        sb.append("debug = ");
        sb.append(debug);
        sb.append("\n");
        sb.append("isAppCert = ");
        sb.append(isAppCert);
        sb.append("\n");
        sb.append("isRootCert = ");
        sb.append(isRootCert);
        sb.append("\n");
        sb.append("isBindingUnitCert = ");
        sb.append(isBindingUnitCert);
        sb.append("\n");
        sb.append("BUMFile = ");
        sb.append(BUMFile);
        sb.append("\n");
        sb.append("onlineDiscRootFile =");
        sb.append(onlineDiscRootFile);
        sb.append("\n");
        sb.append("jarfiles = ");
        sb.append(jarfiles);
        sb.append("\n");
        sb.append("signOriginalOnly =");
        sb.append(signOriginalOnly);
        return sb.toString();
    }

    public void signJars() {
        try {
            initKeyStore();
            for (String jfile : jarfiles) {
                if (signOriginalOnly) {
                    signOriginalJarFile(jfile);
                } else {
                    signJarFile(jfile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // VM exit with an error code
        }
    }

    public void createCerts() throws Exception {
        if (isAppCert) {
            createAppCert();
        } else {
            createRootCert();
        }
    }

    public void createRootCert() throws Exception {
        boolean failed = false;
        cleanup();  // Get rid of any previous key aliases first.
        try {
            initKeyStore();
            generateSelfSignedCertificate(dn, newCertAlias,
                    newKeyPassword, true);
            exportRootCertificate(true);
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }
        if (failed) {
            System.exit(1); // VM exit with an error code+
        }
    }

    public void createAppCert() throws Exception {
        boolean failed = false;
        try {
            initKeyStore();
            generateSelfSignedCertificate(dn, newCertAlias,
                    newKeyPassword, false);
            generateCSR();
            generateCSRResponse();
            importCSRResponse();
            if (debug) {
                verifyCertificate("app", APPCERTFILE);
            }
            if (writeDiscroot) {
                exportRootCertificate(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }
        if (!debug) {
            new File(APPCSRFILE).delete();
            new File(APPCERTFILE).delete();
        }
        if (failed) {
            System.exit(1); // VM exit with an error code
        }
    }

    private void initKeyStore() throws Exception {
        if (ksInitialized) {
            return;
        }
        ksInitialized = true;
        Security.addProvider(new BouncyCastleProvider());
        char[] password = keystorePassword.toCharArray();
        store = KeyStore.getInstance(KeyStore.getDefaultType());
        File kfile = new File(keystoreFile);
        if (!kfile.exists()) {
            try (InputStream sigKs = getClass().getResourceAsStream("/sig.ks")) {
                if (DEF_KEYSTORE_FILE.equals(keystoreFile)) {
                    System.out.println("Using embedded keystore");
                    kfile = Files.createTempFile("keystore", ".store").toFile();
                    kfile.deleteOnExit();
                    keystoreFile = kfile.getAbsolutePath();
                    try (OutputStream sigKsOut = Files.newOutputStream(kfile.toPath())) {
                        sigKsOut.write(sigKs.readAllBytes());
                    }
                    System.out.println("Stored keystore at: " + keystoreFile);
                } else {
                    System.out.println("Using default keystore");
                    store.load(null, password);
                    try (OutputStream fout = Files.newOutputStream(kfile.toPath())) {
                        store.store(fout, password);
                    }
                }
            }
        }
        URL url = new URL("file:" + kfile.getCanonicalPath());
        try (InputStream is = url.openStream()) {
            store.load(is, password);
        }
    }

    private void generateCSR() throws Exception {
        String[] appCSRRequestArgs = {"-certreq", "-alias", newCertAlias,
            "-keypass", newKeyPassword, "-keystore", keystoreFile, "-storepass",
            keystorePassword, "-v", "-file", APPCSRFILE};

        sun.security.tools.keytool.Main.main(appCSRRequestArgs);
    }

    private void generateCSRResponse() throws Exception {
        issueCert(APPCSRFILE, APPCERTFILE, certSignerAlias, certSignerPassword);
    }

    private void importCSRResponse() throws Exception {
        String[] responseImportArgs = {"-import", "-v", "-alias", newCertAlias,
            "-keypass", newKeyPassword, "-keystore", keystoreFile,
            "-storepass", keystorePassword, "-v", "-file", APPCERTFILE};
        sun.security.tools.keytool.Main.main(responseImportArgs);
    }

    private void signJarFile(String jfile) throws Exception {
        String[] jarSigningArgs = {"-sigFile", "SIG-BD00",
            "-digestalg", DIGEST_ALG, "-sigalg", SIG_ALG,
            "-keypass", contentSignerPassword,
            "-keystore", keystoreFile,
            "-storepass", keystorePassword,
            "-verbose", jfile, contentSignerAlias};
        sun.security.tools.jarsigner.Main.main(jarSigningArgs);
        signWithBDJHeader(jfile);
    }

    /**
     * This method signs the Binding Unit Manifest File according to the
     * BD-J specification, Part: 3-2, section: 12.2.8.1 and 
     * 2.2.8.1.4 (verification).
     * @throws java.lang.Exception
     */
    public void signBUMF() throws Exception {
        try {
            initKeyStore();
            Signature signer = Signature.getInstance(SIG_ALG);
            if (debug) {
                System.out.println("Signer of " + BUMFile + " file is:" +
                        contentSignerAlias);
            }
            PrivateKey key = (PrivateKey) store.getKey(contentSignerAlias,
                    contentSignerPassword.toCharArray());
            signer.initSign(key);
            byte[] data = readIntoBuffer(BUMFile);
            signer.update(data);
            byte[] signedData = signer.sign();
            DerOutputStream dos = new DerOutputStream();
            dos.putBitString(signedData);
            int extIndex;
            String prefix = "tmp";
            if ((extIndex = BUMFile.lastIndexOf(".")) != -1) {
                prefix = BUMFile.substring(0, extIndex);
            }
            String sigFile = prefix + ".sf";
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(sigFile));
            dos.derEncode(bos);
            bos.close();
            dos.close();
            if (debug) {
                verifySignatureFile(sigFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // VM exit with an error code
        }
    }

    private void verifySignatureFile(String sigFile) throws Exception {
        Signature verifier = Signature.getInstance(SIG_ALG);
        if (debug) {
            System.out.println("Verifier of " + sigFile + " file is:" + contentSignerAlias);
        }
        Certificate cert = store.getCertificate(contentSignerAlias);
        verifier.initVerify(cert);

        byte[] derData = readIntoBuffer(sigFile);
        DerInputStream din = new DerInputStream(derData);
        byte[] signature = din.getBitString();

        byte[] data = readIntoBuffer(BUMFile);
        verifier.update(data);
        if (verifier.verify(signature)) {
            System.out.println("BUSF Verification PASSED..");
            System.out.println("The signed file is written into:" + sigFile);
        } else {
            System.out.println("BUSF Verification FAILED..");
        }
    }

    private byte[] readIntoBuffer(String filename) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        int INITIAL_BUF_SIZE = 3000; // some value for the buffer
        byte[] buf = new byte[INITIAL_BUF_SIZE];
        int read;
        int off = 0;
        int len = buf.length;
        int size = buf.length;
        while ((read = fis.read(buf, off, len)) != -1) {
            off = off + read;
            if (off >= size) {
                size = size * 2;
                buf = Arrays.copyOf(buf, size);
            }
            len = size - off;
        //System.out.println("off:" + off + ", len:" + len + ", size:" + size);
        }
        fis.close();
        return Arrays.copyOfRange(buf, 0, off);
    }
    //
    // See BDROM part 3-2 version 2.2, Annex DD.
    //
    private static final String ISO = "ISO646-US";

    public void generateOnlineSigFile() throws Exception {
        byte[] typeIndicator = "OSIG".getBytes(ISO);
        byte[] versionNo = "0200".getBytes(ISO);
        byte[] reservedBytes = new byte[32];
        byte[] rootCert;
        byte[] signature;

        initKeyStore();
        PrivateKey key;

        if (onlinePvtKeyFile != null) {
            // import the online keys into the keystore
            OnlineKeytool tool = new OnlineKeytool(onlinePvtKeyFile,
                    onlineCrtFile, debug);
            store = tool.importKeys(store, DEF_ONLINE_CERT_ALIAS,
                    DEF_ONLINEKEY_PASSWORD);
            
            // make the updated keystore persistent
            FileOutputStream fos = new FileOutputStream(keystoreFile);
            store.store(fos, keystorePassword.toCharArray());
            fos.close();
            key = (PrivateKey) store.getKey(DEF_ONLINE_CERT_ALIAS,
                    DEF_ONLINEKEY_PASSWORD.toCharArray());
            if (debug) {
                System.out.println("Imported online keys into keystore: " +
                        keystoreFile + " with an alias: \"" + DEF_ONLINE_CERT_ALIAS +
                        "\"");
            }
        } else if (store.containsAlias(contentSignerAlias)) {
            if (debug) {
                System.out.println("Using alias: \"" + contentSignerAlias +
                        "\" from the keystore to generate online.sig file");
            }
            key = (PrivateKey) store.getKey(contentSignerAlias,
                    contentSignerPassword.toCharArray());
        } else {
            System.err.println("Error: the alias:" + contentSignerAlias +
                    " does not exist in the keystore and no private key file found" +
                    " to generate online.sig file");
            System.err.println("Please provide private key sent by BDA" +
                    " for online authentication");
            return;
        }
        if (skipOnlineDiscroot) {
            rootCert = new byte[1];
        } else {
            rootCert = readIntoBuffer(onlineDiscRootFile);
        }
        Signature signer = Signature.getInstance(SIG_ALG);
        signer.initSign(key);
        signer.update(rootCert);
        signer.update(typeIndicator);
        signer.update(versionNo);
        signer.update(reservedBytes);
        signature = signer.sign();

        // Write the online.sig file
        FileOutputStream fout = new FileOutputStream(ONLINE_SIG_FILE);
        fout.write(typeIndicator);
        fout.write(versionNo);
        fout.write(reservedBytes);
        fout.write(signature);
        fout.close();
        System.out.println("Signed online cert stored in file <" +
                ONLINE_SIG_FILE + ">");
        if (debug) {
            verifyOnlineSigFile();
        }
    }

    private void verifyOnlineSigFile() throws Exception {
        DataInputStream dis = new DataInputStream(
                new FileInputStream(ONLINE_SIG_FILE));
        String typeIndicator = readISO646String(dis, 4);
        if (!typeIndicator.equals("OSIG")) {
            throw new RuntimeException("Invalid TypeIndicator: " + typeIndicator +
                    " in " + ONLINE_SIG_FILE);
        }
        String versionNo = readISO646String(dis, 4);
        if (!versionNo.equals("0200")) {
            throw new RuntimeException("Invalid Version No:" + versionNo +
                    " in " + ONLINE_SIG_FILE);
        }
        // reserved bytes
        dis.skipBytes(32);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        byte[] buf = new byte[32768];
        while ((len = dis.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        dis.close();
        byte[] signature = baos.toByteArray();
        Certificate cert;
        if (onlineCrtFile != null) {
            cert = store.getCertificate(DEF_ONLINE_CERT_ALIAS);
        } else {
            cert = store.getCertificate(contentSignerAlias);
        }
        if (cert == null) {
            if (debug) {
                System.out.println("Could not find public key for " +
                        ONLINE_SIG_FILE + " verification");
            }
            return;
        }
        boolean verified = verifyUsingCert(cert, typeIndicator, versionNo, signature);

        if (verified) {
            System.out.println("Online Cert signature verification PASSED");
        } else {
            System.out.println("Online Cert signature verification FAILED");
        }
    }

    private boolean verifyUsingCert(
            Certificate cert, String typeIndicator, String versionNo, byte[] signature)
            throws Exception {
        Signature verifier = Signature.getInstance(SIG_ALG);
        verifier.initVerify(cert);
        if (skipOnlineDiscroot) {
            verifier.update(new byte[1]);
        } else {
             verifier.update(readIntoBuffer(onlineDiscRootFile));
        }
        verifier.update(typeIndicator.getBytes(ISO));
        verifier.update(versionNo.getBytes(ISO));
        verifier.update(new byte[32]);
        return verifier.verify(signature);
    }

    public static String iso646String(byte[] buf) {
        try {
            return new String(buf, ISO);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }

    public static String readISO646String(DataInputStream dis, int len)
            throws IOException {
        byte[] buf = new byte[len];
        dis.read(buf);
        return iso646String(buf);
    }

    /**
     * This method adds the BD-J specific attribute to the signature file (.SF file)
     * of a signed jar (already signed using JDK's jarsigner). This leads to
     * resigning of the signature file and building a new signature block file
     * (.RSA file) with the updated signature and update the jar with new files.
     * @param jarFile
     * @throws java.lang.Exception
     */
    private void signWithBDJHeader(String jarFile) throws Exception {
        String SIG_FILE = "META-INF/SIG-BD00.SF";
        String SIG_BLOCK_FILE = "META-INF/SIG-BD00.RSA";
        Charset charset = StandardCharsets.ISO_8859_1;
        if (debug) {
            System.out.println("Adding BD header to:" + jarFile);
        }

        PKCS7 signBlock;
        byte[] newContent;
        try (StringWriter sw = new StringWriter()) {
            try (JarFile jf = new JarFile(jarFile)) {
                JarEntry sigFile = (JarEntry) jf.getJarEntry(SIG_FILE);
                if (sigFile == null) {
                    System.out.println("No entry found:" + SIG_FILE);
                }
                JarEntry sigBFile = (JarEntry) jf.getJarEntry(SIG_BLOCK_FILE);
                if (sigBFile == null) {
                    System.out.println("No entry found:" + SIG_BLOCK_FILE);
                }

                try (InputStream pkcs7Is = jf.getInputStream(sigBFile)) {
                    signBlock = new PKCS7(pkcs7Is);
                }

                // Add the desired line to the Signature file.
                try (InputStreamReader in = new InputStreamReader(jf.getInputStream(sigFile), charset);
                     BufferedReader br = new BufferedReader(in)) {

                    String line;
                    boolean addBDLine = false;
                    while ((line = br.readLine()) != null) {
                        if (addBDLine) {
                            // the BD-J header already exists;stop here.
                            if (line.startsWith("BDJ-Signature-Version")) {
                                br.close();
                                jf.close();
                                return;
                            }
                            sw.write("BDJ-Signature-Version: 1.0\n");
                            addBDLine = false;
                        }

                        // BD-J doesn't mandate this attribute; keeping it should be fine
                        if (!line.startsWith("SHA1-Digest-Manifest-Main-Attributes:")) {
                            sw.write(line);
                            sw.write("\n");
                        }
                        if (line.startsWith("Created-By:")) {
                            addBDLine = true;
                        }
                    }
                }
            }
            newContent = sw.toString().getBytes(charset);
        }

        // Re-sign the signature file after adding the BD-J header
        Signature signer = Signature.getInstance(SIG_ALG);
        PrivateKey key = (PrivateKey) store.getKey(contentSignerAlias, contentSignerPassword.toCharArray());
        signer.initSign(key);
        signer.update(newContent);
        byte[] signedContent = signer.sign();

        ContentInfo newContentInfo = new ContentInfo(ContentInfo.DATA_OID, null);
        SignerInfo[] signerInfos = signBlock.getSignerInfos();
        Certificate signerCert = store.getCertificate(contentSignerAlias);
        SignerInfo newSignerInfo = null;

        // Note, BD-J allows only one signerInfo, but let's have this loop since
        // it does not add more code and it's easy to read.
        for (int i = 0; i < signerInfos.length; i++) {
            SignerInfo si = signerInfos[i];
            if (signerCert.equals(si.getCertificate(signBlock))) {

                // update the encrypted digest.
                newSignerInfo = new SignerInfo(si.getIssuerName(),
                        si.getCertificateSerialNumber(),
                        si.getDigestAlgorithmId(),
                        si.getDigestEncryptionAlgorithmId(),
                        signedContent);
                signerInfos[i] = newSignerInfo;
            }
        }

        // generate the updated PKCS7 Block
        PKCS7 newSignBlock = new PKCS7(
                signBlock.getDigestAlgorithmIds(),
                newContentInfo,
                signBlock.getCertificates(),
                signerInfos);
        if (debug) {
            System.out.println("Signer Info Verified:" + (newSignBlock.verify(
                    newSignerInfo, newContent)).toString());
        }

        // Write down the files and re-bundle the jar with the updated signature
        // and signature block files.
        File mif = new File("META-INF");
        File sf, sbf;
        boolean dirCreated = false;
        if (!mif.isDirectory()) {
            if (!mif.mkdir()) {
                System.err.println("Could not create a META-INF directory");
                return;
            }
            dirCreated = true;
        }
        sf = new File(SIG_FILE);
        try (OutputStream fout = Files.newOutputStream(sf.toPath())) {
            fout.write(newContent);
        }
        sbf = new File(SIG_BLOCK_FILE);
        try (OutputStream fos = Files.newOutputStream(sbf.toPath())) {
            newSignBlock.encodeSignedData(fos);
        }
        String[] jarArgs = {"-uvf", jarFile, SIG_FILE, SIG_BLOCK_FILE};
        Main jar = new Main(System.out, System.err, "jar");
        jar.run(jarArgs);

        // Delete the temp files
        sbf.delete();
        sf.delete();
        if (dirCreated) mif.delete();
    }

    private void exportRootCertificate(boolean verify) throws Exception {
        String exportFileName = outputDiscrootFile;
        String type = "root";
        if (isBindingUnitCert) {
            exportFileName = BU_DISC_ROOT_FILE;
            type = "binding";
        }
        String[] exportRootCertificateArgs = {
            "-export", "-alias", certSignerAlias, "-keypass", certSignerPassword,
            "-keystore", keystoreFile, "-storepass", keystorePassword,
            "-v", "-file", exportFileName};
        sun.security.tools.keytool.Main.main(exportRootCertificateArgs);
        if (debug && verify) {
            verifyCertificate(type, exportFileName);
        }
    }

    private void verifyCertificate(String type, String filename) {
        File cert = new File(filename);
        boolean check = new CertificateVerifier().runTest(type, cert);
        if (!check) {
            throw new RuntimeException("Problem with the certification generation");
        }
    }

    private void cleanup() throws IOException {
        File keystore = new File(keystoreFile);
        if (keystore.exists()) {
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                fis = new FileInputStream(keystore);
                ks.load(fis, keystorePassword.toCharArray());
                fos = new FileOutputStream(keystore);
                if (ks.containsAlias(newCertAlias)) {
                    ks.deleteEntry(newCertAlias);
                }

                // Store back the updated keystore to the keystore file.
                ks.store(fos, keystorePassword.toCharArray());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            }
        }
        new File(APPCSRFILE).delete();
        new File(APPCERTFILE).delete();
    }

    private void generateSelfSignedCertificate(String issuer, String alias,
            String keyPassword, boolean isRootCert) throws Exception {
        Date validFrom, validTo;

        // For forcing GeneralizedTime DER encoding, with Bouncy Castle Provider 
        // make the range before 1950 and after 2050. The BD-J spec recommends
        // using the default validity period used below
        Calendar calendar = Calendar.getInstance();
        calendar.set(0000, 1, 1);
        validFrom = calendar.getTime();
        calendar.clear();
        calendar.set(9999, 1, 1);
        validTo = calendar.getTime();

        // Generate a new keypair for this certificate
        KeyPair keyPair = generateKeyPair();

        X509V3CertificateGenerator cg = new X509V3CertificateGenerator();
        cg.reset();
        X509Name name = new X509Name(issuer, new X509BDJEntryConverter());

        // Generate Serial Number
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        BigInteger serNo = new BigInteger(32, prng);
        cg.setSerialNumber(serNo);
        if (!isRootCert) {
            appCertSerNo = serNo;
        }
        cg.setIssuerDN(name);
        cg.setNotBefore(validFrom);
        cg.setNotAfter(validTo);
        cg.setSubjectDN(name);
        cg.setPublicKey(keyPair.getPublic());
        cg.setSignatureAlgorithm(SIG_ALG);
        if (isRootCert) {
            // Need to add root cert extensions.
            if (isBindingUnitCert) {
                // This certificate is used only for signing
                cg.addExtension(X509Extensions.KeyUsage.getId(), true,
                        new X509KeyUsage(X509KeyUsage.digitalSignature));
            } else {
                int usage = X509KeyUsage.digitalSignature +
                        X509KeyUsage.keyCertSign;
                cg.addExtension(X509Extensions.KeyUsage.getId(), true,
                        new X509KeyUsage(usage));
            }
            cg.addExtension(X509Extensions.IssuerAlternativeName.getId(), false,
                    getRfc822Name(altName));
            cg.addExtension(X509Extensions.BasicConstraints.getId(), true,
                    new BasicConstraints(true));
        }
        // For an app cert, most of the extensions will be added when generating
        // a certificate in response to the certificate request file.
        cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
                getRfc822Name(altName));

        Certificate cert = cg.generate(keyPair.getPrivate());
        store.setKeyEntry(alias, keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[]{cert});
        FileOutputStream fos = new FileOutputStream(keystoreFile);
        store.store(fos, keystorePassword.toCharArray());
        fos.close();
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random =
                SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(KEY_LENGTH, random);
        KeyPair keyPair = keyGen.generateKeyPair();
        return keyPair;
    }

    void issueCert(String csrfile, String certfile, String alias, String keypass)
            throws Exception {
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(
                convertFromBASE64(csrfile));
        String subject = csr.getCertificationRequestInfo().getSubject().toString();

        // Generate the app certificate
        X509V3CertificateGenerator cg = new X509V3CertificateGenerator();
        cg.reset();
        X509Certificate rootCert = (X509Certificate) store.getCertificate(alias);
        if (rootCert == null) {
            System.out.println("ERROR: Aborting application certificate creation." +
                    " No root certificate to sign.");
            cleanup(); // removes the self signed certificate from the keystore
            System.exit(1);
        }
        cg.setIssuerDN(new X509Name(true, rootCert.getSubjectDN().getName(),
                new X509BDJEntryConverter()));
        cg.setSubjectDN(new X509Name(subject, new X509BDJEntryConverter()));
        cg.setNotBefore(rootCert.getNotBefore());
        cg.setNotAfter(rootCert.getNotAfter());
        cg.setPublicKey(csr.getPublicKey());
        cg.setSerialNumber(appCertSerNo);

        // BD-J mandates using SHA1WithRSA as a signature Algorithm
        cg.setSignatureAlgorithm(SIG_ALG);
        cg.addExtension(X509Extensions.KeyUsage.getId(), true,
                new X509KeyUsage(X509KeyUsage.digitalSignature));

        // FIXME: Ideally this should be pulled out from the original app cert's
        // extension. Email on X500Name is not encoded with UTF8String.
        cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
                getRfc822Name(altName));

        // Assuming that the root certificate was generated using our tool,
        // the certificate should have IssuerAlternativeNames as an extension.
        if (rootCert.getIssuerAlternativeNames() == null) {
            System.out.println("ERROR: the root certificate must have an alternate name");
            System.exit(1);
        }
        List issuerName = (List) rootCert.getIssuerAlternativeNames().iterator().next();
        cg.addExtension(X509Extensions.IssuerAlternativeName.getId(), false,
                getRfc822Name((String) issuerName.get(1)));
        PrivateKey privateKey = (PrivateKey) store.getKey(alias, keypass.toCharArray());
        X509Certificate cert = cg.generate(privateKey);

        // Now, write leaf certificate
        System.out.println("Writing cert to " + certfile + ".");
        FileOutputStream str = new FileOutputStream(certfile);
        str.write(cert.getEncoded());
        str.close();
    }

    GeneralNames getRfc822Name(String name) {
        GeneralName gn = new GeneralName(GeneralName.rfc822Name,
                new DERIA5String(name));
        DERConstructedSequence seq = new DERConstructedSequence();
        seq.addObject(gn);
        return new GeneralNames(seq);
    }

    //
    // Bouncy castle API requires CSR file in DER (binary format i.e not BASE64)
    // format. CSR PKCS#10 files are normally BASE64 encoded. We remove
    // header, footer lines and decode BASE64 to binary.
    //
    static byte[] convertFromBASE64(String file) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        line = reader.readLine();
        if (!line.equals("-----BEGIN NEW CERTIFICATE REQUEST-----")) {
            throw new IOException("not a valid CSR file");
        }
        boolean seenLastLine = false;
        while ((line = reader.readLine()) != null) {
            if (line.equals("-----END NEW CERTIFICATE REQUEST-----")) {
                seenLastLine = true;
                break;
            }
            buf.append(line);
            buf.append('\n');
        }
        if (!seenLastLine) {
            throw new IOException("not a valid CSR file");
        }
        return Base64.getDecoder().decode(buf.toString());
    }

    /**
     * This method works on only signed jar file that is now being resigned by
     * invalidating the previous signature.
     * This method signs only the jar entries that are part of the original
     * signature. The files that were bundled later (after the jar was signed)
     * will continue to remain unsigned.
     * @param jfile
     * @throws java.lang.Exception
     */
    private void signOriginalJarFile(String jfile) throws Exception {
        JarFile jf = new JarFile(jfile, false); // jar file need not be verified.
        String SIG_FILE = "META-INF/SIG-BD00.SF";
        JarEntry sigFile = (JarEntry) jf.getJarEntry(SIG_FILE);
        if (sigFile == null) {
            System.err.println("ERROR: The jar file is not already signed:" + jfile +
                    " do not use -original-only option");
            jf.close();
            System.exit(1);
        }

        // jar seperation is done based on the files listed in the signature file
        Manifest man = new Manifest(jf.getInputStream(sigFile));
        Map<String, Attributes> sigEntries = man.getEntries();

        //
        // Seperate out the signed and unsigned files into temporary jar files.
        // They are merged after signing.
        // It is required to carry forward jar attributes (such as compression
        // method) for unsigned jar entries. We put them in another jar to
        // file to retian those properties instead of extracting them as regular files.
        // The java.util.jar APIs do not allow updates to the exisiting jar file,
        // hence we end up copying all the entries back and forth the original
        // jar file
        //
        String tmpSignedJar = "tmp-signed-files.jar";
        String tmpUnsignedJar = "tmp-unsigned-files.jar";
        JarOutputStream signedOut = new JarOutputStream(
                new FileOutputStream(tmpSignedJar));

        // If the jar is empty it cannot be closed; however, the
        // file handle it's accessing needs to be closed.
        FileOutputStream fout = new FileOutputStream(tmpUnsignedJar);
        JarOutputStream unsignedOut = new JarOutputStream(fout);

        Enumeration<JarEntry> jarEntries = jf.entries();
        boolean isEmptyUnsigned = true;
        while (jarEntries.hasMoreElements()) {
            JarEntry je = jarEntries.nextElement();
            /**
             * Issue 111.  Do a copy operation on a new JarEntry with 
             * unknown compressed size (-1), to account for a jar file
             * created with a different flavor of zip algorithm.
             * Copy to this new JarEntry relaxes the check for 
             * the compressed data size differece between the original JarEntry 
             * and the new JarEntry that jdk creates.
             */
            JarEntry jeOut = (JarEntry) je.clone();
            jeOut.setCompressedSize(-1);
            String filename = je.getName();

            /** 
             * Issue 112.  Do not include signing related files;
             * these are re-generated during signing.
             */
            if (filename.equals(JarFile.MANIFEST_NAME) ||
                    filename.equals("META-INF/SIG-BD00.RSA") ||
                    filename.equals("META-INF/SIG-BD00.SF")) {
                continue;
            }

            /**
             * "META-INF/" contents are re-generated during signing. The directory
             * entries (new or old) are not signed. To preserve the order of
             * the entries in the original jar we add them to the
             * to-be signed jar
             */
            if (filename.endsWith("/") ||
                    sigEntries.containsKey(filename)) { // this file is signed
                signedOut.putNextEntry(jeOut);
                InputStream jin = jf.getInputStream(je);
                copyfile(signedOut, jin);
                jin.close();
            } else { // this file was added after the jarfile was signed
                isEmptyUnsigned = false;
                unsignedOut.putNextEntry(jeOut);
                InputStream jin = jf.getInputStream(je);
                copyfile(unsignedOut, jin);
                jin.close();
            }
        }

        // close all the files
        jf.close();
        signedOut.closeEntry();
        signedOut.close();
        if (isEmptyUnsigned) {
            fout.close();
            if (debug) {
                System.out.println(
                        "No new files are present in the jar; Signing without splitting");
            }
            signJarFile(jfile);
        } else {
            unsignedOut.closeEntry();
            unsignedOut.close();

            // resign the signed jar
            signJarFile(tmpSignedJar);

            // The next step is to merge the signed and unsigned jars
            JarOutputStream jout = new JarOutputStream(
                    new FileOutputStream(jfile));
            copyJarFile(tmpSignedJar, jout);
            copyJarFile(tmpUnsignedJar, jout);
            jout.close();
        }

        if (debug) {
            return;
        }

        // attempt cleanup
        File signedJar = new File(tmpSignedJar);
        File unsignedJar = new File(tmpUnsignedJar);
        if (!signedJar.delete()) {
            System.out.println("Unable to delete temporary jar:" +
                    tmpSignedJar + " Please try deleting it manually");
            System.exit(1);
        }
        if (unsignedJar.exists() && !unsignedJar.delete()) {
            System.out.println("Unable to delete temporary jar:" +
                    tmpUnsignedJar + " Please try deleting it manually");
            System.exit(1);
        }
    }

    private void copyfile(OutputStream out, InputStream in)
            throws IOException {
        int len;
        byte[] buf = new byte[32768];
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    private void copyJarFile(String jarName, JarOutputStream jout)
            throws IOException {
        JarFile jf = new JarFile(jarName);
        Enumeration<JarEntry> jarEntries = jf.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry je = jarEntries.nextElement();
            jout.putNextEntry(je);
            InputStream jin = jf.getInputStream(je);
            copyfile(jout, jin);
            jin.close();
        }
        jf.close();
    }
}
