package org.ps5jb.client.payloads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.io.File;

/**
 * This sample uses native code execution to list accessible entries starting from root directory.
 * Since the PS5 screen output is rather small, it's advisable to properly configure
 * the JAR loader with a {@link org.ps5jb.loader.RemoteLogger remote logger} which would
 * allow to capture all the textual screen output over network.
 */
public class ListDirEnts implements Runnable {
    /**
     * Use native calls from "dirent" module to list the available filesystem entries.
     */
    public void run() {
        try {
            printDirEnts("/", "");
        } catch (Throwable e) {
            Status.printStackTrace(e.getMessage(), e);
        }
    }

    public void getDirEnts(List dirEnts, String path, boolean recurse) throws IOException {
        File root = new File(path);

        java.io.File[] children = root.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                File childFile = (File) children[i];
                dirEnts.add(childFile);

                if (recurse && childFile.isDirectory()) {
                    getDirEnts(dirEnts, childFile.getCanonicalPath(), recurse);
                }
            }
        }
    }

    private void printDirEnts(String path, String indent) throws IOException {
        List dirEnts = new ArrayList();
        try {
            getDirEnts(dirEnts, path, false);
        } catch (IOException e) {
            Status.println(indent + "  [ERROR] " + e.getMessage());
        }

        Iterator dirEntsIter = dirEnts.iterator();
        while (dirEntsIter.hasNext()) {
            File file = (File) dirEntsIter.next();
            Status.println(indent + file.getName() + (file.isDirectory() ? " [DIR]" : ""));
            if (file.isDirectory()) {
                printDirEnts(file.getCanonicalPath(), indent + "  ");
            }
        }
    }
}
