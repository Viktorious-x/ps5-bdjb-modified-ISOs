/**
 * This package contains classes which offer a Java native array
 * view over native memory. This technique crashes when using
 * certain garbage collectors (including the default one on most platforms).
 * Fortunately, PS5 uses serial GC by default which seems to be
 * unaffected.
 */
package org.ps5jb.sdk.core.dataview;
