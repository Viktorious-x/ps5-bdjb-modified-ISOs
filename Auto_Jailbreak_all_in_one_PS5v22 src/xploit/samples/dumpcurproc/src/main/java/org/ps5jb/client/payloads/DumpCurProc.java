package org.ps5jb.client.payloads;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.ps5jb.client.utils.init.KernelReadWriteUnavailableException;
import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.client.utils.memory.MemoryDumper;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.filedesc.FileDesc;
import org.ps5jb.sdk.include.sys.jail.Prison;
import org.ps5jb.sdk.include.sys.mman.ProtectionFlag;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.include.sys.proc.Thread;
import org.ps5jb.sdk.include.sys.ucred.UCred;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Paylod for dumping memory of proc kernel structure
 * for the current process.
 */
public class DumpCurProc implements Runnable {
    private static final long PROC_SIZE = 0x1400;

    private LibKernel libKernel;

    private int libJvmKey;

    /**
     * Prints {@link #PROC_SIZE} bytes of current process kernel proc structure.
     * The size is approximate and may not represent the real size of the structure.
     * However, even with the overflow, it was observed that another proc
     * structure is adjacent in memory and so no fault will occur.
     */
    @Override
    public void run() {
        libKernel = new LibKernel();
        try {
            SdkInit sdk = SdkInit.init(true, false);

            Status.println("Current Process:");
            Process curProc = new Process(KernelPointer.valueOf(sdk.curProcAddress));
            printProc(curProc);
            printJavaThreadList();
            printModuleList();
        } catch (KernelReadWriteUnavailableException e) {
            Status.println("Kernel R/W is not available, aborting");
        } catch (SdkSoftwareVersionUnsupportedException e) {
            Status.println("Unsupported firmware version: " + e.getMessage());
        } catch (Throwable e) {
            Status.printStackTrace("Unexpected error", e);
        } finally {
            libKernel.closeLibrary();
        }
    }

    private void printProc(Process process) {
        UCred ucred = process.getUserCredentials();
        Prison prison = ucred.getPrison();

        Status.println("Process " + process.getPointer() + ":");
        MemoryDumper.dump(process.getPointer(), PROC_SIZE, true);
        Status.println("Ucred " + ucred.getPointer() + ":");
        MemoryDumper.dump(ucred.getPointer(), UCred.SIZE, true);
        Status.println("Prison " + prison.getPointer() + ":");
        MemoryDumper.dump(prison.getPointer(), Prison.SIZE, true);
        Status.println("Fd " + process.getOpenFiles().getPointer() + ":");
        MemoryDumper.dump(process.getOpenFiles().getPointer(), FileDesc.OFFSET_FD_JDIR + 0x20, true);

        Status.println("Process Data:");
        Status.println("  PID: " + process.getPid());
        Status.println("  Title ID: " + process.getTitleId());
        Status.println("  Content ID: " + process.getContentId());
        Status.println("  GPU VM ID: " + process.getVmSpace().getGpuVmId());
        Status.println("  Command: " + process.getName());
        Status.println("  Arguments: " + process.getArguments());
        Status.println("  Open Files:");
        Status.println("    Cur Vnode: " + process.getOpenFiles().getCurDir());
        Status.println("    Root Vnode: " + process.getOpenFiles().getRootDir());
        Status.println("    Jail Vnode: " + process.getOpenFiles().getJailDir());
        Status.println("  User Credentials:");
        Status.println("    Uid: " + ucred.getUid());
        for (int i = 0; i < ucred.getNgroups(); ++i) {
            Status.println("    Gid" + (i == 0 ? "" : " " + (i + 1)) + ": " + ucred.getGroups().read4(i * 4L));
        }
        Status.println("  Prison:");
        Status.println("    Ref Count: " + prison.getRef());
        Status.println("    Uref Count: " + prison.getUref());
        Status.println("    Root Vnode: " + prison.getRoot());
        if (prison.getIpv4Count() > 0) {
            KernelPointer ipv4 = prison.getIpv4();
            if (!KernelPointer.NULL.equals(ipv4)) {
                Status.println("    IPv4: 0x" + Integer.toHexString(ipv4.read4()));
            }
        }
        Status.println("    Name: " + prison.getName());
        Status.println("    Hostname: " + prison.getHostname());
        Status.println("    Domain Name: " + prison.getDomainName());
        Status.println("    Host UUID: " + prison.getHostUuid());
        Status.println("    OS Release Date: " + prison.getOsRelDate());
        Status.println("    OS Release: " + prison.getOsRelease());
        Process next = process.getNextProcess();
        if (next != null) {
            Status.println("  Next: " + printProcess(next));
        } else {
            Status.println("  Last in allproc");
        }
        Process prev = process.getPreviousProcess();
        if (prev != null) {
            Status.println("  Previous: " + printProcess(prev));
        } else {
            Status.println("  First in allproc");
        }
        Process groupEntry = process.getNextProcessInGroup();
        if (groupEntry != null) {
            Status.println("  Next in group:");
            while (groupEntry != null) {
                Status.println("    " + printProcess(groupEntry));
                groupEntry = groupEntry.getNextProcessInGroup();
            }
        }
        Process parent = process.getParentProcess();
        if (parent != null) {
            String indent = "  ";
            Status.println(indent + "Parent(s):");
            while (parent != null) {
                indent += "  ";
                Status.println(indent + printProcess(parent));
                parent = parent.getParentProcess();
            }
        }
        Process sibling = process.getNextSiblingProcess();
        if (sibling != null) {
            Status.println("  Next sibling(s):");
            while (sibling != null) {
                Status.println("    " + printProcess(sibling));
                sibling = sibling.getNextSiblingProcess();
            }
        }
        sibling = process.getPreviousSiblingProcess();
        if (sibling != null) {
            Status.println("  Prev sibling(s):");
            while (sibling != null) {
                Status.println("    " + printProcess(sibling));
                sibling = sibling.getPreviousSiblingProcess();
            }
        }
        Process child = process.getNextChildProcess();
        if (child != null) {
            Status.println("  Children:");
            while (child != null) {
                Status.println("    " + printProcess(child));
                child = child.getNextSiblingProcess();
            }
        }
        Process reaper = process.getReaperProcess();
        if (reaper != null) {
            Status.println("  Reaper: " + printProcess(reaper));
        }
        Process reapEntry = process.getNextReapListSiblingProcess();
        if (reapEntry != null) {
            Status.println("  Next reap sibling(s) of the reaper:");
            while (reapEntry != null) {
                Status.println("    " + printProcess(reapEntry));
                reapEntry = reapEntry.getNextReapListSiblingProcess();
            }
        }
        reapEntry = process.getPreviousReapListSiblingProcess();
        if (reapEntry != null) {
            Status.println("  Prev reap sibling(s) of the reaper:");
            while (reapEntry != null) {
                Status.println("    " + printProcess(reapEntry));
                reapEntry = reapEntry.getPreviousReapListSiblingProcess();
            }
        }
        Process reapChild = process.getNextProcessInReapList();
        if (reapChild != null) {
            Status.println("  Reap list (of this process):");
            while (reapChild != null) {
                Status.println("    " + printProcess(reapChild));
                reapChild = reapChild.getNextReapListSiblingProcess();
            }
        }

        int curTid = libKernel.pthread_getthreadid_np();
        printNativeThreadList(process, curTid);
    }

    private void printJavaThreadList() throws NoSuchFieldException, IllegalAccessException {
        Status.println("Java Threads: ");

        ThreadGroup tg = java.lang.Thread.currentThread().getThreadGroup();
        while (tg.getParent() != null) {
            tg = tg.getParent();
        }
        printThreadGroup(tg, "  ");
    }

    private void printThreadGroup(ThreadGroup tg, String indent) throws NoSuchFieldException, IllegalAccessException {
        int threadGroupCount;
        ThreadGroup[] threadGroups = new ThreadGroup[tg.activeGroupCount()];
        while ((threadGroupCount = tg.enumerate(threadGroups, false)) == threadGroups.length && threadGroupCount != 0) {
            threadGroups = new ThreadGroup[threadGroups.length * 2];
        }

        for (int i = 0; i < threadGroupCount; ++i) {
            ThreadGroup threadGroup = threadGroups[i];
            Status.println(indent + "[G] " + threadGroup.getName() + ":");
            printThreadGroup(threadGroup, indent + "  ");
        }

        int threadCount;
        java.lang.Thread[] threads = new java.lang.Thread[tg.activeCount()];
        while ((threadCount = tg.enumerate(threads, false)) == threads.length && threadCount != 0) {
            threads = new java.lang.Thread[threads.length * 2];
        }

        for (int i = 0; i < threadCount; ++i) {
            java.lang.Thread thread = threads[i];
            Field targetField = java.lang.Thread.class.getDeclaredField("target");
            targetField.setAccessible(true);
            Runnable target = (Runnable) targetField.get(thread);
            Class clazz = target != null ? target.getClass() : thread.getClass();

            Status.println(indent + thread.getName() + " [" + clazz.getName() + "]" +
                    (thread == java.lang.Thread.currentThread() ? " (this thread)" : ""));
        }
    }

    private void printNativeThreadList(Process proc, int curTid) {
        Thread td = proc.getFirstThread();

        Status.println("Native Threads: ");
        while (td != null) {
            Status.println("  " + printThread(td) + (curTid == td.getTid() ? " (this thread)" : ""));
            td = td.getNextThread();
        }
    }

    private String printThread(Thread td) {
        String name = td.getName();
        int tid = td.getTid();
        return tid + (name.length() == 0 ? "" : " " + name);
    }

    private String printProcess(Process proc) {
        return proc.getName() + " - " + proc.getTitleId() + " - " + proc.getPathName() + " (" + proc.getPid() + ")";
    }
    
    private void printModuleList() {
        final int maxModuleCount = 0x100;
        Pointer moduleInfo = Pointer.calloc(0x160);
        Pointer moduleList = Pointer.calloc(4L * maxModuleCount);
        Pointer moduleCountPtr = Pointer.calloc(8);
        try {
            int res;
            if ((res = libKernel.sceKernelGetModuleList(moduleList, maxModuleCount, moduleCountPtr)) == 0) {
                final long moduleCount = moduleCountPtr.read8();
                final Integer maxName = new Integer(256);
                Status.println("Modules (" + moduleCount + "):");
                for (long i = 0; i < moduleCount; ++ i) {
                    int moduleHandle = moduleList.read4(i * 4);
                    Status.println("  Handle: 0x" + Integer.toHexString(moduleHandle));

                    moduleInfo.write8(0x160);
                    if ((res = libKernel.sceKernelGetModuleInfo(moduleHandle, moduleInfo)) == 0) {
                        int segmentCount = moduleInfo.read4(0x148);
                        String name = moduleInfo.readString(0x08, maxName, Charset.defaultCharset().name());
                        Status.println("  Name: " + name);
                        Status.println("  Segments (" + segmentCount + "):");
                        for (int j = 0; j < segmentCount; ++j) {
                            long start = moduleInfo.read8(0x108 + 0x10L * j);
                            long size = moduleInfo.read4(0x108 + 0x10L * j + 0x08);
                            Status.println("    Start: 0x" + Long.toHexString(start));
                            Status.println("    End: 0x" + Long.toHexString(start + size) + " (size: 0x" + Long.toHexString(size) + ")");
                            Status.println("    Protection: " + Arrays.asList(ProtectionFlag.valueOf(moduleInfo.read4(0x108 + 0x10L * j + 0x0C))));
                        }
                    } else {
                        Status.println("    [ERROR] Unable to obtain the module info: 0x" + Integer.toHexString(res));
                    }
                }
            } else {
                Status.println("Unable to obtain the process module list: 0x" + Integer.toHexString(res));
            }
        } finally {
            moduleCountPtr.free();
            moduleList.free();
            moduleInfo.free();
        }
    }
}
