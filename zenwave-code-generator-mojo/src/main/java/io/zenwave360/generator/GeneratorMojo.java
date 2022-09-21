package io.zenwave360.generator;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Goal which generates code with the configured ZenWave Code Generator plugin.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GeneratorMojo extends AbstractMojo {

    /**
     * The name of the generator to use.
     */
    @Parameter(name = "generatorName", property = "zenwave.generatorName", required = true)
    private String generatorName;

    /**
     * Location of the JSON/YAML spec, as URL or file.
     */
    @Parameter(name = "inputSpec", property = "zenwave.inputSpec", required = true)
    private String inputSpec;

    /**
     * Location of the output directory.
     */
    @Parameter(name = "targetFolder", property = "zenwave.output", defaultValue = "${project.build.directory}/generated-sources/zenwave")
    private File targetFolder;

    /**
     * Add the output directory to the project as a source root, so that the generated java types are compiled and included in the project artifact.
     */
    @Parameter(defaultValue = "true")
    private boolean addCompileSourceRoot = true;

    /**
     * A map of specific options for the called generator plugin.
     */
    @Parameter(name = "configOptions")
    private Map<String, String> configOptions;

    /**
     * A configOptions string as 'key=value1,value2\nkey2=value3'.
     */
    @Parameter(name = "configKeyValueOptions", property = "zenwave.configOptions")
    private String configKeyValueOptions;

    @Parameter(name = "skip", property = "zenwave.skip", required = false, defaultValue = "false")
    private Boolean skip;

    /**
     * The project being built.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject project;

    public void execute() throws MojoExecutionException {
        File inputSpecFile = new File(inputSpec);
        addCompileSourceRootIfConfigured();

        if (skip) {
            getLog().info("Code generation is skipped.");
            return;
        }

        try {
            Map<String, Object> options = new HashMap<>();
            options.put("basePackage", project.getGroupId());

            if (configOptions != null) {
                options.putAll(configOptions);
            }

            if (StringUtils.isNotBlank(configKeyValueOptions)) {
                options.putAll(buildConfigOptions(configKeyValueOptions));
            }

            Configuration configuration = Configuration.of(this.generatorName)
                    .withSpecFile(inputSpecFile.getAbsolutePath())
                    .withTargetFolder(targetFolder.getAbsolutePath())
                    .withOptions(options);

            new MainGenerator().generate(configuration);
        } catch (Exception e) {
            // Maven logs exceptions thrown by plugins only if invoked with -e
            // I find it annoying to jump through hoops to get basic diagnostic information,
            // so let's log it in any case:
            getLog().error(e);
            throw new MojoExecutionException("Code generation failed. See above for the full exception.");
        }
    }

    protected Map<String, String> buildConfigOptions(String configOptions) {
        Map<String, String> configMap = Arrays.asList(configOptions.split("\n"))
                .stream()
                .filter(str -> StringUtils.isNotBlank(str))
                .map(str -> str.split("=", 2))
                .collect(Collectors.toMap(split -> StringUtils.trim(split[0]), split -> StringUtils.trim(split[1])));
        return configMap;
    }

    private String getCompileSourceRoot() {
        final Object sourceFolderObject = configOptions == null ? null : configOptions.get("sourceFolder");
        final String sourceFolder = sourceFolderObject == null ? "src/main/java" : sourceFolderObject.toString();
        // return new File(targetFolder, sourceFolder).getAbsolutePath();
        return targetFolder.getAbsolutePath();
    }

    private void addCompileSourceRootIfConfigured() {
        if (addCompileSourceRoot) {
            System.out.println("Adding source root " + getCompileSourceRoot());
            project.addCompileSourceRoot(getCompileSourceRoot());
        }
    }
}
