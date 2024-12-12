package io.zenwave360.sdk.utils;

import java.util.regex.Pattern;

public class AntStyleMatcher {

    public static boolean match(String pattern, String filePath) {
        // Convert Ant-style pattern to regex
        String regex = pattern
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .replace("?", ".");
        return Pattern.matches(regex, filePath);
    }
}
