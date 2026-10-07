import { readFile, rm, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const reportPath = fileURLToPath(new URL("../coverage/lcov.info", import.meta.url));
const report = await readFile(reportPath, "utf8");

const normalized = report.replace(/^SF:(.+)$/gm, (_line, sourcePath) => {
  const source = sourcePath.replaceAll("\\", "/");
  return `SF:${source.startsWith("frontend/") ? source : `frontend/${source}`}`;
});

await writeFile(reportPath, normalized, "utf8");

await Promise.all([
  rm(fileURLToPath(new URL("../coverage/lcov-report", import.meta.url)), {
    recursive: true,
    force: true,
  }),
  rm(fileURLToPath(new URL("../coverage/tmp", import.meta.url)), {
    recursive: true,
    force: true,
  }),
]);
