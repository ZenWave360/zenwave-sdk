package io.zenwave360.sdk.formatters;

import com.google.googlejavaformat.java.FormatterException;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.eclipse.jface.text.BadLocationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class JavaFormatterTest {

    @ParameterizedTest
    @EnumSource(Formatter.Formatters.class)
    void testJavaFormatterWithAllFormatters(Formatter.Formatters formatterType) throws BadLocationException, FormatterException {
        JavaFormatter formatter = new JavaFormatter();
        formatter.formatter = formatterType;
        formatter.onPropertiesSet();

        var source = """
                package io.zenwave360.sdk.formatters;

                                              import java.io.File;
                                                import org.eclipse.jface.text.BadLocationException;
                import io.spring.javaformat.config.JavaFormatConfig;



                public class SpringJavaFormatterTest {
                   \s
                    void testSpringFormatter() {
                        final io.spring.javaformat.formatter.Formatter              formatter =       new io.spring.javaformat.formatter.Formatter(JavaFormatConfig.findFrom(new File(".")));
                       \s
                        var source = null;
                    }
                }
                """;

        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.add(new TemplateOutput("test.java", source, OutputFormatType.JAVA.toString(), false));

        formatter.format(generatedProjectFiles);

        String formattedContent = generatedProjectFiles.getAllTemplateOutputs().get(0).getContent();
        Assertions.assertNotNull(formattedContent);
        Assertions.assertNotEquals(source, formattedContent);
    }

    @Test
    void testSkipFormatting() {
        JavaFormatter formatter = new JavaFormatter();
        formatter.skipFormatting = true;
        formatter.onPropertiesSet();

        var source = "public class Test{void method(){}}";
        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.add(new TemplateOutput("test.java", source, OutputFormatType.JAVA.toString(), false));

        formatter.format(generatedProjectFiles);

        String formattedContent = generatedProjectFiles.getAllTemplateOutputs().get(0).getContent();
        Assertions.assertEquals(source, formattedContent);
    }

    @Test
    void testHaltOnFailFormatting() {
        JavaFormatter formatter = new JavaFormatter();
        formatter.haltOnFailFormatting = true;
        formatter.onPropertiesSet();

        var invalidSource = "invalid java syntax {{{";
        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.add(new TemplateOutput("test.java", invalidSource, OutputFormatType.JAVA.toString(), false));

        Assertions.assertThrows(RuntimeException.class, () -> {
            formatter.format(generatedProjectFiles);
        });
    }

    @Test
    void testHaltOnFailFormattingDisabled() {
        JavaFormatter formatter = new JavaFormatter();
        formatter.haltOnFailFormatting = false;
        formatter.onPropertiesSet();

        var invalidSource = "invalid java syntax {{{";
        GeneratedProjectFiles generatedProjectFiles = new GeneratedProjectFiles();
        generatedProjectFiles.singleFiles.add(new TemplateOutput("test.java", invalidSource, OutputFormatType.JAVA.toString(), false));

        formatter.format(generatedProjectFiles);

        String formattedContent = generatedProjectFiles.getAllTemplateOutputs().get(0).getContent();
        Assertions.assertEquals(invalidSource, formattedContent);
    }
}
