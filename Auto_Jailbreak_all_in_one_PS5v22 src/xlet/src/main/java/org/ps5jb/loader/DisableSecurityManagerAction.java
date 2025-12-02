package org.ps5jb.loader;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Privileged action to disable the system security manager.
 */
public class DisableSecurityManagerAction implements PrivilegedExceptionAction {
    /**
     * Default constructor
     */
    private DisableSecurityManagerAction() {
    }

    /**
     * Runs the privileged action to disable the system security manager.
     *
     * @return Value of {@link System#getSecurityManager()}
     */
    @Override
    public Object run() {
        System.setSecurityManager(null);
        return System.getSecurityManager();
    }

    /**
     * Static method to execute this privileged action in an elevated context.
     *
     * @return The security manager instance after action execution.
     *   The successful invocation should return null since security manager would be disabled.
     * @throws PrivilegedActionException If execution of the action throws the exception.
     */
    public static SecurityManager execute() throws PrivilegedActionException {
        return (SecurityManager) AccessController.doPrivileged(new DisableSecurityManagerAction());
    }
}
