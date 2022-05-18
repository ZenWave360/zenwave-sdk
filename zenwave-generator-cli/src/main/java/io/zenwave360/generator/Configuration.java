package io.zenwave360.generator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Configuration {

    private static final String PRESET_ID = "base";

    private String specFile;

    private String targetFolder = "target/zenwave";
    private List<Class> chain;

    private Map<String, Object> options = new HashMap<>();

    public Configuration withSpecFile(String specFile) {
        this.specFile = specFile;
        this.options.put("specFile", specFile);
        return this;
    }

    public Configuration withTargetFolder(String targetFolder) {
        if(targetFolder != null) {
            this.targetFolder = targetFolder;
            this.options.put("targetFolder", targetFolder);
        }
        return this;
    }

    public Configuration withOption(String name, Object value) {
        String lastPath = name;
        Map<String, Object> nestedTempObject = options;
        String[] paths = name.split("\\.");
        for (int i = 0; i < paths.length; i++) {
            lastPath = paths[i];
            if(!nestedTempObject.containsKey(lastPath)) {
                nestedTempObject.put(paths[i], new HashMap<>());
            }
            if(i < paths.length -1) {
                nestedTempObject = (Map<String, Object>) nestedTempObject.get(lastPath);
            }
        }
        nestedTempObject.put(lastPath, value);
        return this;
    }

    public Configuration withOptions(Map<String, Object> options) {
        options.entrySet().forEach(o -> withOption(o.getKey(), o.getValue()));
        return this;
    }

    public Configuration withChain(Class... processorClasses) {
        if(processorClasses != null) {
            chain = List.of(processorClasses);
        }
        return this;
    }
    public Configuration withChain(String... processorClasses) {
        if (processorClasses != null) {
            chain = Arrays.stream(processorClasses).map(c -> {
                try {
                    return getClass().getClassLoader().loadClass(c);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
        }
        return this;
    }

    public String getSpecFile() {
        return specFile;
    }

    public void setSpecFile(String specFile) {
        this.specFile = specFile;
    }

    public List<Class> getChain() {
        return chain;
    }

    public void setChain(List<Class> chain) {
        this.chain = chain;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public String getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder(String targetFolder) {
        this.targetFolder = targetFolder;
    }
}
