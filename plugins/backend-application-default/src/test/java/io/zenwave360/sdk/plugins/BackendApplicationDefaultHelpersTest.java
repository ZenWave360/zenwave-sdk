package io.zenwave360.sdk.plugins;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import io.zenwave360.sdk.parsers.ZDLParser;
import io.zenwave360.sdk.processors.ZDLProcessor;
import io.zenwave360.sdk.utils.JSONPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackendApplicationDefaultHelpersTest {

    private BackendDefaultApplicationGenerator generator = new BackendDefaultApplicationGenerator();
    private BackendApplicationDefaultHelpers helpers = new BackendApplicationDefaultHelpers(generator);

    private Map<String, Object> loadZDLModelFromResource(String resource) throws Exception {
        Map<String, Object> model = new ZDLParser().withSpecFile(resource).parse();
        return (Map<String, Object>) new ZDLProcessor().process(model).get("zdl");
    }

    private Options options(Map map) {
        Context context = Context
                .newBuilder(map)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE)
                .build();
        return new Options.Builder(null, null, null, context, null).build();
    }


    @Test
    void methodParametersCallSignature() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = JSONPath.get(zdl, "$.services.CustomerService.methods.updateCustomer");
        var methodParametersCallSignature = helpers.methodParametersCallSignature((Map) method, options(Map.of("zdl", zdl)));
        Assertions.assertEquals("id, input", methodParametersCallSignature);
    }

    @Test
    void methodParametersCallSignature_Inline() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = JSONPath.get(zdl, "$.services.CustomerService.methods.addCustomerAddress");
        var methodParametersCallSignature = helpers.methodParametersCallSignature((Map) method, options(Map.of("zdl", zdl)));
        Assertions.assertEquals("customerId, address", methodParametersCallSignature);
    }

    @Test
    void methodEvents() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var method = JSONPath.get(zdl, "$.services.CustomerService.methods.createCustomer");
        var createCustomer = helpers.methodEvents((Map) method, options(Map.of("zdl", zdl))).stream()
                .map(e -> JSONPath.get(e, "name")).toList();
        Assertions.assertEquals(List.of("CustomerEvent", "CustomerCreated", "CustomerCreatedFailed"), createCustomer);
    }

    @Test
    void listOfPairEventEntity() throws Exception {
        var zdl = loadZDLModelFromResource("classpath:io/zenwave360/sdk/resources/zdl/customer-address.zdl");
        var methodParametersCallSignature = helpers.listOfPairEventEntity(zdl, options(Map.of("zdl", zdl)));
        var entityEvent = methodParametersCallSignature.stream()
                .map(p -> JSONPath.getFirst(p, "entity.name", "method.name") + "=" + JSONPath.get(p, "event.name"))
                .sorted()
                .peek(System.out::println)
                .toList();
        Assertions.assertEquals(List.of(
                "Address=Address",
                "Customer=Customer",
                "Customer=CustomerCreated",
                "Customer=CustomerCreatedFailed",
                "Customer=CustomerEvent",
                "Customer=CustomerUpdated",
                "deleteCustomer=CustomerDeleted"),
                entityEvent);

    }
}
