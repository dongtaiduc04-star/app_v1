# Security reporting

Do not post passwords, tokens, private keys, Terraform state, kubeconfigs,
database exports, or sensitive logs in public issues, pull requests or comments.

If GitHub private vulnerability reporting is enabled, use the repository's
Security tab to report privately. Otherwise ask the maintainer, without
including sensitive details, for a private reporting channel. Do not assume
that an ordinary issue or pull request is confidential.

By default, and always for pull requests/manual runs, CI only verifies code
with read-only repository permissions and no delivery secrets. Tests use
temporary databases, not the deployed database. Owner-enabled `app_v1/main`
pushes additionally contact the existing SonarQube project, publish the shared
GetLink GHCR packages and commit image selections only to `helm_v1`. When
that source is selected by the existing Argo Application, those changes can
deploy into the existing production environment. See
[Shared Azure delivery](docs/shared-azure-delivery.md) before activation.
No Azure/cloud login or database password is needed by the app workflow.
Examples that name a Secret contain references only; real values must be
supplied outside Git. An operator must change any factory/demo credential
before exposing a real service.

If a real credential is exposed, revoke or rotate it at its issuer and update
dependent services before cleaning source/history. Making a file private or
deleting it from the latest commit does not invalidate a leaked credential.

Only owner-approved changes belong in this repository. Do not grant write
permissions to a bot, deploy key or integration just to run checks. Delivery
requires a separately reviewed `helm_v1`-only token and package Actions access;
never expand the token to the original private Helm repository. Keep activation
off until the owner reviews the live source/data/rollback implications. Build
success and a secret scan do not constitute a full security assessment.
