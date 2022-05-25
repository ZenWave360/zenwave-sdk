package io.zenwave360.generator.templating;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import io.zenwave360.generator.plugins.GeneratorPlugin;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HandlebarsEngine implements TemplateEngine {

    Context context;
    Handlebars handlebars = new Handlebars();

    public HandlebarsEngine() {
        context = Context
                .newBuilder(new HashMap<>())
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
                .build();
        handlebars.registerHelpers(HandlebarsHelpers.class);
    }

    @Override
    public TemplateOutput processTemplate(Map<String, Object> model, TemplateInput templateInput) {
        return this.processTemplates(model, Arrays.asList(templateInput)).get(0);
    }
    @Override
    public List<TemplateOutput> processTemplates(Map<String, Object> model, List<TemplateInput> templateInputs) {
        return this.processTemplates(null, model, templateInputs);
    }

    @Override
    public List<TemplateOutput> processTemplates(String modelPrefix, Map<String, Object> apiModel, List<TemplateInput> templateInputs) {
        if(modelPrefix != null) {
            context.combine(modelPrefix, apiModel);
        } else {
            context.combine(apiModel);
        }
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
