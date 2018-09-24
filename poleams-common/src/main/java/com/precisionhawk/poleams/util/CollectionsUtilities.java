package com.precisionhawk.poleams.util;

import java.util.List;

/**
 *
 * @author Philip A. Chapman
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
    
    public static <T extends Object> T firstItemIn(T[] array) {
        if (array == null || array.length == 0) {
            return null;
        } else {
            return array[0];
        }
    }
}
