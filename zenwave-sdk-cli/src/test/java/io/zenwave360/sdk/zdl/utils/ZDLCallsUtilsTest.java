package io.zenwave360.sdk.zdl.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;

class ZDLCallsUtilsTest {

    @TempDir
    Path tempDir;

    private static final String INVENTORY_ZDL = """
            config { basePackage "io.example.inventory" }

            input ReserveStockInput { orderId String }
            output StockReservedResult { reservationId String }
            event OrderCreated { orderId String }
            event StockUnavailable { orderId String }

            service InventoryService for (Inventory) {
                reserveStock(ReserveStockInput) StockReservedResult withEvents [OrderCreated | StockUnavailable]
                releaseStock(ReserveStockInput)
            }

            @aggregate
            entity Inventory { id Long }
            """;

    private Map<String, Object> parse(String ordersZdl) throws IOException {
        Files.writeString(tempDir.resolve("inventory.zdl"), INVENTORY_ZDL, StandardCharsets.UTF_8);
        Path zdlFile = tempDir.resolve("orders.zdl");
        Files.writeString(zdlFile, ordersZdl, StandardCharsets.UTF_8);
        var contextModel = new ZDLParser().withZdlFile(zdlFile.toString()).parse();
        return (Map<String, Object>) new ZDLProcessor().process(contextModel).get("zdl");
    }

    @Test
    void resolvesQualifiedCrossModuleCommand() throws IOException {
        var zdl = parse(ordersWith("""
                @calls(zdl: CatalogInventoryZdl, command: InventoryService.reserveStock)
                placeOrder(PlaceOrderInput) Order
                """));

        Map<String, Object> call = JSONPath.get(zdl, "$.services.OrdersService.methods.placeOrder.calls[0]");
        Assertions.assertEquals("CatalogInventoryZdl", call.get("apiName"));
        Assertions.assertEquals("InventoryService", call.get("serviceName"));
        Assertions.assertEquals("inventoryService", call.get("serviceInstanceName"));
        Assertions.assertEquals("reserveStock", call.get("commandName"));
        Assertions.assertEquals("ReserveStockInput", call.get("parameterType"));
        Assertions.assertEquals("StockReservedResult", call.get("returnType"));
        Assertions.assertEquals(List.of("OrderCreated", "StockUnavailable"), call.get("withEvents"));
    }

    @Test
    void resolvesUnqualifiedSameZdlCommandWhenUnique() throws IOException {
        var zdl = parse("""
                config { basePackage "io.example.orders" }

                input PlaceOrderInput { orderId String }
                output PricingResult { amount BigDecimal }

                service PricingService for (Order) {
                    calculatePrice(PlaceOrderInput) PricingResult
                }

                service OrdersService for (Order) {
                    @calls(command: calculatePrice)
                    placeOrder(PlaceOrderInput) Order
                }

                @aggregate
                entity Order { orderId String }
                """);

        Map<String, Object> call = JSONPath.get(zdl, "$.services.OrdersService.methods.placeOrder.calls[0]");
        Assertions.assertNull(call.get("apiName"));
        Assertions.assertEquals("PricingService", call.get("serviceName"));
        Assertions.assertEquals("calculatePrice", call.get("commandName"));
        Assertions.assertEquals("PricingResult", call.get("returnType"));
    }

    @Test
    void failsClearlyForMissingApiWrongApiTypeAndUnknownCommand() throws IOException {
        var missingApi = Assertions.assertThrows(IllegalArgumentException.class, () -> parse(ordersWith("""
                @calls(zdl: MissingZdl, command: InventoryService.reserveStock)
                placeOrder(PlaceOrderInput) Order
                """)));
        Assertions.assertTrue(missingApi.getMessage().contains("undeclared zdl api: MissingZdl"));

        var valid = parse(ordersWith("""
                @calls(zdl: CatalogInventoryZdl, command: InventoryService.reserveStock)
                placeOrder(PlaceOrderInput) Order
                """));
        JSONPath.set(valid, "$.apis.CatalogInventoryZdl.type", "asyncapi");
        Map<String, Object> method = JSONPath.get(valid, "$.services.OrdersService.methods.placeOrder");
        var wrongType = Assertions.assertThrows(IllegalArgumentException.class,
                () -> ZDLCallsUtils.methodCalls(valid, method));
        Assertions.assertTrue(wrongType.getMessage().contains("of type 'asyncapi'"));
        Assertions.assertTrue(wrongType.getMessage().contains("must be a zdl api"));

        var unknownCommand = Assertions.assertThrows(IllegalArgumentException.class, () -> parse(ordersWith("""
                @calls(zdl: CatalogInventoryZdl, command: InventoryService.notACommand)
                placeOrder(PlaceOrderInput) Order
                """)));
        Assertions.assertTrue(unknownCommand.getMessage().contains("unknown command 'InventoryService.notACommand'"));
        Assertions.assertTrue(unknownCommand.getMessage().contains("zdl api 'CatalogInventoryZdl'"));
    }

    @Test
    void preservesRepeatedCallsOrder() throws IOException {
        var zdl = parse(ordersWith("""
                @calls(zdl: CatalogInventoryZdl, command: InventoryService.reserveStock)
                @calls(zdl: CatalogInventoryZdl, command: InventoryService.releaseStock)
                placeOrder(PlaceOrderInput) Order
                """));

        List<Map<String, Object>> calls = JSONPath.get(zdl,
                "$.services.OrdersService.methods.placeOrder.calls");
        Assertions.assertEquals(List.of("reserveStock", "releaseStock"),
                calls.stream().map(call -> call.get("commandName")).toList());
    }

    private String ordersWith(String methodDeclaration) {
        return """
                config { basePackage "io.example.orders" }

                apis { zdl client CatalogInventoryZdl "inventory.zdl" }

                input PlaceOrderInput { orderId String }

                service OrdersService for (Order) {
                %s
                }

                @aggregate
                entity Order { orderId String }
                """.formatted(methodDeclaration.indent(4));
    }
}
