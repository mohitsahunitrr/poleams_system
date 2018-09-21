package com.precisionhawk.poleams.util;

import java.util.List;

/**
 *
 * @author pchapman
 */
public class CollectionsUtilities {
    
    private CollectionsUtilities() {}
    
    public static <T> T firstItemIn(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }
    
}
