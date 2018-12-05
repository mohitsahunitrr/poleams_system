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
    
    /**
     * Safely attempts to get an item by index.  If the index is out of range, null is returned rather than throwing an OutOfRangeException.
     * @param <T> The type in the list.
     * @param list The list to get the item from.
     * @param index The index to attempt to retrieve.
     * @return The item.  Null if the item at the requested index is null in the list or if the index beyond the high range of the index.
     */
    public static <T extends Object> T getItemSafely(List<T> list, int index) {
        if (list.size() <= index) {
            return null;
        } else {
            return list.get(index);
        }
    }
    
    public static <T extends Object> List<T> copyToMaxSize(List<T> source, List<T> target, int maxLength) {
        if (source != null && (!source.isEmpty())) {
            for (int i = 0; i < source.size() && target.size() < maxLength; i++) {
                target.add(source.get(i));
            }
        }
        return target;
    }
}
