package com.precisionhawk.poleams.util;

import com.precisionhawk.ams.util.Comparators;
import com.precisionhawk.poleams.domain.Feeder;
import java.util.Comparator;

/**
 *
 * @author pchapman
 */
public interface PAComparators extends Comparators {
    
    public static Comparator<Feeder>  FEEDERS_COMPARATOR = new Comparator<Feeder>() {
        @Override
        public int compare(Feeder s1, Feeder s2) {
            String n1 = s1 == null ? null : s1.getName();
            String n2 = s2 == null ? null : s2.getName();
            if (n1 == null) {
                if (n2 == null) {
                    return 0;
                } else {
                    return 1;
                }
            } else if (n2 == null) {
                return -1;
            } else {
                return n1.compareTo(n2);
            }
        }
    };
    
}
