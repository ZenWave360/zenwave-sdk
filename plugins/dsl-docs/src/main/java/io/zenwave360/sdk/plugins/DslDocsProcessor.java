package io.zenwave360.sdk.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.zenwave360.language.docs.DslDocs;
import io.zenwave360.language.docs.DocsOutput;
import io.zenwave360.language.docs.files.DocsDifference;
import io.zenwave360.language.docs.files.DocsFiles;
import io.zenwave360.language.docs.files.DocsFolder;
import io.zenwave360.sdk.ZenWaveException;
import io.zenwave360.sdk.doc.DocumentedOption;
import io.zenwave360.sdk.processors.Processor;
import io.zenwave360.sdk.utils.CliOutput;

/** Runs dsl-docs using paths relative to the CLI working directory. */
public class DslDocsProcessor implements Processor {

    @DocumentedOption(description = "ZDL files to document as separate modules (comma separated)")
    public List<String> zdlFiles = List.of();

    @DocumentedOption(description = "Shortcut for one ZDL module file")
    public String zdlFile;

    @DocumentedOption(description = "ZFL event-flow files, one page per file (comma separated)")
    public List<String> zflFiles = List.of();

    @DocumentedOption(description = "OpenAPI and AsyncAPI files, detected by content (comma separated)")
    public List<String> apiFiles = List.of();

    @DocumentedOption(description = "Shortcut for one OpenAPI or AsyncAPI file")
    public String apiFile;

    @DocumentedOption(description = "Output folder for generated Markdown documentation", defaultValue = "docs")
    public String targetFolder = "docs";

    @DocumentedOption(description = "Documentation title (defaults from the first ZDL or API specification)")
    public String title;

    @DocumentedOption(description = "Documentation version (defaults from the first ZDL or API specification)")
    public String version;

    @DocumentedOption(description = "Date shown on the index page")
    public String date;

    @DocumentedOption(description = "Compare with the output folder without writing files", defaultValue = "false")
    public boolean check = false;

    @Override
    public Map<String, Object> process(Map<String, Object> contextModel) {
        List<String> zdl = files(zdlFile, zdlFiles);
        List<String> zfl = files(null, zflFiles);
        List<String> api = files(apiFile, apiFiles);
        if (zdl.isEmpty() && zfl.isEmpty() && api.isEmpty()) {
            throw findings("At least one input file is required (zdlFile/zdlFiles, zflFiles or apiFile/apiFiles).");
        }

        File baseDir = new File(".").getAbsoluteFile();
        File outDir = new File(targetFolder);
        try {
            DocsOutput output = new DslDocs().generate(DocsFiles.INSTANCE.input(
                    baseDir, outDir, zdl, zfl, api, title, version, date, List.of()));
            output.getProblems().forEach(problem -> out("WARNING " + problem));

            DocsFolder folder = new DocsFolder(outDir);
            if (check) {
                List<DocsDifference> differences = folder.check(output);
                if (differences.isEmpty()) {
                    out("Up to date: " + output.getFiles().size() + " files in " + outDir);
                } else {
                    differences.forEach(difference -> out("OUT OF DATE " + difference.getPath() + " (" + difference.getReason() + ")"));
                    out("Rerun without check=true to update " + outDir + ".");
                    throw findings("Documentation is out of date.");
                }
            } else {
                var result = folder.write(output);
                out("Wrote " + result.getWritten().size() + " files to " + outDir);
            }
            return contextModel;
        } catch (IllegalArgumentException e) {
            throw findings(e.getMessage());
        }
    }

    private List<String> files(String single, List<String> multiple) {
        List<String> result = new ArrayList<>(multiple.size() + 1);
        if (single != null && !single.isBlank()) {
            result.add(single);
        }
        result.addAll(multiple);
        return result;
    }

    private void out(String message) {
        CliOutput.stdout().println(message);
        CliOutput.stdout().flush();
    }

    private ZenWaveException findings(String message) {
        return new ZenWaveException(message, ZenWaveException.EXIT_FINDINGS);
    }
}
