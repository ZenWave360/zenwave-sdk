package ${api.apiPackage};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

<#if api.modelPackage??>
import ${api.modelPackage}.*;
</#if>

/**
 * ${api.description!}
 */
public class ${apiClassName} implements ${interfaceClassName} {

    private JmsTemplate jmsTemplate;
    private MessagePostProcessor messagePostProcessor;

<#list api.operations as operation>
    @Value("${"$"}{amiga.service.jms.${operation.channelName}.destination-fqdn:${operation.channel}}")
    private String ${operation.channelName};

</#list>
    
    public ${apiClassName}(JmsTemplate jmsTemplate) {
        super();
        this.jmsTemplate = jmsTemplate;
    }

    public ${apiClassName}(JmsTemplate jmsTemplate, MessagePostProcessor messagePostProcessor) {
        super();
        this.jmsTemplate = jmsTemplate;
        this.messagePostProcessor = messagePostProcessor;
    }

<#list api.operations as operation>
    <#list operation.messages as message>
    /**
     * ${operation.description!}
     */
    public void ${operation.operationId}${(operation.messages?size > 1)?then(message.name, '')}(${message.paramType} payload, Header... headers) {
            jmsTemplate.convertAndSend(${operation.channelName}, payload, withPostProcessor(headers));
    }

    </#list>
</#list>
    
    protected MessagePostProcessor withPostProcessor(Header... headers) {
        return message -> {
            if (headers != null) {
                for (Header header : headers) {
                    if (header.getType() != Header.Type.FOR_METADATA) {
                        continue;
                    }
                    if (header.getValue() instanceof Boolean) {
                        message.setBooleanProperty(header.getName(), (boolean) header.getValue());
                    } else if (header.getValue() instanceof Float) {
                        message.setFloatProperty(header.getName(), (float) header.getValue());
                    } else if (header.getValue() instanceof Double) {
                        message.setDoubleProperty(header.getName(), (double) header.getValue());
                    } else if (header.getValue() instanceof Byte) {
                        message.setFloatProperty(header.getName(), (byte) header.getValue());
                    } else {
                        message.setStringProperty(header.getName(), header.getValue().toString());
                    }
                }
            }
            if (messagePostProcessor != null) {
                return messagePostProcessor.postProcessMessage(message);
            }
            return message;
        };
    };
}
