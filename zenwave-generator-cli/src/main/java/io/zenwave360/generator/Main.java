package io.zenwave360.generator;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author ivangsa
 */
public class Main implements Callable<Void> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean help;

    @Option(names = {"-p", "--preset"}, arity = "0..1", description = "Preset chain and configuration options")
    String preset;

    @Option(names = {"-c", "--chain"}, split = ",", description = "Comma separated chain of parsers,processors,generators,writters to be used")
    Class[] chain;

//    @Option(names = {"-o", "--options"})
    @CommandLine.Parameters
    Map<String, Object> options;

    public static void main(String... args) {
        CommandLine cmd = new CommandLine(new Main());
        int returnCode = cmd.execute(args);
//        System.exit(returnCode);
    }

    @Override
    public Void call() throws Exception {
        Configuration configuration = new Configuration()
                .withSpecFile((String) options.get("specFile"))
                .withTargetFolder((String) options.get("targetFolder"))
                .withOptions(options)
                .withChain(chain);

        new Generator(configuration).generate();
        return null;
    }
}
