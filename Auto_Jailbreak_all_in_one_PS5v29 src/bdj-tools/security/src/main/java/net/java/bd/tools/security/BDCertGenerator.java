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

/** 
 * BDCertGenerator syntax is : BDCertGenerator [-debug] -orgid 8-digit-hex-organization-ID
 * 
 * Example: java -cp $SECURITY_HOME/build/security.jar:$JDK_HOME/lib/tools.jar:$SECURITY_HOME/resource/bcprov-jdk15-137.jar net.java.bd.tools.signer.BDCertGenerator -orgid 56789abc 00000.jar 
 * 
 * Make sure to put security.jar before tools.jar in the jdk distribution for
 * the jre classpath so that the modified version of the sun.security.* classes
 * in this BDCertGenerator respository are used during the run.
 * bdprov-jdk15-137.jar is a bouncycastle distribution; a copy can be found in
 * the "resources" dir.
 */
public class BDCertGenerator {
    // Represents the certificate type that is being created

    enum CertType {

        NONE, ROOT, APP, BINDING
    };

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsageAndExit("Please enter certificate type");
        }
        SecurityUtil.Builder builder = new SecurityUtil.Builder();
        boolean setOrgId = false;
        CertType certType = CertType.NONE;
        for (int i = 0; i < args.length; i++) {
            String opt = args[i];
            if (opt.equals("-keystore")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.keystoreFile(args[i]);
            } else if (opt.equals("-storepass")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.storepass(args[i]);
            } else if (opt.equals("-root")) {
                builder = builder.setRootCert();
                certType = CertType.ROOT;
            } else if (opt.equals("-app")) {
                builder = builder.setAppCert();
                certType = CertType.APP;
            } else if (opt.equals("-binding")) {
                builder = builder.setBindingUnitCert();
                certType = CertType.BINDING;
            } else if (opt.equals("-dn")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.dn(args[i]);
            } else if (opt.equals("-altname")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.altName(args[i]);
            } else if (opt.equals("-alias")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.newCertAlias(args[i]);
            } else if (opt.equals("-keypass")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.newKeyPassword(args[i]);
            } else if (opt.equals("-signeralias")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.certSignerAlias(args[i]);
            } else if (opt.equals("-signerpass")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.certSignerPassword(args[i]);
            } else if (opt.equals("-file")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.outputDiscrootFile(args[i]);
            } else if (opt.equals("-nodiscroot")) {
                builder = builder.dontWriteDiscroot();
            } else if (opt.equals("-help")) {
                printUsageAndExit("");
            } else if (opt.equals("-debug")) {
                builder = builder.debug();
            } else {
                String orgId = args[i];
                if (orgId.startsWith("0X") || orgId.startsWith("0x")) {
                    orgId = orgId.substring(2);
                }
                if (orgId.length() != 8) {
                    printUsageAndExit("Bad OrgID " + orgId + ", please provide an 8 digit hex.");
                }
                builder = builder.orgId(orgId);
                setOrgId = true;
            }
        }
        if (certType == CertType.NONE) {
            printUsageAndExit("Please enter type of the certificate to generate:" +
                    "root/app/binding");
        }
        if (!setOrgId) {
            switch (certType) {
                case ROOT:
                    break; // It's okay not have the orgID
                case APP:  // May not be okay. But acceptable for non-leaf certs
                    System.err.println(
                            "============================================================");
                    System.err.println(
                            "WARNING: orgID is optional only for the non-leaf certificate.");
                    System.err.println(
                            "You may want specify an orgID for this certificate");
                    System.err.println(
                            "============================================================");
                    break;
                case BINDING:
                    printUsageAndExit("orgID that matches the orgID in the BUMF must be specified");
                    break;
            }
        }
        SecurityUtil util = builder.build();
        util.createCerts();
    }

    static private void tinyHelp() {
        System.err.println("Try BDCertGenerator -help");

        // do not drown user with the help lines.
        System.exit(1);
    }

    static private void errorNeedArgument(String flag) {
        System.err.println("Command option " + flag + " needs an argument.");
        tinyHelp();
    }

    private static void printUsageAndExit(String reason) {
        if (!reason.isEmpty()) {
            System.err.println("\nFailed: " + reason + "\n");
        }
        System.err.println("***This tool generates keystore and certificates for securing BD-J applications***\n");
        System.err.println("usage: BDCertGenerator -root|-app|-binding [options] [organization_id]\n");
        System.err.println("Valid Options:");
        System.err.println(" -keystore    filename\t:Create a keystore file with the given filename");
        System.err.println(" -storepass   password\t:Password for accessing the new keystore");
        System.err.println(" -dn          name    \t:Distinguished name of the certificate");
        System.err.println("                      \t Note: The organization_id is appended to the org name");
        System.err.println("                      \t component of the dn");
        System.err.println(" -altname     name    \t:Subject alternate name for the certificate");
        System.err.println(" -alias       name    \t:Keystore alias for the newly generated certificate");
        System.err.println(" -keypass     password\t:Password for the newly generated key");
        System.err.println(" -signeralias name    \t:Alias of the application certificate signer");
        System.err.println(" -signerpass  password\t:Key password of the application certificate signer");
        System.err.println(" -file                \t:Name of the resulting app disc root crt file");
        System.err.println("                      \t If none is specified the cert is stored in the file: app.discroot.crt\n");
        System.err.println(" -nodiscroot          \t Do not write out app disc root crt file");
        System.err.println(" -debug               \t:Prints debug messages");
        System.err.println(" -help                \t:Prints this message");
        System.err.println("\nExample: java net.java.bd.tools.security.BDCertGenerator -root 56789abc \n");
        System.exit(1);
    }
}
