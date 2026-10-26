
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

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.XmlType;

/**
 * ApplicationDescriptor - section 10.2.6
 *
 * @author A. Sundararajan
 */
@XmlType(
  propOrder= {
      "profiles","priority", "binding", "visibility", "names",
      "iconLocator", "iconFlags", "baseDirectory", "classpathExtension",
      "initialClassName", "parameters"
  }
)
public class ApplicationDescriptor {
    // Application_profiles_count is specified in 4 bits
    public static final int NUM_PROFILES_BITS = 4;
    public static final int MAX_PROFILES = (1 << NUM_PROFILES_BITS);
    // application_icon_locator_length is specified in 8 bits
    public static final int NUM_ICON_BITS = 8;
    public static final int MAX_ICON_LENGTH = (1 << NUM_ICON_BITS);
    // base_directory_length is specified in 8 bits
    public static final int NUM_BASEDIR_BITS = 8;
    public static final int MAX_BASEDIR_LENGTH = (1 << NUM_BASEDIR_BITS);
    // classpath_extension_length is specified in 8 bits
    public static final int NUM_CLASSPATH_BITS = 8;
    public static final int MAX_CLASSPATH_LENGTH = (1 << NUM_CLASSPATH_BITS);
    // initial_class_name_length is specified in 8 bits
    public static final int NUM_CLASSNAME_BITS = 8;
    public static final int MAX_CLASSNAME_LENGTH = (1 << NUM_CLASSNAME_BITS);
    // number_of_parameter_bytes is specified in 8 bits
    public static final int NUM_PARAMETER_BITS = 8;
    public static final int MAX_PARAMETER_LENGTH = (1 << NUM_PARAMETER_BITS);
    
    private AppProfile[] profiles;
    private short priority;
    private Binding binding = Binding.TITLE_BOUND_DISC_BOUND;
    private Visibility visibility = Visibility.V_00;
    private AppName[] names;
    private String iconLocator;
    private short iconFlags;
    private String baseDirectory;
    private String classpathExtension;
    private String initialClassName;
    private String[] parameters;

    public AppProfile[] getProfiles() {
        return profiles;
    }

    public void setProfiles(AppProfile[] profiles) {
        if (profiles != null) {
            if (profiles.length > MAX_PROFILES) {
                throw new IllegalArgumentException("overflow profiles : " +
                    profiles.length);
            }
        }
        this.profiles = profiles;
    }

    public short getPriority() {
        return priority;
    }

    public void setPriority(short priority) {
        this.priority = priority;
    }

    public Binding getBinding() {
        return binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public String getIconLocator() {
        return iconLocator;
    }

    public void setIconLocator(String iconLocator) {
        if (iconLocator != null) {
            if (iconLocator.length() >= MAX_ICON_LENGTH) {
                throw new IllegalArgumentException("iconLocator length exceeded : " +
                    iconLocator.length());
            }
        }
        this.iconLocator = iconLocator;
    }

    @XmlJavaTypeAdapter(HexStringShortAdapter.class)    
    public Short getIconFlags() {
        return iconFlags;
    }

    public void setIconFlags(Short iconFlags) {
        this.iconFlags = iconFlags;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        if (baseDirectory != null) {
            if (baseDirectory.length() >= MAX_BASEDIR_LENGTH) {
                throw new IllegalArgumentException("baseDirectory length exceeded : " +
                    baseDirectory.length());
            }
        }
        this.baseDirectory = baseDirectory;
    }

    public String getClasspathExtension() {
        return classpathExtension;
    }

    public void setClasspathExtension(String classpathExtension) {
        if (classpathExtension != null) {
            int len = classpathExtension.length();
            if (len >= MAX_CLASSPATH_LENGTH) {
                throw new IllegalArgumentException("classpathExtension length exceeded : " + len);
            }
        }
        this.classpathExtension = classpathExtension;
    }

    public String getInitialClassName() {
        return initialClassName;
    }

    public void setInitialClassName(String initialClassName) {
        if (initialClassName != null) {
            int len = BDJO.utf8Length(initialClassName);
            if (len >= MAX_CLASSNAME_LENGTH) {
                throw new IllegalArgumentException("initialClassName length exceeded : " + len);
            }
        }
        this.initialClassName = initialClassName;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        if (parameters != null) {
            for (String param : parameters) {
                int len = BDJO.utf8Length(param);
                if (len >= MAX_PARAMETER_LENGTH) {
                    throw new IllegalArgumentException("parameter length exceeded : " + len);
                }
            }
        }
        this.parameters = parameters;
    }

    public AppName[] getNames() {
        return names;
    }

    public void setNames(AppName[] names) {
        this.names = names;
    }
}
