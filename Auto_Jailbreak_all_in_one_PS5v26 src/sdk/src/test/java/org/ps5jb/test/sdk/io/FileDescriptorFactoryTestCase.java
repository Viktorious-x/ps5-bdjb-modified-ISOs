package org.ps5jb.test.sdk.io;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.sys.fcntl.OpenFlag;
import org.ps5jb.sdk.io.FileDescriptorFactory;
import org.ps5jb.sdk.io.ProxiedFileDescriptorException;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.test.Mockito;
import org.ps5jb.test.MockitoTestCase;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

public class FileDescriptorFactoryTestCase extends MockitoTestCase {
    @Test
    public void testCreateFileDescriptor() {
        FileDescriptor fileDesc = FileDescriptorFactory.createFileDescriptor(5);
        Assertions.assertEquals(5, FileDescriptorFactory.getFd(fileDesc));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testOpenFileAndGetFd(boolean append) throws SdkException {
        MockedConstruction<LibKernel> mc = Mockito.mockedConstruction(LibKernel.class,
                (libKernel, ctx) ->
                        Mockito.when(new Integer(libKernel.open(anyString(), anyInt())))
                                .thenReturn(new Integer(25)));
        try {
            OpenFlag[] openFlags;
            if (append) {
                openFlags = new OpenFlag[] { OpenFlag.O_WRONLY, OpenFlag.O_APPEND };
            } else {
                openFlags = new OpenFlag[] { OpenFlag.O_RDONLY };
            }
            FileDescriptor fileDesc = FileDescriptorFactory.openFile("test", openFlags);
            Assertions.assertEquals(1, mc.constructed().size());
            Assertions.assertEquals(25, FileDescriptorFactory.getFd(fileDesc));
        } finally {
            mc.close();
        }
    }

    @Test
    public void testGetSocketFileDescriptor() throws IOException {
        ServerSocket ss = new ServerSocket(0);
        try {
            FileDescriptor serverFileDesc = FileDescriptorFactory.getSocketFileDescriptor(ss);
            int serverFd = FileDescriptorFactory.getFd(serverFileDesc);
            Assertions.assertTrue(serverFd > 0,
                    "Server socket fd is expected to be a positive int: " + serverFd);

            Socket cs = new Socket((String) null, ss.getLocalPort());
            try {
                FileDescriptor clientFileDesc = FileDescriptorFactory.getSocketFileDescriptor(cs);
                int clientFd = FileDescriptorFactory.getFd(clientFileDesc);
                Assertions.assertTrue(clientFd > 0,
                        "Client socket fd is expected to be a positive int: " + clientFd);
            } finally {
                cs.close();
            }
        } finally {
            ss.close();
        }
    }

    @Test
    public void testGetDatagramSocketFileDescriptor() throws IOException {
        DatagramSocket ss = new DatagramSocket();
        try {
            FileDescriptor serverFileDesc = FileDescriptorFactory.getSocketFileDescriptor(ss);
            int serverFd = FileDescriptorFactory.getFd(serverFileDesc);
            Assertions.assertTrue(serverFd > 0,
                    "Server socket fd is expected to be a positive int: " + serverFd);
        } finally {
            ss.close();
        }
    }

    @Test
    public void testGetFdProxied() {
        try {
            FileDescriptor proxiedFd = FileDescriptorFactory.createFileDescriptor(6000);
            FileDescriptorFactory.getFd(proxiedFd);
            Assertions.fail("Should have thrown a ProxiedFileDescriptorException");
        } catch (ProxiedFileDescriptorException e) {
            Assertions.assertNotNull(e.getMessage(), "Should have thrown an exception with a message");
        }
    }
}
