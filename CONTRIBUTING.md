# Maintenance policy

This is a public portfolio/operating-source publication. Only the owner is
intended to have human push and merge permissions; readers do not need
collaborator access to view, download or fork it. The opt-in delivery workflow
uses an owner-authorized token to write the four image selections to `helm_v1`.

A fork or pull request is a separate copy or proposed change, not permission to
update this repository. External suggestions are not automatically accepted.
Do not submit third-party code unless you can identify its source and confirm
the rights needed to publish it. Existing copyright/license notices must be
preserved. No new open-source license has been selected for original code.

Never include credentials, Terraform state, kubeconfigs or private logs in
public contributions. Follow SECURITY.md for sensitive reports.

Owner changes should use a branch and pull request, run the documented checks,
and be reviewed before merge. Azure delivery requires the explicit owner setup
described in [Shared Azure delivery](docs/shared-azure-delivery.md); the owner
has already activated the current shared environment. Pull-request checks
must remain read-only and must not receive delivery secrets. The restored old
Azure workflow has push/PR triggers, not a manual deployment trigger. Any added
auto-merge, broader automatic deployment, image package, cloud login or
cross-repository write permission requires a new owner/security review; it
must not be inferred from authorization to run checks.
