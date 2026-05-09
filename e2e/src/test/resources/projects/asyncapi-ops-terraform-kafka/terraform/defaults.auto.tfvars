kafka_bootstrap_servers = ["localhost:9092"]
kafka_tls_enabled       = false
kafka_skip_tls_verify   = false
kafka_sasl_username     = ""
kafka_sasl_password     = ""
kafka_sasl_mechanism    = ""

schema_registry_url      = "http://localhost:8081"
schema_registry_username = ""
schema_registry_password = ""

default_compatibility = "BACKWARD"
default_partitions = 2
default_replication_factor = 1
default_topic_config = {
  "cleanup.policy" = "delete"
  "retention.ms"   = "86400000"
}
