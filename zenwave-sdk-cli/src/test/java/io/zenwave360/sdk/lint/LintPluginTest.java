package io.zenwave360.sdk.lint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zenwave360.sdk.Main;
import io.zenwave360.sdk.utils.CliOutput;
import picocli.CommandLine;

class LintPluginTest {

    public static class AcmeLinter implements Linter {
        @Override
        public void lint(LintContext context) {
            context.report(LintDiagnostic.Severity.warning, "acme.custom", "Custom finding", null);
        }
    }

    @TempDir
    Path tempDir;

    private final ObjectMapper json = new ObjectMapper();

    @AfterEach
    void resetStdout() {
        CliOutput.setStdout(null);
    }

    @Test
    void jsonReportHasStableEnvelopeAndOneBasedSemanticLocation() throws Exception {
        Path model = write("broken.zdl", """
                entity Customer {
                    name Strin
                }
                """);

        Execution execution = lint(model, "format=json");

        assertEquals(1, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode report = json.readTree(execution.stdout());
        assertEquals("1", report.path("schemaVersion").asText());
        assertEquals("zenwave-lint", report.path("tool").path("name").asText());
        assertEquals(1, report.path("summary").path("files").asInt());
        assertEquals(1, report.path("summary").path("errors").asInt());
        JsonNode diagnostic = report.path("diagnostics").get(0);
        assertEquals("broken.zdl", diagnostic.path("file").asText());
        assertEquals(2, diagnostic.path("line").asInt());
        assertEquals(10, diagnostic.path("column").asInt());
        assertEquals("zdl.unknown-type", diagnostic.path("ruleId").asText());
        assertEquals("entities.Customer.fields.name.type", diagnostic.path("modelPath").asText());
    }

    @Test
    void syntaxErrorsDoNotCascade() throws Exception {
        Path model = write("syntax.zdl", "entity Broken { value ??? }");

        Execution execution = lint(model, "format=json");

        assertEquals(1, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode diagnostics = json.readTree(execution.stdout()).path("diagnostics");
        assertEquals(1, diagnostics.size());
        assertEquals("zdl.syntax", diagnostics.get(0).path("ruleId").asText());
        assertTrue(diagnostics.get(0).path("column").asInt() >= 1);
    }

    @Test
    void cleanSarifHasEmptyResultsAndSourceRoot() throws Exception {
        Path model = write("valid.zdl", "entity Customer { name String }");

        Execution execution = lint(model, "format=sarif");

        assertEquals(0, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode report = json.readTree(execution.stdout());
        assertEquals("2.1.0", report.path("version").asText());
        JsonNode run = report.path("runs").get(0);
        assertTrue(run.path("results").isArray());
        assertEquals(0, run.path("results").size());
        assertTrue(run.path("originalUriBaseIds").has("%SRCROOT%"));
        assertTrue(run.path("invocations").get(0).path("executionSuccessful").asBoolean());
    }

    @Test
    void cleanJsonHasEmptyDiagnosticsArray() throws Exception {
        Path model = write("valid.zdl", "entity Customer { name String }");

        Execution execution = lint(model, "format=json");

        assertEquals(0, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode report = json.readTree(execution.stdout());
        assertEquals("1", report.path("schemaVersion").asText());
        assertTrue(report.path("diagnostics").isArray());
        assertEquals(0, report.path("diagnostics").size());
        assertEquals(0, report.path("summary").path("errors").asInt());
        assertFalse(report.has("toolFailures"));
    }

    @Test
    void outputIsUtf8AndLeavesStdoutEmpty() throws Exception {
        Path model = write("cliente-ñ.zdl", "entity Cliente { nombre TipoInvalido }");
        Path output = tempDir.resolve("report.json");

        Execution execution = lint(model, "format=json", "output=" + path(output));

        assertEquals(1, execution.exitCode(), execution.stderr() + execution.stdout());
        assertEquals("", execution.stdout());
        byte[] bytes = Files.readAllBytes(output);
        assertFalse(bytes.length >= 2 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xfe);
        JsonNode report = json.readTree(bytes);
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("cliente-ñ.zdl"), new String(bytes, StandardCharsets.UTF_8));
        assertEquals("zdl.unknown-type", report.path("diagnostics").get(0).path("ruleId").asText());
    }

    @Test
    void strictTurnsWarningsIntoFailingFindings() throws Exception {
        Path model = write("warning.zdl", """
                @asyncapi({channel: "payment-authorized"})
                event PaymentAuthorized { paymentId String }

                input ConfirmPayment { orderId String required }

                service PaymentService for (Payment) {
                    @listener({event: PaymentAuthorized})
                    confirmPayment(ConfirmPayment)
                }

                @aggregate entity Payment { paymentId String }
                """);

        Execution normal = lint(model, "format=json");
        Execution strict = lint(model, "format=json", "strict=true");

        assertEquals(0, normal.exitCode(), normal.stderr() + normal.stdout());
        assertEquals(1, strict.exitCode(), strict.stderr() + strict.stdout());
        JsonNode diagnostics = json.readTree(normal.stdout()).path("diagnostics");
        assertTrue(diagnostics.findValuesAsText("ruleId").contains("zdl.mixed-event-binding"));
        assertTrue(diagnostics.findValuesAsText("ruleId").contains("zdl.data-lineage"));
    }

    @Test
    void unresolvedZdlClientIsAnError() throws Exception {
        Path model = write("client.zdl", "apis { zdl client MissingApi \"missing.zdl\" }");

        Execution execution = lint(model, "format=json");

        assertEquals(1, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode diagnostic = json.readTree(execution.stdout()).path("diagnostics").get(0);
        assertEquals("zdl.reference-unresolved", diagnostic.path("ruleId").asText());
    }

    @Test
    void invalidTransitionStateUsesDedicatedRule() throws Exception {
        Path model = write("transition.zdl", """
                @aggregate
                @lifecycle(field: status, initial: DRAFT)
                entity Order { status OrderStatus required }
                enum OrderStatus { DRAFT, PLACED }
                input PlaceOrder { id String }
                event OrderPlaced { id String }
                aggregate OrderAggregate (Order) {
                    @transition(from: DRAFT, to: MISSING)
                    place(PlaceOrder) withEvents OrderPlaced
                }
                """);

        Execution execution = lint(model, "format=json");

        assertEquals(1, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode transition = null;
        for (JsonNode diagnostic : json.readTree(execution.stdout()).path("diagnostics")) {
            if ("zdl.transition-state".equals(diagnostic.path("ruleId").asText())) transition = diagnostic;
        }
        assertTrue(transition != null, execution.stdout());
        // the parser gives this problem no location; it is located through its model path, not reported at 1:1
        assertTrue(transition.path("line").asInt() >= 8, transition.toString());
    }

    @Test
    void missingReferencedListenerEventAndCallTargetAreErrors() throws Exception {
        write("inventory.zdl", """
                @aggregate entity Inventory { id String }
                input ReserveStock { id String }
                service InventoryService for (Inventory) { reserve(ReserveStock) }
                event StockReserved { id String }
                """);
        Path model = write("orders.zdl", """
                apis { zdl client InventoryApi "inventory.zdl" }
                @aggregate entity Order { id String }
                input PlaceOrder { id String }
                service OrderService for (Order) {
                    @listener(zdl: InventoryApi, event: MissingEvent)
                    @calls(zdl: InventoryApi, command: InventoryService.missing)
                    place(PlaceOrder)
                }
                """);

        Execution execution = lint(model, "format=json");

        assertEquals(1, execution.exitCode(), execution.stderr() + execution.stdout());
        var rules = json.readTree(execution.stdout()).path("diagnostics").findValuesAsText("ruleId");
        assertTrue(rules.contains("zdl.listener-event"), rules.toString());
        assertTrue(rules.contains("zdl.calls-command"), rules.toString());
    }

    @Test
    void customLinterIsLoadedByClassName() throws Exception {
        Path model = write("valid.zdl", "entity Customer { name String }");

        Execution execution = lint(model, "format=json", "linters=" + AcmeLinter.class.getName());

        assertEquals(0, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode diagnostics = json.readTree(execution.stdout()).path("diagnostics");
        assertEquals(1, diagnostics.size());
        assertEquals("acme.custom", diagnostics.get(0).path("ruleId").asText());
        assertEquals("warning", diagnostics.get(0).path("severity").asText());
    }

    @Test
    void builtInLinterResolvesByShortName() throws Exception {
        Path model = write("broken.zdl", """
                entity Customer {
                    name Strin
                }
                """);

        Execution execution = lint(model, "format=json", "linters=ZdlProblems");

        assertEquals(1, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode diagnostics = json.readTree(execution.stdout()).path("diagnostics");
        assertEquals("zdl.unknown-type", diagnostics.get(0).path("ruleId").asText());
    }

    @Test
    void unknownLinterIsToolFailureInJson() throws Exception {
        Path model = write("valid.zdl", "entity Customer { name String }");

        Execution execution = lint(model, "format=json", "linters=com.acme.DoesNotExist");

        assertEquals(2, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode failures = json.readTree(execution.stdout()).path("toolFailures");
        assertTrue(failures.isArray());
        assertEquals(1, failures.size());
        assertTrue(failures.get(0).asText().contains("com.acme.DoesNotExist"));
    }

    @Test
    void unknownLinterIsUnsuccessfulSarifInvocation() throws Exception {
        Path model = write("valid.zdl", "entity Customer { name String }");

        Execution execution = lint(model, "format=sarif", "linters=com.acme.DoesNotExist");

        assertEquals(2, execution.exitCode(), execution.stderr() + execution.stdout());
        JsonNode invocation = json.readTree(execution.stdout()).path("runs").get(0).path("invocations").get(0);
        assertFalse(invocation.path("executionSuccessful").asBoolean());
        assertEquals(2, invocation.path("exitCode").asInt());
    }

    @Test
    void textReportUsesColonSeparatedFields() throws Exception {
        Path model = write("broken.zdl", """
                entity Customer {
                    name Strin
                }
                """);

        Execution execution = lint(model);

        assertEquals(1, execution.exitCode(), execution.stderr() + execution.stdout());
        assertTrue(execution.stdout().contains("broken.zdl:2:10: error zdl.unknown-type "), execution.stdout());
        assertTrue(execution.stdout().contains("Strin is not a valid type"), execution.stdout());
    }

    @Test
    void missingZdlFileIsToolFailure() {
        Execution execution = lint(tempDir.resolve("missing.zdl"), "format=json");

        assertEquals(2, execution.exitCode(), execution.stderr() + execution.stdout());
    }

    private Path write(String name, String content) throws Exception {
        return Files.writeString(tempDir.resolve(name), content, StandardCharsets.UTF_8);
    }

    private static String path(Path path) {
        return path.toString().replace('\\', '/');
    }

    private Execution lint(Path zdlFile, String... options) {
        var args = new ArrayList<>(List.of("-p", "Lint", "zdlFile=" + path(zdlFile), "root=" + path(tempDir)));
        args.addAll(List.of(options));
        var stdout = new ByteArrayOutputStream();
        var stderr = new ByteArrayOutputStream();
        CliOutput.setStdout(new PrintStream(stdout, true, StandardCharsets.UTF_8));
        CommandLine command = Main.createCommandLine(new Main());
        command.setErr(new PrintWriter(stderr, true, StandardCharsets.UTF_8));
        int exitCode = command.execute(args.toArray(String[]::new));
        return new Execution(exitCode, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Execution(int exitCode, String stdout, String stderr) {
    }
}
