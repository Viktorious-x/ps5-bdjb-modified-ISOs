package org.ps5jb.client.utils.stdio;

import java.io.FileDescriptor;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.ErrNo;
import org.ps5jb.sdk.io.FileDescriptorFactory;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Stores the current state of stdio redirection and implements operations
 * to redirect or restore stdio streams to a given file descriptor.
 */
public class StdioRedirectionState {
    protected LibKernel libKernel;
    protected ErrNo errNo;

    protected int origStdOut = -1;
    protected int origStdErr = -1;
    protected int stdOutDup = -1;
    protected int stdErrDup = -1;

    public StdioRedirectionState() {
        this.libKernel = new LibKernel();
        this.errNo = new ErrNo(libKernel);
    }

    public StdioRedirectionState(LibKernel libKernel, ErrNo errNo) {
        this.libKernel = libKernel;
        this.errNo = errNo;
    }

    /**
     * Redirects stdio to the given file descriptor.
     *
     * @param targetFileDescriptor File descriptor to redirect to.
     * @throws SdkException If any exceptions occur during native calls required to perform redirection.
     */
    public void redirectStdIo(FileDescriptor targetFileDescriptor) throws SdkException {
        if (origStdOut != -1) {
            throw new IllegalStateException("Stdio already redirected");
        }

        try {
            int toFd = FileDescriptorFactory.getFd(targetFileDescriptor);

            origStdOut = FileDescriptorFactory.getFd(FileDescriptor.out);
            stdOutDup = errNo.checkLastException(libKernel.dup(origStdOut), getClass(), "redirectStdIo");
            origStdErr = FileDescriptorFactory.getFd(FileDescriptor.err);
            stdErrDup = errNo.checkLastException(libKernel.dup(origStdErr), getClass(), "redirectStdIo");

            errNo.checkLastException(libKernel.dup2(toFd, origStdOut), getClass(), "redirectStdIo");
            errNo.checkLastException(libKernel.dup2(toFd, origStdErr), getClass(), "redirectStdIo");
        } catch (SdkException | RuntimeException | Error e) {
            restoreStdIo();
            throw e;
        }
    }

    /**
     * Restore original stdio streams. Does nothing if stdio is not redirected.
     * If there are any errors during restoration, exception is not thrown
     * but a warning is output in {@link Status}.
     */
    public void restoreStdIo() {
        if (stdOutDup != -1 && origStdOut != -1) {
            if (libKernel.dup2(stdOutDup, origStdOut) == -1) {
                Status.println("Warning, unable to restore the original stdout. Error: " + errNo.getLastError());
            }
            if (libKernel.close(stdOutDup) == -1) {
                Status.println("Warning, unable to close duplicated stdout. Error: " + errNo.getLastError());
            }
            stdOutDup = -1;
            origStdOut = -1;
        }
        if (stdErrDup != -1 && origStdErr != -1) {
            if (libKernel.dup2(stdErrDup, origStdErr) == -1) {
                Status.println("Warning, unable to restore the original stderr. Error: " + errNo.getLastError());
            }
            if (libKernel.close(stdErrDup) == -1) {
                Status.println("Warning, unable to close duplicated stderr. Error: " + errNo.getLastError());
            };
            stdErrDup = -1;
            origStdErr = -1;
        }
    }
}
