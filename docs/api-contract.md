# API contract conventions

The external API prefix is `/api/v1`. JSON is UTF-8 and timestamps use RFC 3339
UTC. IDs are opaque UUID strings. The gateway propagates a request ID in
`X-Request-Id`.

## Authentication

Protected endpoints use `Authorization: Bearer <access-token>`. Refresh tokens
are never returned in JSON; they use an HttpOnly cookie. Clients call the refresh
endpoint after an expired access token and keep the replacement access token in
memory.

Access tokens are HMAC-signed, short-lived, and validated for signature, expiry,
and issuer. Refresh tokens are opaque 256-bit values; only SHA-256 hashes are
stored. Every refresh rotates the token. Reuse of an older token revokes its
entire token family. Logout can revoke either the current session or every
session owned by the account.

Password reset requests always return the same accepted response to prevent
account enumeration. Reset tokens are opaque, hashed at rest, expiring,
single-use, and revoke all refresh sessions when consumed.

## Error envelope

```json
{
  "type": "https://getlink.example/problems/validation-error",
  "title": "Validation failed",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "One or more fields are invalid.",
  "requestId": "opaque-request-id",
  "errors": [{ "field": "username", "message": "invalid format" }]
}
```

Errors follow `application/problem+json`. Stable machine-readable `code` values
must not expose credentials, tokens, stack traces, or account-enumeration clues.

## Resource rules

- Usernames match `^[a-z0-9-]{3,30}$`, are case-insensitively unique, and reject
  reserved system names.
- Link destinations are absolute HTTP/HTTPS URLs and are validated again when
  saved. A user can own at most 30 non-deleted links.
- Reordering submits the complete ordered list with an optimistic version to
  prevent lost updates.
- Public redirects use `/api/v1/r/{linkId}` and look up the saved destination.
- Pagination uses `page`, `size`, and deterministic sorting. Admin list sizes are
  capped server-side.

The YAML files in `contracts/openapi` are the executable contract baseline. More
detailed schemas and examples will be added alongside the implementing service.
