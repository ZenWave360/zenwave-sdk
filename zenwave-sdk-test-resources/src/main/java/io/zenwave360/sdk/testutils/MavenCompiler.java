package io.zenwave360.sdk.testutils;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

public class MavenCompiler {

    public static int compile(String pom, String baseDir, String... properties) throws MavenInvocationException, IOException {
        System.out.println("Maven Invoker - compile:" + pom + " - " + baseDir);
        FileUtils.copyFile(new File(pom), new File(baseDir, "pom.xml"));

        InvocationRequest request = new DefaultInvocationRequest();
        if(properties != null) {
            Properties props = new Properties();
            for (String property : properties) {
                props.setProperty(property.split("=")[0], property.split("=")[1]);
            }
            request.setProperties(props);
        }
        request.setBaseDirectory(new File(baseDir));
        request.setGoals(Collections.singletonList("test-compile"));

        Invoker invoker = new DefaultInvoker();
        var results = invoker.execute(request);
        return results.getExitCode();
    }
}
