package io.zenwave360.sdk.utils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class MultiValueMap<T extends Serializable> extends LinkedHashMap<String, Set<T>> {

    public void add(String key, T value) {
        Set<T> values = this.computeIfAbsent(key, k -> new LinkedHashSet<>());
        values.add(value);
    }

}
