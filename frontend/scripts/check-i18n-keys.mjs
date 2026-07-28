import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..", "..");

const messageKeysPath = path.join(root, "frontend/src/lib/i18n/messageKeys.ts");
const propertiesPaths = {
  en: path.join(root, "gateway/src/main/resources/i18n/messages.properties"),
  ru: path.join(root, "gateway/src/main/resources/i18n/messages_ru.properties"),
};

function extractFrontendKeys(source) {
  const match = source.match(/export type MessageKey =([\s\S]*?);/);
  if (!match) throw new Error("MessageKey union not found in messageKeys.ts");
  return new Set([...match[1].matchAll(/"([^"]+)"/g)].map((m) => m[1]));
}

function extractPropertiesKeys(source) {
  return new Set(
    source
      .split("\n")
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith("#"))
      .map((line) => line.split("=")[0].trim()),
  );
}

function diff(a, b) {
  return [...a].filter((key) => !b.has(key));
}

const frontendKeys = extractFrontendKeys(readFileSync(messageKeysPath, "utf-8"));
const enKeys = extractPropertiesKeys(readFileSync(propertiesPaths.en, "utf-8"));
const ruKeys = extractPropertiesKeys(readFileSync(propertiesPaths.ru, "utf-8"));

const errors = [];

const missingInEn = diff(frontendKeys, enKeys);
if (missingInEn.length > 0) errors.push(`Keys in messageKeys.ts missing from messages.properties: ${missingInEn.join(", ")}`);

const missingInRu = diff(frontendKeys, ruKeys);
if (missingInRu.length > 0) errors.push(`Keys in messageKeys.ts missing from messages_ru.properties: ${missingInRu.join(", ")}`);

const enOnlyRu = diff(enKeys, ruKeys);
if (enOnlyRu.length > 0) errors.push(`Keys in messages.properties missing from messages_ru.properties: ${enOnlyRu.join(", ")}`);

const ruOnlyEn = diff(ruKeys, enKeys);
if (ruOnlyEn.length > 0) errors.push(`Keys in messages_ru.properties missing from messages.properties: ${ruOnlyEn.join(", ")}`);

if (errors.length > 0) {
  console.error("i18n key mismatch:\n" + errors.map((e) => `  - ${e}`).join("\n"));
  process.exit(1);
}

console.log(`i18n keys in sync (${frontendKeys.size} frontend keys, ${enKeys.size} properties keys).`);
