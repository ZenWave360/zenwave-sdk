package io.zenwave360.sdk.templating;

import com.github.jknack.handlebars.Options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisitedHelper {
    private Map<String, List<Object>> visited = new HashMap<>();

    public void initVisited(String key, Options options) {
        visited.put(key, new ArrayList<>());
    }

    public boolean isVisited(String key, Options options) {
        Object object = options.param(0);
        boolean register = options.hash("register", false);
        boolean isVisited = visited.containsKey(key) && visited.get(key).contains(object);
        if(register && !isVisited) {
            visited.get(key).add(object);
        }
        return isVisited;
    }
}
