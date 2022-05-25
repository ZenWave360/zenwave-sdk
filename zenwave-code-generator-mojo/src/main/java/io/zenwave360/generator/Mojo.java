package io.zenwave360.generator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Goal which touches a timestamp file.
 *
 * @goal touch
 * @phase process-sources
 */
public class Mojo extends AbstractMojo {
    /**
     * Location of the JSON/YAML spec, as URL or file.
     */
    @Parameter(name = "inputSpec", property = "zenwave.generator.inputSpec", required = true)
    private String inputSpec;

    /**
     * Location of the output directory.
     */
    @Parameter(name = "output", property = "zenwave.generator.output",
            defaultValue = "${project.build.directory}/generated-sources/zenwave")
    private File output;

    /**
     * Add the output directory to the project as a source root, so that the generated java types
     * are compiled and included in the project artifact.
     */
    @Parameter(defaultValue = "true")
    private boolean addCompileSourceRoot = true;

    /**
     * Add the output directory to the project as a test source root.
     */
    @Parameter(defaultValue = "true")
    private boolean addTestSourceRoot = true;

    /**
     * A map of language-specific parameters as passed with the -c option to the command line
     */
    @Parameter(name = "configOptions")
    private Map<String, String> configOptions;

    /**
     * A configOptions string as 'key=value1,value2\nkey2=value3'.
     */
    @Parameter(name = "configKeyValueOptions", property = "zenwave.generator.configOptions")
    private String configKeyValueOptions;

    /**
     * The project being built.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        File inputSpecFile = new File(inputSpec);
        addCompileSourceRootIfConfigured();
     }

    private String getCompileSourceRoot() {
        final Object sourceFolderObject = configOptions == null ? null : configOptions.get("sourceFolder");
        final String sourceFolder = sourceFolderObject == null ? "src/main/java" : sourceFolderObject.toString();
        return output.toString() + "/" + sourceFolder;
    }

    private void addCompileSourceRootIfConfigured() {
        if (addCompileSourceRoot) {
            System.out.println("Adding source root " + getCompileSourceRoot());
            project.addCompileSourceRoot(getCompileSourceRoot());
        }
        if (addTestSourceRoot) {
            System.out.println("Adding source root " + getCompileSourceRoot());
            project.addTestCompileSourceRoot(getCompileSourceRoot());
        }
    }
}
