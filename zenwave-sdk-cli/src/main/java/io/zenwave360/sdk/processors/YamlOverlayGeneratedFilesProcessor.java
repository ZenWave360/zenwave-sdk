package io.zenwave360.sdk.processors;

import io.zenwave360.jsonrefparser.AuthenticationValue;
import io.zenwave360.sdk.parsers.DefaultYamlParser;
import io.zenwave360.sdk.parsers.WithProjectClassLoader;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public abstract class YamlOverlayGeneratedFilesProcessor
        implements GeneratedFilesProcessor, WithProjectClassLoader<YamlOverlayGeneratedFilesProcessor> {

    public List<AuthenticationValue> authentication = List.of();

    private ClassLoader projectClassLoader;

    protected abstract String getMergeFile();

    protected abstract List<String> getOverlayFiles();

    protected UnaryOperator<Map<String, Object>> getDocumentOrderer() {
        return UnaryOperator.identity();
    }

    @Override
    public YamlOverlayGeneratedFilesProcessor withProjectClassLoader(ClassLoader projectClassLoader) {
        this.projectClassLoader = projectClassLoader;
        return this;
    }

    @Override
    public void process(GeneratedProjectFiles generatedProjectFiles) throws IOException {
        String mergeFile = getMergeFile();
        List<String> overlayFiles = getOverlayFiles();
        if (mergeFile == null && (overlayFiles == null || overlayFiles.isEmpty())) {
            return;
        }

        DefaultYamlParser resourceLoader = new DefaultYamlParser()
                .withProjectClassLoader(projectClassLoader)
                .withAuthentication(authentication);

        for (TemplateOutput output : generatedProjectFiles.getAllTemplateOutputs()) {
            if (OutputFormatType.YAML.toString().equals(output.getMimeType()) && output.getContent() != null) {
                output.merge(applyMergeAndOverlays(resourceLoader, output, mergeFile, overlayFiles));
            }
        }
    }

    private TemplateOutput applyMergeAndOverlays(DefaultYamlParser resourceLoader,
                                                  TemplateOutput output,
                                                  String mergeFile,
                                                  List<String> overlayFiles) throws IOException {
        try {
            String content = resourceLoader.mergeAndOverlay(
                    output.getContent(), mergeFile, overlayFiles, getDocumentOrderer());
            return new TemplateOutput(
                    output.getTargetFile(), content, output.getMimeType(), output.isSkipOverwrite());
        } catch (IOException e) {
            throw new IOException("Failed to apply merge or overlay resources to " + output.getTargetFile(), e);
        }
    }
}
