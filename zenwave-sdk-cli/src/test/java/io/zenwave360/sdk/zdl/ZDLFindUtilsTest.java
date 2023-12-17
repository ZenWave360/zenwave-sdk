package io.zenwave360.sdk.zdl;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.JDLProcessor;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.zdl.ZDLFindUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ZDLFindUtilsTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withSpecFile(resource).parse();
        return new ZDLProcessor().process(model);
    }

    @Test
    void testFindAllServiceFacingEntities() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findAllServiceFacingEntities((Map) model.get("zdl"));
        Assertions.assertEquals(List.of("OrderBusinessId", "OrderFaultType", "AttachmentFileId", "AttachmentFileOutput", "AttachmentFile", "PurchaseOrder", "OrderBusinessId", "OrderStatus", "AttachmentFile"), entities);
    }

    @Test
    void testFindAllPaginatedEntities() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findAllPaginatedEntities((Map) model.get("zdl"));
        Assertions.assertEquals(List.of(), entities);
    }

    @Test
    void testFindAllEntitiesReturnedAsList() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findAllEntitiesReturnedAsList((Map) model.get("zdl"));
        Assertions.assertEquals(List.of("AttachmentFile"), entities);
    }

    @Test
    void testFindMethodParameterAndReturnTypes() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findMethodParameterAndReturnTypes((Map) model.get("zdl"));
        Assertions.assertEquals(List.of("OrderBusinessId", "AttachmentFileId", "AttachmentFileOutput", "AttachmentFile", "PurchaseOrder"), entities);
    }

    @Test
    public void testFindDependentEntitiesMongodb() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl");
        var entities = ZDLFindUtils.findDependentEntities((Map) model.get("zdl"), "CustomerOrder");
        Assertions.assertEquals(List.of("CustomerOrder", "OrderStatus", "Customer", "OrderedItem", "PaymentDetails", "ShippingDetails"), entities);
    }

    @Test
    public void testFindDependentEntitiesMongodbZdl() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLFindUtils.findDependentEntities((Map) model.get("zdl"), "PurchaseOrder");
        Assertions.assertEquals(List.of("PurchaseOrder", "OrderBusinessId", "OrderStatus", "AttachmentFile"), entities);
    }

    @Test
    public void testFindDependentEntitiesRelational() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl");
        var entities = ZDLFindUtils.findDependentEntities((Map) model.get("zdl"), "CustomerOrder");
        Assertions.assertEquals(List.of("CustomerOrder", "OrderStatus", "OrderShippingDetails", "OrderShippingDetails2", "OrderedItem"), entities);
    }

    @Test
    public void testFindServiceName() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var serviceName = ZDLFindUtils.findServiceName("PurchaseOrder", (Map) model.get("zdl"));
        Assertions.assertEquals("AttachmentService", serviceName);

        serviceName = ZDLFindUtils.findServiceName("OrderBusinessId", (Map) model.get("zdl"));
        Assertions.assertEquals("AttachmentService", serviceName);

        serviceName = ZDLFindUtils.findServiceName("AttachmentFileId", (Map) model.get("zdl"));
        Assertions.assertEquals("AttachmentService", serviceName);
    }

    @Test
    public void testFindServiceMethod() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = ZDLFindUtils.findServiceMethod("uploadFile", (Map) model.get("zdl"));
        Assertions.assertEquals("AttachmentService", method.get("serviceName"));
    }
}
