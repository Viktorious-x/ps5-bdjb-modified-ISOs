package org.ps5jb.sdk.core;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.dataview.LongViewPointer;
import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Internal class which implements byte-code manipulation to achieve calling of the
 * functions in native libraries from Java code. This technique has been developed by theflow0.
 */
class CallContext {
    private static long rtld_JVM_NativePath;
    private static long libkernel_sigsetjmp;
    private static long libkernel___Ux86_64_setcontext;

    private native long multiNewArray(long componentType, int[] dimensions);

    static {
        try {
            initSymbols();
            installMultiNewArrayHook();
        } catch (Throwable e) {
            Status.printStackTrace(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Lookup symbols in various native libraries which are used to achieve native execution.
     *
     * @throws SdkSymbolNotFoundException If at least one of the required symbols is not found.
     * @throws SdkRuntimeException If any other error occurred during symbol initialization.
     */
    private static void initSymbols() {
        Library rtld = new Library(-2);
        rtld_JVM_NativePath = rtld.addrOf("JVM_NativePath").addr();

        Library libkernel = new Library(0x2001);
        libkernel___Ux86_64_setcontext = libkernel.addrOf("__Ux86_64_setcontext").addr();
        libkernel_sigsetjmp = libkernel.addrOf("sigsetjmp").addr();
    }

    private static Pointer findMultiNewArrayAddress() {
        Pointer result = null;
        SdkSymbolNotFoundException lastException = null;

        int[] knownHandles = { 0x4A, 0x4B };
        for (int handle : knownHandles) {
            Library libjava = new Library(handle);
            try {
                result = libjava.addrOf("Java_java_lang_reflect_Array_multiNewArray");
                lastException = null;
                break;
            } catch (SdkSymbolNotFoundException e) {
                lastException = e;
            }
        }

        // Nothing found, throw the original symbol not found exception for the last known handle
        if (lastException != null) {
            throw lastException;
        }

        return result;
    }

    /**
     * Install the hook for "multiNewArray" symbol in libjava.
     *
     * @throws SdkSymbolNotFoundException If native "multiNewArray" function could not be found in libjava.
     * @throws SdkRuntimeException If any other error occurred during "multiNewArray" hook installation.
     */
    private static void installMultiNewArrayHook() {
        boolean installed = false;

        Pointer instance = Pointer.valueOf(Pointer.addrOf(new CallContext()));
        Pointer klass = Pointer.valueOf(instance.read8(0x08));
        Pointer methods = Pointer.valueOf(klass.read8(0x170));
        int numMethods = methods.read4();
        for (long i = 0; i < numMethods; i++) {
            Pointer method = Pointer.valueOf(methods.read8(0x08 + i * 8));
            Pointer constMethod = Pointer.valueOf(method.read8(0x08));
            Pointer constants = Pointer.valueOf(constMethod.read8(0x08));
            short nameIndex = constMethod.read2(0x2A);
            Pointer nameSymbol = Pointer.valueOf(constants.read8(0x40 + nameIndex * 8) & -2);
            short nameLength = nameSymbol.read2();
            String name = nameSymbol.inc(0x06).readString(new Integer(nameLength));

            if (name.equals("multiNewArray")) {
                method.write8(0x50, findMultiNewArrayAddress().addr());
                installed = true;
                break;
            }
        }

        if (!installed) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(CallContext.class,"installMultiNewArrayHook"));
        }
    }

    /** Native memory buffer used to jump into the native calls. */
    private Pointer callBuffer;
    /** Shadow Java heap buffer that is used to populate the {@link #callBuffer} data. */
    private long[] callBufferData;
    /**
     * Whether to use data view pointers and if so to what extent. Data view pointers
     * significantly accelerates the native calls at the possible expense of stability.
     * Levels:
     * 0 - don't use data view
     * 1 - create data view in local function scope, don't pass it across method boundary to minimize GC risk.
     * 2 - cache data view in call context which further minimizes overhead but may be subject to invalid memory access by GC.
     */
    private int dataViewUsageLevel = 2;

    private static final int fakeClassOff = 0;
    private static final int fakeKlassOff = fakeClassOff + 0x100;
    private static final int fakeKlassVTableOff = fakeKlassOff + 0x200;
    private static final int fakeClassOopOff = fakeKlassVTableOff + 0x400;

    /** Value needed to call "multiNewArray" function. */
    private int[] dimensions;

    /** When true, this calling context is executing a native call. It's not possible to simultaneously use the same context within two threads. */
    private boolean executing;

    /**
     * Default constructor.
     */
    CallContext() {
        super();
    }

    /**
     * Called by garbage collector when this instance is about to be disposed.
     * Note that finalization is not guaranteed and should not be relied upon
     * to close the resources. Ensure to call {@link #close()} method manually
     * when this call context is no longer needed.
     *
     * @throws Throwable If finalization failed.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Closes the resources associated with this call context.
     * Should be called before the call context is garbage collected.
     */
    void close() {
        if (this.callBuffer != null) {
            this.callBuffer.free();
            this.callBuffer = null;
            this.callBufferData = null;
        }
    }

    private void buildCallContext(long rip, long rdi, long rsi, long rdx, long rcx, long r8, long r9) {
        long[] localCallBuffer;
        if (this.dataViewUsageLevel != 1) {
            localCallBuffer = this.callBufferData;
        } else {
            localCallBuffer = ((LongViewPointer) this.callBuffer).dataView();
        }

        long rbx = localCallBuffer[(fakeKlassOff + 0x08) / 8];
        long rsp = localCallBuffer[(fakeKlassOff + 0x10) / 8];
        long rbp = localCallBuffer[(fakeKlassOff + 0x18) / 8];
        long r12 = localCallBuffer[(fakeKlassOff + 0x20) / 8];
        long r13 = localCallBuffer[(fakeKlassOff + 0x28) / 8];
        long r14 = localCallBuffer[(fakeKlassOff + 0x30) / 8];
        long r15 = localCallBuffer[(fakeKlassOff + 0x38) / 8];

        localCallBuffer[(fakeKlassOff + 0x48) / 8] = rdi;
        localCallBuffer[(fakeKlassOff + 0x50) / 8] = rsi;
        localCallBuffer[(fakeKlassOff + 0x58) / 8] = rdx;
        localCallBuffer[(fakeKlassOff + 0x60) / 8] = rcx;
        localCallBuffer[(fakeKlassOff + 0x68) / 8] = r8;
        localCallBuffer[(fakeKlassOff + 0x70) / 8] = r9;
        localCallBuffer[(fakeKlassOff + 0x80) / 8] = rbx;
        localCallBuffer[(fakeKlassOff + 0x88) / 8] = rbp;
        localCallBuffer[(fakeKlassOff + 0xA0) / 8] = r12;
        localCallBuffer[(fakeKlassOff + 0xA8) / 8] = r13;
        localCallBuffer[(fakeKlassOff + 0xB0) / 8] = r14;
        localCallBuffer[(fakeKlassOff + 0xB8) / 8] = r15;
        localCallBuffer[(fakeKlassOff + 0xE0) / 8] = rip;
        localCallBuffer[(fakeKlassOff + 0xF8) / 8] = rsp;

        // Remove any references to call buffer to prevent GC from considering it.
        localCallBuffer = null;
    }

    /**
     * Call "multiNewArray" hokked method to jump into a native call.
     *
     * @param rip Address to jump into.
     * @param lastInvoke Is this call the last invocation in the chain?
     * @return Return value from the native call execution.
     */
    private long invokeMultiNewArrayHook(long rip, boolean lastInvoke) {
        long[] localCallBuffer;
        if (this.dataViewUsageLevel != 1) {
            localCallBuffer = this.callBufferData;
        } else {
            localCallBuffer = ((LongViewPointer) this.callBuffer).dataView();
        }
        localCallBuffer[fakeKlassOff / 8] = callBuffer.addr() + fakeKlassVTableOff;
        localCallBuffer[(fakeKlassVTableOff + 0x158) / 8] = rip;

        if (dataViewUsageLevel == 0) {
            callBuffer.write(0, localCallBuffer, 0, localCallBuffer.length);
        }

        // Remove any references to call buffer to prevent GC from considering it.
        localCallBuffer = null;

        long ret = this.multiNewArray(callBuffer.addr() + fakeClassOopOff, this.dimensions);

        if (!lastInvoke && dataViewUsageLevel == 0) {
            callBuffer.read(0, this.callBufferData, 0, this.callBufferData.length);
        }

        return ret;
    }

    /**
     * Execute a native function at a given address and return the result of the execution.
     *
     * @param function Address of the function to execute.
     * @param args Arguments to pass to the function. Maximum number of arguments is 6.
     * @return Return value of the function execution.
     * @throws IllegalArgumentException If the number of arguments is greater than 6.
     *   Currently, native execution is limited to a maximum of 6 arguments.
     * @throws SdkRuntimeException If there is already a native execution in progress
     *   or for any other exception that may happen during the execution attempt.
     */
    long execute(Pointer function, long ... args) {
        if (args.length > 6) {
            throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(CallContext.class,"maxCallArguments", new Integer(6), new Integer(args.length)));
        }

        if (this.executing) {
            throw new SdkRuntimeException(ErrorMessages.getClassErrorMessage(CallContext.class,"nestedCall"));
        }
        this.executing = true;

        // On the first call, initialize internal structures
        long[] localCallBuffer;
        if (this.callBuffer == null) {
            final int callBufferSize = 0x800;
            if (dataViewUsageLevel != 0) {
                LongViewPointer longViewCallBuffer = new LongViewPointer(callBufferSize / 8);
                this.callBuffer = longViewCallBuffer;
                localCallBuffer = longViewCallBuffer.dataView();
                if (dataViewUsageLevel != 1) {
                    this.callBufferData = localCallBuffer;
                }
            } else {
                this.callBuffer = Pointer.malloc(callBufferSize);
                localCallBuffer = new long[callBufferSize / 8];
                this.callBufferData = localCallBuffer;
            }

            localCallBuffer[fakeClassOopOff / 8] = this.callBuffer.addr();
            localCallBuffer[(fakeClassOff + 0x98) / 8] = this.callBuffer.addr() + fakeKlassOff;
            localCallBuffer[(fakeKlassVTableOff + 0xD8) / 8] = rtld_JVM_NativePath;

            this.dimensions = new int[] { 1 };
        } else {
            if (this.dataViewUsageLevel != 1) {
                localCallBuffer = this.callBufferData;
            } else {
                localCallBuffer = ((LongViewPointer) this.callBuffer).dataView();
            }
        }

        try {
            localCallBuffer[(fakeKlassOff + 0xC0) / 8] = 0;

            // Remove any references to call buffer to prevent GC from considering it.
            localCallBuffer = null;

            this.invokeMultiNewArrayHook(libkernel_sigsetjmp + 0x23, false);

            this.buildCallContext(function.addr(),
                    args.length > 0 ? args[0] : 0,
                    args.length > 1 ? args[1] : 0,
                    args.length > 2 ? args[2] : 0,
                    args.length > 3 ? args[3] : 0,
                    args.length > 4 ? args[4] : 0,
                    args.length > 5 ? args[5] : 0);
            long ret = this.invokeMultiNewArrayHook(libkernel___Ux86_64_setcontext + 0x39, true);

            if (ret != 0) {
                ret = Pointer.valueOf(ret).read8();
            }
            return ret;
        } catch (SdkRuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        } finally {
            this.executing = false;
        }
    }
}
