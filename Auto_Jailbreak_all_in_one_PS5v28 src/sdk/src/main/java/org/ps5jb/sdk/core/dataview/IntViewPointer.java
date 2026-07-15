package org.ps5jb.sdk.core.dataview;

import jdk.internal.misc.Unsafe;
import org.ps5jb.sdk.core.Pointer;

/**
 * Wrapper class over pointer to allow manipulation of the allocated native memory
 * as an array of Java <code>int</code> type.
 *
 * Note that this technique only works with serial GC (used on PS5).
 */
public class IntViewPointer extends Pointer {
    private static final long serialVersionUID = 4698355706655405326L;

    private final int[][] dataViewPtr;

    public IntViewPointer(int length) {
        super(UNSAFE.allocateMemory(length * Unsafe.ARRAY_INT_INDEX_SCALE + Unsafe.ARRAY_INT_BASE_OFFSET) + Unsafe.ARRAY_INT_BASE_OFFSET,
                new Long(length * Unsafe.ARRAY_INT_INDEX_SCALE));

        // Obtain klass value for the array type.
        int[] arrayKlassInstance = new int[0];
        Pointer arrayKlassAddress = Pointer.valueOf(addrOf(arrayKlassInstance));
        long klass = arrayKlassAddress.read8(0x08);

        // Write a class header to the current pointer to simulate it as a Java heap array.
        write8impl(-0x18, 0x79);
        write8impl(-0x10, klass);
        write8impl(-0x08, length);

        dataViewPtr = new int[1][0];
    }

    @Override
    public void free() {
        // Make sure to free the memory starting at the simulated header
        UNSAFE.freeMemory(this.addr - Unsafe.ARRAY_INT_BASE_OFFSET);
    }

    /**
     * Returns a simulated view of the pointer data as a Java native array of ints.
     * Note that this technique seems to work on PS5 but does not work well on
     * local JVM. In the latter case, the garbage collector may attempt to free
     * the data view and crash because the array instance is not an actual Java heap
     * object. For usage with non-PS5 system, the variable in which the return value
     * is stored should be nulled as soon as possible. Example:
     * <pre>
     *     int[] dataView = intViewPointer.dataView();
     *     dataView[3] = 0x12345678;
     *     dataView = null;
     * </pre>
     * Similarly, the return value of this method should not be stored as an attribute
     * of a class on non-pS5 system as the garbage collector will crash while traversing
     * the object graph.
     *
     * @return A simulated Java native array which points to the same native memory location
     *   as the pointer address.
     */
    public int[] dataView() {
        Pointer dataViewPtrAddr = Pointer.valueOf(addrOf(dataViewPtr));
        dataViewPtrAddr.write8(Unsafe.ARRAY_OBJECT_BASE_OFFSET, this.addr - Unsafe.ARRAY_INT_BASE_OFFSET);
        int[] dataView = dataViewPtr[0];
        dataViewPtrAddr.write8(Unsafe.ARRAY_OBJECT_BASE_OFFSET, 0);

        return dataView;
    }
}
