package org.ps5jb.sdk.core.kernel;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.UniStd;
import org.ps5jb.sdk.include.inet.in.ProtocolType;
import org.ps5jb.sdk.include.machine.Param;
import org.ps5jb.sdk.include.netinet6.in6.OptionIPv6;
import org.ps5jb.sdk.include.sys.ErrNo;
import org.ps5jb.sdk.include.sys.Socket;
import org.ps5jb.sdk.include.sys.socket.AddressFamilyType;
import org.ps5jb.sdk.include.sys.socket.SocketType;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Kernel read/write accessor which uses IPv6 sockets.
 * Implementation inspired from SpecterDev.
 * Requires an existing kernel accessor on creation.
 */
public class KernelAccessorIPv6 implements KernelAccessor {
    private static final long serialVersionUID = 8937512308105266961L;

    private Pointer master_target_buffer;
    private Pointer slave_buffer;
    private Pointer pipemap_buffer;
    private Pointer krw_buffer;

    private int master_sock;
    private int victim_sock;

    private transient LibKernel libKernel;
    private transient ErrNo errNo;
    private transient Socket socket;

    private int[] pipe_fd;
    private KernelPointer pipe_addr;

    private long kernelBaseAddr;

    /**
     * Constructor for kernel IPv6 accessor.
     *
     * @param ofilesAddress Address of "fdt_ofiles" structure from the "proc" structure of the current process.
     * @param kernelBase Address of the base of kernel text segment.
     * @throws SdkException If an error occurs during accessor creation.
     */
    public KernelAccessorIPv6(KernelPointer ofilesAddress, KernelPointer kernelBase) throws SdkException {
        this.libKernel = new LibKernel();
        this.errNo = new ErrNo(libKernel);
        this.socket = new Socket(this.libKernel);
        this.kernelBaseAddr = kernelBase == null ? 0 : kernelBase.addr();

        final long sock_opt_size = 0x14;
        master_target_buffer = Pointer.calloc(sock_opt_size);
        slave_buffer = Pointer.calloc(sock_opt_size);
        pipemap_buffer = Pointer.calloc(sock_opt_size);
        krw_buffer = Pointer.calloc(Param.PAGE_SIZE);

        master_sock = socket.createSocket(AddressFamilyType.AF_INET6, SocketType.SOCK_DGRAM, ProtocolType.IPPROTO_UDP);
        victim_sock = socket.createSocket(AddressFamilyType.AF_INET6, SocketType.SOCK_DGRAM, ProtocolType.IPPROTO_UDP);
        socket.setSocketOptionsIPv6(master_sock, OptionIPv6.IPV6_PKTINFO, master_target_buffer);
        socket.setSocketOptionsIPv6(victim_sock, OptionIPv6.IPV6_PKTINFO, slave_buffer);

        // Find sockets and get pktopts-based r/w
        KernelPointer master_sock_filedescent_addr = ofilesAddress.inc(master_sock * 0x30L);
        KernelPointer victim_sock_filedescent_addr = ofilesAddress.inc(victim_sock * 0x30L);

        KernelPointer master_sock_file_addr = master_sock_filedescent_addr.pptr(0);
        KernelPointer victim_sock_file_addr = victim_sock_filedescent_addr.pptr(0);

        KernelPointer master_sock_socket_addr = master_sock_file_addr.pptr(0);
        KernelPointer victim_sock_socket_addr = victim_sock_file_addr.pptr(0);

        KernelPointer master_pcb = master_sock_socket_addr.pptr(0x18);
        KernelPointer slave_pcb = victim_sock_socket_addr.pptr(0x18);

        KernelPointer master_pktopts = master_pcb.pptr(0x120);
        KernelPointer slave_pktopts = slave_pcb.pptr(0x120);

        master_pktopts.write8(0x10, slave_pktopts.inc(0x10).addr());

        // Create pipe pair
        final UniStd uniStd = new UniStd(this.libKernel);
        pipe_fd = uniStd.pipe();
        int pipe_read = pipe_fd[0];
        KernelPointer pipe_filedescent = ofilesAddress.inc(pipe_read * 0x30L);
        KernelPointer pipe_file = KernelPointer.valueOf(ipv6_kread8(pipe_filedescent));
        pipe_addr = KernelPointer.valueOf(ipv6_kread8(pipe_file), false);

        // Increase refcounts on socket fds which we corrupt
        inc_socket_refcount(master_sock, ofilesAddress);
        inc_socket_refcount(victim_sock, ofilesAddress);
    }

    public int getMasterSock() {
        return master_sock;
    }

    public int getVictimSock() {
        return victim_sock;
    }

    public KernelPointer getPipeAddress() {
        return pipe_addr;
    }

    public int getPipeReadFd() {
        return pipe_fd[0];
    }

    public int getPipeWriteFd() {
        return pipe_fd[1];
    }

    /**
     * Frees resources in use by this accessor. After calling this method
     * this instance should not be used and kernel access is no longer available.
     */
    public synchronized void free() {
        if (master_target_buffer != null) {
            master_target_buffer.free();
            master_target_buffer = null;
        }
        if (slave_buffer != null) {
            slave_buffer.free();
            slave_buffer = null;
        }
        if (pipemap_buffer != null) {
            pipemap_buffer.free();
            pipemap_buffer = null;
        }
        if (krw_buffer != null) {
            krw_buffer.free();
            krw_buffer = null;
        }

        if (libKernel != null) {
            if (pipe_fd[0] != -1) {
                this.libKernel.close(pipe_fd[0]);
                pipe_fd[0] = -1;
            }
            if (pipe_fd[1] != -1) {
                this.libKernel.close(pipe_fd[1]);
                pipe_fd[1] = -1;
            }

            libKernel.closeLibrary();
            libKernel = null;
        }
    }

    private void write_to_victim(KernelPointer kernelAddress) throws SdkException {
        master_target_buffer.write8(0, kernelAddress.addr());
        master_target_buffer.write8(0x08, 0);
        master_target_buffer.write4(0x10, 0);
        socket.setSocketOptionsIPv6(master_sock, OptionIPv6.IPV6_PKTINFO, master_target_buffer);
    }

    private void ipv6_kread(KernelPointer kernelAddress, Pointer buffer) throws SdkException {
        try {
            write_to_victim(kernelAddress);
            socket.getSocketOptionsIPv6(victim_sock, OptionIPv6.IPV6_PKTINFO, buffer);
        } catch (SdkException e) {
            throw new SdkException(ErrorMessages.getClassErrorMessage(getClass(), "ipv6_kread", kernelAddress, buffer), e);
        }
    }

    private void ipv6_kwrite(KernelPointer kernelAddress, Pointer buffer) throws SdkException {
        try {
            write_to_victim(kernelAddress);
            socket.setSocketOptionsIPv6(victim_sock, OptionIPv6.IPV6_PKTINFO, buffer);
        } catch (SdkException e) {
            throw new SdkException(ErrorMessages.getClassErrorMessage(getClass(), "ipv6_kwrite", kernelAddress, buffer), e);
        }
    }

    private long ipv6_kread8(KernelPointer kernelAddress) throws SdkException {
        ipv6_kread(kernelAddress, slave_buffer);
        return slave_buffer.read8();
    }

    private synchronized int copyout(long src, long length) throws SdkException {
        final long value0 = 0x4000000040000000L;
        final long value1 = 0x4000000000000000L;

        pipemap_buffer.write8(value0);
        pipemap_buffer.write8(0x8, value1);
        pipemap_buffer.write4(0x10, 0);
        ipv6_kwrite(pipe_addr, pipemap_buffer);

        // ipv6 bypass fails on some non-aligned pointers
        // When the read is not aligned to 4-byte boundary -> realign and keep track of offset
        long srcAligned = src & -4;
        int srcOffset = (int) (src - srcAligned);
        long lengthAligned = (src + length + 3) / 4 * 4 - srcAligned;

        if (lengthAligned > Param.PAGE_SIZE) {
            throw new SdkException(ErrorMessages.getClassErrorMessage(getClass(), "copyout.unalignedPageRead", "0x" + Long.toHexString(src)));
        }

        pipemap_buffer.write8(srcAligned);
        pipemap_buffer.write8(0x8, 0);
        pipemap_buffer.write4(0x10, 0);
        ipv6_kwrite(pipe_addr.inc(0x10), pipemap_buffer);

        long readCount = this.libKernel.read(pipe_fd[0], krw_buffer, lengthAligned);
        if (readCount != lengthAligned) {
            if (readCount == -1) {
                throw errNo.getLastException(getClass(), "copyout", new Long(length), "0x" + Long.toHexString(src));
            }
            throw new SdkException(ErrorMessages.getClassErrorMessage(getClass(), "copyout.count", new Long(readCount), new Long(length), "0x" + Long.toHexString(src)));
        }

        return srcOffset;
    }

    private synchronized void copyin(Pointer src, long dest, long length) throws SdkException {
        final long value = 0x4000000000000000L;

        pipemap_buffer.write8(0);
        pipemap_buffer.write8(0x8, value);
        pipemap_buffer.write4(0x10, 0);
        ipv6_kwrite(pipe_addr, pipemap_buffer);

        // Note, write can likely fail the same way as read if dest is not aligned a certain way.
        // But realigning is more difficult since it requires knowing missing bytes.
        // For now, this code just lets this situation fail.

        pipemap_buffer.write8(dest);
        pipemap_buffer.write8(0x8, 0);
        pipemap_buffer.write4(0x10, 0);
        ipv6_kwrite(pipe_addr.inc(0x10), pipemap_buffer);

        long writeCount = this.libKernel.write(pipe_fd[1], src, length);
        if (writeCount != length) {
            if (writeCount == -1) {
                throw errNo.getLastException(getClass(), "copyin", new Long(length), "0x" + Long.toHexString(dest));
            }
            throw new SdkException(ErrorMessages.getClassErrorMessage(getClass(), "copyin.count", new Long(writeCount), new Long(length), "0x" + Long.toHexString(dest)));
        }
    }

    private void inc_socket_refcount(int target_fd, KernelPointer ofilesAddress) {
        KernelPointer filedescent_addr = ofilesAddress.inc(target_fd * 0x30L);
        KernelPointer file_addr = filedescent_addr.pptr(0x00);
        KernelPointer file_data_addr = file_addr.pptr(0x00);
        file_data_addr.write4(0x100);
    }

    @Override
    public byte read1(long kernelAddress) {
        try {
            long readOffset = copyout(kernelAddress, 0x1);
            return krw_buffer.read1(readOffset);
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    @Override
    public short read2(long kernelAddress) {
        try {
            long readOffset = copyout(kernelAddress, 0x2);
            return krw_buffer.read2(readOffset);
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    @Override
    public int read4(long kernelAddress) {
        try {
            long readOffset = copyout(kernelAddress, 0x4);
            return krw_buffer.read4(readOffset);
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    @Override
    public long read8(long kernelAddress) {
        try {
            long readOffset = copyout(kernelAddress, 0x8);
            return krw_buffer.read8(readOffset);
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    /**
     * Read memory into the target buffer, one page at a time.
     *
     * @param kernelAddress Kernel address to read.
     * @param buffer Buffer where the read value will be stored.
     * @param offset Offset in the buffer where to start writing the value.
     * @param length Number of bytes to read.
     * @throws IndexOutOfBoundsException If <code>buffer</code> size starting at <code>offset</code>
     *   is less than <code>length</code>.
     * @throws SdkRuntimeException If reading error occurs.
     */
    public void read(long kernelAddress, byte[] buffer, int offset, int length) {
        if ((buffer.length - offset) < length) {
            throw new IndexOutOfBoundsException();
        }

        int srcOffset = 0;
        int targetOffset = offset;
        while (srcOffset < length) {
            int readSize = (int) Param.PAGE_SIZE - (kernelAddress % 4 != 0 ? 4 : 0);
            if ((srcOffset + readSize) > length) {
                readSize = length - srcOffset;
            }
            try {
                int readOffset = copyout(kernelAddress + srcOffset, readSize);
                krw_buffer.read(readOffset, buffer, targetOffset, readSize);
                srcOffset += readSize;
                targetOffset += readSize;
            } catch (SdkException e) {
                throw new SdkRuntimeException(e);
            }
        }
    }

    @Override
    public void write1(long kernelAddress, byte value) {
        try {
            krw_buffer.write1(value);
            copyin(krw_buffer, kernelAddress, 0x1);
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    @Override
    public void write2(long kernelAddress, short value) {
        try {
            krw_buffer.write2(value);
            copyin(krw_buffer, kernelAddress, 0x2);
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    @Override
    public void write4(long kernelAddress, int value) {
        try {
            krw_buffer.write4(value);
            copyin(krw_buffer, kernelAddress, 0x4);
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    @Override
    public void write8(long kernelAddress, long value) {
        try {
            krw_buffer.write8(value);
            copyin(krw_buffer, kernelAddress, 0x8);
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    /**
     * Write to kernel memory from the source buffer, one page at a time.
     *
     * @param kernelAddress Kernel address to write to.
     * @param buffer Buffer from where to read the data.
     * @param offset Offset in the buffer from which to start reading the data.
     * @param length Number of bytes to write.
     * @throws IndexOutOfBoundsException If <code>buffer</code> size starting at <code>offset</code>
     *   is less than <code>length</code>.
     * @throws SdkRuntimeException If writing error occurs.
     */
    public void write(long kernelAddress, byte[] buffer, int offset, int length) {
        if ((buffer.length - offset) < length) {
            throw new IndexOutOfBoundsException();
        }

        int srcOffset = offset;
        int targetOffset = 0;
        while (targetOffset < length) {
            int writeSize = (int) Param.PAGE_SIZE;
            if ((targetOffset + writeSize) > length) {
                writeSize = length - targetOffset;
            }
            try {
                krw_buffer.write(0, buffer, srcOffset, writeSize);
                copyin(krw_buffer, kernelAddress + targetOffset, writeSize);
                srcOffset += writeSize;
                targetOffset += writeSize;
            } catch (SdkException e) {
                throw new SdkRuntimeException(e);
            }
        }
    }

    @Override
    public long getKernelBase() {
        return kernelBaseAddr;
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        // Make sure to restore libraries when de-serializing
        libKernel = new LibKernel();
        errNo = new ErrNo(libKernel);
        socket = new Socket(this.libKernel);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        if (master_target_buffer == null) {
            // Don't allow to serialize if freed.
            throw new NotActiveException(ErrorMessages.getClassErrorMessage(getClass(), "nonSerializable"));
        }

        stream.defaultWriteObject();
    }
}
