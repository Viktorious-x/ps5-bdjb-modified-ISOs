/**
 * This package contains helper classes for doing native code execution
 * and wrappers around some of the FreeBSD and PS5 standard native libraries.
 *
 * The structure of this package is as follows:
 * <ul>
 *   <li><code>core</code> - Base classes to perform native code execution.</li>
 *   <li><code>lib</code> - This package contains raw native library mappings which correspond to sprx dynamic libraries on PS5.
 *     It's not advisable to use these functions directly. Instead, create a proper wrapper in "include" directory.
 *   </li>
 *   <li><code>include</code> - Mirrors FreeBSD "include" directory. Classes in this package correspond to
 *     headers where the appropriate function is defined. Because enums and structures in Java are declared
 *     in their own files, sometimes there is a sub-package named after the header file name where these
 *     symbols are declared.
 *     The function wrappers in these classes should be user-friendly (i.e. properly handle exceptions and
 *     provide wrapper data-types over structs and enums to make it easier to call).
 *   </li>
 *   <li><code>res</code> - Utilities to handle error message strings assigned to exceptions.</li>
 * </ul>
 */
package org.ps5jb.sdk;
