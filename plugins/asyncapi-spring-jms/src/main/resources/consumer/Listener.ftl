package ${api.apiPackage};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

@Component
public class ${apiClassName} {

    private ${interfaceClassName} ${interfaceClassName?uncap_first};
    
    @Autowired
    public ${apiClassName}(${interfaceClassName} ${interfaceClassName?uncap_first}) {
        super();
        this.${interfaceClassName?uncap_first} = ${interfaceClassName?uncap_first};
    }

<#assign jmsId = 'jms.id'/>
<#assign jmsIdTag = 'jms.' + tagName + '.id'/>
<#assign factory = 'jms.containerFactory'/>
<#assign factoryTag = 'jms.' + tagName + '.containerFactory'/>
<#assign subscription = 'jms.subscription'/>
<#assign subscriptionTag = 'jms.' + tagName + '.subscription'/>
<#assign concurrency = 'jms.concurrency'/>
<#assign concurrencyTag = 'jms.' + tagName + '.concurrency'/>
<#assign selectorTag = 'jms.' + tagName + '.selector'/>
<#list api.operations as operation>

    <#assign jmsIdOperation = 'jms.' + operation.operationId + '.id'/>
    <#assign selectorOperation = 'jms.' + operation.operationId + '.selector'/>
    <#assign destinationKey = 'jms.' + operation.operationId + '.destination'/>
    <#assign destinationDefault = '$' + '{amiga.service.jms.' + operation.operationId + '.destination-fqdn}'/>
    <#if operation.operation.extensions?? && operation.operation.extensions['x-transactional']??>
    @org.springframework.transaction.annotation.Transactional
    </#if>
    @JmsListener(<#rt>
        <#lt><#if settings[jmsId]?? || settings[jmsIdTag]?? || settings[jmsIdOperation]??>id = "${settings[jmsIdOperation]!settings[jmsIdTag]!settings[jmsId]}", </#if><#rt>
        <#lt><#if settings[factory]?? || settings[factoryTag]??>containerFactory = "${settings[factoryTag]!settings[factory]}", </#if><#rt>
        <#lt><#if settings[subscription]?? || settings[subscriptionTag]??>subscription = "${settings[subscriptionTag]!settings[subscription]}", </#if><#rt>
        <#lt><#if settings[selectorTag]?? || settings[selectorOperation]??>selector = "${settings[selectorOperation]!settings[selectorTag]}", </#if><#rt>
        <#lt><#if settings[concurrency]?? || settings[concurrencyTag]??>concurrency = "${settings[concurrencyTag]!settings[concurrency]}", </#if><#rt>
        <#lt>destination = "${settings[destinationKey]!destinationDefault}")
    public void ${operation.operationId}Handler(final Message<?> message) throws InterruptedException {
        final MessageHeaders messageHeaders = message.getHeaders();
        final Object payload = message.getPayload();
        final Header[] headers = this.of(messageHeaders);
        <#list operation.messages as message>
            <#if message.paramType?starts_with('java.util.List<') >
                <#assign paramType = message.paramType?remove_beginning("java.util.List<")?remove_ending(">")>
        if (payload instanceof java.util.List && ((java.util.List<${paramType}>) message).get(0) instanceof ${paramType}) {
            ${interfaceClassName?uncap_first}.${operation.operationId}${(operation.messages?size > 1)?then(message.name, '')}((java.util.List<${paramType}>) payload, headers);
            return;
        }
            <#else>
        if(payload instanceof ${message.paramType}) {
            ${interfaceClassName?uncap_first}.${operation.operationId}${(operation.messages?size > 1)?then(message.name, '')}((${message.paramType}) payload, headers);
            return;
        }
            </#if>

        </#list>
    }
</#list>

    protected Header[] of(MessageHeaders messageHeaders) {
        return messageHeaders.entrySet().stream().map(entry -> new Header(entry.getKey(), entry.getValue()))
                .toArray(Header[]::new);
    }
}
