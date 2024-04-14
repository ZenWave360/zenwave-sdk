package io.zenwave360.sdk.templating;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.*;

public class HandlebarsEngine implements TemplateEngine {

    Context context;
    Handlebars handlebars = new Handlebars()
            .with(new CompositeTemplateLoader(new FolderTemplateLoader(), new ClassPathTemplateLoader()));

    public HandlebarsEngine() {
        context = Context
                .newBuilder(new HashMap<>())
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, new FieldValueResolver() {
                    @Override
                    protected Object invokeMember(FieldWrapper field, Object context) {
                        try {
                            return super.invokeMember(field, context);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                })
                .build();
        handlebars.registerHelpers(CustomHandlebarsHelpers.class);
        handlebars.registerHelpers(StringHelpers.class);
    }

    public Handlebars getHandlebars() {
        return handlebars;
    }

    @Override
    public String processInline(String template, Map<String, Object> model) throws IOException {
        return handlebars.compileInline(template).apply(model);
    }

    @Override
    public TemplateOutput processTemplate(Map<String, Object> model, TemplateInput templateInput) {
        return this.processTemplates(model, List.of(templateInput)).get(0);
    }

    @Override
    public List<TemplateOutput> processTemplates(Map<String, Object> apiModel, List<TemplateInput> templateInputs) {
        var currentModel = new HashMap((Map) context.model());
        var contextModel = (Map) context.model();
        contextModel.putAll(apiModel);
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        templateInputs.forEach(templateInput -> {
            if (templateInput.getSkip() == null || !Boolean.TRUE.equals(templateInput.getSkip().apply(apiModel))) {
                try {
                    String targetFile = handlebars.compileInline(templateInput.getTargetFile()).apply(context);
                    String templateLocation = handlebars.compileInline(templateInput.getTemplateLocation()).apply(context);
                    String content = handlebars.compile(templateLocation).apply(context);
                    templateOutputList.add(new TemplateOutput(targetFile, content, templateInput.getMimeType(), templateInput.isSkipOverwrite()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        contextModel.clear();
        contextModel.putAll(currentModel);
        return templateOutputList;
    }

    @Override
    public List<TemplateOutput> processTemplateNames(Map<String, Object> model, TemplateInput templateInput) {
        return this.processTemplateNames(model, List.of(templateInput));
    }

    @Override
    public List<TemplateOutput> processTemplateNames(Map<String, Object> apiModel, List<TemplateInput> templateInputs) {
        var currentModel = new HashMap((Map) context.model());
        var contextModel = (Map) context.model();
        contextModel.putAll(apiModel);
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        templateInputs.forEach(templateInput -> {
            if (templateInput.getSkip() == null || !Boolean.TRUE.equals(templateInput.getSkip().apply(apiModel))) {
                try {
                    String targetFile = handlebars.compileInline(templateInput.getTargetFile()).apply(context);
                    String templateLocation = handlebars.compileInline(templateInput.getTemplateLocation()).apply(context);
                    //                    String content = handlebars.compile(templateLocation).apply(context);
                    templateOutputList.add(new TemplateOutput(templateInput, targetFile, apiModel, templateInput.getMimeType(), templateInput.isSkipOverwrite()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        contextModel.clear();
        contextModel.putAll(currentModel);
        return templateOutputList;
    }

    private static class FolderTemplateLoader extends URLTemplateLoader {
        private File root = new File("./.zenwave/templates");

        @Override
        protected URL getResource(final String location) throws IOException {
            File file = new File(root, location);
            return file.exists() ? file.toURI().toURL() : null;
        }
    }
}
