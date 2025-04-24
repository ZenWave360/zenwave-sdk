package io.zenwave360.sdk.plugins;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.templating.TemplateOutput;

public class JavaToJDLGenerator implements Generator {

    private Logger log = LoggerFactory.getLogger(getClass());

    public enum PersistenceType {
        JPA, MONGODB
    }

    @DocumentedOption(description = "Persistence type to search for annotations for")
    public PersistenceType persistenceType = PersistenceType.MONGODB;

    @DocumentedOption(description = "Package name to scan for entities")
    public String packageName;

    public JavaToJDLGenerator withPersistenceType(PersistenceType persistenceType) {
        this.persistenceType = persistenceType;
        return this;
    }

    public JavaToJDLGenerator withPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String generate() {
        return generate(null).singleFiles.get(0).getContent();
    }

    @Override
    public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        Class entityAnnotationClass = persistenceType == PersistenceType.JPA ? Entity.class : Document.class;
        Set<Class> entitySubClasses = getAnnotatedEntities(entityAnnotationClass);

        // scan enum types in entities field type
        Set<Class<?>> enums = new LinkedHashSet<>();
        for (Class<?> e : entitySubClasses) {
            Field[] declaredFields = e.getFields();
            for (Field f : declaredFields) {
                Class<?> fieldType = f.getType();
                if (fieldType.isEnum()) {
                    if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    enums.add(fieldType);
                }
            }
        }

        // ** generate **
        StringBuilder jdl = new StringBuilder();
        for (Class<?> e : enums) {
            generateEnum2Jdl(jdl, e);
        }

        // ** generate **
        generateClasses(entitySubClasses, jdl);

        var generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.add(new TemplateOutput("", jdl.toString(), "text/jdl"));
        return generatedProjectFiles;
    }

    protected void generateClasses(Collection<Class> entityClasses, StringBuilder jdl) {
        Set<Class> embeddedClasses = new HashSet<>();
        StringBuilder relationShips = new StringBuilder();
        for (Class<?> e : entityClasses) {
            if (persistenceType == PersistenceType.JPA) {
                generateJPA2Jdl(jdl, relationShips, e);
            } else {
                embeddedClasses.addAll(generateMongodb2Jdl(jdl, e));
                if(!embeddedClasses.isEmpty()) {
                    generateClasses(embeddedClasses, jdl); // may be repeated classes
                }
            }
        }
        jdl.append(relationShips);
    }

    protected Set<Class> getAnnotatedEntities(Class<? extends Annotation> entityAnnotationClass) {
        Reflections reflections = new Reflections(packageName);

        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(entityAnnotationClass);
        Set<Class> entitySubClasses = new HashSet<>();
        entitySubClasses.addAll(entityClasses);
        for (Class e : entityClasses) {
            Set<Class> subClasses = reflections.getSubTypesOf(e);
            if (subClasses != null && !subClasses.isEmpty()) {
                entitySubClasses.addAll(subClasses);
            }
        }

        log.info("Found @Entity classes:" + entityClasses.size() + " : " + entityClasses);
        if (entitySubClasses.size() > entityClasses.size()) {
            log.info("Found sub-classes of @Entity classes:" + entitySubClasses.size() + " : " + entitySubClasses);
        }
        return entitySubClasses;
    }

    protected void generateJPA2Jdl(StringBuilder out, StringBuilder relationShips, Class<?> e) {
        String entityClassName = e.getSimpleName();
        out.append("entity " + entityClassName + " {\n");

        Field[] declaredFields = FieldUtils.getAllFields(e);
        for (Field f : declaredFields) {
            String fieldName = f.getName();
            Type fieldType = f.getType();
            if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            // Annotation[] fieldAnnotations = f.getDeclaredAnnotations();

            Id idAnnotation = f.getDeclaredAnnotation(Id.class);
            jakarta.persistence.Id jpaIdAnnotation = f.getDeclaredAnnotation(jakarta.persistence.Id.class);
            Version versionAnnotation = f.getDeclaredAnnotation(Version.class);
            jakarta.persistence.Version jpaVersionAnnotation = f.getDeclaredAnnotation(jakarta.persistence.Version.class);
            if(idAnnotation != null || versionAnnotation != null || jpaIdAnnotation != null || jpaVersionAnnotation != null) {
                continue;
            }

            if (f.getDeclaredAnnotation(Transient.class) != null) {
                continue;
            }

            String relationType = null;
            Class<?> targetEntityClass = null;
            boolean fromMany = false;
            boolean toMany = false;
            String mappedBy = "";
            OneToMany oneToManyAnnotation = f.getDeclaredAnnotation(OneToMany.class);
            if (oneToManyAnnotation != null) {
                relationType = "OneToMany";
                targetEntityClass = oneToManyAnnotation.targetEntity();
                fromMany = false;
                toMany = true;
                mappedBy = oneToManyAnnotation.mappedBy();
            }
            OneToOne oneToOneAnnotation = f.getDeclaredAnnotation(OneToOne.class);
            if (oneToOneAnnotation != null) {
                relationType = "OneToOne";
                targetEntityClass = oneToOneAnnotation.targetEntity();
                fromMany = false;
                toMany = false;
                mappedBy = oneToOneAnnotation.mappedBy();
            }
            ManyToMany manyToManyAnnotation = f.getDeclaredAnnotation(ManyToMany.class);
            if (manyToManyAnnotation != null) {
                relationType = "ManyToMany";
                targetEntityClass = manyToManyAnnotation.targetEntity();
                fromMany = true;
                toMany = true;
                mappedBy = manyToManyAnnotation.mappedBy();
            }
            ManyToOne manyToOneAnnotation = f.getDeclaredAnnotation(ManyToOne.class);
            if (manyToOneAnnotation != null) {
                relationType = "ManyToOne";
                targetEntityClass = manyToOneAnnotation.targetEntity();
                fromMany = true;
                toMany = false;
            }

            if (relationType != null) {
                // relationship
                relationShips.append("relationship " + relationType + " {\n");

                if (targetEntityClass == void.class || targetEntityClass == null) {
                    targetEntityClass = typeToClass(fieldType);
                }

                if (toMany && targetEntityClass != null) {
                    if (Collection.class.isAssignableFrom(targetEntityClass)) {
                        Class<?> compType = targetEntityClass.getComponentType();
                        if (compType != null) {
                            targetEntityClass = compType;
                        } else {
                            Type fieldGenericType = f.getGenericType();
                            if (fieldGenericType instanceof ParameterizedType) {
                                ParameterizedType pt = (ParameterizedType) fieldGenericType;
                                targetEntityClass = typeToClass(pt.getActualTypeArguments()[0]);
                            }
                        }
                    }
                }

                String targetEntityClassName = targetEntityClass != null ? targetEntityClass.getSimpleName() : "";

                if (fromMany && toMany
                // fieldName.equals("")
                ) {
                    log.info("ManyToMany .. mappedBy ??");
                }
                relationShips.append("  " + entityClassName + "{" + fieldName);
                if (mappedBy != null && !"".equals(mappedBy)) {
                    relationShips.append("(" + mappedBy + ")");
                }
                relationShips.append("} to " + targetEntityClassName + "\n");

                relationShips.append("}\n\n");
            } else {
                // simple field
                out.append("  " + fieldName + " " + f.getType().getSimpleName());
                out.append("\n");
            }

        }

        out.append("\n");
        out.append("}\n\n");
    }

    protected Set<Class> generateMongodb2Jdl(StringBuilder out, Class<?> entityClass) {
        Set<Class> embeddedClasses = new HashSet<>();
        String entityClassName = entityClass.getSimpleName();
        if(entityClass.getAnnotation(Document.class) == null) {
            out.append("@embedded\n");
        }
        out.append("entity " + entityClassName + " {\n");

        Field[] declaredFields = FieldUtils.getAllFields(entityClass);
        for (Field f : declaredFields) {
            String fieldName = f.getName();
            if (f.isSynthetic() || Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }

            Id idAnnotation = f.getDeclaredAnnotation(Id.class);
            Version versionAnnotation = f.getDeclaredAnnotation(Version.class);
            if(idAnnotation != null || versionAnnotation != null) {
                continue;
            }

            DBRef dbRefAnnotation = f.getDeclaredAnnotation(DBRef.class);
            DocumentedOption documentedOptionAnnotation = f.getDeclaredAnnotation(DocumentedOption.class);
            Class<?> targetEntityClass = f.getType();
            boolean isCollection = Collection.class.isAssignableFrom(targetEntityClass);
            if (isCollection) {
                targetEntityClass = typeToClass(f.getGenericType());
            }
            if(targetEntityClass.getAnnotation(Document.class) == null &&
                    targetEntityClass.getPackage() != null && targetEntityClass.getPackage().getName().startsWith(packageName))
            {
                embeddedClasses.add(targetEntityClass);
            }

            out.append("  ");
            if(dbRefAnnotation != null) {
                out.append("@DBRef ");
            } else if (documentedOptionAnnotation != null) {
                out.append("@DocumentedOption ");
            }
            out.append(fieldName + " " + targetEntityClass.getSimpleName());
            if(isCollection) {
                out.append("[]");
            }
            out.append("\n");
        }

        out.append("\n");
        out.append("}\n\n");

        return embeddedClasses;
    }

    protected void generateEnum2Jdl(StringBuilder out, Class<?> e) {
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

    protected String asJdlType(Class type) {
        if(Date.class.isAssignableFrom(type)) {
            return "Instant";
        }
        if(type.isPrimitive()) {
            if(type == int.class) {
                return "Integer";
            }
            return StringUtils.capitalize(type.getSimpleName());
        }
        return type.getSimpleName();
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
}
