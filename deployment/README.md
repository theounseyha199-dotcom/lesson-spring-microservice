# Local dependencies

Compose definitions are provided for PostgreSQL, MongoDB, Consul and Vault. They use the shared external ite-net Docker
network. Create it once if absent, then start only the dependencies you need. There is no repository run-all.sh script.
From the repository root, for IAM:

```sh
docker network create ite-net
docker compose --env-file .local/runtime.properties -f deployment/postgres/docker-compose.yaml up -d
```

The network-create command is only for a missing network. PostgreSQL is exposed on 9543; MongoDB uses 2026, Consul 8500
and Vault 8200. Other definitions are in deployment/mongodb, deployment/consul and deployment/vault. See the
[project guide](../README.md) and [IAM setup](../microservices/iam-service/README.md) for application configuration.

PostgreSQL requires external IAM_DB_PASSWORD instead of a source-code password. For a current database, use its existing
value; editing the compose environment does not rotate a stored PostgreSQL role password. Initialization scripts run
only when the volume is first created. Preserve existing volumes/data; no volume reset is needed for this configuration
change. On deployment, inject IAM_DB_PASSWORD through the environment or a protected runtime env file.

The workstation's .local/runtime.properties is Git-ignored. Never print a fully expanded compose configuration into
logs containing credentials. `docker compose ... config --quiet` validates the definition without dumping its values.
The audit validated the configuration without recreating or restarting the running database container.
