package ${api.apiPackage};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import java.util.function.Consumer;

<#if api.modelPackage??>
import ${api.modelPackage}.*;
</#if>

@Component("${operation.operationIdKebabCase}")
public class ${operation.operationIdCamelCase} implements Consumer<Message<Object>> {

    @Autowired
    <#assign business = operation.operationIdCamelCase?uncap_first>
    private I${operation.operationIdCamelCase} ${business};

    @Override
    public void accept(Message<Object> message) {
        Object payload = message.getPayload();
        <#list operation.messages as message>
        if(payload instanceof ${message.paramType}) {
            <#if exposeMessage>
            ${business}.${operation.operationId?uncap_first}${(operation.messages?size > 1)?then(message.name, '')}(MessageBuilder.createMessage((${message.paramType}) payload, message.getHeaders()));
            <#else>
            ${business}.${operation.operationId?uncap_first}${(operation.messages?size > 1)?then(message.name, '')}((${message.paramType}) payload);
            </#if>
            return;
        }
        </#list>
    }
}