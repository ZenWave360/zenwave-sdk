package ${api.apiPackage};

<#if api.modelPackage??>
import ${api.modelPackage}.*;
</#if>

public interface I${operation.operationIdCamelCase} {

<#list operation.messages as message>
    /**
     * ${operation.description!}
     */
    <#assign messageType = exposeMessage?string('Message<' + message.paramType + '>', message.paramType)>
    <#if returnType??>${returnType}<#else>void</#if> ${operation.operationId?uncap_first}${(operation.messages?size > 1)?then(message.name, '')}(${messageType} msg);
</#list>

}