# app_v1 — GetLink DTD

A portfolio publication of the GetLink DTD link-in-bio application for Azure.
Local Docker Compose remains available for development. This is a
public code copy alongside the private repositories used to operate the
original environment. Delivery is disabled by default. After the owner
explicitly activates it, a push to `app_v1/main` publishes to the **same**
GetLink image packages and updates `helm_v1`; the existing Argo CD Application
deploys whichever Helm repository the owner has selected.

Companion publications:
- [helm_v1](https://github.com/dongtaiduc04-star/helm_v1): Azure/k3s application Helm chart.
- [infra_v1](https://github.com/dongtaiduc04-star/infra_v1): Azure infrastructure source and shared-environment operating instructions.

## Application

| Module | Responsibility |
| --- | --- |
| `frontend` | Next.js profile pages, account flows and dashboards |
| `api-gateway` | API routing, CORS, authentication forwarding and rate limits |
| `auth-service` | Accounts, password handling, JWT and refresh sessions |
| `link-service` | Profiles, links, redirects, QR codes and aggregate analytics |
| `contracts/openapi` | HTTP API contracts |
| `docs` | Architecture and security decisions |

The backend targets Java 17 with Maven. Local MySQL uses separate `authdb`
and `linkdb` databases. Uploaded avatars are stored in a named volume.

## Local development

1. Copy `.env.example` to `.env`; replace every `change-me` value.
2. Generate a high-entropy `JWT_SECRET` of at least 32 UTF-8 bytes.
3. Make ports 3000, 3306 and 8080–8082 available.
4. Run `docker compose up --build -d`.
5. Open `http://localhost:3000`; check services with `docker compose ps`.

`docker compose down` preserves named volumes. Adding `--volumes` deletes
local database and avatar data; do not use it unless that deletion is intended.

Never commit `.env`, real passwords, private keys or database exports.
`PASSWORD_RESET_LOG_TOKEN` is only for development; never enable it for a
production deployment. Keep secure refresh cookies enabled outside local HTTP.

## Checks and opt-in Azure delivery

`.github/workflows/ci.yml` runs on pull requests and pushes to `main`:

- Maven build, tests and JaCoCo coverage reports.
- Frontend lint, 13 current unit tests with coverage thresholds, and build.
- Production dependency audit and offline delivery-helper tests.

Checks use GitHub-hosted Ubuntu 24.04 runners and read-only repository
permissions. With `ENABLE_AZURE_DELIVERY` unset or not exactly `true`, there is
no Sonar upload, image publication or Helm write. Pull requests and manual
`workflow_dispatch` runs are always checks-only. Checkout does not retain its
authentication credential. Backend integration tests use temporary
Testcontainers databases, not the deployed database.

The owner-authorized delivery path reuses the existing Azure/k3s environment,
domain, MySQL data, avatars and SonarQube project `getlink-dtd`; it does not
create a second environment. Read [Shared Azure delivery](docs/shared-azure-delivery.md)
before enabling it. Publishing to `helm_v1` is not itself an Argo repository
switch. The original app/Helm/infra repositories remain unchanged.

Run the same checks locally:

```sh
mvn -B -DskipTests=false clean verify
cd frontend
npm ci
npm run lint
npm test
npm run build
npm audit --omit=dev
cd ..
node --test scripts/tests/azure-delivery.test.mjs
```

A successful build is not a complete security audit. At the reviewed source
snapshot, production npm audit reported zero findings; the full npm audit still
had five high development-tool findings in the braces chain and an unsupported
ESLint 9 release. These are known open risks, not suppressed or fixed by copying
the repository. Recheck advisories before reusing this code.

## Publication and maintenance policy

Only the repository owner is intended to have human write/merge access. The
opt-in workflow also uses an owner-authorized, `helm_v1`-only token to commit
the four image selections; outsiders do not receive that token or Git access.
Public
visibility allows inspection, download and forks; it does not grant write
access to this repository. See [CONTRIBUTING.md](CONTRIBUTING.md) and
[SECURITY.md](SECURITY.md).

No MIT or other new open-source license has been added. Public visibility is
not a blanket license to reuse or redistribute the code. Existing third-party
license terms and notices remain applicable; source provenance must be checked
before publication. Deployment credentials and the old operational history
are intentionally not part of this copy.
