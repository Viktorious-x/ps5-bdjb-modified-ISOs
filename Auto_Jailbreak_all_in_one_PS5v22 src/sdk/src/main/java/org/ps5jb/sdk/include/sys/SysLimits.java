package org.ps5jb.sdk.include.sys;

/**
 * This class represents <code>include/sys/syslimits.h</code> from FreeBSD source.
 */
public class SysLimits {
    /* Max bytes for an exec function */
    public static final int ARG_MAX = 262144;

    /* Max bytes in a file name */
    public static final int NAME_MAX = 255;

    /* Max bytes in pathname */
    public static final int PATH_MAX = 1024;

    /* Max bytes for atomic pipe writes */
    public static final int PIPE_BUF = 512;

    /* Max elements in i/o vector */
    public static final int IOV_MAX = 1024;
}
