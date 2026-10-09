# Azure delivery from app_v1

This public copy follows the **old Azure workflow**, not a new architecture:
build/test/Checkstyle → Sonar quality gate → four GHCR images → Azure Helm
values → the existing Argo Application. The application/business source and
Dockerfiles are unchanged; only v1 repository targets/access and minimal public
safety differ. The old private repositories remain unchanged. No AWS workflow,
fresh Azure environment, second Helm release or second Argo Application is used.

## Recorded activation and current setup

On 2026-10-08 the owner confirmed successful delivery of app commit
`d6a86653ec927e5049de476ee67d0d66e2a9c437`, Helm commit
`6f05266eb2e3034f28c8d5492c5c20a1fa1307dc`, Argo Synced/Healthy and all four
application Pods Ready. The owner confirmed the existing website works.
These are historical observations, not a live availability check or validation
of subsequent source changes. See [the GUI-first Vietnamese runbook](azure-operations.vi.md).

Before publishing the restored original workflow, add `HELM_REPO_NAME=helm_v1`
in **app_v1 → Settings → Secrets and variables → Actions → Variables**. Keep
the existing two secrets and other two variables:

| Type | Name | Required value/purpose |
| --- | --- | --- |
| Secret | `AZURE_SONAR_TOKEN` | Analysis token for the existing Azure Sonar project `getlink-dtd` |
| Secret | `GITOPS_PAT` | Separate fine-grained token: only `helm_v1`, Contents read/write and default Metadata read |
| Variable | `AZURE_SONAR_HOST_URL` | `https://sonar-azure.dongtaiduc.me` (optional trailing slash) |
| Variable | `HELM_REPO_NAME` | Exactly `helm_v1`; any other target stops before publication |
| Variable | `ENABLE_AZURE_DELIVERY` | Exactly `true` for owner-authorized main delivery |

The four existing GHCR packages retain their existing visibility and old
repository access. `app_v1` already has Actions access with Write on each.
Publishing uses its built-in short-lived `GITHUB_TOKEN`; no registry PAT or
Azure/AWS/cluster/database/Cloudflare credential is needed in this app repo.
Never paste secret values into Git, workflow logs, screenshots or chat.

## The original three jobs

1. **Build, Test, Checkstyle & SonarQube (PRs)** (`build-and-quality`): the
   original Java 17/Maven cache, Node 22/npm cache and Sonar cache; Maven clean
   verify, legacy non-blocking Checkstyle, frontend install/lint/tests/build.
   An activated owner `main` push also scans the existing Sonar server/project
   and waits for its blocking quality gate. PRs run the same original code
   checks without Sonar secrets or publication.
2. **Build Docker images and push to GHCR (push to main)** (`docker-build-push`):
   the original Docker login action, four builds and SHA + `latest` pushes to
   `ghcr.io/dongtaiduc04-star/getlink-dtd-{frontend,api-gateway,auth-service,link-service}`.
   Only this job has `packages: write`.
3. **Update GetLink Helm values** (`update-helm`): checkout `helm_v1/main`, use
   the original `mikefarah/yq` v4.34.1 expression to update the four image
   repositories/tags in `helm/getlink-dtd/values-azure.yaml`, then commit and
   normally push. The one existing Argo Application `getlink-dtd`, already
   selecting `helm_v1` with auto-sync/prune/self-heal, deploys that desired state.

Triggers are the original `push main` and `pull_request main`; there is no
`workflow_dispatch` release. No new audit/helper-test stage, custom YAML
updater, VM automation or Terraform apply stage has been added. Unset/off
`ENABLE_AZURE_DELIVERY` leaves code checks only; forks cannot deliver into the
owner's shared environment.

## Minimal public safety differences

- Actions are pinned to full official commit SHAs at the same original versions.
- Writes require the exact `dongtaiduc04-star/app_v1` repository, a `main` push,
  the owner opt-in flag and successful preceding jobs. Sonar HTTPS URL and
  `HELM_REPO_NAME=helm_v1` are checked before image publication.
- The official yq binary's reviewed SHA-256 is checked **before execution**.
- Checkout does not persist credentials. Only the Helm writer uses its narrow
  token; the push authorization header is masked and exists only for that
  command. Only the Azure values file can be staged/committed.
- Main releases are not canceled mid-publication. A concurrent Helm update
  rejects the normal push instead of force-pushing or automatically rebasing.

The updater intentionally uses the original `yq` behavior rather than a new
custom schema/parser. It may normalize YAML formatting in that one file.
Publishing four packages is not atomic; inspect a failed run and Helm commit
before retrying, without broadening token permissions or bypassing the gate.

## Shared resources and infrastructure boundary

The old and v1 pipelines share the four packages and Sonar project; whichever
finishes last can replace `latest` and the newest Sonar analysis. Avoid parallel
old/v1 publication. The website deploys the full SHA from the selected Helm
source, not `latest`. SHA tags are a convention, not registry-enforced
immutability; rebuilding the same commit may replace a digest.

The website, database and avatar volumes are shared. Selecting old manifests or
images does not restore database contents or guarantee schema compatibility;
data/schema changes need a separate reviewed backup/rollback plan. Routine
owner confirmation can use login, old link/avatar data and a public profile in
the browser; no broad test suite or new data is required just to confirm a
normal release.

Terraform remains manual as in the old Azure project. `infra_v1` CI checks
source only and is not a missing application CD stage. Before changing
infrastructure from v1, verify the explicit handover to the **same private
state**, choose one writer and never run old/v1 Terraform simultaneously. See
[shared infrastructure control](https://github.com/dongtaiduc04-star/infra_v1/blob/main/azure/SHARED-CONTROL.md).
Creating these source files does not itself change any GitHub setting or live
Azure/Argo resource.

## Official pin and checksum evidence

Pins were checked on 2026-10-08 against official release/commit pages:

- [checkout v6.0.2](https://github.com/actions/checkout/releases/tag/v6.0.2): `de0fac2e4500dabe0009e67214ff5f5447ce83dd`.
- [setup-java v5.2.0](https://github.com/actions/setup-java/releases/tag/v5.2.0): `be666c2fcd27ec809703dec50e508c2fdc7f6654`.
- [setup-node v7.0.0](https://github.com/actions/setup-node/releases/tag/v7.0.0): `820762786026740c76f36085b0efc47a31fe5020`.
- [cache v4](https://github.com/actions/cache/releases/tag/v4): `0057852bfaa89a56745cba8c7296529d2fc39830` (v4.3.0).
- [docker/login-action v3](https://github.com/docker/login-action/releases/tag/v3): `c94ce9fb468520275223c153574b00df6fe4bcc9`.
- [Sonar scan v8.3.0](https://github.com/SonarSource/sonarqube-scan-action/releases/tag/v8.3.0): `d209202bc7d53ff1cc128f7f907dac145c9d6ae9`.
- [Sonar quality gate v1.2.0](https://github.com/SonarSource/sonarqube-quality-gate-action/releases/tag/v1.2.0): `cf038b0e0cdecfa9e56c198bbb7d21d751d62c3b`.

[yq v4.34.1](https://github.com/mikefarah/yq/releases/tag/v4.34.1) Linux amd64
SHA-256: `c5a92a572b3bd0024c7b1fe8072be3251156874c05f017c23f9db7b3254ae71a`.
Read from the official [checksums](https://github.com/mikefarah/yq/releases/download/v4.34.1/checksums)
and [algorithm order](https://github.com/mikefarah/yq/releases/download/v4.34.1/checksums_hashes_order)
assets; no unverified executable was run during source preparation.
