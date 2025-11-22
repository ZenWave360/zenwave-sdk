package io.zenwave360.sdk;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

import io.zenwave360.jsonrefparser.AuthenticationValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Goal which generates code with the configured ZenWave SDK plugin.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GeneratorMojo extends AbstractMojo {

      @Parameter( defaultValue = "${project.artifact}", readonly = true, required = true )
      private Artifact projectArtifact;
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
     * Location of the ZDL model, as URL or file.
     */
    @Parameter(name = "zdlFile", property = "zenwave.zdlFile")
    private String zdlFile;

    /**
     * Location of the ZDL model, as URL or file.
     */
    @Parameter(name = "zdlFiles", property = "zenwave.zdlFiles")
    private String[] zdlFiles;

    /**
     * Authentication configuration values for fetching remote resources.
     */
    @Parameter(name = "authentication", property = "zenwave.authentication")
    private List<AuthenticationValue> authentication = List.of();

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
     * Add the 'src/test/java' directory to the project as a tests source, so that the generated java types are compiled and included in the project tests classpath.
     */
    @Parameter(defaultValue = "false")
    private boolean addTestCompileSourceRoot = false;


    /**
     * Include project classpath to the classpath of the generator. Useful to generate code from specs inside project dependencies.
     */
    @Parameter(defaultValue = "false")
    private boolean includeProjectClasspath = false;

    /**
     * A map of specific options for the called generator plugin.
     */
    @Parameter(name = "configOptions")
    private Map<String, Object> configOptions;

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

            URLClassLoader projectClassLoader = null;
            if(includeProjectClasspath) {
                var classpathFiles = getProjectClasspathElements(project);
                projectClassLoader = new URLClassLoader(classpathFiles.toArray(new URL[0]), this.getClass().getClassLoader());
            }

            String apiFile = isRemoteOrClasspathResource(inputSpec) ? inputSpec : new File(inputSpec).toURI().toString();
            List<String> zdls = new ArrayList<>();
            if(zdlFile != null) {
                zdls.add(zdlFile);
            }
            if(apiFile.endsWith(".zdl") && !zdls.contains(apiFile)) {
                zdls.add(apiFile);
                apiFile = null;
            }
            if(zdlFiles != null) {
                zdls.addAll(Arrays.asList(zdlFiles));
            }
            zdls = zdls.stream().map(zdl -> zdl.startsWith("classpath:") ? zdl : new File(zdl).toURI().toString()).collect(Collectors.toList());
            Plugin plugin = Plugin.of(this.generatorName)
                    .withApiFile(apiFile)
                    .withZdlFiles(zdls)
                    .withAuthentication(authentication)
                    .withTargetFolder(targetFolder.getAbsolutePath())
                    .withProjectClassLoader(projectClassLoader)
                    .withOptions(options);

            new MainGenerator().generate(plugin);
        } catch (Exception e) {
            // Maven logs exceptions thrown by plugins only if invoked with -e
            // I find it annoying to jump through hoops to get basic diagnostic information,
            // so let's log it in any case:
            e.printStackTrace();
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
        return new File(targetFolder, sourceFolder).getAbsolutePath();
    }

    private String getTestCompileSourceRoot() {
        final Object sourceFolderObject = configOptions == null ? null : configOptions.get("testSourceFolder");
        final String sourceFolder = sourceFolderObject == null ? "src/test/java" : sourceFolderObject.toString();
        return new File(targetFolder, sourceFolder).getAbsolutePath();
    }

    private void addCompileSourceRootIfConfigured() {
        if (addCompileSourceRoot) {
            String compileSourceRoot = getCompileSourceRoot();
            System.out.println("Adding source root " + compileSourceRoot);
            project.addCompileSourceRoot(compileSourceRoot);
        }
        if (addTestCompileSourceRoot) {
            String testCompileSourceRoot = getTestCompileSourceRoot();
            System.out.println("Adding tests source root " + testCompileSourceRoot);
            project.addTestCompileSourceRoot(testCompileSourceRoot);
        }
    }

    private List<URL> getProjectClasspathElements(MavenProject project) {
        List<File> list = new ArrayList<>();
        project.getResources().stream().map(r -> new File(r.getDirectory())).forEach(list::add);
        list.add(new File(project.getBuild().getOutputDirectory()));
        list.addAll(project.getArtifacts().stream().map(Artifact::getFile).collect(Collectors.toList()));
        return list.stream().map(file -> {
            try {
                return file.toURI().toURL();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private boolean isRemoteOrClasspathResource(String spec) {
        return spec.startsWith("classpath:") || spec.startsWith("http://") || spec.startsWith("https://");
    }
}
