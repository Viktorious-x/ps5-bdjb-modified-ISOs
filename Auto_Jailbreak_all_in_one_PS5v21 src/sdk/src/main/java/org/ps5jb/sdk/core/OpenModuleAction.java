package org.ps5jb.sdk.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Use reflection to add access to the internal classes of the JDK from the unnamed module.
 */
public class OpenModuleAction implements PrivilegedExceptionAction {
    private String className;

    /**
     * Constructor.
     *
     * @param className Class name to access.
     */
    private OpenModuleAction(String className) {
        this.className = className;
    }

    /**
     * Execute the access granting logic.
     *
     * @return Always null.
     * @throws ClassNotFoundException Re-raise this exception.
     * @throws NoSuchMethodException Re-raise this exception.
     * @throws IllegalAccessException Re-raise this exception.
     * @throws InvocationTargetException Re-raise this exception.
     */
    @Override
    public Object run() throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {

        Method Class_getModule = Class.class.getDeclaredMethod("getModule", new Class[0]);
        Class_getModule.setAccessible(true);

        Class targetClass = Class.forName(this.className);
        Object targetModule = Class_getModule.invoke(targetClass, new Object[0]);

        Class moduleClass = Class.forName("java.lang.Module");
        Method Module_implAddOpensToAllUnnamed = moduleClass.getDeclaredMethod("implAddOpensToAllUnnamed", new Class[] { String.class });
        Module_implAddOpensToAllUnnamed.setAccessible(true);

        Module_implAddOpensToAllUnnamed.invoke(targetModule, new Object[] { targetClass.getPackage().getName() });

        return null;
    }

    /**
     * Static method to execute this privileged action in an elevated context.
     *
     * @param className Name of the class to which grant the access.
     *   Note that access to the whole package of the given class will be granted.
     * @throws PrivilegedActionException If execution of the action throws the exception.
     */
    public static void execute(String className) throws PrivilegedActionException {
        AccessController.doPrivileged(new OpenModuleAction(className));
    }

    /**
     * Disables warnings emitted by JDK about illegal access to unopened modules.
     */
    private static void disableIllegalAccessWarnings() {
        // Disable warnings about illegal access
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);

            Method putObjectVolatileMethod = unsafeClass.getDeclaredMethod("putObjectVolatile", new Class[] { Object.class, long.class, Object.class });
            Method staticFieldOffsetMethod = unsafeClass.getDeclaredMethod("staticFieldOffset", new Class[] { Field.class });

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long) staticFieldOffsetMethod.invoke(unsafe, new Object[] { loggerField });
            putObjectVolatileMethod.invoke(unsafe, new Object[] { loggerClass, offset, null });
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            // Ignore
        }
    }

    static {
        disableIllegalAccessWarnings();
    }
}
