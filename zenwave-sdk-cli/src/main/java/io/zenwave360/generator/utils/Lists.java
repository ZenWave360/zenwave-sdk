package io.zenwave360.generator.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Lists {

    public static <T> List<T> of(Collection<T> list) {
        return new ArrayList<T>(list);
    }

    public static <T> List<T> concat(List<T> list, Collection<T> collection) {
        var newList = of(list);
        newList.addAll(collection);
        return newList;
    }
}
