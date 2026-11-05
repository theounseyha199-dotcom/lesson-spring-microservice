# Local dependencies

Run `./run-all.sh` from the repository root to start dependencies and applications.
See [the project guide](../README.md) for ports, logs and API authentication.

The Compose files use the shared `ite-net` Docker network. The startup script
creates it if needed. PostgreSQL is exposed on 9543, MongoDB on 2026, and Consul
on 8500. PostgreSQL initialization scripts run only when its volume is first
created. Existing volumes and data are preserved.
