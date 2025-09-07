package org.ps5jb.client.payloads.umtx.impl1;

import org.ps5jb.sdk.core.SdkException;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.include.PThread;
import org.ps5jb.sdk.include.PThreadNp;
import org.ps5jb.sdk.include.sys.pthreadtypes.PThreadType;
import org.ps5jb.sdk.lib.LibKernel;

public class CommonJob implements Runnable {
    LibKernel libKernel;
    PThread pthread;
    PThreadNp pthreadNp;
    String jobName;

    protected CommonJob() {
        this.libKernel = new LibKernel();
        this.pthread = new PThread(libKernel);
        this.pthreadNp = new PThreadNp(libKernel);
    }

    public void run() {
        try {
            prepare();
            work();
            postprocess();
        } catch (SdkException e) {
            throw new SdkRuntimeException(e);
        }
    }

    protected void prepare() throws SdkException {
        PThreadType pthread = this.pthread.self();
        this.pthreadNp.rename(pthread, jobName);
    }

    protected void work() throws SdkException {
        thread_yield();
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    protected void thread_yield() {
        Thread.yield();
    }

    protected void postprocess() {
        this.libKernel.closeLibrary();
    }

    public String getJobName() {
        return jobName;
    }
}
