import assert from "node:assert/strict";
import test from "node:test";

import {
  detectLinkIcon,
  editableCustomIconValue,
  resolveLinkIcon,
} from "../src/lib/link-icons";

test("detectLinkIcon recognizes supported domains and their subdomains", () => {
  const cases = [
    ["https://github.com/getlink/project", "github"],
    ["https://music.youtube.com/watch?v=123", "youtube"],
    ["  GitHub.com/getlink/project  ", "github"],
    ["https://slack.com", "slack"],
    ["https://workspace.slack.com/archives/general", "slack"],
  ] as const;

  for (const [url, expected] of cases) {
    assert.equal(detectLinkIcon(url), expected, url);
  }
});

test("detectLinkIcon rejects lookalike and malformed domains", () => {
  const cases = [
    "https://github.com.evil.example",
    "https://notgithub.com",
    "https://github.com@evil.example/path",
    "https://workspace.slack.com.evil.example",
    "not a url",
    "",
  ];

  for (const url of cases) {
    assert.equal(detectLinkIcon(url), "globe", url);
  }
});

test("resolveLinkIcon detects automatic icons and honors manual values", () => {
  assert.deepEqual(resolveLinkIcon(null, "https://facebook.com/getlink"), {
    kind: "facebook",
  });
  assert.deepEqual(resolveLinkIcon("  ", "https://youtube.com/@getlink"), {
    kind: "youtube",
  });
  assert.deepEqual(resolveLinkIcon(" GH ", "https://facebook.com/getlink"), {
    kind: "github",
  });
  assert.deepEqual(resolveLinkIcon(" website ", "https://github.com/getlink"), {
    kind: "globe",
  });
  assert.deepEqual(resolveLinkIcon("  blog  ", "https://example.com"), {
    kind: "custom",
    label: "BL",
  });
});

test("editableCustomIconValue clears legacy automatic values", () => {
  for (const icon of [null, undefined, "", " GitHub ", "SLACK", "x", "globe"]) {
    assert.equal(editableCustomIconValue(icon), "", String(icon));
  }

  assert.equal(editableCustomIconValue("GH"), "GH");
  assert.equal(editableCustomIconValue("twitter"), "twitter");
  assert.equal(editableCustomIconValue("  brand  "), "brand");
});
