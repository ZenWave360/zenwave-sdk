package io.zenwave360.sdk.formatters;

import io.zenwave360.sdk.templating.OutputFormatType;
import io.zenwave360.sdk.templating.TemplateOutput;
import io.zenwave360.sdk.zdl.GeneratedProjectFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.List;

public class KotlinFormatterTest {

    private KotlinFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new KotlinFormatter();
    }

    @Test
    void testFormatKotlinCode() {
        String unformattedKotlin = "class Test{fun hello(){println(\"Hello World\")}}";
        TemplateOutput templateOutput = new TemplateOutput("Test.kt", unformattedKotlin, OutputFormatType.KOTLIN.toString(), false);

        TemplateOutput result = formatter.format(templateOutput);

        Assertions.assertNotEquals(unformattedKotlin, result.getContent());
        Assertions.assertTrue(result.getContent().contains("class Test"));
        Assertions.assertTrue(result.getContent().contains("fun hello()"));
    }

    @Test
    void testSkipFormattingWhenDisabled() {
        formatter.skipFormatting = true;
        String kotlinCode = "class Test{fun hello(){println(\"Hello World\")}}";
        TemplateOutput templateOutput = new TemplateOutput("Test.kt", kotlinCode, OutputFormatType.KOTLIN.toString(), false);

        TemplateOutput result = formatter.format(templateOutput);

        Assertions.assertEquals(kotlinCode, result.getContent());
    }

    @Test
    void testSkipFormattingWithFormatterOffComment() {
        String kotlinCode = "// formatter:off\nclass Test{fun hello(){println(\"Hello World\")}}";
        TemplateOutput templateOutput = new TemplateOutput("Test.kt", kotlinCode, OutputFormatType.KOTLIN.toString(), false);

        TemplateOutput result = formatter.format(templateOutput);

        Assertions.assertEquals(kotlinCode, result.getContent());
    }

    @Test
    void testNonKotlinFilePassthrough() {
        String javaCode = "public class Test { }";
        TemplateOutput templateOutput = new TemplateOutput("Test.java", javaCode, OutputFormatType.JAVA.toString(), false);

        TemplateOutput result = formatter.format(templateOutput);

        Assertions.assertEquals(javaCode, result.getContent());
    }

    @Test
    void testNullMimeTypePassthrough() {
        String code = "some content";
        TemplateOutput templateOutput = new TemplateOutput("Test.txt", code, null, false);

        TemplateOutput result = formatter.format(templateOutput);

        Assertions.assertEquals(code, result.getContent());
    }

    @Test
    void testFormattingErrorWithHaltEnabled() {
        formatter.haltOnFailFormatting = true;
        String invalidKotlin = "invalid kotlin syntax {{{";
        TemplateOutput templateOutput = new TemplateOutput("Test.kt", invalidKotlin, OutputFormatType.KOTLIN.toString(), false);

        Assertions.assertThrows(RuntimeException.class, () -> {
            formatter.format(templateOutput);
        });
    }

    @Test
    void testFormattingErrorWithHaltDisabled() {
        formatter.haltOnFailFormatting = false;
        String invalidKotlin = "invalid kotlin syntax {{{";
        TemplateOutput templateOutput = new TemplateOutput("Test.kt", invalidKotlin, OutputFormatType.KOTLIN.toString(), false);

        TemplateOutput result = formatter.format(templateOutput);

        Assertions.assertEquals(invalidKotlin, result.getContent());
    }

    @Test
    void testFormatGeneratedProjectFiles() {
        String kotlinCode1 = "class Test1{fun hello(){println(\"Hello\")}}";
        String kotlinCode2 = "class Test2{fun world(){println(\"World\")}}";
        String javaCode = "public class Test3 { }";

        TemplateOutput kotlinOutput1 = new TemplateOutput("Test1.kt", kotlinCode1, OutputFormatType.KOTLIN.toString(), false);
        TemplateOutput kotlinOutput2 = new TemplateOutput("Test2.kt", kotlinCode2, OutputFormatType.KOTLIN.toString(), false);
        TemplateOutput javaOutput = new TemplateOutput("Test3.java", javaCode, OutputFormatType.JAVA.toString(), false);

        GeneratedProjectFiles projectFiles = new GeneratedProjectFiles();
        projectFiles.singleFiles.add(kotlinOutput1);
        projectFiles.singleFiles.add(kotlinOutput2);
        projectFiles.singleFiles.add(javaOutput);

        formatter.format(projectFiles);

        List<TemplateOutput> outputs = projectFiles.getAllTemplateOutputs();

        // Kotlin files should be formatted
        Assertions.assertNotEquals(kotlinCode1, outputs.get(0).getContent());
        Assertions.assertNotEquals(kotlinCode2, outputs.get(1).getContent());

        // Java file should remain unchanged
        Assertions.assertEquals(javaCode, outputs.get(2).getContent());
    }

    @Test
    void testPreserveTemplateOutputProperties() {
        String kotlinCode = "class Test{fun hello(){println(\"Hello\")}}";
        TemplateOutput templateOutput = new TemplateOutput("Test.kt", kotlinCode, OutputFormatType.KOTLIN.toString(), true);

        TemplateOutput result = formatter.format(templateOutput);

        Assertions.assertEquals("Test.kt", result.getTargetFile());
        Assertions.assertEquals(OutputFormatType.KOTLIN.toString(), result.getMimeType());
        Assertions.assertTrue(result.isSkipOverwrite());
    }
}
