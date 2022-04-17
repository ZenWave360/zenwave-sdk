package ${api.apiPackage};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

<#if api.modelPackage??>
import ${api.modelPackage}.*;
</#if>

@Component("${operation.operationIdKebabCase}")
public class ${operation.operationIdCamelCase} implements I${operation.operationIdCamelCase} {

    <#list operation.messages as message>
    @Autowired
    private I${operation.operationIdCamelCase}.${message.name} ${message.name?uncap_first};

    </#list>

    @Override
    public void accept(Message<?> message) {
        Object payload = message.getPayload();
        <#list operation.messages as message>
        if(payload instanceof ${message.paramType}) {
            <#assign messageType = exposeMessage?string('Message<' + message.paramType + '>', message.paramType)>
            ${message.name?uncap_first}.accept(Flux.just((<${messageType}>) payload));
            return;
        }
        </#list>
    }
}