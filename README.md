# Quarkus on Azure

This project describes the steps involves setting a quarkus application up and running on Azure using Azure DevOps, along with some Azure services (SQL Server, App Service, AAD SSO) 

## Getting Started
These instructions will get you set the project up and running on your local machine for development and testing purposes. Setting up your services using the Azure CLI tool and initlaising your Azure Devops pipeline. 

### Prerequisites
- **Java 11**
- [GraalVM](https://www.graalvm.org/downloads/)
- [Azure CLI \| Microsoft Docs](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest)
- Docker
- Maven 3.6.x
- cURL (or alternative)
- jq
- sed

An Azure subscription (free tier)
- Owner rights on the used Resource Group

An Azure Devops organisation & a project
- Administration rights on the used project

### Setting up Azure services
After you install the Azure CLI, set up the azure services.
You will require the following environment variables:
```bash
export RESOURCE_GROUP='quarkus'

export APP_SERVICE_PLAN='QuarkusServicePlan'
export APP='quarkus-mssql-azure'

export SQLSERVER='quarkusdatabaseserver'
export DATABASE='airport'
export SQL_USR='quarkusadmin'
export SQL_PWD='yourStrong(!)Password'

export LOCATION='northeurope'

export CONTAINER_REGISTRY='quarkuscontainerregistry'

export CLIENT_SECRET='yourStrong(!)Secret'
```
Note: Run `az account list-locations -o table` for a list of locations after you sign in.

Next log in & create the following services:
- Container Registry
- App Registration
- SQL Server
  - Configure to allow azure services
  - Create a database
- App Service/Plan

```bash
# Log into azure cli
az login

# App Service plan & App Service
az appservice plan create --name $APP_SERVICE_PLAN --resource-group $RESOURCE_GROUP --sku F1
az webapp create --resource-group $RESOURCE_GROUP --plan $APP_SERVICE_PLAN --name $APP --deployment-container-image-name nginx

# SQL Server
az sql server create --name $SQLSERVER --resource-group $RESOURCE_GROUP --location "$LOCATION" --admin-user $SQL_USR --admin-password $SQL_PWD
az sql server firewall-rule create --resource-group $RESOURCE_GROUP --server $SQLSERVER -n AllowAccess --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0
az sql db create --resource-group $RESOURCE_GROUP --server $SQLSERVER --name $DATABASE --sample-name AdventureWorksLT --edition Basic --family Gen4 --capacity 1 --zone-redundant false 


# Azure Container Registry
az acr create --resource-group $RESOURCE_GROUP --name $CONTAINER_REGISTRY --admin-enabled true  --sku Basic

# App registration
# The app ID is set to the CLIENT_ID
# "appId": "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
export CLIENT_ID=`az ad app create --display-name $APP --reply-urls http://localhost:8080/auth https://$APP.azurewebsites.net/  --password $CLIENT_SECRET | jq -r .appId`
```

### Configurations
Retrieve the connection string, tenant id & the generated container registry password.

```bash
# Connection string
export CONNECTION_STRING=`az sql db show-connection-string -s $SQLSERVER -n $DATABASE -c jdbc | sed "s/<username>/$SQL_USR/;s/<password>/$SQL_PWD/"`

# Tenant ID
export TENANT_ID=`az account show | jq -r .tenantId`

# Container registry password
export DOCKER_REGISTRY_SERVER_PASSWORD=`az acr credential show -n $CONTAINER_REGISTRY | jq -r .passwords[0].value`

```
#### Azure configurations
                        
Now that all services are created the App Service requires permissions for both container registry and sql server.
```bash
az webapp config appsettings set --resource-group $RESOURCE_GROUP --name $APP \
  --settings DOCKER_REGISTRY_SERVER_URL=$CONTAINER_REGISTRY.azurecr.io DOCKER_REGISTRY_SERVER_USERNAME=$CONTAINER_REGISTRY DOCKER_REGISTRY_SERVER_PASSWORD=$DOCKER_REGISTRY_SERVER_PASSWORD

az webapp config appsettings set --resource-group $RESOURCE_GROUP --name $APP \
  --settings QUARKUS_OIDC_AUTH-SERVER-URL=https://login.microsoftonline.com/$TENANT_ID/v2.0 QUARKUS_OIDC_CLIENT=$CLIENT_ID QUARKUS_OIDC_CREDENTIALS_SECRET=$CLIENT_SECRET

az webapp config appsettings set --resource-group $RESOURCE_GROUP --name $APP \
  --settings QUARKUS_DATASOURCE_URL=$CONNECTION_STRING
```

#### Local configurations

Run a mssql container for local development using docker.

`docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=yourStrong(!)Password' -d --name mssql -p 1433:1433 mcr.microsoft.com/mssql/server:latest`

Either run the command below in the root directory or create an `.env` file using the `.env-sample` template.

```bash
cat << EOF > .env
# DB Configuration
QUARKUS_DATASOURCE_PASSWORD=yourStrong(!)Password
QUARKUS_DATASOURCE_USERNAME=sa

# OIDC Configuration
QUARKUS_OIDC_AUTH-SERVER-URL=https://login.microsoftonline.com/$TENANT_ID/v2.0
QUARKUS_OIDC_CLIENT-ID=$CLIENT_ID
QUARKUS_OIDC_CREDENTIALS_SECRET=$CLIENT_SECRET
EOF
```

## Running the locally

Using the maven wrapper, run the application.
The configurations for each profile are included in the application.yaml (%profile).

Profile: dev
This will use the running mssql container for development

Profile: test
The testcontainers library will spin up a container for test cases.

```bash
./mvnw test

./mvnw quarkus:dev
curl localhost:8080/airports/random
curl localhost:8080/hello
```

## Azure DevOps - CI/CD pipeline
All required azure services are created, the next step will be setting up our CI/CD pipeline.
Install the cli extension & set up the default organisation & create a project if necessary.
```bash
az extension add --name azure-devops
```
Variables used in devops project
```bash
export GITHUB_REPO="https://github.com/j-madden/quarkus-mssql-azure"
export BRANCH="azure-pipelines"

export ORGANISATION="<name-of-your-devops-organisation>"
export PROJECT_NAME="<name-of-your-devops-project>"
export PIPELINE_NAME="azure-pipelines"

export SERVICE_CONNECTION="SERVICE_CONNECTION"
export ACR_REPO="quarkus/airports"

export SUBSCRIPTION_NAME=`az account show | jq -r .name`
export SUBSCRIPTION_ID=`az account show | jq -r .id`
export SERVICE_PRINCIPAL_ID=` | jq -r .appId`

export ORGANISATION="<devops-organisation-name>"
export PROJECT_NAME="<devops-project-name>"
```

Create a pipeline for the project.
```bash
az devops configure --defaults organzation=https://dev.azure.com/$ORGANISATION project=$PROJECT_NAME

az ad sp create-for-rbac --name devops-$APP

az devops service-endpoint azurerm create --azure-rm-service-principal-id $SERVICE_PRINCIPAL_ID \
                                          --azure-rm-subscription-id $SUBSCRIPTION_ID \
                                          --azure-rm-subscription-name "$SUBSCRIPTION_NAME" \
                                          --azure-rm-tenant-id $TENANT_ID \
                                          --name $SERVICE_CONNECTION

cat << EOF > acr-service-connection-config.json
{
  "id": "$SERVICE_PRINCIPAL_ID",
  "description": "",
  "administratorsGroup": null,
  "authorization": {
    "parameters": {
      "username": "$CONTAINER_REGISTRY ",
      "password": "$DOCKER_REGISTRY_SERVER_PASSWORD",
      "email": "<some-email-address>",
      "registry": "$CONTAINER_REGISTRY.azurecr.io"
    },
    "scheme": "UsernamePassword"
  },
  "createdBy": null,
  "data": {
    "registrytype": "Others"
  },
  "name": "$ACR_REPO",
  "type": "dockerregistry",
  "url": "$CONTAINER_REGISTRY.azurecr.io",
  "readersGroup": null,
  "groupScopeId": null,
  "serviceEndpointProjectReferences": null,
  "operationStatus": null
}
EOF

az devops service-endpoint create --service-endpoint-configuration acr-service-connection-config.json
  
az pipelines create --name $PIPELINE_NAME --description 'Pipeline for $APP project' --repository-type github --repository $GITHUB_REPO --branch $BRANCH --project $PROJECT_NAME --yaml-path ./azure-pipelines.yml

az pipelines variable create --name AZURE_SUBSCRIPTION --value $SERVICE_CONNECTION --pipeline-name $PIPELINE_NAME
az pipelines variable create --name APP --value $APP --pipeline-name $PIPELINE_NAME
az pipelines variable create --name REGSITRY --value $CONTAINER_REGISTRY --pipeline-name $PIPELINE_NAME
az pipelines variable create --name REPOSITORY --value $ACR_REPO --pipeline-name $PIPELINE_NAME

az pipelines run --branch $BRANCH --name $PIPELINE_NAME --project quarkus
```
Due to az devops limitations continue to the devops page and accept pending permissions.
https://dev.azure.com/ <organisation> / <project> /_build
- click on pipeline
- click on run
- view pending permissions
- permit both

