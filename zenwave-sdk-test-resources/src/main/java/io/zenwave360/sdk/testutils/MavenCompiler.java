package io.zenwave360.sdk.testutils;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

public class MavenCompiler {

    public static int compile(File baseDir, String... properties) throws MavenInvocationException, IOException {
        return compile("pom.xml", baseDir, properties);
    }

    public static int compile(String pom, File baseDir, String... properties) throws MavenInvocationException, IOException {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFileName(pom.endsWith("/pom.xml") ? pom : pom + "/pom.xml");
        if(properties != null) {
            Properties props = new Properties();
            for (String property : properties) {
                props.setProperty(property.split("=")[0], property.split("=")[1]);
            }
            request.setProperties(props);
        }
        request.setBaseDirectory(baseDir);
        request.setGoals(Collections.singletonList("test-compile"));

        Invoker invoker = new DefaultInvoker();
        var results = invoker.execute(request);
        return results.getExitCode();
    }

    public static int copyPomAndCompile(String pom, String baseDir, String... properties) throws MavenInvocationException, IOException {
        System.out.println("Maven Invoker - compile:" + pom + " - " + baseDir);
        FileUtils.copyFile(new File(pom), new File(baseDir, "pom.xml"));
        return compile(new File(baseDir), properties);
    }
}
