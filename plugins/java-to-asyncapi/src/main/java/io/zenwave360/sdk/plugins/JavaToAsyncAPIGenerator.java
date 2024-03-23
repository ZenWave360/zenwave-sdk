package io.zenwave360.sdk.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.options.asyncapi.AsyncapiVersionType;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.NamingUtils;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

public class JavaToAsyncAPIGenerator {

    private Logger log = LoggerFactory.getLogger(getClass());

    private ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private ObjectMapper jsonMapper = new ObjectMapper();


    public enum SchemaFormat {
        schema, avro
    }


    @DocumentedOption(description = "Events Producer class to reverse engineer from")
    public Class eventProducerClass;

    @DocumentedOption(description = "Target file")
    public String targetFile = "asyncapi.yml";

    @DocumentedOption(description = "Schema format for messages' payload")
    public SchemaFormat schemaFormat = SchemaFormat.schema;

    @DocumentedOption(description = "Target AsyncAPI version.")
    public AsyncapiVersionType asyncapiVersion = AsyncapiVersionType.v3;

    @DocumentedOption(description = "AsyncAPI file to be merged on top of generated AsyncAPI file")
    public String asyncapiMergeFile;

    @DocumentedOption(description = "Overlay Spec file to apply on top of generated AsyncAPI file")
    public List<String> asyncapiOverlayFiles;

    public JavaToAsyncAPIGenerator withEventProducerClass(Class eventProducerClass) {
        this.eventProducerClass = eventProducerClass;
        return this;
    }

    public JavaToAsyncAPIGenerator withTargetFile(String targetFile) {
        this.targetFile = targetFile;
        return this;
    }

    public JavaToAsyncAPIGenerator withAsyncapiVersion(AsyncapiVersionType asyncapiVersion) {
        this.asyncapiVersion = asyncapiVersion;
        return this;
    }

    public JavaToAsyncAPIGenerator withSchemaFormat(SchemaFormat schemaFormat) {
        this.schemaFormat = schemaFormat;
        return this;
    }

    public JavaToAsyncAPIGenerator withAsyncapiMergeFile(String asyncapiMergeFile) {
        this.asyncapiMergeFile = asyncapiMergeFile;
        return this;
    }

    public JavaToAsyncAPIGenerator withAsyncapiOverlayFiles(List<String> asyncapiOverlayFiles) {
        this.asyncapiOverlayFiles = asyncapiOverlayFiles;
        return this;
    }

    public String generate() throws IOException {
        StringBuilder zdl = new StringBuilder();
        var methods = eventProducerClass.getDeclaredMethods();
        if (methods != null) {
            zdl.append("@aggregate entity Dummy {} \n");
            zdl.append("service EventsProducer for (Dummy) {\n");
            for (Method method : methods) {
                if (method.getParameters().length > 0) {
                    var event = method.getParameters()[0].getType();
                    zdl.append("  " + method.getName() + "() withEvents " + event.getSimpleName() + "\n");
                }
            }
            zdl.append("}\n");
            Set<Class> embeddedClasses = new HashSet<>();
            for (Method method : methods) {
                if (method.getParameters().length > 0) {
                    var event = method.getParameters()[0].getType();
                    embeddedClasses.addAll(generateEventsZdl(zdl, event, method.getName()));
                }
            }
            for (Class embeddedClass : embeddedClasses) {
                if (embeddedClass.isEnum()) {
                    generateEnumZdl(zdl, embeddedClass);
                } else {
                    generateEventsZdl(zdl, embeddedClass, null);
                }
            }
        }

        Map<String, Object> model = new ZDLProcessor()
                .process(new ZDLParser()
                        .withContent(zdl.toString())
                        .parse());

        ZDLToAsyncAPIGenerator generator = new ZDLToAsyncAPIGenerator();
        generator.sourceProperty = "zdl";
        generator.targetFile = targetFile;
        generator.asyncapiVersion = asyncapiVersion;
        generator.schemaFormat = ZDLToAsyncAPIGenerator.SchemaFormat.valueOf(schemaFormat.name());
        generator.asyncapiMergeFile = asyncapiMergeFile;
        generator.asyncapiOverlayFiles = asyncapiOverlayFiles;

        var templates = generator.generate(model);

        if (targetFile != null) {
            new TemplateFileWriter()
                    .withTargetFolder(new File("."))
                    .write(templates);
        }
        return templates.get(templates.size() - 1).getContent();
    }

    protected Set<Class> generateEventsZdl(StringBuilder out, Class<?> entityClass, String operationName) {
        Set<Class> embeddedClasses = new HashSet<>();
        String entityClassName = entityClass.getSimpleName();
        if (operationName != null) {
            out.append("@asyncapi({ operation: " + operationName + ", channel: on" + NamingUtils.camelCase(entityClassName) + " })\n");
        }
        out.append("event " + entityClassName + " {\n");

        Field[] declaredFields = FieldUtils.getAllFields(entityClass);
        for (Field f : declaredFields) {
            String fieldName = f.getName();
            if (f.isSynthetic() || Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }

            Class<?> targetEntityClass = f.getType();
            boolean isCollection = Collection.class.isAssignableFrom(targetEntityClass);
            if (isCollection) {
                targetEntityClass = typeToClass(f.getGenericType());
            }
            if (!isBasicType(targetEntityClass)) {
                embeddedClasses.add(targetEntityClass);
            }

            out.append("  ");
            out.append(fieldName + " " + targetEntityClass.getSimpleName());
            if (isCollection) {
                out.append("[]");
            }
            out.append("\n");
        }

        out.append("\n");
        out.append("}\n\n");

        return embeddedClasses;
    }

    protected void generateEnumZdl(StringBuilder out, Class<?> e) {
        String entityClassName = e.getSimpleName();
        boolean firstField = true;
        out.append("enum " + entityClassName + " {\n");
        Field[] declaredFields = e.getDeclaredFields();
        for (Field f : declaredFields) {
            String fieldName = f.getName();
            if (f.isSynthetic() || !Modifier.isStatic(f.getModifiers()) || !Modifier.isFinal(f.getModifiers())) {
                continue;
            }
            if (firstField) {
                firstField = false;
            } else {
                out.append(",\n");
            }
            out.append("  " + fieldName);
        }

        out.append("\n");
        out.append("}\n\n");
    }

    /**
     * Get the underlying class for a type, or null if the type is a variable type.
     *
     * @param type the type
     * @return the underlying class
     */
    public static Class<?> typeToClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return typeToClass(((ParameterizedType) type).getActualTypeArguments()[0]);
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = typeToClass(componentType);
            if (componentClass != null) {
                return Array.newInstance(componentClass, 0).getClass();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean isBasicType(Class<?> type) {
        type = type.isArray() ? type.getComponentType() : type;
        return type.isPrimitive() || type.getPackage().getName().startsWith("java.");
    }

}
