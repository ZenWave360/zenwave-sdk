package io.zenwave360.sdk.e2e;

import io.zenwave360.sdk.MainGenerator;
import io.zenwave360.sdk.plugins.AsyncAPIOpsGeneratorPlugin;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestAsyncAPIOpsTerraformKafkaE2E {

    private static final String ASYNCAPI_PROVIDER = "classpath:retail-domain-catalog/merchandising/inventory/inventory-adjustment/asyncapi.yml";
    private static final String COMPOSE_PROJECT = "asyncapi-ops-terraform-kafka-e2e";
    private static final String DOCKER_COMPOSE_FILE = "docker-compose.yml";

    @Test
    public void testTerraformKafkaTemplatesAgainstLocalKafkaAndSchemaRegistry() throws Exception {
        String sourceFolder = "src/test/resources/projects/asyncapi-ops-terraform-kafka";
        String targetFolder = "target/projects/asyncapi-ops-terraform-kafka";
        String terraformFolder = targetFolder + "/terraform";
        File targetDirectory = new File(targetFolder);
        File terraformDirectory = new File(terraformFolder);

        FileUtils.deleteDirectory(targetDirectory);
        FileUtils.forceMkdir(targetDirectory);
        FileUtils.copyDirectory(new File(sourceFolder), targetDirectory);

        boolean terraformInitialized = false;
        try {
            runCommand(targetDirectory, Duration.ofMinutes(2), dockerCompose("down", "-v", "--remove-orphans"));
            runCommand(targetDirectory, Duration.ofMinutes(3), dockerCompose("up", "-d"));

            waitForKafka("localhost", 9092, Duration.ofMinutes(2));
            waitForHttpOk("http://localhost:8081/subjects", Duration.ofMinutes(2));
            waitForComposeServices(targetDirectory, List.of("kafka", "schema-registry"), Duration.ofMinutes(1));

            new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                    .withApiFile(ASYNCAPI_PROVIDER)
                    .withOption("server", "dev")
                    .withOption("templates", "TerraformKafka")
                    .withTargetFolder(terraformFolder)
                    .withOption("skipFormatting", true));

            String versions = Files.readString(Path.of(terraformFolder, "versions.tf"));
            Assertions.assertTrue(versions.contains("Mongey/kafka"));
            Assertions.assertTrue(versions.contains("cultureamp/schemaregistry"));

            runCommand(terraformDirectory, Duration.ofMinutes(5), terraform("init", "-input=false", "-no-color"));
            terraformInitialized = true;

            runCommand(terraformDirectory, Duration.ofMinutes(5), terraform("plan", "-input=false", "-no-color", "-out=tfplan"));
            runCommand(terraformDirectory, Duration.ofMinutes(5), terraform("apply", "-input=false", "-no-color", "-auto-approve", "tfplan"));

            CommandResult stateList = runCommand(terraformDirectory, Duration.ofMinutes(2), terraform("state", "list"));
            Assertions.assertTrue(stateList.output.contains("kafka_topic."));
            Assertions.assertTrue(stateList.output.contains("schemaregistry_schema."));
            Assertions.assertTrue(stateList.output.contains("kafka_acl."));

            CommandResult topicList = runCommand(targetDirectory, Duration.ofMinutes(2),
                    dockerCompose("exec", "-T", "kafka", "kafka-topics", "--bootstrap-server", "kafka:29092", "--list"));
            Assertions.assertTrue(topicList.output.contains("merchandising.inventory.inventory-adjustment.reserve-stock.command.avro.v0"));

            String subjects = httpGet("http://localhost:8081/subjects");
            Assertions.assertTrue(subjects.contains("ReserveStockCommand-value"));
        } finally {
            if (terraformInitialized) {
                try {
                    runCommand(terraformDirectory, Duration.ofMinutes(5),
                            terraform("destroy", "-input=false", "-no-color", "-auto-approve"));
                } catch (Exception ignored) {
                }
            }
            try {
                runCommand(targetDirectory, Duration.ofMinutes(2), dockerCompose("down", "-v", "--remove-orphans"));
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void testTerraformKafkaDefaultingAgainstLocalKafkaAndSchemaRegistry() throws Exception {
        String sourceFolder = "src/test/resources/projects/asyncapi-ops-terraform-kafka";
        String targetFolder = "target/projects/asyncapi-ops-terraform-kafka-defaulting";
        String terraformFolder = targetFolder + "/terraform";
        File targetDirectory = new File(targetFolder);
        File terraformDirectory = new File(terraformFolder);

        FileUtils.deleteDirectory(targetDirectory);
        FileUtils.forceMkdir(targetDirectory);
        FileUtils.copyDirectory(new File(sourceFolder), targetDirectory);

        boolean terraformInitialized = false;
        try {
            runCommand(targetDirectory, Duration.ofMinutes(2), dockerCompose("down", "-v", "--remove-orphans"));
            runCommand(targetDirectory, Duration.ofMinutes(3), dockerCompose("up", "-d"));

            waitForKafka("localhost", 9092, Duration.ofMinutes(2));
            waitForHttpOk("http://localhost:8081/subjects", Duration.ofMinutes(2));
            waitForComposeServices(targetDirectory, List.of("kafka", "schema-registry"), Duration.ofMinutes(1));

            new MainGenerator().generate(new AsyncAPIOpsGeneratorPlugin()
                    .withApiFile(new File(targetDirectory, "defaulting-asyncapi.yml").getAbsolutePath())
                    .withOption("templates", "TerraformKafka")
                    .withTargetFolder(terraformFolder)
                    .withOption("skipFormatting", true));

            String versions = Files.readString(Path.of(terraformFolder, "versions.tf"));
            Assertions.assertTrue(versions.contains("variable \"default_partitions\""));
            Assertions.assertTrue(versions.contains("variable \"default_replication_factor\""));
            Assertions.assertTrue(versions.contains("variable \"default_topic_config\""));

            String topics = Files.readString(Path.of(terraformFolder, "topics.tf"));
            Assertions.assertTrue(topics.contains("partitions         = var.default_partitions"));
            Assertions.assertTrue(topics.contains("replication_factor = coalesce(var.default_replication_factor, -1)"));
            Assertions.assertTrue(topics.contains("config = length(var.default_topic_config) > 0 ? var.default_topic_config : null"));

            runCommand(terraformDirectory, Duration.ofMinutes(5), terraform("init", "-input=false", "-no-color"));
            terraformInitialized = true;

            runCommand(terraformDirectory, Duration.ofMinutes(5), terraform("plan", "-input=false", "-no-color", "-out=tfplan"));
            runCommand(terraformDirectory, Duration.ofMinutes(5), terraform("apply", "-input=false", "-no-color", "-auto-approve", "tfplan"));

            CommandResult stateList = runCommand(terraformDirectory, Duration.ofMinutes(2), terraform("state", "list"));
            Assertions.assertTrue(stateList.output.contains("kafka_topic.sample_topic_v1"));

            CommandResult topicDescription = runCommand(targetDirectory, Duration.ofMinutes(2),
                    dockerCompose("exec", "-T", "kafka", "kafka-topics", "--bootstrap-server", "kafka:29092", "--describe", "--topic", "sample.topic.v1"));
            Assertions.assertTrue(topicDescription.output.contains("Topic: sample.topic.v1"));
            Assertions.assertTrue(topicDescription.output.contains("PartitionCount: 2"));
        } finally {
            if (terraformInitialized) {
                try {
                    runCommand(terraformDirectory, Duration.ofMinutes(5),
                            terraform("destroy", "-input=false", "-no-color", "-auto-approve"));
                } catch (Exception ignored) {
                }
            }
            try {
                runCommand(targetDirectory, Duration.ofMinutes(2), dockerCompose("down", "-v", "--remove-orphans"));
            } catch (Exception ignored) {
            }
        }
    }

    private static List<String> dockerCompose(String... args) {
        List<String> command = new ArrayList<>();
        String composeCommand = resolveDockerComposeCommand();
        command.add(composeCommand);
        if ("docker".equals(composeCommand)) {
            command.add("compose");
        }
        command.add("-f");
        command.add(DOCKER_COMPOSE_FILE);
        command.add("-p");
        command.add(COMPOSE_PROJECT);
        for (String arg : args) {
            command.add(arg);
        }
        return command;
    }

    private static List<String> terraform(String... args) {
        List<String> command = new ArrayList<>();
        command.add(resolveTerraformCommand());
        for (String arg : args) {
            command.add(arg);
        }
        return command;
    }

    private static String resolveDockerComposeCommand() {
        return Stream.of("docker-compose", "docker-compose.exe", "docker")
                .filter(TestAsyncAPIOpsTerraformKafkaE2E::isCommandAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Neither docker-compose nor docker is available"));
    }

    private static String resolveTerraformCommand() {
        String override = System.getenv("TERRAFORM_CMD");
        if (override != null && !override.isBlank() && isCommandAvailable(override)) {
            return override;
        }
        return Stream.of("terraform", "terraform.exe")
                .filter(TestAsyncAPIOpsTerraformKafkaE2E::isCommandAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Neither terraform nor terraform.exe is available"));
    }

    private static boolean isCommandAvailable(String command) {
        try {
            List<String> probe;
            if ("docker".equals(command)) {
                probe = List.of(command, "compose", "version");
            } else {
                probe = List.of(command, "version");
            }
            Process process = new ProcessBuilder(probe).redirectErrorStream(true).start();
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void waitForKafka(String host, int port, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        Exception lastException = null;
        while (Instant.now().isBefore(deadline)) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 1000);
                return;
            } catch (Exception e) {
                lastException = e;
                Thread.sleep(1000);
            }
        }
        throw new IllegalStateException("Kafka did not become reachable on " + host + ":" + port, lastException);
    }

    private static void waitForHttpOk(String url, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        Exception lastException = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                String body = httpGet(url);
                if (body != null) {
                    return;
                }
            } catch (Exception e) {
                lastException = e;
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException("HTTP endpoint did not become ready: " + url, lastException);
    }

    private static void waitForComposeServices(File workingDirectory, List<String> serviceNames, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        Exception lastException = null;
        while (Instant.now().isBefore(deadline)) {
            try {
                if (getDockerComposeRunningServices(workingDirectory).containsAll(serviceNames)) {
                    return;
                }
            } catch (Exception e) {
                lastException = e;
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException("Docker Compose services not running: " + serviceNames, lastException);
    }

    private static List<String> getDockerComposeRunningServices(File workingDirectory) throws Exception {
        CommandResult result = runCommand(workingDirectory, Duration.ofSeconds(30), dockerCompose("ps", "--services"));
        List<String> services = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(result.output.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    services.add(line.trim());
                }
            }
        }
        return services;
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(2000);
        connection.setReadTimeout(2000);
        connection.setRequestMethod("GET");
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("Unexpected HTTP status " + responseCode + " for " + url);
        }
        return new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static CommandResult runCommand(File workingDirectory, Duration timeout, List<String> command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory);
        builder.redirectErrorStream(true);

        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            } catch (Exception ignored) {
            }
        });
        outputReader.start();

        boolean finished = process.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Command timed out: " + String.join(" ", command));
        }
        outputReader.join();

        CommandResult result = new CommandResult(process.exitValue(), output.toString());
        if (result.exitCode != 0) {
            throw new IllegalStateException("Command failed (" + result.exitCode + "): " + String.join(" ", command)
                    + System.lineSeparator() + result.output);
        }
        return result;
    }

    private static class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }
}
