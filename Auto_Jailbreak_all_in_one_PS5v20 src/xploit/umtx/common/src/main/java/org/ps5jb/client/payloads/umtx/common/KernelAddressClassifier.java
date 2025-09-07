package org.ps5jb.client.payloads.umtx.common;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;

public class KernelAddressClassifier {
    Map counts = new TreeMap();

    public static KernelAddressClassifier fromBuffer(Pointer buffer) {
        if (buffer.size() == null) {
            throw new IllegalArgumentException("Buffer must have a defined size for kernel address scanning");
        }

        KernelAddressClassifier result = new KernelAddressClassifier();
        for (long i = 0; (i + 8) <= buffer.size().longValue(); i += 8) {
            KernelPointer kptr = new KernelPointer(buffer.read8(i), null, false);
            try {
                KernelPointer.validRange(kptr);

                Long val = new Long(kptr.addr());
                Integer curCount = (Integer) result.counts.get(val);
                if (curCount == null) {
                    curCount = new Integer(0);
                } else {
                    curCount = new Integer(curCount.intValue() + 1);
                }
                result.counts.put(val, curCount);
            } catch (IllegalAccessError e) {
                // Ignore invalid pointers
            }
        }
        return result;
    }

    public Long getMostOccuredHeapAddress(int threshold) {
        Long result = null;
        int maxCount = 0;
        Iterator iter = counts.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry count = (Map.Entry) iter.next();
            int countVal = ((Integer) count.getValue()).intValue();
            if (countVal > maxCount && countVal >= threshold) {
                result = (Long) count.getKey();
            }
        }
        return result;
    }
}
