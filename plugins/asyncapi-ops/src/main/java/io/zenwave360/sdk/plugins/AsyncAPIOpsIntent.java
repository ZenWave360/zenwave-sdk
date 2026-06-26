package io.zenwave360.sdk.plugins;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AsyncAPIOpsIntent {

    public String server;
    public List<TopicIntent> topics = new ArrayList<>();
    public List<SchemaIntent> schemas = new ArrayList<>();
    public List<AclIntent> acls = new ArrayList<>();
    public List<RoleBindingIntent> roleBindings = new ArrayList<>();

    private final Set<String> aclKeys = new LinkedHashSet<>();
    private final Set<String> roleBindingKeys = new LinkedHashSet<>();

    public void addAcl(AclIntent acl) {
        String key = acl.resourceType + "|" + acl.kafkaResourceName + "|" + acl.patternType + "|" + acl.principal + "|" + acl.operation;
        if (aclKeys.add(key)) {
            acls.add(acl);
        }
    }

    public void addRoleBinding(RoleBindingIntent roleBinding) {
        String key = roleBinding.principal + "|" + roleBinding.roleName + "|" + roleBinding.crnPattern;
        if (roleBindingKeys.add(key)) {
            roleBindings.add(roleBinding);
        }
    }

    public static class TopicIntent {
        /** Snake_case Terraform resource identifier */
        public String resourceName;
        /** Actual Kafka topic address */
        public String topicName;
        public Integer partitions;
        public Integer replicationFactor;
        public Map<String, String> config = new LinkedHashMap<>();
        /** True for auto-generated retry/DLQ topics — rendered without config block */
        public boolean isRetryOrDlq;
    }

    public static class SchemaIntent {
        /** Snake_case Terraform resource identifier */
        public String resourceName;
        /** Schema Registry subject — TopicRecordNameStrategy: {topic}-{MessageName}-value */
        public String subject;
        public String schemaType = "AVRO";
        /** BACKWARD, FORWARD, or null (template falls back to var.default_compatibility) */
        public String compatibility;
        /** Relative path to generated bundled .avsc file inside targetFolder */
        public String schemaFile;
        /** Source .avsc URI resolved from the owning AsyncAPI file */
        public String sourceSchemaUri;
    }

    public static class AclIntent {
        /** Snake_case Terraform resource identifier */
        public String resourceName;
        public String kafkaResourceName;
        public String topicName;
        /** TOPIC, GROUP, or TRANSACTIONAL_ID for Confluent; Topic, Group, or TransactionalId for Kafka OSS templates */
        public String resourceType = "TOPIC";
        public String kafkaResourceType = "Topic";
        public String patternType = "LITERAL";
        public String kafkaPatternType = "Literal";
        /** e.g. User:merchandising.inventory.inventory-adjustment.baas */
        public String principal;
        /** Read, Write, or Describe */
        public String operation;
        public String permissionType = "Allow";
    }

    public static class RoleBindingIntent {
        /** Snake_case Terraform resource identifier */
        public String resourceName;
        /** e.g. User:merchandising.inventory.inventory-adjustment.baas */
        public String principal;
        public String roleName;
        public String crnPattern;
    }
}
