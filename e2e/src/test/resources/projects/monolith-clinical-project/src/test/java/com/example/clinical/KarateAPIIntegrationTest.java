package com.example.clinical;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import com.intuit.karate.RuntimeHook;
import com.intuit.karate.StringUtils;
import com.intuit.karate.cli.IdeMain;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.http.HttpRequest;
import com.intuit.karate.http.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
//@DockerComposeInitializer.EnableDockerCompose
class KarateAPIIntegrationTest {

    private String classpath = "classpath:" + getClass().getPackageName().replace(".", "/") + "/";

    @LocalServerPort
    private int port;

    @Test
    void run() throws Exception {
        System.setProperty("karate.server.port", String.valueOf(port));

        String karateEnv = defaultString(System.getProperty("karate.env"), "local").toLowerCase();
        String launchCommand = defaultString(System.getProperty("KARATE_OPTIONS"), "-t ~@ignore " + classpath);

        com.intuit.karate.Main options = IdeMain.parseIdeCommandLine(launchCommand);

        Results results = Runner.path(Optional.ofNullable(options.getPaths()).orElse(Arrays.asList(classpath)))
                .hook(coverageRuntimeHook)
                .tags(options.getTags())
                .configDir(options.getConfigDir())
                .karateEnv(karateEnv)
                .outputHtmlReport(true)
                .outputCucumberJson(true)
                .outputJunitXml(true)
                .parallel(options.getThreads());

        moveJUnitReports(results.getReportDir(), "target/surefire-reports");

        // here you can analyze/process coverage
        System.out.println("SUCCESS ENDPOINTS");
        System.out.println(StringUtils.join(httpCalls, "\n"));
        System.out.println("FAILED ENDPOINTS");
        System.out.println(StringUtils.join(failedHttpCalls, "\n"));

        Assertions.assertEquals(0, results.getFailCount());
    }
    
    public static void moveJUnitReports(String karateReportDir, String surefireReportDir) throws IOException {
        new File(surefireReportDir).mkdirs();
        Collection<File> xmlFiles = Files.find(Paths.get(karateReportDir), Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().endsWith(".xml"))
                .map(p -> p.toFile()).collect(Collectors.toList());

        xmlFiles.forEach((x) -> {
            try {
                Files.copy(x.toPath(), Paths.get(surefireReportDir, "/TEST-" + x.getName()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException var3) {
                var3.printStackTrace();
            }

        });
    }

    List<String> httpCalls = new ArrayList<>();
    List<String> failedHttpCalls = new ArrayList<>();
    private RuntimeHook coverageRuntimeHook = new RuntimeHook() {

        List<String> scenarioHttpCalls = null;

        @Override
        public boolean beforeScenario(ScenarioRuntime sr) {
            scenarioHttpCalls = new ArrayList<>();
            return true;
        }

        @Override
        public void afterHttpCall(HttpRequest request, Response response, ScenarioRuntime sr) {
            scenarioHttpCalls.add(String.format("%s %s %s", request.getMethod(), request.getUrl(), response.getStatus()));
        }

        @Override
        public void afterScenario(ScenarioRuntime sr) {
            (sr.isFailed()? failedHttpCalls : httpCalls).addAll(scenarioHttpCalls);
        }
    };

    private String defaultString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

}
