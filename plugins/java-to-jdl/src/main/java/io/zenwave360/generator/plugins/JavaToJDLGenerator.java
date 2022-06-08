package io.zenwave360.generator.plugins;

import io.zenwave360.generator.DocumentedOption;
import io.zenwave360.generator.GeneratorPlugin;
import io.zenwave360.generator.templating.TemplateOutput;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JavaToJDLGenerator implements GeneratorPlugin {

    private Logger log = LoggerFactory.getLogger(getClass());

    enum PersistenceType {
        JPA, MONGODB
    }

    @DocumentedOption(description = "Persistence type to search for annotations for (JPA|MONGODB)")
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
        return generate(null).get(0).getContent();
    }

    @Override
    public List<TemplateOutput> generate(Map<String, ?> contextModel) {
        Class entityAnnotationClass = persistenceType == PersistenceType.JPA? Entity.class : Document.class;
        Set<Class> entitySubClasses = getAnnotatedEntities(entityAnnotationClass);

        // scan enum types in entities field type
        Set<Class<?>> enums = new LinkedHashSet<>();
        for (Class<?> e : entitySubClasses) {
            Field[] declaredFields = e.getDeclaredFields();
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

        StringBuilder relationShips = new StringBuilder();
        for (Class<?> e : entitySubClasses) {
            if(persistenceType == PersistenceType.JPA) {
                generateJPA2Jdl(jdl, relationShips, e);
            } else {
                generateMongodb2Jdl(jdl, relationShips, e);
            }
        }
        jdl.append(relationShips);

        return List.of(new TemplateOutput("", jdl.toString(), "text/jdl"));
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
        boolean firstField = true;
        out.append("entity " + entityClassName + " {\n"); // inheritance NOT SUPPORTED YET in JDL ???

        Field[] declaredFields = e.getDeclaredFields();
        for (Field f : declaredFields) {
            String fieldName = f.getName();
            Type fieldType = f.getType();
            if (f.isSynthetic() || Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            // Annotation[] fieldAnnotations = f.getDeclaredAnnotations();

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
                if (firstField) {
                    firstField = false;
                } else {
                    out.append(",\n");
                }
                out.append("  " + fieldName + " " + f.getType().getSimpleName());
            }

        }

        out.append("\n");
        out.append("}\n\n");
    }


    protected void generateMongodb2Jdl(StringBuilder out, StringBuilder relationShips, Class<?> e) {
        String entityClassName = e.getSimpleName();
        boolean firstField = true;
        out.append("entity " + entityClassName + " {\n"); // inheritance NOT SUPPORTED YET in JDL ???

        Field[] declaredFields = e.getDeclaredFields();
        for (Field f : declaredFields) {
            String fieldName = f.getName();
            Type fieldType = f.getType();
            if (f.isSynthetic() || Modifier.isStatic(f.getModifiers()) || Modifier.isTransient(f.getModifiers())) {
                continue;
            }
            // Annotation[] fieldAnnotations = f.getDeclaredAnnotations();

            String relationType = null;
            Class<?> targetEntityClass = null;
            boolean fromMany = false;
            boolean toMany = false;
            String mappedBy = "";
            DBRef dbRefAnnotation = f.getDeclaredAnnotation(DBRef.class);
            if (dbRefAnnotation != null) {
                targetEntityClass = f.getType();
                boolean isCollection = Collection.class.isAssignableFrom(targetEntityClass);
                relationType = isCollection? "OneToMany" : "ManyToOne";
                fromMany = false;
                toMany = isCollection;
                mappedBy = null;
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
                if (firstField) {
                    firstField = false;
                } else {
                    out.append(",\n");
                }
                out.append("  " + fieldName + " " + f.getType().getSimpleName());
            }

        }

        out.append("\n");
        out.append("}\n\n");
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
            return typeToClass(((ParameterizedType) type).getRawType());
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
