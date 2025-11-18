package org.ps5jb.client.payloads;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

import org.ps5jb.loader.Config;
import org.ps5jb.loader.Status;

/**
 * This simple payload outputs the values of all the system properties.
 */
public class PrintSystemProperties implements Runnable {
    /**
     * Print all system properties sorted alphabetically by property name.
     */
    @Override
    public void run() {
        // Sample: disable remote logger
        Status.resetLogger(null, 0, 0);
        Status.println("The following message will not show up on the remote logging server");

        // Sample: enable default remote logger
        Status.resetLogger(Config.getLoggerHost(), Config.getLoggerPort(), Config.getLoggerTimeout());
        Status.println("The following message will show up on the remote logging server");

        Properties props = System.getProperties();

        Enumeration propNames = props.propertyNames();
        TreeSet sortedPropNames = new TreeSet();
        while (propNames.hasMoreElements()) {
            String propName = (String) propNames.nextElement();
            sortedPropNames.add(propName);
        }

        Iterator sortedPropIter = sortedPropNames.iterator();
        while (sortedPropIter.hasNext()) {
            String sortedPropName = (String) sortedPropIter.next();
            String value = props.getProperty(sortedPropName);
            Status.println(sortedPropName + " = " + value);
        }
    }
}
