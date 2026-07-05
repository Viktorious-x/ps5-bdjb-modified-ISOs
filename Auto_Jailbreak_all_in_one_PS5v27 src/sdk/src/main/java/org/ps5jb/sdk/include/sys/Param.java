package org.ps5jb.sdk.include.sys;

/**
 * This class represents <code>include/sys/param.h</code> from FreeBSD source.
 */
public class Param {
    /* Max command name remembered */
    public static final int MAXCOMLEN = 19;
    /* Max hostname size */
    public static final int MAXHOSTNAMELEN = 256;
    /* MAXPATHLEN defines the longest permissible path length after expanding symbolic links */
    public static final int MAXPATHLEN = SysLimits.PATH_MAX;

    public static long rounddown(long x, long y) {
        return (x / y) * y;
    }
}
