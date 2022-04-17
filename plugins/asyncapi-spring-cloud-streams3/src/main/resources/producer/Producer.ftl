package ${api.apiPackage};

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

<#if api.modelPackage??>
import ${api.modelPackage}.*;
</#if>

/**
 * ${api.description!}
 */
@Component
public class ${apiClassName} implements ${interfaceClassName} {

    @Autowired
    private StreamBridge streamBridge;

<#list api.operations as operation>
    <#list operation.messages as message>
    /**
     * ${operation.description!}
     */
    public boolean ${operation.operationId}${(operation.messages?size > 1)?then(message.name, '')}(${message.paramType} payload, Header... headers) {
        MimeType outputContentType = MimeType.valueOf("${operation.operation.message.contentType!api.asyncAPI.defaultContentType}");
        Message message = MessageBuilder.createMessage(payload, asMessageHeaders(headers));
        return streamBridge.send("${operation.operationIdKebabCase}", message, outputContentType);
    }

    </#list>
</#list>

    protected MessageHeaders asMessageHeaders(Header... headers) {
        Map<String, Object> map = Header.asMap(headers, Header.Type.FOR_HEADERS);
        return new MessageHeaders(map);
    }
}
