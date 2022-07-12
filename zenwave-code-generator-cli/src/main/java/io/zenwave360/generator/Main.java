package io.zenwave360.generator;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author ivangsa
 */
public class Main implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean help;

    @Option(names = {"-p", "--plugin"}, arity = "0..1", description = "Plugin Configuration class")
    String preset;

    @Option(names = {"-c", "--chain"}, split = ",", description = "deprecated use --plugin instead")
    Class[] chain;

    @CommandLine.Parameters
    Map<String, Object> options = new HashMap<>();

    public static void main(String... args) {
        CommandLine cmd = new CommandLine(new Main());
        int returnCode = cmd.execute(args);
        if(returnCode != 0) {
            System.exit(returnCode);
        }
    }

    @Override
    public Integer call() throws Exception {
        Configuration configuration = createConfiguration(this.preset)
                .withSpecFile((String) options.get("specFile"))
                .withTargetFolder((String) options.get("targetFolder"))
                .withOptions(options)
                .withChain(chain);

        new Generator(configuration).generate();
        return 0;
    }

    protected Configuration createConfiguration(String preset) throws Exception {
        if (preset != null) {
            return (Configuration) getClass().getClassLoader().loadClass(preset).getDeclaredConstructor().newInstance();
        }
        return new Configuration();
    }
}
