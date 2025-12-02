package org.ps5jb.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.mockito.MockedConstruction;
import org.ps5jb.sdk.core.SdkRuntimeException;

/**
 * Helper class which uses reflection to walk around
 * some of the typing difficulties using the regular
 * {@link org.mockito.Mockito} methods with
 * Personal Basis Profile classes.
 */
public class Mockito extends org.mockito.Mockito {
    public static <T> MockedConstruction<T> mockedConstruction(Class mockedClass) {
        try {
            Method mockConstructionMethod = org.mockito.Mockito.class.getDeclaredMethod("mockConstruction", new Class[]{
                    Class.class
            });
            @SuppressWarnings("unchecked")
            MockedConstruction<T> mc =
                    (MockedConstruction<T>) mockConstructionMethod.invoke(null, new Object[] {
                            mockedClass
                    });
            return mc;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }

    public static <T> MockedConstruction<T> mockedConstruction(Class mockedClass, MockedConstruction.MockInitializer<T> mockInitializer) {
        try {
            Method mockConstructionMethod = org.mockito.Mockito.class.getDeclaredMethod("mockConstruction", new Class[] {
                    Class.class, MockedConstruction.MockInitializer.class
            });
            @SuppressWarnings("unchecked")
            MockedConstruction<T> mc =
                    (MockedConstruction<T>) mockConstructionMethod.invoke(null, new Object[] {
                            mockedClass, mockInitializer
                    });
            return mc;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new SdkRuntimeException(e.getMessage(), e);
        }
    }
}
