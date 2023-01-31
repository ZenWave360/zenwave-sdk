package io.zenwave360.generator.doc;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

@SupportedAnnotationTypes({"io.zenwave360.generator.doc.DocumentedPlugin"})
public final class PluginAnnotationProcessor extends AbstractProcessor {

    private Elements elementUtils;

    @Override
    public void init(final ProcessingEnvironment procEnv) {
        super.init(procEnv);
        this.elementUtils = procEnv.getElementUtils();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> processableAnnotations, final RoundEnvironment roundEnv) {
        try {
            var pluginElements = roundEnv.getElementsAnnotatedWith(DocumentedPlugin.class);
            for (Element pluginElement : pluginElements) {
                DocumentedPlugin documentedPluginAnnotation = pluginElement.getAnnotation(DocumentedPlugin.class);
                String docComment = elementUtils.getDocComment(pluginElement);
                if ("${javadoc}".contentEquals(documentedPluginAnnotation.description())) {
                    Object jcTree = MethodUtils.invokeMethod(elementUtils, "getTree", pluginElement);
                    Object mods = FieldUtils.readField(jcTree, "mods", true);
                    List annotations = (List) FieldUtils.readField(mods, "annotations", true);
                    for (Object annotation : annotations) {
                        Object annotationType = FieldUtils.readField(annotation, "annotationType", true);
                        Object type = FieldUtils.readField(annotationType, "type", true);
                        if (DocumentedPlugin.class.getName().contentEquals(type.toString())) {
                            List args = (List) FieldUtils.readField(annotation, "args", true);
                            for (Object arg : args) {
                                Object lhs = FieldUtils.readField(arg, "lhs", true);
                                if ("description".contentEquals(lhs.toString())) {
                                    Object rhs = FieldUtils.readField(arg, "rhs", true);
                                    FieldUtils.writeField(rhs, "value", docComment);
                                }
                            }

                        }
                    }
                }
                DocumentedPlugin annotation2 = pluginElement.getAnnotation(DocumentedPlugin.class);
                System.out.println("Annotation2 " + annotation2.description());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing DocumentedPlugin javadoc into annotation description", e);
        }
        return true;
    }

    private void updateAnnotationField(Annotation annotation, String field, String value) {
        Object handler = Proxy.getInvocationHandler(annotation);
        Field f;
        try {
            f = handler.getClass().getDeclaredField("memberValues");
        } catch (NoSuchFieldException | SecurityException e) {
            throw new IllegalStateException(e);
        }
        f.setAccessible(true);
        Map<String, Object> memberValues;
        try {
            memberValues = (Map<String, Object>) f.get(handler);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        Object oldValue = memberValues.get(field);
        if (oldValue == null || oldValue.getClass() != value.getClass()) {
            throw new IllegalArgumentException();
        }
        memberValues.put(field, value);
    }

}
