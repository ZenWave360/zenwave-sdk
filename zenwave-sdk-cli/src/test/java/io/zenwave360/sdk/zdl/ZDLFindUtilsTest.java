package io.zenwave360.sdk.zdl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;

public class ZDLFindUtilsTest {

    public static Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    @Test
    void testFindAllServiceFacingEntities() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findAllServiceFacingEntities(model);
        Collections.sort(entities);
        Assertions.assertEquals(List.of("AttachmentFile", "AttachmentFile", "AttachmentFileId", "AttachmentFileOutput", "OrderBusinessId", "OrderBusinessId", "OrderFaultType", "OrderStatus", "PurchaseOrder"), entities);
    }

    @Test
    void testFindAllPaginatedEntities() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findAllPaginatedEntities(model);
        Assertions.assertEquals(List.of("AttachmentFile"), entities);
    }

    @Test
    void testFindAllEntitiesReturnedAsList() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findAllEntitiesReturnedAsList(model);
        Assertions.assertEquals(List.of("AttachmentFile"), entities);
    }

    @Test
    void testFindMethodParameterAndReturnTypes() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findMethodParameterAndReturnTypes(model);
        Collections.sort(entities);
        Assertions.assertEquals(List.of("AttachmentFile", "AttachmentFileId", "AttachmentFileOutput", "OrderBusinessId", "PurchaseOrder"), entities);
    }

    @Test
    public void testFindDependentEntitiesMongodb() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl");
        var entities = ZDLFindUtils.findDependentEntities(model, "CustomerOrder");
        Assertions.assertEquals(List.of("CustomerOrder", "OrderStatus", "Customer", "OrderedItem", "PaymentDetails", "ShippingDetails"), entities);
    }

    @Test
    public void testFindDependentEntitiesMongodbZdl() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findDependentEntities(model, "PurchaseOrder");
        Assertions.assertEquals(List.of("PurchaseOrder", "OrderBusinessId", "OrderStatus", "AttachmentFile"), entities);
    }

    @Test
    public void testFindDependentEntitiesRelational() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl");
        var entities = ZDLFindUtils.findDependentEntities(model, "CustomerOrder");
        Assertions.assertEquals(List.of("CustomerOrder", "OrderStatus", "OrderShippingDetails", "OrderShippingDetails2", "OrderedItem"), entities);
    }

    @Test
    public void testFindServiceName() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var serviceName = ZDLFindUtils.findServiceName("PurchaseOrder", model);
        Assertions.assertEquals("AttachmentService", serviceName);

        serviceName = ZDLFindUtils.findServiceName("OrderBusinessId", model);
        Assertions.assertEquals("AttachmentService", serviceName);

        serviceName = ZDLFindUtils.findServiceName("AttachmentFileId", model);
        Assertions.assertEquals("AttachmentService", serviceName);
    }

    @Test
    public void testFindServiceMethod() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = ZDLFindUtils.findServiceMethod("uploadFile", model);
        Assertions.assertEquals("AttachmentService", method.get("serviceName"));
    }

    @Test
    public void isAggregateRoot() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        Assertions.assertTrue(ZDLFindUtils.isAggregateRoot(model, "CustomerOrder"));
        Assertions.assertFalse(ZDLFindUtils.isAggregateRoot(model, "Restaurant"));
    }

    @Test
    public void aggregateEvents() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/orders-with-aggregate.zdl");
        var aggregate = JSONPath.get(model, "$.aggregates.CustomerOrderAggregate", Map.<String, Object>of());
        var events = ZDLFindUtils.aggregateEvents(aggregate);
        Assertions.assertEquals(Set.of("OrderEvent", "OrderStatusUpdated"), events);
    }

    @Test
    public void methodEventsFlatList() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = ZDLFindUtils.findServiceMethod("createCustomer", model);
        var events = ZDLFindUtils.methodEventsFlatList(method);
        Assertions.assertEquals(List.of("CustomerEvent", "CustomerCreated", "CustomerCreatedFailed"), events);
    }

}
