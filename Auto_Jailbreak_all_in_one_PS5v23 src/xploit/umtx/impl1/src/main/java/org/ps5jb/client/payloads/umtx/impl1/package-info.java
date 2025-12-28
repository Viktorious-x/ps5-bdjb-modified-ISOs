/**
 * UMTX bug triggering implemented by
 * <a href="https://gist.github.com/flatz/89dfe9ed662076742f770f92e95e12a7">flat_z</a>
 * and adapted to run as a PS5 payload.
 *
 * To create a JAR with this payload, add
 * <pre>-Dxploit.payload=org.ps5jb.client.payloads.umtx.impl1.UmtxExploit</pre>
 * when building the remote JAR.
 *
 * Note this was an earlier attempt and is left for historical reasons.
 * It is not fully functional and may never become.
 */
package org.ps5jb.client.payloads.umtx.impl1;