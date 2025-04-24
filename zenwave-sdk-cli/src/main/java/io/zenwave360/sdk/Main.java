package io.zenwave360.sdk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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

    @Option(names = {"-f", "--force"}, description = "Force overwrite", defaultValue = "false")
    boolean forceOverwrite = false;

    @CommandLine.Parameters
    Map<String, Object> options = new HashMap<>();

    public static void main(String... args) {
        var main = new Main();
        CommandLine cmd = new CommandLine(main);
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
