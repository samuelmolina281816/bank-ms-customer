# bank-ms-customer

## Architecture and scope

This service owns customer identity, contact, profile, status and eligibility.
It does not own accounts, credits, cards, transactions, authentication or KYC.
The implementation separates `domain`, `application` and `framework` according
to the hexagonal architecture. Public APIs use `customerNumber`; the database
primary key is internal.

The project uses PostgreSQL with Spring Data R2DBC as an approved exception to
the initial MongoDB recommendation. Relational uniqueness, transactional
constraints and atomic sequences protect customer-document uniqueness and
public-number allocation. This service owns its database; other services must
consume its API rather than access its tables. No `@Query` or dynamically
constructed SQL is used.

## Run locally

Requirements: Java 17+, PostgreSQL, and Maven (or the included wrapper).

Create a local `.env` from the template and set the real PostgreSQL password.
The `.env` file is ignored by Git; do not commit it:

```bash
cp .env .env
# Edit .env and set CUSTOMER_DB_PASSWORD to the local PostgreSQL password.
```

Start the portable Vault server in a separate terminal. In Git Bash, the
Windows path `C:\Dev\Software\vault\vault.exe` is written as
`/c/Dev/Software/vault/vault.exe`; no system-wide Vault installation is needed:

```bash
/c/Dev/Software/vault/vault.exe server -dev -dev-listen-address=127.0.0.1:8200
```

With Vault running, open a second Git Bash terminal at the project root and
run the script:

```bash
bash scripts/vault-crear-secret.sh
```

The script loads local values from `.env`, then writes and verifies the secret
using the portable CLI without printing secret values to the terminal.

Vault stores `CUSTOMER_DB_USERNAME` and `CUSTOMER_DB_PASSWORD` at
`secret/banca-project/bank-ms-customer`; the service reads those keys when it
starts. The corresponding contexts for the other services can be
`secret/banca-project/bank-ms-account`, `.../bank-ms-card`, and
`.../bank-ms-credit` on the same Vault instance and port. Separate paths
organize each application's secrets; they do not start separate Vault servers
or require separate ports.

The development Vault server uses in-memory storage, so its secrets are lost
when the process stops; restart it and rerun the seed script as needed. Its
development root token is only for local testing and must never be used in a
shared or production environment.

For IntelliJ, define `VAULT_TOKEN` and, if needed, `VAULT_ADDR`,
`CONFIG_SERVER_URL` and `DB_URL` in the Run/Debug Configuration environment
variables. Environment variables set in another terminal are not inherited by
an IDE that was already running. The service uses the `local` profile, reads
Vault at `http://127.0.0.1:8200` by default, and connects to
Config Server at `http://localhost:8888`; keep `CONFIG_SERVER_ENABLED`
enabled during integration. The Config Server import remains optional, but the
Vault import is required so startup does not silently fall back to credentials
embedded in application configuration. Set `VAULT_TOKEN` in the terminal or
IDE process used to start the service; environment variables set in a
different terminal are not inherited. For example, in Git Bash:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

The portable Vault command above listens only on loopback, which is appropriate
when the microservices run directly on the host. A service running in Docker
cannot reach that loopback listener through `localhost`; for local Docker
clients, bind Vault to an address reachable from the containers and set
`VAULT_ADDR` accordingly (for example, `http://host.docker.internal:8200` on
Docker Desktop). Restrict that listener with the host firewall and never expose
the development Vault server to an untrusted network.

On startup, Spring executes the single `src/main/resources/schema.sql`. It is
idempotent: it creates missing objects and recreates only the integrity
triggers, so subsequent starts preserve customer data. For an existing local
database created with the earlier table layout, stop the service and run this
one-time destructive reset **only against the dedicated `bank_customer_dev`
database** before starting the new version:

```sql
DROP TABLE IF EXISTS customer_contact, business_customer, personal_customer,
    customers, customer_profiles, flyway_schema_history CASCADE;
DROP SEQUENCE IF EXISTS customer_number_seq CASCADE;
DROP FUNCTION IF EXISTS enforce_customer_aggregate_integrity() CASCADE;
DROP FUNCTION IF EXISTS assert_customer_aggregate_integrity(BIGINT) CASCADE;
```

This reset permanently removes all customer data. Do not execute it against a
shared or production database. After the reset, starting the service creates
the current schema; do not repeat the reset on later restarts. The sequence
starts `customerNumber` at `10000001` and is limited to 8 digits.

The relational model keeps common identity in `customers`, names in
`personal_customer` or `business_customer`, and the one-to-one contact record
in `customer_contact`. `customer_profiles` is a type-scoped catalog and a
composite foreign key prevents incompatible profile/type pairs. Deferred
PostgreSQL constraint triggers require exactly one matching subtype at commit.
The catalog currently enables `STANDARD` only; future profiles can be added
without changing the table structure, while their behavior still requires
application support.

For shared environments, configure a properly initialized and secured Vault
deployment with production authentication and policies; do not use the
development server, its root token, or its in-memory storage. Ensure
Spring Cloud Config remains enabled and set `CONFIG_SERVER_URL` and `VAULT_ADDR`
to reachable service addresses. Keep non-sensitive configuration in Config
Server and store database credentials in Vault.
The PostgreSQL API integration test runs against an isolated Testcontainers
database when Docker is available; it is skipped automatically without Docker.

## OpenAPI

The source contract is `src/main/resources/properties/openapi.yaml`. Maven
generates the WebFlux API interfaces and REST models into
`target/generated-sources/openapi`; do not edit or commit generated files.
The handwritten REST adapters implement those interfaces and delegate to
application input ports.

```powershell
.\mvnw.cmd clean verify
```

Swagger UI is at `http://localhost:8080/swagger-ui/index.html`. Displaying the
YAML contract is not itself proof that routes are implemented; the REST
adapters register the operations.

## Postman

Import `src/test/resources/postman/bank-ms-customer.postman_collection.json`
and optionally `bank-ms-customer-local.postman_environment.json`. Start the
service and select the `bank-ms-customer - local` environment, then run the
collection in order. The personal flow generates a fresh DNI and verifies
create/list/find/update/validation/duplicate/logical-delete/eligibility behavior;
the business flow generates a unique RUC and checks business-customer rules.
The collection targets a test/development instance and does not require
credentials.

## Customer rules

- Personal customers: DNI (8 digits), CE or passport (5–12 alphanumeric characters).
- Business customers: RUC (11 digits).
- Customer documents are unique; new customers start ACTIVE with STANDARD profile.
- A customer has exactly one subtype matching its customer type and one contact
  record. Account, credit, card, transfer, Kafka, Redis, JWT and Yanki features
  remain outside this Phase I implementation.
- DELETE deactivates logically and retains the record.
- Eligibility returns only public customer data and is true for ACTIVE customers.
