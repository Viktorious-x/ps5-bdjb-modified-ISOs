package org.ps5jb.client.payloads.umtx.common;

import org.ps5jb.loader.Status;

/** Quick local hack to provide level-based message output */
public class DebugStatus {
    public static class Level {
        public static final Level TRACE = new Level(5, "TRACE");
        public static final Level DEBUG = new Level(10, "DEBUG");
        public static final Level NOTICE = new Level(20, "NOTICE");
        public static final Level INFO = new Level(30, "INFO");
        public static final Level ERROR = new Level(40, "ERROR");

        private String name;
        private int level;

        private Level(int level, String name) {
            this.level = level;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static Level level = Level.INFO;

    public static boolean isTraceEnabled() {
        return level.level <= Level.TRACE.level;
    }

    public static void trace(String message) {
        if (isTraceEnabled()) {
            Status.println(message);
        }
    }

    public static boolean isDebugEnabled() {
        return level.level <= Level.DEBUG.level;
    }

    public static void debug(String message) {
        if (isDebugEnabled()) {
            Status.println(message);
        }
    }

    public static boolean isNoticeEnabled() {
        return level.level <= Level.NOTICE.level;
    }

    public static void notice(String message) {
        if (isNoticeEnabled()) {
            Status.println(message);
        }
    }

    public static boolean isInfoEnabled() {
        return level.level <= Level.INFO.level;
    }

    public static void info(String message) {
        if (isInfoEnabled()) {
            Status.println(message);
        }
    }

    public static boolean isErrorEnabled() {
        return level.level <= Level.ERROR.level;
    }

    public static void error(String message) {
        if (isErrorEnabled()) {
            Status.println(message);
        }
    }

    public static void error(String message, Throwable ex) {
        if (isErrorEnabled()) {
            Status.printStackTrace(message, ex);
        }
    }
}
