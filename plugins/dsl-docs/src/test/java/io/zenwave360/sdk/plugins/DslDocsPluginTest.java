package io.zenwave360.sdk.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.zenwave360.sdk.Main;
import io.zenwave360.sdk.utils.CliOutput;
import picocli.CommandLine;

class DslDocsPluginTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void resetStdout() {
        CliOutput.setStdout(null);
    }

    @Test
    void generatesAllPagesAndChecksThemThroughTheCli() throws Exception {
        Path orders = write("orders.zdl", """
                entity Order { id String required }
                event OrderPlaced { id String }
                """);
        Path payments = write("payments.zdl", """
                entity Payment { id String required }
                event PaymentCaptured { id String }
                """);
        Path openapi = write("openapi.yml", """
                openapi: 3.0.3
                info:
                  title: Orders API
                  version: 1.0.0
                paths: {}
                """);
        Path asyncapi = write("asyncapi.yml", """
                asyncapi: 2.6.0
                info:
                  title: Orders events
                  version: 1.0.0
                channels: {}
                """);
        Path flow = write("place-order.zfl", """
                systems {
                    Orders { service OrdersService { commands: placeOrder } }
                }
                flow PlaceOrder {
                    start PlaceOrder { id String }
                    when PlaceOrder do placeOrder { service Orders.OrdersService emits OrderPlaced }
                    end { completed: OrderPlaced }
                }
                """);
        Path docs = tempDir.resolve("docs");

        Execution generated = run("zdlFiles=" + path(orders) + "," + path(payments), "zflFiles=" + path(flow),
                "apiFiles=" + path(openapi) + "," + path(asyncapi), "targetFolder=" + path(docs),
                "title=Arcadia Editions", "version=2026.10", "date=2026-10-17");

        assertEquals(0, generated.exitCode(), generated.stderr());
        assertTrue(generated.stdout().contains("Wrote"));
        assertTrue(Files.exists(docs.resolve("README.md")));
        assertTrue(Files.exists(docs.resolve("architecture.md")));
        assertTrue(Files.exists(docs.resolve("modules/orders.md")));
        assertTrue(Files.exists(docs.resolve("modules/payments.md")));
        assertTrue(Files.exists(docs.resolve("flows.md")));
        assertTrue(Files.exists(docs.resolve("flows/place-order.md")));
        assertTrue(Files.exists(docs.resolve("apis.md")));
        assertTrue(Files.exists(docs.resolve("events.md")));
        assertTrue(Files.readString(docs.resolve("architecture.md")).contains("```mermaid"));

        Execution checked = run("zdlFiles=" + path(orders) + "," + path(payments), "zflFiles=" + path(flow),
                "apiFiles=" + path(openapi) + "," + path(asyncapi), "targetFolder=" + path(docs),
                "title=Arcadia Editions", "version=2026.10", "date=2026-10-17", "check=true");
        assertEquals(0, checked.exitCode(), checked.stderr());
        assertTrue(checked.stdout().contains("Up to date:"));

        Files.writeString(orders, "entity Order { id String required, number String }");
        Execution stale = run("zdlFiles=" + path(orders) + "," + path(payments), "zflFiles=" + path(flow),
                "apiFiles=" + path(openapi) + "," + path(asyncapi), "targetFolder=" + path(docs), "check=true");
        assertEquals(1, stale.exitCode());
        assertTrue(stale.stdout().contains("OUT OF DATE"));
        assertTrue(stale.stderr().contains("Documentation is out of date"));
    }

    @Test
    void deletesStaleGeneratedPagesButPreservesHandWrittenMarkdown() throws Exception {
        Path orders = write("orders.zdl", "entity Order { id String required }");
        Path payments = write("payments.zdl", "entity Payment { id String required }");
        Path docs = tempDir.resolve("docs");

        assertEquals(0, run("zdlFiles=" + path(orders) + "," + path(payments), "targetFolder=" + path(docs)).exitCode());
        Path handwritten = docs.resolve("notes.md");
        Files.writeString(handwritten, "# Notes\n");

        Execution regenerated = run("zdlFile=" + path(orders), "targetFolder=" + path(docs));
        assertEquals(0, regenerated.exitCode(), regenerated.stderr());
        assertTrue(Files.exists(docs.resolve("domain.md")));
        assertTrue(Files.notExists(docs.resolve("modules/orders.md")));
        assertEquals("# Notes\n", Files.readString(handwritten));
    }

    @Test
    void mapsInputAndOwnershipFailuresToFindings() throws Exception {
        Execution noInput = run();
        assertEquals(1, noInput.exitCode());
        assertTrue(noInput.stderr().contains("At least one input file is required"));

        Execution missing = run("zdlFile=" + path(tempDir.resolve("missing.zdl")));
        assertEquals(1, missing.exitCode());
        assertTrue(missing.stderr().contains("Files not found"));

        Path unknown = write("unknown.yml", "title: not an API");
        Execution invalid = run("apiFile=" + path(unknown));
        assertEquals(1, invalid.exitCode());
        assertTrue(invalid.stderr().contains("Not an OpenAPI or AsyncAPI"));

        Path orders = write("orders.zdl", "entity Order { id String required }");
        Path docs = tempDir.resolve("handwritten-docs");
        Files.createDirectories(docs);
        Path readme = docs.resolve("README.md");
        Files.writeString(readme, "# Hand-written\n");
        Execution collision = run("zdlFile=" + path(orders), "targetFolder=" + path(docs));
        assertEquals(1, collision.exitCode());
        assertTrue(collision.stderr().contains("Not overwriting files"));
        assertEquals("# Hand-written\n", Files.readString(readme));
    }

    private Path write(String name, String content) throws Exception {
        return Files.writeString(tempDir.resolve(name), content, StandardCharsets.UTF_8);
    }

    private Execution run(String... options) {
        var args = new java.util.ArrayList<>(List.of("-p", "DslDocs"));
        args.addAll(List.of(options));
        var stdout = new ByteArrayOutputStream();
        var stderr = new ByteArrayOutputStream();
        CliOutput.setStdout(new PrintStream(stdout, true, StandardCharsets.UTF_8));
        CommandLine command = Main.createCommandLine(new Main());
        command.setErr(new PrintWriter(stderr, true, StandardCharsets.UTF_8));
        int exitCode = command.execute(args.toArray(String[]::new));
        return new Execution(exitCode, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private static String path(Path path) {
        return path.toString().replace('\\', '/');
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
