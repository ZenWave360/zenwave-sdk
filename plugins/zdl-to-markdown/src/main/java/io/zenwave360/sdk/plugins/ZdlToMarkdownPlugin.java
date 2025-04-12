package io.zenwave360.sdk.plugins;

import java.io.IOException;
import java.util.Map;

import io.zenwave360.sdk.Plugin;
import io.zenwave360.sdk.doc.DocumentedPlugin;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.writers.TemplateFileWriter;
import io.zenwave360.sdk.writers.TemplateStdoutWriter;

import static io.zenwave360.sdk.plugins.ZdlToMarkdownGenerator.OutputFormat.aggregate;
import static io.zenwave360.sdk.plugins.ZdlToMarkdownGenerator.OutputFormat.task_list;

@DocumentedPlugin(summary = "Generates Markdown glossary from Zdl Models",
        hiddenOptions = {"apiFile", "apiFiles", "layout", "targetFolder", "basePackage"})
public class ZdlToMarkdownPlugin extends Plugin {

    public ZdlToMarkdownPlugin() {
        super();
        withChain(ZDLParser.class, ZDLProcessor.class, ZdlToMarkdownGenerator.class, TemplateFileWriter.class);
    }

    public static String generateMarkdown(String zdlContent) throws IOException {
        Map<String, Object> model = new ZDLParser().withContent(zdlContent).parse();
        model = new ZDLProcessor().process(model);
        model = new PathsProcessor().process(model);
        var out = new ZdlToMarkdownGenerator().generate(model);
        return out.get(0).getContent();
    }

    public static String generateTaskList(String zdlContent) throws IOException {
        Map<String, Object> model = new ZDLParser().withContent(zdlContent).parse();
        model = new ZDLProcessor().process(model);
        model = new PathsProcessor().process(model);
        var out = new ZdlToMarkdownGenerator().withOutputFormat(task_list).withSkipDiagrams(true).generate(model);
        return out.get(0).getContent();
    }

    public static String generateTaskListWithDiagrams(String zdlContent) throws IOException {
        Map<String, Object> model = new ZDLParser().withContent(zdlContent).parse();
        model = new ZDLProcessor().process(model);
        model = new PathsProcessor().process(model);
        var out = new ZdlToMarkdownGenerator().withOutputFormat(task_list).generate(model);
        return out.get(0).getContent();
    }


    public static String generateAggregateUML(String zdlContent, String aggregateName) throws IOException {
        Map<String, Object> model = new ZDLParser().withContent(zdlContent).parse();
        model = new ZDLProcessor().process(model);
        model = new PathsProcessor().process(model);
        var out = new ZdlToMarkdownGenerator()
                .withOutputFormat(aggregate)
                .withAggregateName(aggregateName)
                .generate(model);
        return out.get(0).getContent();
    }



    @Override
    public <T extends Plugin> T processOptions() {
        if (!getOptions().containsKey("targetFile")) {
            replaceInChain(TemplateFileWriter.class, TemplateStdoutWriter.class);
        }
        return (T) this;
    }
}
