package io.zenwave360.sdk.formatters;

import com.google.googlejavaformat.java.FormatterException;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.eclipse.jface.text.BadLocationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

public class JavaFormatterTest {

    @Test
    void testJavaFormatter() throws BadLocationException, FormatterException {
        JavaFormatter formatter = new JavaFormatter();

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
//        System.out.println(formattedContent);
    }

    @Test
    void testPalantirJavaFormatter() throws BadLocationException, FormatterException, com.palantir.javaformat.java.FormatterException {
        JavaFormatter formatter = new JavaFormatter();
        formatter.formatter = Formatter.Formatters.palantir;
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
//        System.out.println(formattedContent);

        // This code is raw palantir formatter usage
//        var formatter = com.palantir.javaformat.java.Formatter.create();
//        var formated = formatter.formatSourceAndFixImports(source);
//        System.out.println(formated);
    }


}
