import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";
import { assertReleaseIdentity, assertSonarConfiguration, updateAzureImages } from "../azure-delivery.mjs";

const previous = "a".repeat(40);
const next = "b".repeat(40);
const components = { frontend: "frontend", apiGateway: "api-gateway", authService: "auth-service", linkService: "link-service" };
const fixture = [
  "fullnameOverride: getlink-dtd", "images:",
  ...Object.entries(components).flatMap(([key, image]) => [
    `  ${key}:`, `    repository: ghcr.io/dongtaiduc04-star/getlink-dtd-${image}`,
    `    tag: ${previous} # preserve this comment`, "    pullPolicy: IfNotPresent",
  ]),
  "app:", "  publicBaseUrl: https://getlink-azure.dongtaiduc.me", "mysql:",
  "  existingSecret: getlink-dtd-db", "  persistence:", "    storageClass: local-path", "",
].join("\n");

test("only four image tags change; unrelated bytes and comments stay intact", () => {
  const updated = updateAzureImages(fixture, next);
  assert.equal(updated, fixture.replaceAll(previous, next));
  assert.equal(updateAzureImages(updated, next), updated);
});
test("CRLF bytes are preserved", () => {
  const crlf = fixture.replaceAll("\n", "\r\n");
  assert.equal(updateAzureImages(crlf, next), crlf.replaceAll(previous, next));
});
test("malformed SHA, latest and placeholders fail closed", () => {
  for (const sha of ["main", "latest", "a".repeat(7), "B".repeat(40), `${next}\n`]) {
    assert.throws(() => updateAzureImages(fixture, sha));
  }
  assert.throws(() => updateAzureImages(fixture.replace(previous, "latest"), next));
  assert.throws(() => updateAzureImages(fixture.replace(previous, "replace-with-your-image-tag"), next));
});
test("old/new package-name changes, unknown mappings and duplicate fields are rejected", () => {
  const invalid = [
    fixture.replace("getlink-dtd-frontend", "getlink-dtd-v1-frontend"),
    fixture.replace("  frontend:", "  frontend: &shared"),
    fixture.replace("  frontend:", "  unexpected:"),
    fixture.replace("    pullPolicy: IfNotPresent", "    pullPolicy: IfNotPresent\n    tag: " + previous),
    fixture.replace("    pullPolicy: IfNotPresent", "    extra: true"),
    fixture + "images:\n",
    fixture + "'images': {}\n",
    fixture + '"images": {}\n',
    fixture + '"im\\u0061ges": {}\n',
    fixture + "&duplicate images: {}\n",
    fixture + "---\nother: value\n",
    fixture.replace("images:", "images: { frontend: {} }"),
    fixture.replace("    pullPolicy: IfNotPresent", "\tpullPolicy: IfNotPresent"),
    fixture.replace("\napp:", "\r\napp:"),
    fixture.replace(previous, "c".repeat(40)),
  ];
  for (const content of invalid) assert.throws(() => updateAzureImages(content, next));
});
test("release identity accepts only the owner-enabled exact main push", () => {
  const env = { ENABLE_AZURE_DELIVERY: "true", GITHUB_REPOSITORY: "dongtaiduc04-star/app_v1", GITHUB_EVENT_NAME: "push", GITHUB_REF: "refs/heads/main", GITHUB_SHA: next };
  assert.doesNotThrow(() => assertReleaseIdentity(env));
  for (const [key, value] of Object.entries({ ENABLE_AZURE_DELIVERY: "false", GITHUB_REPOSITORY: "dongtaiduc04-star/app", GITHUB_EVENT_NAME: "pull_request", GITHUB_REF: "refs/heads/test", GITHUB_SHA: "latest" })) {
    assert.throws(() => assertReleaseIdentity({ ...env, [key]: value }));
  }
  assert.throws(() => assertReleaseIdentity({ ...env, GITHUB_EVENT_NAME: "workflow_dispatch" }));
});
test("Sonar configuration requires a token and the exact existing HTTPS server", () => {
  assert.doesNotThrow(() => assertSonarConfiguration({ SONAR_TOKEN: "test-fixture-only", SONAR_HOST_URL: "https://sonar-azure.dongtaiduc.me" }));
  assert.doesNotThrow(() => assertSonarConfiguration({ SONAR_TOKEN: "test-fixture-only", SONAR_HOST_URL: "https://sonar-azure.dongtaiduc.me/" }));
  for (const url of ["http://sonar-azure.dongtaiduc.me", "https://token:secret@sonar-azure.dongtaiduc.me", "https://sonar.example.com", "https://localhost", "https://sonar-azure.dongtaiduc.me?token=test", "https://sonar-azure.dongtaiduc.me/other", "https://sonar-azure.dongtaiduc.me:443", "https://another-real-server.dongtaiduc.me"]) {
    assert.throws(() => assertSonarConfiguration({ SONAR_TOKEN: "test-fixture-only", SONAR_HOST_URL: url }));
  }
  assert.throws(() => assertSonarConfiguration({ SONAR_HOST_URL: "https://sonar-azure.dongtaiduc.me" }));
});
test("workflow uses only pinned actions and keeps the Helm writer target fixed", () => {
  const workflow = readFileSync(new URL("../../.github/workflows/ci.yml", import.meta.url), "utf8");
  const actions = [...workflow.matchAll(/uses: ([^\s]+)\s/g)].map((match) => match[1]);
  assert.ok(actions.length > 0);
  assert.ok(actions.every((action) => /^[\w-]+\/[\w-]+@[0-9a-f]{40}$/.test(action)));
  assert.ok(workflow.includes("repository: dongtaiduc04-star/helm_v1"));
  assert.ok(!workflow.includes("HELM_REPO_NAME"));
  assert.ok(!workflow.includes("pull_request_target"));
  assert.ok(!workflow.includes("git pull --rebase"));
  assert.equal((workflow.match(/packages: write/g) ?? []).length, 1);
  assert.equal((workflow.match(/persist-credentials: false/g) ?? []).length, 4);
});
