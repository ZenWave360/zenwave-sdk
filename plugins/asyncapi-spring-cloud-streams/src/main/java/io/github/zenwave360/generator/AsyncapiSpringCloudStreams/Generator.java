package io.github.zenwave360.generator.plugins;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import io.zenwave360.generator.processors.GeneratorPlugin;
import io.zenwave360.generator.templating.NonPrivateFieldValueResolver;
import io.zenwave360.generator.templating.TemplateOutput;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.management.relation.Role;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Generator implements GeneratorPlugin {

    @DocumentedOption(description = "Java API package name")
    public String apiPackage = "pack";
    public String modelPackage = "modelPack";
    public String bindingType;

    public boolean isProducer(Map<String, Object> operation) {
        //        if ("PROVIDER" == role && OperationType.PUBLISH == operationType
        //                || "CONSUMER" == role && OperationType.SUBSCRIBE == operationType) {
        return false;
    }

    public String getApiPackageFolder() {
        return this.apiPackage.replaceAll("\\.", "/");
    }

    public String getApiClassName() {
        return "apiClassName";
    }

    public String getInterfaceClassName() {
        return "interfaceClassName";
    }

    public List<ApiTemplate> reduce(AsyncAPI asyncAPI) {
        Map<String, List<Operation>> publishOperations = new HashMap<>();
        Map<String, List<Operation>> subscribeOperations = new HashMap<>();
        for (ChannelItem channel : asyncAPI.getChannels().values()) {
            if (channel.getPublish() != null) {
                String tag = "";
                if (channel.getPublish().getTags() != null && channel.getPublish().getTags().size() > 0) {
                    tag = ObjectUtils.firstNonNull(firstTagNameOrNull(channel.getPublish().getTags()), "Service");
                }
                if (!publishOperations.containsKey(tag)) {
                    publishOperations.put(tag, new ArrayList<Operation>());
                }
                publishOperations.get(tag).add(channel.getPublish());
            }
            if (channel.getSubscribe() != null) {
                String tag = "";
                if (channel.getSubscribe().getTags() != null && channel.getSubscribe().getTags().size() > 0) {
                    tag = ObjectUtils.firstNonNull(firstTagNameOrNull(channel.getSubscribe().getTags()), "Service");
                }
                if (!subscribeOperations.containsKey(tag)) {
                    subscribeOperations.put(tag, new ArrayList<Operation>());
                }
                subscribeOperations.get(tag).add(channel.getSubscribe());
            }
        }

        List<ApiTemplate> apiTemplates = new ArrayList<>();
        for (Map.Entry<String, List<Operation>> entry : publishOperations.entrySet()) {
            reduceOperations(entry.getKey(), OperationType.PUBLISH, entry.getValue(), bindingType, asyncAPI)
                    .ifPresent(apiTemplates::add);
        }
        for (Map.Entry<String, List<Operation>> entry : subscribeOperations.entrySet()) {
            reduceOperations(entry.getKey(), OperationType.SUBSCRIBE, entry.getValue(), bindingType, asyncAPI)
                    .ifPresent(apiTemplates::add);
        }
        return apiTemplates;
    }

    @Override
    public List<TemplateOutput> generate(Map<String, Object> apiModel) {
        return null;
    }
}
