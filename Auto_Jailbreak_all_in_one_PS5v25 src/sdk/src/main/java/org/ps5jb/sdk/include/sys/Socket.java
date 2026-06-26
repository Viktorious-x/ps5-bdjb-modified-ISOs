package org.ps5jb.sdk.include.sys;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.include.inet.in.ProtocolType;
import org.ps5jb.sdk.include.netinet6.in6.OptionIPv6;
import org.ps5jb.sdk.include.sys.socket.AddressFamilyType;
import org.ps5jb.sdk.include.sys.socket.SocketType;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * This class represents <code>include/sys/socket.h</code> from FreeBSD source.
 */
public class Socket {
    private final LibKernel libKernel;
    private final ErrNo errNo;

    /**
     * Constructor.
     *
     * @param libKernel Instance of the 'libkernel' native library wrapper.
     */
    public Socket(LibKernel libKernel) {
        this.libKernel = libKernel;
        this.errNo = new ErrNo(this.libKernel);
    }

    public int createSocket(AddressFamilyType domain, SocketType socketType, ProtocolType protocol) throws SdkException {
        int ret = libKernel.socket(domain.value(), socketType.value(), protocol.value());
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "createSocket");
        }
        return ret;
    }

    /**
     * Creates an unnamed pair of connected sockets in the specified communications domain,
     * of the specified type, and using the optionally specified protocol.
     * The two sockets are indistinguishable.
     *
     * @param domain Specifies a communications domain within which communication will take place.
     * @param socketType The socket has the indicated type, which specifies the semantics of communication.
     * @param protocol The protocol argument specifies a particular protocol to be used with the socket.
     *   Normally only a single protocol exists to support a particular socket type within a given protocol family.
     * @return Array containing two file descriptors of the created sockets.
     * @throws SdkException If sockets could not be created.
     */
    public int[] createSocketPair(AddressFamilyType domain, SocketType socketType, ProtocolType protocol) throws SdkException {
        Pointer sv = Pointer.calloc(8);
        try {
            int ret = libKernel.socketpair(domain.value(), socketType.value(), protocol.value(), sv);
            if (ret == -1) {
                throw errNo.getLastException(getClass(), "createSocketPair");
            }

            return new int[] { sv.read4(), sv.read4(4) };
        } finally {
            sv.free();
        }
    }

    public void setSocketOptionsIPv6(int socket, OptionIPv6 optionName, Pointer optionValue) throws SdkException {
        long optSize = optionValue.size().longValue();
        int ret = libKernel.setsockopt(socket, ProtocolType.IPPROTO_IPV6.value(), optionName.value(), optionValue, optSize);
        if (ret == -1) {
            throw errNo.getLastException(getClass(), "setSocketOptionsIPv6");
        }
    }

    public Pointer getSocketOptionsIPv6(int socket, OptionIPv6 optionName, Pointer optionValue) throws SdkException {
        Pointer optlen = Pointer.calloc(0x4);
        try {
            optlen.write4(optionValue.size().intValue());

            int ret = libKernel.getsockopt(socket, ProtocolType.IPPROTO_IPV6.value(), optionName.value(), optionValue, optlen);
            if (ret == -1) {
                throw errNo.getLastException(getClass(), "getSocketOptionsIPv6");
            }

            int newLen = optlen.read4();
            return (newLen == optionValue.size().intValue()) ? optionValue : new Pointer(optionValue.addr(), new Long(newLen));
        } finally {
            optlen.free();
        }
    }
}
