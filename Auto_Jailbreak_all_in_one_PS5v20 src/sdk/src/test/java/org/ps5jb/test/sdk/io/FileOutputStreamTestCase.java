package org.ps5jb.test.sdk.io;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.include.sys.errno.OperationNotPermittedException;
import org.ps5jb.sdk.io.File;
import org.ps5jb.sdk.io.FileDescriptorFactory;
import org.ps5jb.sdk.io.FileOutputStream;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.test.Mockito;
import org.ps5jb.test.MockitoTestCase;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

public class FileOutputStreamTestCase extends MockitoTestCase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testNewFileOutputStreamFromString(boolean append) throws IOException {
        MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class,
                (libKernel, ctx) ->
                        Mockito.when(new Integer(libKernel.open(anyString(), anyInt())))
                                .thenReturn(new Integer(65)));
        try {
            FileOutputStream out;
            if (!append) {
                out = new FileOutputStream("test");
            } else {
                out = new FileOutputStream("test", append);
            }
            try {
                Assertions.assertEquals(1, mc.constructed().size());
                Assertions.assertEquals(65, FileDescriptorFactory.getFd(out.getFd()));
            } finally {
                out.close();
            }
        } finally {
            mc.close();
        }
    }
    @Test
    public void testNewFileOutputStreamNotFound() throws IOException {
        Pointer ptr = Pointer.calloc(4);
        try {
            ptr.write4(ErrNo.ord(ErrNo.EPERM));
            MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class,
                    (libKernel, ctx) -> {
                        Mockito.when(new Integer(libKernel.open(anyString(), anyInt())))
                                .thenReturn(new Integer(-1));
                        Mockito.when(libKernel.__error())
                                .thenReturn(ptr);
                    });
            try {
                FileOutputStream out = new FileOutputStream("test-file");
                out.close();
                Assertions.fail("FileNotFoundException should have been thrown");
            } catch (FileNotFoundException e) {
                Assertions.assertTrue(e.getMessage().indexOf("test-file") != -1);
                Assertions.assertTrue(e.getMessage().indexOf(ErrNo.EPERM) != -1);
                Assertions.assertTrue(e.getCause() instanceof OperationNotPermittedException);
            } finally {
                mc.close();
            }
        } finally {
            ptr.free();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testNewFileOutputStreamFromFile(boolean append) throws IOException {
        MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class,
                (libKernel, ctx) ->
                        Mockito.when(new Integer(libKernel.open(anyString(), anyInt())))
                                .thenReturn(new Integer(49)));
        try {
            FileOutputStream out;
            if (!append) {
                out = new FileOutputStream(new File("test"));
            } else {
                out = new FileOutputStream(new File("test"), append);
            }
            try {
                // One in stream, one in file
                Assertions.assertEquals(2, mc.constructed().size());
                Assertions.assertEquals(49, FileDescriptorFactory.getFd(out.getFd()));
            } finally {
                out.close();
            }
        } finally {
            mc.close();
        }
    }

    @Test
    public void testNewFileOutputStreamFromDescriptor() throws IOException {
        MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class);
        try {
            FileOutputStream out = new FileOutputStream(FileDescriptorFactory.createFileDescriptor(39));
            try {
                // One in stream, one in file
                Assertions.assertEquals(0, mc.constructed().size());
                Assertions.assertEquals(39, FileDescriptorFactory.getFd(out.getFd()));
            } finally {
                out.close();
            }
        } finally {
            mc.close();
        }
    }
}
