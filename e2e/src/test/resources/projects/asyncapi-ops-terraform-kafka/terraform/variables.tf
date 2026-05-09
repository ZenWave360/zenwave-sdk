variable "kafka_bootstrap_servers" {
  description = "Bootstrap servers for the local Kafka broker."
  type        = list(string)
}

variable "kafka_tls_enabled" {
  description = "Enable TLS for Kafka provider connections."
  type        = bool
}

variable "kafka_skip_tls_verify" {
  description = "Skip TLS verification for Kafka provider connections."
  type        = bool
}

variable "kafka_sasl_username" {
  description = "Optional SASL username for Kafka."
  type        = string
}

variable "kafka_sasl_password" {
  description = "Optional SASL password for Kafka."
  type        = string
  sensitive   = true
}

variable "kafka_sasl_mechanism" {
  description = "Optional SASL mechanism for Kafka."
  type        = string
}

variable "schema_registry_url" {
  description = "Base URL for Schema Registry."
  type        = string
}

variable "schema_registry_username" {
  description = "Optional username for Schema Registry basic authentication."
  type        = string
}

variable "schema_registry_password" {
  description = "Optional password for Schema Registry basic authentication."
  type        = string
  sensitive   = true
}

variable "default_compatibility" {
  description = "Default compatibility level for generated schema resources."
  type        = string
}
