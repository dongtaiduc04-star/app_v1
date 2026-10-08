import { readFileSync, realpathSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

export const SOURCE_REPOSITORY = "dongtaiduc04-star/app_v1";
export const HELM_REPOSITORY = "dongtaiduc04-star/helm_v1";
export const VALUES_PATH = "helm/getlink-dtd/values-azure.yaml";
const images = Object.freeze({
  frontend: "ghcr.io/dongtaiduc04-star/getlink-dtd-frontend",
  apiGateway: "ghcr.io/dongtaiduc04-star/getlink-dtd-api-gateway",
  authService: "ghcr.io/dongtaiduc04-star/getlink-dtd-auth-service",
  linkService: "ghcr.io/dongtaiduc04-star/getlink-dtd-link-service",
});

export function assertReleaseIdentity(env) {
  if (env.ENABLE_AZURE_DELIVERY !== "true" ||
      env.GITHUB_REPOSITORY !== SOURCE_REPOSITORY ||
      env.GITHUB_EVENT_NAME !== "push" || env.GITHUB_REF !== "refs/heads/main" ||
      !/^[0-9a-f]{40}$/.test(env.GITHUB_SHA ?? "")) {
    throw new Error("Delivery requires the exact app_v1 main push and owner opt-in.");
  }
}

export function assertSonarConfiguration(env) {
  if (!env.SONAR_TOKEN?.trim()) throw new Error("AZURE_SONAR_TOKEN is required.");
  if (!["https://sonar-azure.dongtaiduc.me", "https://sonar-azure.dongtaiduc.me/"].includes(env.SONAR_HOST_URL)) {
    throw new Error("AZURE_SONAR_HOST_URL must be the existing https://sonar-azure.dongtaiduc.me server.");
  }
}

// This is deliberately NOT a general-purpose YAML parser. Accept only the
// reviewed chart's scalar images mapping; unfamiliar structures fail closed.
// Retain all other bytes, including the hostname, Secret names and PVC options.
export function updateAzureImages(content, sha) {
  if (!/^[0-9a-f]{40}$/.test(sha ?? "")) throw new Error("A full lowercase source SHA is required.");
  if (content.includes("\t") || /\r(?!\n)/.test(content)) throw new Error("Unsupported YAML whitespace.");
  const newline = content.includes("\r\n") ? "\r\n" : "\n";
  if (newline === "\r\n" && /(?<!\r)\n/.test(content)) throw new Error("Mixed line endings are not supported.");
  const lines = content.split(newline);
  // Quoted/anchored/escaped root keys and multi-document YAML could hide an
  // effective duplicate images key. The reviewed values use plain root keys.
  const rootKeys = new Set();
  for (const line of lines) {
    if (!/^[^\s#]/.test(line)) continue;
    const key = /^([A-Za-z][A-Za-z0-9_-]*):(?:\s|$)/.exec(line)?.[1];
    if (!key || rootKeys.has(key)) throw new Error("Unsupported or duplicate root YAML mapping.");
    rootKeys.add(key);
  }
  const roots = lines.flatMap((line, index) => /^images\s*:/.test(line) ? [index] : []);
  if (roots.length !== 1 || !/^images:\s*(?:#.*)?$/.test(lines[roots[0]])) {
    throw new Error("Expected exactly one root images mapping.");
  }
  const start = roots[0];
  let end = lines.length;
  for (let index = start + 1; index < lines.length; index++) {
    if (/^[^\s#]/.test(lines[index])) { end = index; break; }
  }
  const seen = new Map();
  const existingTags = new Set();
  let component;
  for (let index = start + 1; index < end; index++) {
    const line = lines[index];
    if (/^\s*(?:#.*)?$/.test(line)) continue;
    const header = /^  ([A-Za-z][A-Za-z0-9]*):\s*(?:#.*)?$/.exec(line);
    if (header) {
      component = header[1];
      if (!Object.hasOwn(images, component) || seen.has(component)) {
        throw new Error("Unknown or duplicate image component.");
      }
      seen.set(component, new Set());
      continue;
    }
    const field = /^    (repository|tag|pullPolicy): (\S+)(\s+#.*)?$/.exec(line);
    if (!component || !field || seen.get(component).has(field[1])) {
      throw new Error("Unknown, nested or duplicate image field.");
    }
    const [, key, value, comment = ""] = field;
    seen.get(component).add(key);
    if (key === "repository") {
      if (value !== images[component]) throw new Error("Unexpected image repository; review Helm before release.");
      lines[index] = `    repository: ${images[component]}${comment}`;
    } else if (key === "tag") {
      if (!/^[0-9a-f]{40}$/.test(value)) throw new Error("Expected a reviewed full-SHA Helm tag, never latest/placeholders.");
      existingTags.add(value);
      lines[index] = `    tag: ${sha}${comment}`;
    } else if (!["IfNotPresent", "Always", "Never"].includes(value)) {
      throw new Error("Unsupported image pull policy.");
    }
  }
  if (seen.size !== 4 || [...seen.values()].some((fields) => fields.size !== 3)) {
    throw new Error("Expected all four complete application image mappings.");
  }
  if (existingTags.size !== 1) throw new Error("Mixed existing image SHAs require owner review before release.");
  return lines.join(newline);
}

function main(args) {
  const [command, root, sha, ...extra] = args;
  if (command === "check-release" && !root) {
    assertReleaseIdentity(process.env);
  } else if (command === "check-sonar" && !root) {
    assertSonarConfiguration(process.env);
    const properties = readFileSync("sonar-project.properties", "utf8");
    if (!/^sonar\.projectKey=getlink-dtd\r?$/m.test(properties) ||
        properties.match(/^sonar\.projectKey=/gm)?.length !== 1) {
      throw new Error("The shared Sonar project must remain getlink-dtd.");
    }
  } else if (command === "check-writer" && !root) {
    assertReleaseIdentity(process.env);
    if (!process.env.GITOPS_PAT?.trim()) throw new Error("A helm_v1-only GITOPS_PAT is required.");
  } else if (command === "update-values" && root && sha && extra.length === 0) {
    const rootPath = realpathSync(resolve(root));
    const path = resolve(rootPath, VALUES_PATH);
    if (realpathSync(path) !== path) throw new Error("Symlinked Helm values are not allowed.");
    const before = readFileSync(path, "utf8");
    const after = updateAzureImages(before, sha);
    if (before !== after) writeFileSync(path, after, "utf8");
    console.log(`Only ${HELM_REPOSITORY}/${VALUES_PATH} reviewed image fields selected: ${sha}`);
  } else {
    throw new Error("Unsupported delivery command/arguments.");
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try { main(process.argv.slice(2)); } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
