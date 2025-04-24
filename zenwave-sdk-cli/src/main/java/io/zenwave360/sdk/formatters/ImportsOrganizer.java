package io.zenwave360.sdk.formatters;

import com.palantir.javaformat.java.FormatterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportsOrganizer {
    private static final Logger log = LoggerFactory.getLogger(ImportsOrganizer.class);
    private static boolean warnFixImports = true;

    public static String organizeImports(String source) {
        try {
            return PalantirImportsOrganizer.organizeImports(source);
        } catch (Throwable e) {
            if(warnFixImports) {
                warnFixImports = false;
                log.warn("Failed to fix imports due to restricted access to jdk.compiler/com.sun.tools internal apis");
            }
            return source;
        }
    }

    public static String formatSourceAndOrganizeImports(String source) throws FormatterException {
        try {
            return PalantirImportsOrganizer.formatSourceAndOrganizeImports(source);
        } catch (FormatterException e) {
            throw e;
        } catch (Throwable e) {
            if(warnFixImports) {
                warnFixImports = false;
                log.warn("Failed to fix imports due to restricted access to jdk.compiler/com.sun.tools internal apis");
            }
            return source;
        }
    }

}
