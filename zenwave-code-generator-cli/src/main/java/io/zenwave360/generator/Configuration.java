package io.zenwave360.generator;

import io.zenwave360.generator.doc.DocumentedOption;
import io.zenwave360.generator.processors.utils.NamingUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Configuration {

    private static final String CONFIG_ID = "configuration";

    @DocumentedOption(description = "OpenAPI file to parse", required = true)
    public String specFile;

    @DocumentedOption(description = "Target folder for generated output", required = false)
    public String targetFolder;
    private List<Class> chain;

    private Map<String, Object> options = new HashMap<>();

    public static Configuration of(String pluginConfigAsString) throws Exception {
        if (pluginConfigAsString != null) {
            if(pluginConfigAsString.contains(".")) {
                return (Configuration) Configuration.class.getClassLoader().loadClass(pluginConfigAsString).getDeclaredConstructor().newInstance();
            }
            String simpleClassName = NamingUtils.asJavaTypeName(pluginConfigAsString);
            var allConfigClasses = new Reflections("io.zenwave360.generator.plugins").getSubTypesOf(Configuration.class);
            Optional<Class<? extends Configuration>> pluginClass = allConfigClasses.stream().filter(c -> matchesClassName(c, pluginConfigAsString, simpleClassName)).findFirst();
            if(pluginClass.isPresent()) {
                return pluginClass.get().getDeclaredConstructor().newInstance();
            }
        }
        return new Configuration();
    }

    private static boolean matchesClassName(Class c, String pluginConfigAsString, String simpleClassName) {
        Field configIdField = FieldUtils.getField(c,"CONFIG_ID");
        if(configIdField != null) {
            try {
                if(configIdField.get(null).equals(pluginConfigAsString)) {
                    return true;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return c.getSimpleName().matches(simpleClassName + "(Configuration){0,1}$");
    }

    public <T extends Configuration> T processOptions() {
        return (T) this;
    }

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

        try {
            if(FieldUtils.getField(this.getClass(), name) != null) {
                FieldUtils.writeField(this, name, value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Configuration withOptions(Map<String, Object> options) {
        options.entrySet().forEach(o -> withOption(o.getKey(), o.getValue()));
        return this;
    }

    public Configuration withChain(Class... processorClasses) {
        if(processorClasses != null) {
            chain = new ArrayList<>(List.of(processorClasses));
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

    public Configuration removeFromChain(Class ...processorClasses) {
        Arrays.stream(processorClasses).forEach(processorClass -> chain.remove(processorClass));
        return this;
    }

    public Configuration replaceInChain(Class current, Class replacement) {
        chain.replaceAll(chainedProcessor -> chainedProcessor.equals(current)? replacement : current);
        return this;
    }

    public Configuration addAfterInChain(Class pivot, Class newProcessor) {
        int index = chain.indexOf(pivot) + 1;
        if(index == -1) {
            chain.add(newProcessor);
        } else {
            chain.add(index, newProcessor);
        }
        return this;
    }

    public Configuration addBeforeInChain(Class pivot, Class newProcessor) {
        int index = chain.indexOf(pivot);
        if(index >= 0) {
            chain.add(index, newProcessor);
        } else {
            chain.add(newProcessor);
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
