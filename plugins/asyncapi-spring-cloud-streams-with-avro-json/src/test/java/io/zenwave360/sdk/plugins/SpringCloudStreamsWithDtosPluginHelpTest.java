package io.zenwave360.sdk.plugins;

import io.zenwave360.sdk.Help;
import org.junit.jupiter.api.Test;

public class SpringCloudStreamsWithDtosPluginHelpTest {

    @Test
    public void testPrintMarkdownHelp() {
        AvroSchemaGeneratorPlugin plugin = new AvroSchemaGeneratorPlugin();
        Help help = new Help();
        String markdownHelp = help.help(plugin, Help.Format.markdown);
        System.out.println(markdownHelp);
    }
}
