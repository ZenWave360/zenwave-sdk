package io.zenwave360.sdk.processors;

import io.zenwave360.sdk.parsers.ZDLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ZDLUtilsTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withSpecFile(resource).parse();
        return new JDLProcessor().process(model);
    }

    @Test
    void testFindAllServiceFacingEntities() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLUtils.findAllServiceFacingEntities((Map) model.get("jdl"));
        Assertions.assertEquals(List.of("OrderBusinessId", "OrderFaultType", "AttachmentFileId", "AttachmentFileOutput", "AttachmentFile", "PurchaseOrder", "OrderBusinessId", "OrderStatus", "AttachmentFile"), entities);
    }

    @Test
    void testFindAllPaginatedEntities() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLUtils.findAllPaginatedEntities((Map) model.get("jdl"));
        Assertions.assertEquals(List.of(), entities);
    }

    @Test
    void testFindAllEntitiesReturnedAsList() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLUtils.findAllEntitiesReturnedAsList((Map) model.get("jdl"));
        Assertions.assertEquals(List.of("AttachmentFile"), entities);
    }

    @Test
    void testFindMethodParameterAndReturnTypes() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLUtils.findMethodParameterAndReturnTypes((Map) model.get("jdl"));
        Assertions.assertEquals(List.of("OrderBusinessId", "AttachmentFileId", "AttachmentFileOutput", "AttachmentFile", "PurchaseOrder"), entities);
    }

    @Test
    public void testFindDependentEntitiesMongodb() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/jdl/orders-model.jdl");
        var entities = ZDLUtils.findDependentEntities((Map) model.get("jdl"), "CustomerOrder");
        Assertions.assertEquals(List.of("CustomerOrder", "OrderStatus", "Customer", "OrderedItem", "PaymentDetails", "ShippingDetails"), entities);
    }

    @Test
    public void testFindDependentEntitiesMongodbZdl() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var entities = ZDLUtils.findDependentEntities((Map) model.get("jdl"), "PurchaseOrder");
        Assertions.assertEquals(List.of("PurchaseOrder", "OrderBusinessId", "OrderStatus", "AttachmentFile"), entities);
    }

    @Test
    public void testFindDependentEntitiesRelational() throws Exception {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/jdl/orders-model-relational.jdl");
        var entities = ZDLUtils.findDependentEntities((Map) model.get("jdl"), "CustomerOrder");
        Assertions.assertEquals(List.of("CustomerOrder", "OrderStatus", "OrderShippingDetails", "OrderShippingDetails2", "OrderedItem"), entities);
    }
}
