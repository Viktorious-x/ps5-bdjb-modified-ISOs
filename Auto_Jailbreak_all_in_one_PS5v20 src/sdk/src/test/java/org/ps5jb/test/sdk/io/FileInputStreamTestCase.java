package org.ps5jb.test.sdk.io;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.include.sys.errno.NotFoundException;
import org.ps5jb.sdk.io.File;
import org.ps5jb.sdk.io.FileDescriptorFactory;
import org.ps5jb.sdk.io.FileInputStream;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.test.Mockito;
import org.ps5jb.test.MockitoTestCase;
import org.ps5jb.test.sdk.core.TestPointer;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

public class FileInputStreamTestCase extends MockitoTestCase {
    @Test
    public void testNewFileInputStreamFromString() throws IOException {
        MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class,
                (libKernel, ctx) ->
                        Mockito.when(new Integer(libKernel.open(anyString(), anyInt())))
                                .thenReturn(new Integer(35)));
        try {
            FileInputStream in = new FileInputStream("test");
            try {
                Assertions.assertEquals(1, mc.constructed().size());
                Assertions.assertEquals(35, FileDescriptorFactory.getFd(in.getFd()));
            } finally {
                in.close();
            }
        } finally {
            mc.close();
        }
    }
    @Test
    public void testNewFileInputStreamNotFound() throws IOException {
        Pointer ptr = Pointer.calloc(4);
        try {
            ptr.write4(ErrNo.ord(ErrNo.ESRCH));
            MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class,
                    (libKernel, ctx) -> {
                        Mockito.when(new Integer(libKernel.open(anyString(), anyInt())))
                                .thenReturn(new Integer(-1));
                        Mockito.when(libKernel.__error())
                                .thenReturn(ptr);
                    });
            try {
                FileInputStream in = new FileInputStream("test-file");
                in.close();
                Assertions.fail("FileNotFoundException should have been thrown");
            } catch (FileNotFoundException e) {
                Assertions.assertTrue(e.getMessage().indexOf("test-file") != -1);
                Assertions.assertTrue(e.getMessage().indexOf(ErrNo.ESRCH) != -1);
                Assertions.assertTrue(e.getCause() instanceof NotFoundException);
            } finally {
                mc.close();
            }
        } finally {
            ptr.free();
        }
    }

    @Test
    public void testNewFileInputStreamFromFile() throws IOException {
        MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class,
                (libKernel, ctx) ->
                        Mockito.when(new Integer(libKernel.open(anyString(), anyInt())))
                                .thenReturn(new Integer(40)));
        try {
            FileInputStream in = new FileInputStream(new File("test"));
            try {
                // One in stream, one in file
                Assertions.assertEquals(2, mc.constructed().size());
                Assertions.assertEquals(40, FileDescriptorFactory.getFd(in.getFd()));
            } finally {
                in.close();
            }
        } finally {
            mc.close();
        }
    }

    @Test
    public void testNewFileInputStreamFromDescriptor() throws IOException {
        MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class);
        try {
            FileInputStream in = new FileInputStream(new FileDescriptor());
            try {
                // One in stream, one in file
                Assertions.assertEquals(0, mc.constructed().size());
                Assertions.assertEquals(-1, FileDescriptorFactory.getFd(in.getFd()));
            } finally {
                in.close();
            }
        } finally {
            mc.close();
        }
    }
}
