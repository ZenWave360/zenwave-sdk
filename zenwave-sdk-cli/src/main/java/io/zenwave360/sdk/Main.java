package io.zenwave360.sdk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.zenwave360.sdk.utils.CliOutput;
import io.zenwave360.sdk.utils.MavenLoader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * @author ivangsa
 */
public class Main implements Callable<Integer> {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Option(names = {"-h", "--help"}, arity = "0..1", description = "Help with output format", converter = HelpFormatConverter.class)
    Help.Format helpFormat;

    @Option(names = {"-p", "--plugin"}, arity = "0..1", description = "Plugin Class or short-code")
    String pluginClass;

    @Option(names = {"-d", "--deps"}, split = ",", description = "Dependencies to include in classpath")
    List<String> deps;

    @Option(names = {"-r", "--repos"}, split = ",", description = "Extra Maven repositories for --deps (id=url). Also reads JBang run.repos / repos from jbang.properties")
    List<String> repos;

    @Option(names = {"-f", "--force"}, description = "Force overwrite", defaultValue = "false")
    boolean forceOverwrite = false;

    @CommandLine.Parameters
    Map<String, Object> options = new HashMap<>();

    /**
     * Exit codes: {@code 0} success, {@code 1} the input is invalid (a {@link ZenWaveException} with that code, e.g.
     * lint findings), {@code 2} invalid arguments or any other failure of the tool itself.
     */
    public static CommandLine createCommandLine(Main main) {
        CommandLine cmd = new CommandLine(main);
        cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
            commandLine.getErr().println(ex instanceof ZenWaveException ? ex.getMessage() : ex.toString());
            return exitCode(ex);
        });
        cmd.setParameterExceptionHandler((ex, args) -> {
            ex.getCommandLine().getErr().println(ex.getMessage());
            return ZenWaveException.EXIT_TOOL_FAILURE;
        });
        return cmd;
    }

    static int exitCode(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof ZenWaveException zenWaveException) {
                return zenWaveException.getExitCode();
            }
        }
        return ZenWaveException.EXIT_TOOL_FAILURE;
    }

    public static void main(String... args) {
        var main = new Main();
        CommandLine cmd = createCommandLine(main);
        CommandLine.ParseResult parsed = cmd.parseArgs(args);

        boolean noOptions = !parsed.hasMatchedOption("h") && !parsed.hasMatchedOption("p");
        boolean noPlugin = !parsed.hasMatchedOption("p");
        boolean usage = parsed.hasMatchedOption("h") && !parsed.hasMatchedOption("p") && main.helpFormat == null;

        if(main.helpFormat == Help.Format.json && !parsed.hasMatchedOption("p")) {
            main.help();
            return;
        }
        if(usage || noOptions || noPlugin) {
            cmd.usage(System.out);
            main.helpFormat = Help.Format.list;
            main.help();
            return;
        }
        if (parsed.hasMatchedOption("h") && parsed.hasMatchedOption("p")) {
            main.help();
            return;
        }


        CliOutput.reserveStdout();
        int returnCode = cmd.execute(args);
        if (returnCode != 0) {
            System.exit(returnCode);
        }
    }

    @Override
    public Integer call() throws Exception {

        if(forceOverwrite) {
            options.put("forceOverwrite", true);
        }
        var layout = (String) options.get("layout");
        options.remove("layout");
        var specFile = (String) options.get("specFile");
        var apiFile = isApi(specFile) ? specFile : null;
        var zdlFile = specFile != null && specFile.endsWith(".zdl") ? specFile : null;
        Plugin plugin = Plugin.of(this.pluginClass)
                .withLayout(layout)
                .withApiFile(apiFile)
                .withZdlFile(zdlFile)
                .withApiFiles(split(options.get("apiFiles")))
                .withZdlFiles(split(options.get("zdlFiles")))
                .withTargetFolder((String) options.get("targetFolder"))
                .withForceOverwrite(forceOverwrite)
                .withProjectClassLoader(MavenLoader.loadJBangDependencies(deps, repos))
                .withOptions(options);
        new MainGenerator().generate(plugin);
        return 0;
    }

    private boolean isApi(String specFile) {
        return specFile != null && (specFile.endsWith(".yml") || specFile.endsWith(".yaml") || specFile.endsWith(".json"));
    }

    private List<String> split(Object property) {
        if(property instanceof String) {
            return Arrays.stream(((String) property).split(",")).map(String::trim).toList();
        }
        return null;
    }

    public void help() {
        try {
            Plugin plugin = null;
            if(StringUtils.isNotBlank(this.pluginClass)) {
                plugin = Plugin.of(this.pluginClass)
                        .withApiFile((String) options.get("specFile"))
                        .withTargetFolder((String) options.get("targetFolder"))
                        .withOptions(options);
            }
            String help = new Help().help(plugin, helpFormat);
            System.setProperty("file.encoding", "UTF-8");
            System.out.println(help);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class HelpFormatConverter implements CommandLine.ITypeConverter<Help.Format> {
        @Override
        public Help.Format convert(String value) throws Exception {
            if(value == null || value.isEmpty()) {
                return null;
            }
            return Help.Format.valueOf(value);
        }
    }
}
