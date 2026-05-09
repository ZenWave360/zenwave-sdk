provider "kafka" {
  bootstrap_servers = var.kafka_bootstrap_servers
  tls_enabled       = var.kafka_tls_enabled
  skip_tls_verify   = var.kafka_skip_tls_verify
  sasl_username     = var.kafka_sasl_username != "" ? var.kafka_sasl_username : null
  sasl_password     = var.kafka_sasl_password != "" ? var.kafka_sasl_password : null
  sasl_mechanism    = var.kafka_sasl_mechanism != "" ? var.kafka_sasl_mechanism : null
}

provider "schemaregistry" {
  schema_registry_url = var.schema_registry_url
  username            = var.schema_registry_username != "" ? var.schema_registry_username : null
  password            = var.schema_registry_password != "" ? var.schema_registry_password : null
}
