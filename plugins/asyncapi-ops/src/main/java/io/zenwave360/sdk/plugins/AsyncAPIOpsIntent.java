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

    private final Set<String> aclKeys = new LinkedHashSet<>();

    public void addAcl(AclIntent acl) {
        String key = acl.topicName + "|" + acl.principal + "|" + acl.operation;
        if (aclKeys.add(key)) {
            acls.add(acl);
        }
    }

    public static class TopicIntent {
        /** Snake_case Terraform resource identifier */
        public String resourceName;
        /** Actual Kafka topic address */
        public String topicName;
        public int partitions = 1;
        public int replicationFactor = 1;
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
        public String topicName;
        /** e.g. User:merchandising.inventory.inventory-adjustment.baas */
        public String principal;
        /** Read or Write */
        public String operation;
        public String permissionType = "Allow";
    }
}
