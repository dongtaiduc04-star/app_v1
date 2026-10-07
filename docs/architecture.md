# Platform architecture

## Request flow

The Kubernetes ingress exposes the Next.js frontend and routes `/api/**` to the
Spring Cloud Gateway. The gateway is the only externally reachable backend.
It applies CORS, request correlation, authentication forwarding, and endpoint
rate limits before routing to an internal service.

```text
Browser -> Ingress -> Next.js frontend
                  -> /api -> API Gateway -> Auth service
                                         -> Link service
Auth service -> authdb, Redis, RabbitMQ
Link service -> linkdb, RabbitMQ
```

Services never read another service's database. The link service receives the
authenticated account ID and role from verified JWT claims. Account lifecycle
changes cross the service boundary through versioned RabbitMQ events.

## Service ownership

### API Gateway

- Routes versioned APIs and exposes no business persistence.
- Verifies access-token signatures and forwards trusted identity headers only
  after removing user-supplied copies.
- Applies configuration-driven CORS and rate limits backed by Redis.

### Auth service

- Owns accounts, password credentials, connected identities, roles, sessions,
  password-reset tokens, and account deletion state in `authdb`.
- Issues short-lived access JWTs and rotates refresh tokens stored as hashes in
  Redis. Refresh tokens are transported in HttpOnly cookies.
- Publishes `USER_REGISTERED` and account lifecycle events using a transactional
  outbox. Consumers must be idempotent.

### Link service

- Owns public profiles, links, ordering, redirect records, QR generation, and
  aggregate analytics in `linkdb`.
- Enforces a maximum of 30 links per active account transactionally.
- Publishes `PROFILE_VIEWED` and `LINK_CLICKED` through an outbox and consumes
  events with a durable event ID to reject duplicates.

## Data and privacy

- Raw IP addresses are never persisted. A rotating, environment-supplied salt
  is used to derive a bounded technical hash when abuse detection requires it.
- Anonymous visitors receive a random session identifier; owners only see
  aggregates. Analytics expires after 90 days.
- Public URLs are derived from `PUBLIC_BASE_URL`.
- User-provided destinations accept only absolute HTTP/HTTPS URLs. Redirects
  resolve stored link IDs and never accept an arbitrary destination parameter.
- Favicon retrieval must block private, loopback, link-local, and metadata IPs;
  re-check DNS after redirects; restrict redirects, size, MIME type, and timeout.

## Authentication

- Access tokens are short-lived and held in application memory, not
  `localStorage`. A refresh cookie is HttpOnly, Secure in non-local environments,
  SameSite=Lax, and scoped to the refresh endpoint where practical.
- State-changing cookie endpoints validate Origin and a CSRF token.
- Passwords use an adaptive password hash. Login failures are rate-limited and
  can temporarily lock an account.

## Account deletion

Deletion immediately disables login and public profile access, then marks the
account for permanent removal after 30 days. The proposed MVP recovery path is
Super Admin restoration during that window; this remains a product decision and
must be approved before implementation.

## Runtime configuration

Secrets are never committed. Local values come from an ignored `.env`; Kubernetes
workloads reference an existing Secret. Domain, allowed origins, token settings,
database addresses, RabbitMQ, Redis, and build metadata are configurable.
