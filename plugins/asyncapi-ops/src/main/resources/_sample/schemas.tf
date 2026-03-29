# Generated from asyncapi.yml for environment: staging
# Command: jbang zw -p AsyncAPIToTerraform server=staging

resource "schemaregistry_schema" "shopping_cart_created_value" {
  subject             = "ecommerce.checkout.cart-ShoppingCartCreated-value"
  schema_type         = "AVRO"
  compatibility_level = var.default_compatibility
  schema              = file("${path.module}/../avro/ShoppingCartCreated.avsc")
}

resource "schemaregistry_schema" "shopping_cart_item_added_value" {
  subject             = "ecommerce.checkout.cart-ShoppingCartItemAdded-value"
  schema_type         = "AVRO"
  compatibility_level = var.default_compatibility
  schema              = file("${path.module}/../avro/ShoppingCartItemAdded.avsc")
}

resource "schemaregistry_schema" "shopping_cart_item_removed_value" {
  subject             = "ecommerce.checkout.cart-ShoppingCartItemRemoved-value"
  schema_type         = "AVRO"
  compatibility_level = var.default_compatibility
  schema              = file("${path.module}/../avro/ShoppingCartItemRemoved.avsc")
}

resource "schemaregistry_schema" "shopping_cart_item_updated_value" {
  subject             = "ecommerce.checkout.cart-ShoppingCartItemUpdated-value"
  schema_type         = "AVRO"
  compatibility_level = var.default_compatibility
  schema              = file("${path.module}/../avro/ShoppingCartItemUpdated.avsc")
}

resource "schemaregistry_schema" "shopping_cart_checked_out_value" {
  subject             = "ecommerce.checkout.cart-ShoppingCartCheckedOut-value"
  schema_type         = "AVRO"
  compatibility_level = var.default_compatibility
  schema              = file("${path.module}/../avro/ShoppingCartCheckedOut.avsc")
}
