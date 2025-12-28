package org.ps5jb.sdk.include;

import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/errno.h</code> from FreeBSD source.
 */
public class ErrNo extends org.ps5jb.sdk.include.sys.ErrNo {
    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public ErrNo(LibKernel libKernel) {
        super(libKernel);
    }
}
