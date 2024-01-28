package io.zenwave360.sdk.e2e;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class TextUtils {

    public static void replaceInFile(File file, String regex, String replacement) throws IOException {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        content = Pattern.compile(fixMultilineRegex(regex), Pattern.MULTILINE).matcher(content).replaceAll(replacement);
        FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
    }

    public static String fixMultilineRegex(String text) {
        return StringUtils.replace(text, "\r\n", "\\n").replace("\n", "\\r?\\n");
    }
}
