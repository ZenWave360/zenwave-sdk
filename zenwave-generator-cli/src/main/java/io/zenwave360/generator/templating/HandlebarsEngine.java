package io.zenwave360.generator.templating;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import io.zenwave360.generator.processors.GeneratorPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HandlebarsEngine {

    Context context;
    Handlebars handlebars = new Handlebars();

    public HandlebarsEngine(GeneratorPlugin generator) {
        context = Context
                .newBuilder(generator)
                .resolver(
                        MapValueResolver.INSTANCE,
                        JavaBeanValueResolver.INSTANCE,
                        NonPrivateFieldValueResolver.INSTANCE)
                .build();
    }

    public List<TemplateOutput> processTemplates(Map<String, Object> apiModel, List<TemplateInput> templateInputs) {
        return this.processTemplates("api", apiModel, templateInputs);
    }

    public List<TemplateOutput> processTemplates(String modelPrefix, Map<String, Object> apiModel, List<TemplateInput> templateInputs) {
        context.combine(modelPrefix, apiModel);
        List<TemplateOutput> templateOutputList = new ArrayList<>();
        templateInputs.forEach(templateInput -> {
            if(templateInput.getSkip() == null || !templateInput.getSkip().get()) {
                try {
                    String targetFile = handlebars.compileInline(templateInput.getTargetFile()).apply(context);
                    String content = handlebars.compile(templateInput.getTemplateLocation()).apply(context);
                    templateOutputList.add(new TemplateOutput(targetFile, content));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return templateOutputList;
    }
}
