package io.zenwave360.sdk.parsers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.zenwave360.sdk.utils.JSONPath;

public class ZDLParserTest {

    @TempDir
    Path tempDir;

    private File getClasspathResourceAsFile(String resource) throws URISyntaxException {
        return new File(getClass().getClassLoader().getResource(resource).toURI());
    }

    @Test
    public void testParseZDL() throws URISyntaxException, IOException {
        String targetProperty = "model";
        ZDLParser parser = new ZDLParser().withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl").withTargetProperty(targetProperty);
        long startTime = System.currentTimeMillis();
        Map<String, Object> model = (Map) parser.parse().get(targetProperty);
        System.out.println("ZDLParser load time: " + (System.currentTimeMillis() - startTime));
        Assertions.assertNotNull(model);
        Assertions.assertEquals("String", JSONPath.get(model, "$.entities.Customer.fields.username.type"));
        Assertions.assertEquals("CustomerInput", JSONPath.get(model, "$.services.CustomerService.methods.createCustomer.parameter"));
    }

    @Test
    public void testParseZDLWithProblems() throws URISyntaxException, IOException {
        String targetProperty = "model";
        ZDLParser parser = new ZDLParser()
                .withZdlFile("classpath:io/zenwave360/sdk/resources/zdl/customer-address-problems.zdl")
                .withTargetProperty(targetProperty);
        parser.continueOnZdlError = false;

        try {
            Map<String, Object> model = (Map) parser.parse().get(targetProperty);
            Assertions.fail("ZDL Errors not detected");
        } catch (RuntimeException e) {
            Assertions.assertEquals(Parser.ParseProblemsException.class, e.getClass());
        }

    }

    @Test
    public void testParseJDL() throws URISyntaxException, IOException {
        String targetProperty = "model";
        ZDLParser parser = new ZDLParser().withZdlFile("classpath:io/zenwave360/sdk/resources/jdl/21-points.jh").withTargetProperty(targetProperty);
        long startTime = System.currentTimeMillis();
        Map<String, Object> model = (Map) parser.parse().get(targetProperty);
        System.out.println("ZDLParser load time: " + (System.currentTimeMillis() - startTime));
        Assertions.assertNotNull(model);
        Assertions.assertEquals("Integer", JSONPath.get(model, "$.entities.Points.fields.exercise.type"));
    }

    @Test
    public void loadsEveryReferencedApiIndependentlyAndPreservesMetadata() throws IOException {
        Files.writeString(tempDir.resolve("schemas.yml"), """
                Payment:
                  type: object
                  properties:
                    id:
                      type: integer
                """, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("payments.yml"), """
                asyncapi: 3.0.0
                info:
                  title: Payments API
                  version: 1.0.0
                channels: {}
                components:
                  schemas:
                    Payment:
                      $ref: './schemas.yml#/Payment'
                """, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("orders.yml"), """
                asyncapi: 3.0.0
                info:
                  title: Orders API
                  version: 1.0.0
                channels: {}
                """, StandardCharsets.UTF_8);
        Path zdlFile = tempDir.resolve("domain.zdl");
        Files.writeString(zdlFile, """
                config {}

                apis {
                    asyncapi(client) PaymentsAPI {
                        uri "payments.yml"
                    }
                    asyncapi(provider) OrdersAPI {
                        uri "orders.yml"
                    }
                    asyncapi(client) UndeclaredAPI {
                        uri ""
                    }
                }
                """, StandardCharsets.UTF_8);

        Map<String, Object> zdl = (Map<String, Object>) new ZDLParser()
                .withZdlFile(zdlFile.toString())
                .parse()
                .get("zdl");
        Map<String, Object> payments = JSONPath.get(zdl, "$.apis.PaymentsAPI");
        Map<String, Object> orders = JSONPath.get(zdl, "$.apis.OrdersAPI");
        Map<String, Object> undeclared = JSONPath.get(zdl, "$.apis.UndeclaredAPI");

        Assertions.assertEquals("client", payments.get("role"));
        Assertions.assertEquals("payments.yml", payments.get("uri"));
        Assertions.assertEquals("Payments API", JSONPath.get(ZDLParser.getReferencedApiModel(payments), "$.info.title"));
        Assertions.assertEquals(
                "integer",
                JSONPath.get(ZDLParser.getReferencedApiModel(payments), "$.components.schemas.Payment.properties.id.type"));
        Assertions.assertEquals("Orders API", JSONPath.get(ZDLParser.getReferencedApiModel(orders), "$.info.title"));
        Assertions.assertNotSame(
                ZDLParser.getReferencedApiModel(payments), ZDLParser.getReferencedApiModel(orders));
        Assertions.assertFalse(undeclared.containsKey(ZDLParser.REFERENCED_API_MODEL_PROPERTY));
    }

    @Test
    public void missingReferencedApisHonorContinueOnZdlError() throws IOException {
        String zdl = """
                apis {
                    asyncapi provider MissingApi "missing/asyncapi.yml"
                }
                """;

        Map<String, Object> tolerant = (Map<String, Object>) new ZDLParser()
                .withContent(zdl)
                .parse()
                .get("zdl");
        Assertions.assertNull(ZDLParser.getReferencedApiModel(JSONPath.get(tolerant, "$.apis.MissingApi")));

        ZDLParser strict = new ZDLParser().withContent(zdl);
        strict.continueOnZdlError = false;
        Assertions.assertThrows(IOException.class, strict::parse);
    }

    @Test
    public void loadsReferencedZdlApisWithTheZdlParser() throws IOException {
        Files.writeString(tempDir.resolve("payments.zdl"), """
                config {
                    basePackage "io.example.payments"
                }

                event PaymentAuthorized {
                    paymentId String
                    orderId String
                }
                """, StandardCharsets.UTF_8);
        Path zdlFile = tempDir.resolve("orders.zdl");
        Files.writeString(zdlFile, """
                config {}

                apis {
                    zdl client PaymentsZdl "payments.zdl"
                }
                """, StandardCharsets.UTF_8);

        Map<String, Object> zdl = (Map<String, Object>) new ZDLParser()
                .withZdlFile(zdlFile.toString())
                .parse()
                .get("zdl");
        Map<String, Object> api = JSONPath.get(zdl, "$.apis.PaymentsZdl");

        Assertions.assertEquals("zdl", api.get("type"));
        Assertions.assertEquals("client", api.get("role"));
        Map<String, Object> referencedZdl = ZDLParser.getReferencedZdlModel(api);
        Assertions.assertNotNull(referencedZdl, "zdl references must be parsed with the ZDL parser");
        Assertions.assertNull(ZDLParser.getReferencedApiModel(api), "zdl references are not YAML Models");
        Assertions.assertEquals("io.example.payments", JSONPath.get(referencedZdl, "$.config.basePackage"));
        Assertions.assertNotNull(JSONPath.get(referencedZdl, "$.events.PaymentAuthorized"));
    }

}
