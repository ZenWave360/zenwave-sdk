package io.zenwave360.sdk.formatters;

import com.google.googlejavaformat.java.FormatterException;
import io.spring.javaformat.config.JavaFormatConfig;
import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.jupiter.api.Test;

import java.io.File;

public class SpringJavaFormatterTest {

    @Test
    void testSpringFormatter() throws BadLocationException, FormatterException {
        SpringJavaFormatter formatter = new SpringJavaFormatter();

        var source = """
                package io.zenwave360.sdk.formatters;
                              
                                              import java.io.File;
                                                import org.eclipse.jface.text.BadLocationException;  
                import io.spring.javaformat.config.JavaFormatConfig;
                                

                                
                public class SpringJavaFormatterTest {
                   \s
                    void testSpringFormatter() {
                        final io.spring.javaformat.formatter.Formatter              formatter =
                                new io.spring.javaformat.formatter.Formatter(JavaFormatConfig.findFrom(new File(".")));
                       \s
                        var source = null;
                    }
                }
                """;

        String formattedContent = formatter.format(new TemplateOutput("test.java", source, OutputFormatType.JAVA.toString(), false)).getContent();
        System.out.println(formattedContent);
    }

}
