package io.zenwave360.generator.templating;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.StringHelpers;

public class HandlebarsEngine implements TemplateEngine {

    Context context;
    Handlebars handlebars = new Handlebars();

    public HandlebarsEngine() {
        context = Context
                .newBuilder(new HashMap<>())
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
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
    public List<TemplateOutput> processTemplate(Map<String, Object> model, TemplateInput templateInput) {
        return this.processTemplates(model, List.of(templateInput));
    }

    @Override
    public List<TemplateOutput> processTemplates(Map<String, Object> model, List<TemplateInput> templateInputs) {
        return this.processTemplates(null, model, templateInputs);
    }

    @Override
    public List<TemplateOutput> processTemplates(String modelPrefix, Map<String, Object> apiModel, List<TemplateInput> templateInputs) {
        Context context = Context.newBuilder(this.context).build();
        if (modelPrefix != null) {
            context.combine(modelPrefix, apiModel);
        } else {
            context.combine(apiModel);
        }
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
        return templateOutputList;
    }
}
