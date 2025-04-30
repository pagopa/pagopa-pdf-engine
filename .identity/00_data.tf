data "azurerm_resource_group" "dashboards" {
  name = "dashboards"
}

data "azurerm_storage_account" "tfstate_app" {
  name                = "pagopainfraterraform${var.env}"
  resource_group_name = "io-infra-rg"
}

data "azurerm_kubernetes_cluster" "aks" {
  name                = local.aks_cluster.name
  resource_group_name = local.aks_cluster.resource_group_name
}

data "github_organization_teams" "all" {
  root_teams_only = true
  summary_only    = true
}

data "azurerm_key_vault" "key_vault" {

  name = "pagopa-${var.env_short}-kv"
  resource_group_name = "pagopa-${var.env_short}-sec-rg"
}

data "azurerm_key_vault" "key_vault_domain" {
  name                = "pagopa-${var.env_short}-receipts-kv"
  resource_group_name = "pagopa-${var.env_short}-receipts-sec-rg"
}

data "azurerm_key_vault_secret" "key_vault_sonar" {

  name = "sonar-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_bot_token" {

  name = "bot-token-github"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_cucumber_token" {

  name = "cucumber-token"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_subkey" {
  name         = "apikey-pdf-engine"
  key_vault_id = data.azurerm_key_vault.key_vault_domain.id
}

data "azurerm_key_vault_secret" "key_vault_integration_test_webhook_slack" {
  name         = "webhook-slack"
  key_vault_id = data.azurerm_key_vault.key_vault_domain.id
}

data "azurerm_key_vault_secret" "key_vault_az_devops" {
  name         = "azure-devops-token"
  key_vault_id = data.azurerm_key_vault.key_vault_domain.id
}

data "azurerm_user_assigned_identity" "workload_identity_clientid" {
  name                = "shared-workload-identity"
  resource_group_name = "${local.product}-weu-${var.env}-aks-rg"
}

