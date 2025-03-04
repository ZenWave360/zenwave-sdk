package io.zenwave360.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.utils.NamingUtils;

public class Plugin {

    @DocumentedOption(description = "API Spec file to parse", required = true)
    public String apiFile;

    @DocumentedOption(description = "API Spec files to parse", required = true)
    public List<String> apiFiles;


    @DocumentedOption(description = "ZDL file to parse", required = true)
    public String zdlFile;

    @DocumentedOption(description = "ZDL files to parse")
    public List<String> zdlFiles;

    @DocumentedOption(description = "Target folder for generated output", required = false)
    public String targetFolder;
    private List<Class> chain;

    private boolean forceOverwrite = false;

    private Map<String, Object> options = new HashMap<>();

    private ClassLoader projectClassLoader;

    public static Plugin of(String pluginConfigAsString) throws Exception {
        if (pluginConfigAsString != null) {
            if (pluginConfigAsString.contains(".")) {
                return (Plugin) Plugin.class.getClassLoader().loadClass(pluginConfigAsString).getDeclaredConstructor().newInstance();
            }
            String simpleClassName = NamingUtils.asJavaTypeName(pluginConfigAsString);
            var allConfigClasses = new Reflections("io.zenwave360.sdk.plugins").getSubTypesOf(Plugin.class);
            Optional<Class<? extends Plugin>> pluginClass = allConfigClasses.stream().filter(c -> matchesClassName(c, pluginConfigAsString, simpleClassName)).findFirst();
            if (pluginClass.isPresent()) {
                return pluginClass.get().getDeclaredConstructor().newInstance();
            }
        }
        return new Plugin();
    }

    private static boolean matchesClassName(Class c, String pluginConfigAsString, String simpleClassName) {
        DocumentedPlugin documentedPlugin = (DocumentedPlugin) c.getAnnotation(DocumentedPlugin.class);
        if (documentedPlugin != null && pluginConfigAsString.contentEquals(documentedPlugin.shortCode())) {
            return true;
        }
        return c.getSimpleName().matches(simpleClassName + "(Configuration){0,1}$");
    }

    public <T extends Plugin> T processOptions() {
        return (T) this;
    }

    public Plugin withApiFile(String apiFile) {
        this.apiFile = apiFile != null? apiFile.replaceAll("\\\\", "/") : apiFile;
        this.options.put("apiFile", this.apiFile);
        return this;
    }

    public Plugin withApiFiles(List<String> apiFiles) {
        if(apiFiles == null) {
            return this;
        }
        this.apiFiles = apiFiles.stream().map(apiFile -> apiFile != null? apiFile.replaceAll("\\\\", "/") : apiFile).toList();
        this.options.put("zdlFiles", this.apiFiles);
        return this;
    }

    public Plugin withZdlFile(String zdlFile) {
        this.zdlFile = zdlFile != null? zdlFile.replaceAll("\\\\", "/") : zdlFile;
        this.options.put("zdlFile", this.zdlFile);
        return this;
    }

    public Plugin withZdlFiles(List<String> zdlFiles) {
        if(zdlFiles == null) {
            return this;
        }
        this.zdlFiles = zdlFiles.stream().map(zdlFile -> zdlFile != null? zdlFile.replaceAll("\\\\", "/") : zdlFile).toList();
        this.options.put("zdlFiles", this.zdlFiles);
        return this;
    }

    public Plugin withTargetFolder(String targetFolder) {
        if (targetFolder != null) {
            this.targetFolder = targetFolder;
            this.options.put("targetFolder", targetFolder);
        }
        return this;
    }

    public Plugin withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    public Plugin withForceOverwrite(boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
        return this;
    }

    public Plugin withOption(String name, Object value) {
        var field = FieldUtils.getField(this.getClass(), name);

        String lastPath = name;
        Map<String, Object> nestedTempObject = options;
        String[] paths = name.split("\\.");
        for (int i = 0; i < paths.length; i++) {
            lastPath = paths[i];
            if (!nestedTempObject.containsKey(lastPath)) {
                nestedTempObject.put(paths[i], new HashMap<>());
            }
            if (i < paths.length - 1) {
                if (nestedTempObject.get(lastPath) instanceof Map) {
                    nestedTempObject = (Map<String, Object>) nestedTempObject.get(lastPath);
                } else {
                    nestedTempObject = options;
                    lastPath = name;
                    break;
                }
            }
        }
        if(value instanceof String && ((String) value).contains(",")) {
            value = Arrays.stream(((String) value).split(",")).map(String::trim).toList();
        }
        if (value instanceof String && field != null && List.class.isAssignableFrom(field.getType())) {
            value = List.of(value);
        }
        nestedTempObject.put(lastPath, value);

        try {
            if(field != null) {
                FieldUtils.writeField(this, name, value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public Plugin withOptions(Map<String, Object> options) {
        options.entrySet().forEach(o -> withOption(o.getKey(), o.getValue()));
        return this;
    }

    public Plugin withChain(Class... processorClasses) {
        if (processorClasses != null) {
            chain = new ArrayList<>(List.of(processorClasses));
        }
        return this;
    }

    public Plugin removeFromChain(Class... processorClasses) {
        Arrays.stream(processorClasses).forEach(processorClass -> chain.remove(processorClass));
        return this;
    }

    public Plugin replaceInChain(Class current, Class replacement) {
        chain.replaceAll(chainedProcessor -> chainedProcessor.equals(current) ? replacement : chainedProcessor);
        return this;
    }

    public Plugin addAfterInChain(Class pivot, Class newProcessor) {
        int index = chain.indexOf(pivot) + 1;
        if (index == -1) {
            chain.add(newProcessor);
        } else {
            chain.add(index, newProcessor);
        }
        return this;
    }

    public Plugin addBeforeInChain(Class pivot, Class newProcessor) {
        int index = chain.indexOf(pivot);
        if (index >= 0) {
            chain.add(index, newProcessor);
        } else {
            chain.add(newProcessor);
        }
        return this;
    }

    public boolean hasOption(String name) {
        return options.containsKey(name);
    }

    public boolean hasOption(String name, Object value) {
        if(name == null || options.get(name) == null || value == null) {
            return false;
        }
        return options.get(name).toString().equals(value.toString());
    }

    @DocumentedOption(description = "Spec file to parse (@deprecated user apiFile or zdlFile)", required = true)
    public String getSpecFile() {
        return apiFile;
    }

    public void setSpecFile(String specFile) {
        this.apiFile = specFile;
    }

    public String getApiFile() {
        return apiFile;
    }

    public void setApiFile(String apiFile) {
        this.apiFile = apiFile;
    }

    public String getZdlFile() {
        return zdlFile;
    }

    public void setZdlFile(String zdlFile) {
        this.zdlFile = zdlFile;
    }

    public List<Class> getChain() {
        return chain;
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

    public ClassLoader getProjectClassLoader() {
        return this.projectClassLoader;
    }
}
