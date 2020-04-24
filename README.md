# Quarkus on Azure

This project describes the steps involves setting a quarkus application up and running on Azure using Azure DevOps, along with some Azure services (SQL Server, App Service, AAD SSO) 

## Getting Started
These instructions will get you set the project up and running on your local machine for development and testing purposes. Setting up your services using the Azure CLI tool and initlaising your Azure Devops pipeline. 

### Prerequisites
An Azure subscription (free tier)
- [GraalVM](https://www.graalvm.org/downloads/)
- [Azure CLI \| Microsoft Docs](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest)
- Java 11
- Docker
- Maven 3.6.x
- cURL (or alternative)
- Devops organisation

### Setting up Azure services
After you install the Azure CLI, set up the azure services.
You will require the following environment variables:
```bash
RESOURCE_GROUP="quarkus"

APP_SERVICE_PLAN="QuarkusServicePlan"
APP="quarkus-mssql-azure"

SQLSERVER="quarkusDatabaseServer"
DATABASE="airport"
SQL_USR="<replace-with-a-username>"
SQL_PWD="<replace-with-a-password>"

LOCATION="northeurope"

CONTAINER_REGISTRY="quarkuscontainerregistry"

CLIENT_SECRET="<replace-with-a-secret>"
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
az sql db create --resource-group $RESOURCE_GROUP --server $SQLSERVER --name $DATABASE --sample-name AdventureWorksLT --edition GeneralPurpose --family Gen4 --capacity 1 --zone-redundant false 


# Azure Container Registry
az acr create --resource-group $RESOURCE_GROUP --name $CONTAINER_REGISTRY --admin-enabled true  --sku Basic

# App registration - Outputs CLIENT_ID
az ad app create --display-name $APP --reply-urls http://localhost:8080/auth https://$APP.azurewebsites.net/  --password $CLIENT_SECRET
```
**Take note of the appId, this as the `CLIENT_ID`.**
`"appId": "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"`

### Configurations
Retrieve the connection string, tenant id & the generated container registry password.
Remember to add the `$SQL_PWD` to the connection string.

```bash
# Connection string
az sql db show-connection-string -s $SQLSERVER -n $DATABASE -c jdbc
# Tenant ID
azure account show
# Container registry password
az acr credential show -n $CONTAINER_REGISTRY
```
#### Azure configurations
                        
Now that all services are created the App Service requires permissions for both container registry and sql server.
```bash
az webapp config appsettings set --resource-group $RESOURCE_GROUP$ --name $APP \
  --settings DOCKER_REGISTRY_SERVER_URL=$CONTAINER_REGISTRY.azurecr.io DOCKER_REGISTRY_SERVER_USERNAME=$CONTAINER_REGISTRY DOCKER_REGISTRY_SERVER_PASSWORD=$DOCKER_REGISTRY_SERVER_PASSWORD

az webapp config appsettings set --resource-group $RESOURCE_GROUP$ --name $APP \
  --settings QUARKUS_OIDC_AUTH-SERVER-URL=https://login.microsoftonline.com/$TENANT_ID/v2.0 QUARKUS_OIDC_CLIENT=$CLIENT_ID QUARKUS_OIDC_CREDENTIALS_SECRET=$CLIENT_SECRET

az webapp config appsettings set --resource-group $RESOURCE_GROUP$ --name $APP \
  --settings QUARKUS_DATASOURCE_URL=$CONNECTION_STRING
```

#### Local configurations

Run a mssql container for local development using docker.

`docker run -e 'ACCEPT_EULA=Y' -e 'SA_PASSWORD=yourStrong(!)Password' -d --name mssql -p 1433:1433 mcr.microsoft.com/mssql/server:latest`

Create an `.env` file in the root directory, using the `.env-sample` template including variables for the tenant id, client id and client secret.

```
# DB Configuration
QUARKUS_DATASOURCE_PASSWORD=yourStrong(!)Password
QUARKUS_DATASOURCE_USERNAME=sa

# OIDC Configuration
QUARKUS_OIDC_AUTH-SERVER-URL=https://login.microsoftonline.com/<TENANT_ID>/v2.0
QUARKUS_OIDC_CLIENT-ID=<CLIENT_ID>
QUARKUS_OIDC_CREDENTIALS_SECRET=<CLIENT_SECRET>
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
az devops configure --defaults organzation=https://dev.azure.com/$ORGANISATION
az devops project create --name $PROJECT_NAME
```
Variables used in devops project
* AZURE_SUBSCRIPTION
* WEB_APPP
* REGSITRY
* REPOSITORY


Create a pipeline for the project.
```bash
az pipelines create --name $APP --description 'Pipeline for $APP project'
--repository $GITHUB_REPO$ --branch $BRANCH --yaml-path ./azure-pipelines.yml
