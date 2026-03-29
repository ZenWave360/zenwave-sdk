package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.generators.Generator;
import io.zenwave360.sdk.parsers.Model;
import io.zenwave360.sdk.plugins.templates.TerraformKafkaTemplates;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateInput;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.utils.JSONPath;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.zenwave360.sdk.templating.OutputFormatType.JAVA;

public class AsyncAPIOpsGenerator extends Generator {

    private Logger log = LoggerFactory.getLogger(getClass());

    @DocumentedOption(description = "Templates to use for code generation.", values = {"TerraformKafka", "FQ Class Name"})
    public String templates = "TerraformKafka";

    public String sourceProperty = "api";

    @Override public GeneratedProjectFiles generate(Map<String, Object> contextModel) {
        Model apiModel = (Model) contextModel.get(sourceProperty);
        Templates templates = configureTemplates();

        var outputList = new ArrayList<TemplateOutput>();

        // Pass the intent model (built by AsyncAPIOpsIntentProcessor) to common templates
        var commonExtModel = new HashMap<String, Object>();
        var intent = contextModel.get("intent");
        if (intent != null) {
            commonExtModel.put("intent", intent);
        }
        outputList.addAll(generateTemplateOutput(contextModel, templates.commonTemplates, commonExtModel));

        if (apiModel != null) {
            var channels = JSONPath.get(apiModel, "$.channels", Map.<String, Map>of());
            outputList.addAll(generateTemplateOutput(contextModel, templates.allChannelsTemplates, Map.of("channels", channels)));

            var allMessages = new HashMap<>();

            for (Map.Entry<String, Map> channelEntry : channels.entrySet()) {
                var channelModel = Map.of("channelName", channelEntry.getKey(), "channel", channelEntry.getValue());
                outputList.addAll(generateTemplateOutput(contextModel, templates.channelTemplates, channelModel));

                var messages = JSONPath.get(channelEntry.getValue(), "$.messages", Map.<String, Map>of());
                outputList.addAll(generateTemplateOutput(contextModel, templates.channelMessagesTemplates, channelModel));

                for (Map.Entry<String, Map> messageEntry : messages.entrySet()) {
                    var messageModel = Map.of(
                            "channelName", channelEntry.getKey(),
                            "channel", channelEntry.getValue(),
                            "messageName", messageEntry.getKey(),
                            "message", messageEntry.getValue());
                    outputList.addAll(generateTemplateOutput(contextModel, templates.messageTemplates, messageModel));
                }
                allMessages.putAll(messages);
            }

            outputList.addAll(generateTemplateOutput(contextModel, templates.allMessagesTemplates, Map.of("messages", allMessages)));
        }

        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.addAll(outputList);
        return generatedProjectFiles;
    }

    protected Templates configureTemplates() {
        Templates templatesObject = null;
        if("TerraformKafka".equals(templates)) {
            templatesObject = new TerraformKafkaTemplates(this);
        } else {
            // Instantiate FQ class name
            try {
                templatesObject = (Templates) Class.forName(templates).getConstructor(
                        AsyncAPIOpsGenerator.class).newInstance(this);
            } catch (Exception e) {
                try {
                    templatesObject = (Templates) Class.forName(templates).getConstructor().newInstance();
                } catch (Exception ex) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            MainGenerator.applyConfiguration(0, templatesObject, configuration);
            return templatesObject;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<TemplateOutput> generateTemplateOutput(Map<String, Object> contextModel, List<TemplateInput> templates, Map<String, Object> extModel) {
        Map<String, Object> baseModel = new HashMap<>();
        baseModel.putAll(this.asConfigurationMap());
        baseModel.put("context", contextModel);
        baseModel.put("asyncapi", contextModel.get(sourceProperty));
        // baseModel.putAll(this.templates.getDocumentedOptions());

        var templateOutputList = new ArrayList<TemplateOutput>();
        for (TemplateInput template : templates) {
            var model = new HashMap<>(baseModel);
            model.putAll(extModel);
            templateOutputList.addAll(getTemplateEngine().processTemplates(model, List.of(template)));
        }
        return templateOutputList;
    }


    public static class Templates {

        public final String templatesFolder;

        public Templates(String templatesFolder) {
            this.templatesFolder = templatesFolder;
        }


        public List<TemplateInput> commonTemplates = new ArrayList<>();

        public List<TemplateInput> allChannelsTemplates = new ArrayList<>();
        public List<TemplateInput> channelTemplates = new ArrayList<>();
        public List<TemplateInput> channelMessagesTemplates = new ArrayList<>();
        public List<TemplateInput> messageTemplates = new ArrayList<>();
        public List<TemplateInput> allMessagesTemplates = new ArrayList<>();

        public void addTemplate(List<TemplateInput> templates, String templateLocation, String targetFile) {
            addTemplate(templates, templateLocation, targetFile, JAVA, null, false);
        }

        public void addTemplate(List<TemplateInput> templates, String templateLocation, String targetFile, OutputFormatType mimeType, Function<Map<String, Object>, Boolean> skip, boolean skipOverwrite) {
            var template = new TemplateInput()
                    .withTemplateLocation(templatesFolder + "/" + templateLocation)
                    .withTargetFile(targetFile)
                    .withMimeType(mimeType)
                    .withSkipOverwrite(skipOverwrite)
                    .withSkip(skip);
            templates.add(template);
        }
    }
}
