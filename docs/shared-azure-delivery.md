# Shared Azure delivery from app_v1

This is a public operating source for the **existing** GetLink deployment,
not a new Azure environment. The old private repositories are not edited or
disabled by this preparation. Do not run Terraform apply, recreate the VM,
install a second Helm release, or create a second Argo Application as a way
to activate this workflow. Those actions can change or duplicate production
resources and are outside this release path.

## What a main push does

By default, every push/PR/manual run only checks the code. When the owner sets
the repository variable `ENABLE_AZURE_DELIVERY` to exactly `true`, a push to
`dongtaiduc04-star/app_v1` on `main` runs:

1. Backend build/tests/coverage; frontend install/lint/tests/build/production
   audit; delivery-helper tests.
   The old Checkstyle command is retained as explicitly **non-blocking**;
   it is not an enforced quality gate.
2. Analysis and a blocking quality gate on the existing SonarQube project
   `getlink-dtd`, using the existing SonarQube server.
3. Docker builds for the four existing packages below. Build all four before
   pushing any. Push full source-SHA tags, then replace their shared `latest`
   tags. Only this job grants its GitHub token `packages: write`.
4. A normal Git commit/push to **only** `dongtaiduc04-star/helm_v1/main`, changing
   only the four image selections in `helm/getlink-dtd/values-azure.yaml`.

| Component | Existing package |
| --- | --- |
| frontend | `ghcr.io/dongtaiduc04-star/getlink-dtd-frontend` |
| API gateway | `ghcr.io/dongtaiduc04-star/getlink-dtd-api-gateway` |
| auth service | `ghcr.io/dongtaiduc04-star/getlink-dtd-auth-service` |
| link service | `ghcr.io/dongtaiduc04-star/getlink-dtd-link-service` |

The updater is intentionally restricted to the reviewed scalar image mapping;
unexpected packages, placeholders, aliases, duplicate fields, mixed existing
SHA selections or non-SHA tags
stop the writer. It preserves domains, database/PVC settings and Secret names.
It downloads no YAML-editing executable. Checkout never persists Git credentials;
the Helm token is provided only to the checkout and normal push steps. A
concurrent owner's Helm edit causes a non-fast-forward rejection, not an
automatic rebase, reset or force-push.

## Owner setup before activation

Keep delivery **off** while reviewing and publishing the prepared source.
These are owner actions in GitHub, not actions performed by this repository:

| app_v1 setting | Purpose |
| --- | --- |
| Variable `AZURE_SONAR_HOST_URL` | Exactly `https://sonar-azure.dongtaiduc.me` (an optional trailing `/` is accepted), not a new server or placeholder. |
| Secret `AZURE_SONAR_TOKEN` | An analysis token authorized for the existing `getlink-dtd` project. Never paste its value into Git, logs or chat. |
| Secret `GITOPS_PAT` | A **separate** fine-grained personal access token whose repository selection is only `dongtaiduc04-star/helm_v1`, with Contents read/write and default Metadata read. Do not reuse or alter the old private Helm token. |
| Variable `ENABLE_AZURE_DELIVERY` | Leave unset/off until all shared-environment checks are complete; `true` opts main pushes into the writes above. |

For each existing GHCR package, verify its package settings grant the
`app_v1` repository **Actions access with write permission**, while retaining
the old `app` repository's access. Its `GITHUB_TOKEN` is repository-scoped;
`packages: write` alone must not be assumed to grant access to an existing
package belonging to a different repository. Keep the existing package
visibility and existing `ghcr-pull-secret` unless a separate reviewed change
is needed. Adding the new source label does not replace an access review.

Allow the five pinned actions in the workflow if an Actions allowlist is used.
No Azure login/subscription token, AWS credential, cluster admin credential,
database password, Cloudflare credential or old Helm write token is required
by this app workflow. Keep public pull-request workflows without secrets;
there is no `pull_request_target` delivery path.

Before the first enabled push, verify `helm_v1` has the reviewed existing
domain, resource/PVC/Secret names, package repositories and full SHA tags,
and compare rendered manifests with the selected live release. A missing
writer credential or package access can fail after some packages have been
published: publishing four packages is not an atomic transaction. Inspect
the failed run and existing Helm state before retrying.

## One application; manual source selection

The **same existing** Argo Application selects either `helm` or `helm_v1`,
not both. Retain its Application/release name, destination namespace, chart
path, values files and existing PVC/Secret references when switching source.
The owner separately reviews the diff and changes its source. Automatic sync
applies only the selected repository's desired state; it must not be enabled
against a placeholder or a materially different chart. Database and avatar
backups are required before changing a live deployment; source selection is
not a database backup or data-isolation mechanism.

**A push to the non-selected app repository still has shared effects.** Both
pipelines use the same four packages and Sonar project. Whichever run finishes
last can replace `latest` and become the newest Sonar analysis. The workflows'
concurrency controls are repository-local, not a cross-repository lock. Avoid
publishing from both app repositories at the same time. The website uses the
full SHA recorded by the selected Helm repository, not `latest`; a source SHA
tag is a convention, not registry-enforced immutability, and a rerun may
overwrite it with a rebuilt image. Track the deployed SHA and image digest
when reviewing a release or rollback.

Once Argo selects `helm_v1` with reviewed auto-sync settings, future activated
`app_v1/main` pushes can change the **existing website and existing database's
application code** without a second manual Argo sync. A new application version
can change data or schemas. Returning to the old repository restores its
selected application manifests/images, not database contents or guaranteed
schema compatibility. Use backward-compatible changes and a reviewed backup
and rollback plan.

## Verification and boundaries

After the first activated delivery, check all workflow jobs, the `helm_v1`
image SHA commit, the selected Argo source/status and running image digests.
Then test registration/login, profile/link editing, public pages, redirects,
QR/analytics and avatar persistence against the existing site. CI success
alone does not prove the live site works or that its data is safe.

These files only prepare the workflow. No activation variable, secret, package
permission, Argo selection, live sync, Git push or Azure resource operation is
performed by creating or testing them locally.

## Pinned official actions

Pins were checked against official release and commit pages (2026-10-08):

- [checkout v6.0.2](https://github.com/actions/checkout/releases/tag/v6.0.2): `de0fac2e4500dabe0009e67214ff5f5447ce83dd`.
- [setup-java v5.2.0](https://github.com/actions/setup-java/releases/tag/v5.2.0): `be666c2fcd27ec809703dec50e508c2fdc7f6654`.
- [setup-node v7.0.0](https://github.com/actions/setup-node/releases/tag/v7.0.0): `820762786026740c76f36085b0efc47a31fe5020`.
- [Sonar scan v8.3.0](https://github.com/SonarSource/sonarqube-scan-action/releases/tag/v8.3.0): `d209202bc7d53ff1cc128f7f907dac145c9d6ae9` (scanner 8.1.0.6389, signature verification enabled).
- [Sonar quality gate v1.2.0](https://github.com/SonarSource/sonarqube-quality-gate-action/releases/tag/v1.2.0): `cf038b0e0cdecfa9e56c198bbb7d21d751d62c3b`.

For token/permission behavior, see [GitHub's token guide](https://docs.github.com/en/actions/tutorials/authenticate-with-github_token).
