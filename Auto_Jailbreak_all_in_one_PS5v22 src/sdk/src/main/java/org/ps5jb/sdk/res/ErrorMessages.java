package org.ps5jb.sdk.res;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import org.ps5jb.loader.Status;

/**
 * Central class to manage all the SDK error message strings.
 */
public final class ErrorMessages {
    private static ResourceBundle resourceBundle;

    private static final String packagePrefix = ErrorMessages.class.getPackage().getName();
    private static final String rootPackage = packagePrefix.substring(0, packagePrefix.lastIndexOf("."));

    /**
     * Retrieve the locale used to load the resource bundle. This value
     * can be specified using the system property "org.ps5jb.sdk.res.Locale".
     * If such property does not exist, default system locale will be used.
     * The default resource bundle supplied with the SDK only has messages
     * in English.
     *
     * @return Locale used for the resource bundle.
     */
    private static Locale getLocale() {
        Locale locale;
        String localeVal = System.getProperty(packagePrefix + ".Locale");
        if (localeVal == null) {
            locale = Locale.getDefault();
        } else {
            String[] localeComponents = new String[] { null, null, null };
            int localeCompIndx = 0;
            StringTokenizer localeTok = new StringTokenizer(localeVal, "_");
            while (localeTok.hasMoreTokens()) {
                localeComponents[localeCompIndx++] = localeTok.nextToken();
            }
            locale = new Locale(localeComponents[0], localeComponents[1], localeComponents[2]);
        }
        return locale;
    }

    /**
     * Default constructor. Should not be used as all methods in this class are static.
     */
    private ErrorMessages() {
    }

    static {
        try {
            resourceBundle = ResourceBundle.getBundle(packagePrefix + ".error_messages", getLocale());
        } catch (MissingResourceException e) {
            // Resource bundle could not be loaded, all messages will be just the message key names
            Status.printStackTrace(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the localized error message for a given message key.
     *
     * @param key Unique message identifier.
     * @param formatArgs Optional array of dynamic parameters which will be replaced in the message using {@link MessageFormat}.
     * @return Value of the message or <code>key</code> as is if message with the given identifier could not be found.
     */
    public static String getErrorMessage(String key, Object ... formatArgs) {
        if (resourceBundle != null) {
            try {
                String val = resourceBundle.getString(key);
                return MessageFormat.format(val, formatArgs);
            } catch (MissingResourceException e) {
                // Fallback to return the message key name
            }
        }
        return key;
    }

    /**
     * Retrieve the localized error message by deriving the key from the class name.
     *
     * @param clazz Class which claims ownership of the error message.
     * @param keySuffix Suffix to append to the derived class prefix to produce the final message key.
     * @param formatArgs Optional array of dynamic parameters which will be replaced in the message using {@link MessageFormat}.
     * @return Value returned by {@link #getErrorMessage(String, Object...)} for the key derived from the class name.
     * @see #getErrorMessage(String, Object...)
     */
    public static String getClassErrorMessage(Class clazz, String keySuffix, Object ... formatArgs) {
        String keyPrefix = clazz.getName();
        if (keyPrefix.startsWith(rootPackage)) {
            keyPrefix = keyPrefix.substring(rootPackage.length() + 1);
        }
        return getErrorMessage(keyPrefix + "." + keySuffix, formatArgs);
    }
}
