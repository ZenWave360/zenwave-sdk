package io.zenwave360.sdk.formatters;

import com.palantir.javaformat.java.Formatter;
import com.palantir.javaformat.java.FormatterException;
import com.palantir.javaformat.java.JavaFormatterOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PalantirImportsOrganizer {
    private static final Logger log = LoggerFactory.getLogger(PalantirImportsOrganizer.class);
    private static boolean warnFixImports = true;

    private static final Formatter formatter =
            Formatter.createFormatter(JavaFormatterOptions.builder().style(JavaFormatterOptions.Style.PALANTIR).build());


    public static String organizeImports(String source) {
        try {
            return formatter.fixImports(source);
        } catch (FormatterException e) {
            if(warnFixImports) {
                warnFixImports = false;
                log.warn("Failed to fix imports due to restricted access to jdk.compiler/com.sun.tools internal apis");
            }
            return source;
        }
    }

    public static String formatSourceAndOrganizeImports(String source) throws FormatterException{
        return formatter.formatSourceAndFixImports(source);
    }
}
