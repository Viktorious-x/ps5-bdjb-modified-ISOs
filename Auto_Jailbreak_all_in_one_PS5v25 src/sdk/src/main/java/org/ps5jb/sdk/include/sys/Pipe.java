package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.include.machine.Param;

/**
 * This class represents <code>include/sys/pipe.h</code> from FreeBSD source.
 */
public class Pipe {
    /** Pipe buffer size, keep moderate in value, pipes take kva space. */
    public static final long PIPE_SIZE = 16384;

    public static final long BIG_PIPE_SIZE = 64 * 1024;

    public static final long SMALL_PIPE_SIZE = Param.PAGE_SIZE;

    /** PIPE_MINDIRECT MUST be smaller than PIPE_SIZE and MUST be bigger than PIPE_BUF. */
    public static final long PIPE_MINDIRECT = 8192;

    public static final long PIPENPAGES = (BIG_PIPE_SIZE / Param.PAGE_SIZE + 1);
}
