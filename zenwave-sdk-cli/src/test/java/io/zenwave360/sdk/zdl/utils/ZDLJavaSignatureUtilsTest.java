package io.zenwave360.sdk.zdl.utils;

import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ZDLJavaSignatureUtilsTest {

    private Map<String, Object> loadZDL(String resource) throws IOException {
        Map<String, Object> model = new ZDLParser().withZdlFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    @Test
    void naturalIdsRepoMethodSignature() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/natural-ids.zdl");
        var entity = JSONPath.get(model, "$.entities.Customer", Map.of());
        var signature = ZDLJavaSignatureUtils.naturalIdsRepoMethodSignature(entity);
        Assertions.assertEquals("java.util.Optional<Customer> findByCustomerIdAndAnotherId(Long customerId, String anotherId)", signature);
    }

    @Test
    void naturalIdsRepoMethodCallSignature() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/natural-ids.zdl");
        var entity = JSONPath.get(model, "$.entities.Customer", Map.of());
        var signature = ZDLJavaSignatureUtils.naturalIdsRepoMethodCallSignature(entity);
        Assertions.assertEquals("findByCustomerIdAndAnotherId(customerId, anotherId)", signature);
    }


    @Test
    void methodParameterType() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.uploadFile", Map.of());
        var inputDTOSuffix = "InputDTO";
        var parameterType = ZDLJavaSignatureUtils.methodParameterType(method, model, inputDTOSuffix);
        Assertions.assertEquals("PurchaseOrderInputDTO", parameterType);
    }

    @Test
    void methodParametersSignature() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.downloadAttachmentFile", Map.of());
        var inputDTOSuffix = "InputDTO";
        var signature = ZDLJavaSignatureUtils.methodParametersSignature("String", method, model, inputDTOSuffix);
        Assertions.assertEquals("String businessUnit, String orderId, String documentManagerId", signature);
    }

    @Test
    void methodParametersSignatureWithNaturalIds() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/natural-ids.zdl");
        var method = JSONPath.get(model, "$.services.CustomerService.methods.getCustomer", Map.of());
        var signature = ZDLJavaSignatureUtils.methodParametersSignature("String", method, model, "");
        Assertions.assertEquals("Long customerId, String anotherId", signature);
    }

    @Test
    void methodParametersSignatureWithNaturalIdsAndParameter() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/natural-ids.zdl");
        var method = JSONPath.get(model, "$.services.CustomerService.methods.updateCustomer", Map.of());
        var signature = ZDLJavaSignatureUtils.methodParametersSignature("String", method, model, "");
        Assertions.assertEquals("Long customerId, String anotherId, Customer input", signature);
    }

    @Test
    void methodParametersCallSignature() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.downloadAttachmentFile", Map.of());
        var inputDTOSuffix = "InputDTO";
        var signature = ZDLJavaSignatureUtils.methodParametersCallSignature(method, model, inputDTOSuffix);
        Assertions.assertEquals("businessUnit, orderId, documentManagerId", signature);
    }

    @Test
    void mapperInputCallSignature() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var signature = ZDLJavaSignatureUtils.mapperInputCallSignature("PurchaseOrder", model);
        Assertions.assertEquals("input", signature);
    }

    @Test
    void mapperInputCallSignatureInline() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var signature = ZDLJavaSignatureUtils.mapperInputCallSignature("AttachmentFileId", model);
        Assertions.assertEquals("businessUnit, orderId, documentManagerId", signature);
    }


    @Test
    void mapperInputSignature() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var inputDTOSuffix = "InputDTO";
        var signature = ZDLJavaSignatureUtils.mapperInputSignature("AttachmentFileId", model, inputDTOSuffix);
        Assertions.assertEquals("String businessUnit, String orderId, String documentManagerId", signature);
    }

    @Test
    void inputFieldInitializer() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var inputDTOSuffix = "InputDTO";
        var signature = ZDLJavaSignatureUtils.inputFieldInitializer("AttachmentFileId", model, inputDTOSuffix);
        Assertions.assertEquals("""
                        String businessUnit = null;
                        String orderId = null;
                        String documentManagerId = null;
                        """, signature);
    }

    @Test
    void methodInputCall() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.downloadAttachmentFile", Map.of());
        var inputDTOSuffix = "InputDTO";
        var signature = ZDLJavaSignatureUtils.methodInputCall(method, model, inputDTOSuffix);
        Assertions.assertEquals(List.of("businessUnit", "orderId", "documentManagerId"), signature);
    }


    @Test
    void methodReturnType() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.uploadFile", Map.of());
        var returnType = ZDLJavaSignatureUtils.methodReturnType(method);
        Assertions.assertEquals("CompletableFuture<PurchaseOrder>", returnType);
    }

    @Test
    void methodReturnTypeVoid() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.startBackgroundAdminProcess", Map.of());
        var returnType = ZDLJavaSignatureUtils.methodReturnType(method);
        Assertions.assertEquals("void", returnType);
    }

    @Test
    void methodReturnTypeArray() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.listAttachmentFiles", Map.of());
        var returnType = ZDLJavaSignatureUtils.methodReturnType(method);
        Assertions.assertEquals("List<AttachmentFile>", returnType);
    }

    @Test
    void methodReturnTypeArrayPaginated() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.listAttachmentFilesPaginated", Map.of());
        var returnType = ZDLJavaSignatureUtils.methodReturnType(method);
        Assertions.assertEquals("Page<AttachmentFile>", returnType);
    }


    @Test
    void methodReturnTypeOptional() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var method = JSONPath.get(model, "$.services.AttachmentService.methods.updatePurchaseOrder", Map.of());
        var returnType = ZDLJavaSignatureUtils.methodReturnType(method);
        Assertions.assertEquals("Optional<PurchaseOrder>", returnType);
    }


    @Test
    void fieldType() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var field = JSONPath.get(model, "$.entities.PurchaseOrder.fields.attachments", Map.of());
        var prefix = "";
        var suffix = "";
        var fieldType = ZDLJavaSignatureUtils.fieldType(field, prefix, suffix);
        Assertions.assertEquals("List<AttachmentFile>", fieldType);
    }

    @Test
    void fieldTypeInitializer() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var field = JSONPath.get(model, "$.entities.PurchaseOrder.fields.attachments", Map.of());
        var fieldTypeInitializer = ZDLJavaSignatureUtils.fieldTypeInitializer(field);
        Assertions.assertEquals("= new ArrayList<>()", fieldTypeInitializer);
    }

    @Test
    void populateField_String() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var field = JSONPath.get(model, "$.entities.OrderBusinessId.fields.orderId", Map.of());
        var fieldTypeInitializer = ZDLJavaSignatureUtils.populateField(field);
        Assertions.assertEquals("\"\"", fieldTypeInitializer);
    }

    @Test
    void populateField_all_types() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/zdl/populate-fields.zdl");
        var fields = JSONPath.get(model, "$.entities.Entity.fields[*]", List.<Map>of());
        for (var field : fields) {
            var fieldTypeInitializer = ZDLJavaSignatureUtils.populateField(field);
            Assertions.assertEquals(((String)field.get("javadoc")).trim(), fieldTypeInitializer);
        }
    }

    @Test
    void populateField_enum() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/order-faults-attachments-model.zdl");
        var field = JSONPath.get(model, "$.entities.OrderBusinessId.fields.orderFaultType", Map.of());
        var fieldTypeInitializer = ZDLJavaSignatureUtils.populateField(field);
        Assertions.assertEquals("OrderFaultType.values()[0]", fieldTypeInitializer);
    }

    @Test
    void relationshipFieldType() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        var relationship = JSONPath.get(model, "$.entities.Customer.relationships[0]", Map.of());
        var prefix = "";
        var suffix = "";
        var fieldType = ZDLJavaSignatureUtils.relationshipFieldType(relationship, prefix, suffix);
        Assertions.assertEquals("Set<Address>", fieldType);
    }

    @Test
    void relationshipFieldTypeInitializer() throws IOException {
        var model = loadZDL("classpath:io/zenwave360/sdk/resources/zdl/customer-address-relational.zdl");
        var relationship = JSONPath.get(model, "$.entities.Customer.relationships[0]", Map.of());
        var fieldTypeInitializer = ZDLJavaSignatureUtils.relationshipFieldTypeInitializer(relationship);
        Assertions.assertEquals("= new HashSet<>()", fieldTypeInitializer);
    }

}
