# Generated from asyncapi.yml for environment: staging
# Command: jbang zw -p AsyncAPIToTerraform server=staging

locals {
  topic_name = "ecommerce.checkout.cart"

  # Merged values for staging environment
  partitions         = 10
  replication_factor = 2

  # Topic configuration
  cleanup_policy      = "delete,compact"
  retention_ms        = "604800000"
  retention_bytes     = "1000000000"
  delete_retention_ms = "86400000"
  max_message_bytes   = "1048588"
}

# Local / self-hosted Kafka topic (staging uses local Kafka)
resource "kafka_topic" "shopping_cart" {
  name               = local.topic_name
  replication_factor = local.replication_factor
  partitions         = local.partitions

  config = {
    "cleanup.policy"      = local.cleanup_policy
    "retention.ms"        = local.retention_ms
    "retention.bytes"     = local.retention_bytes
    "delete.retention.ms" = local.delete_retention_ms
    "max.message.bytes"   = local.max_message_bytes
  }
}
