package org.ps5jb.sdk.io;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

public class FileReader extends InputStreamReader {
    public FileReader(String fileName) throws FileNotFoundException {
        super(new FileInputStream(fileName));
    }

    public FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
    }

    public FileReader(FileDescriptor fd) {
        super(new FileInputStream(fd));
    }
}
