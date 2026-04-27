#!/usr/bin/env node
/**
 * Validates the wire contracts against their schemas.
 *
 *   - Every *.schema.json under contracts/ compiles as valid JSON Schema draft-07.
 *   - Every example under contracts/examples/events/ validates against
 *     contracts/events/envelope.schema.json AND its `data` validates against
 *     the matching contracts/types/<TYPE>.schema.json.
 *   - Every committed run-file example under contracts/examples/runfile/
 *     validates against contracts/runfile/run.v9.schema.json.
 *   - If references/run-samples/ exists locally (gitignored), each *.run.json
 *     in there also validates against the same run-file schema.
 *   - contracts/api/openapi.yaml parses as a valid OpenAPI 3.0 document.
 */

import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, resolve, basename } from "node:path";
import { fileURLToPath } from "node:url";
import Ajv from "ajv";
import addFormats from "ajv-formats";
import YAML from "yaml";
import SwaggerParser from "@apidevtools/swagger-parser";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = resolve(HERE, "..", "..");
const CONTRACTS = join(REPO_ROOT, "contracts");

let errors = 0;
let okSchemas = 0;
let okExamples = 0;

function fail(file, reason) {
  errors += 1;
  console.error(`FAIL  ${file}: ${reason}`);
}

function info(msg) {
  console.log(`OK    ${msg}`);
}

function readJson(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function listJsonFiles(dir, suffix = ".json") {
  if (!exists(dir)) return [];
  return readdirSync(dir)
    .filter((f) => f.endsWith(suffix))
    .map((f) => join(dir, f));
}

function exists(p) {
  try {
    statSync(p);
    return true;
  } catch {
    return false;
  }
}

const ajv = new Ajv({ allErrors: true, strict: false });
addFormats(ajv);

// --- 1. Validate that every *.schema.json compiles as a JSON Schema. ---

const schemaFiles = [
  join(CONTRACTS, "events", "envelope.schema.json"),
  join(CONTRACTS, "runfile", "run.v9.schema.json"),
  ...listJsonFiles(join(CONTRACTS, "types"), ".schema.json"),
];

const compiled = new Map(); // path → validator
for (const path of schemaFiles) {
  if (!exists(path)) {
    fail(path, "schema file not found");
    continue;
  }
  try {
    const schema = readJson(path);
    const validate = ajv.compile(schema);
    compiled.set(path, validate);
    okSchemas += 1;
    info(`schema ${rel(path)}`);
  } catch (e) {
    fail(rel(path), `not a valid JSON Schema: ${e.message}`);
  }
}

// --- 2. Validate event examples. ---

const envelopePath = join(CONTRACTS, "events", "envelope.schema.json");
const envelope = compiled.get(envelopePath);

if (envelope) {
  const eventExamples = listJsonFiles(join(CONTRACTS, "examples", "events"));
  for (const path of eventExamples) {
    let payload;
    try {
      payload = readJson(path);
    } catch (e) {
      fail(rel(path), `not valid JSON: ${e.message}`);
      continue;
    }
    if (!envelope(payload)) {
      fail(rel(path), `envelope validation: ${ajv.errorsText(envelope.errors)}`);
      continue;
    }
    const typeSchemaPath = join(CONTRACTS, "types", `${payload.type}.schema.json`);
    const typeValidate = compiled.get(typeSchemaPath);
    if (!typeValidate) {
      console.warn(`WARN  ${rel(path)}: no type schema for "${payload.type}" — envelope OK only`);
      okExamples += 1;
      continue;
    }
    if (!typeValidate(payload.data)) {
      fail(rel(path), `data validation: ${ajv.errorsText(typeValidate.errors)}`);
      continue;
    }
    okExamples += 1;
    info(`example ${rel(path)}`);
  }
} else {
  fail("contracts/events/envelope.schema.json", "envelope schema didn't compile; skipping event examples");
}

// --- 3. Validate run-file examples (committed + optional local samples). ---

const runFileSchemaPath = join(CONTRACTS, "runfile", "run.v9.schema.json");
const runFileValidate = compiled.get(runFileSchemaPath);

if (runFileValidate) {
  const committed = listJsonFiles(join(CONTRACTS, "examples", "runfile"))
    .filter((p) => p.endsWith(".run.json"));
  const localSamplesDir = join(REPO_ROOT, "references", "run-samples");
  const localSamples = exists(localSamplesDir)
    ? listJsonFiles(localSamplesDir).filter((p) => p.endsWith(".run.json"))
    : [];
  for (const path of [...committed, ...localSamples]) {
    let payload;
    try {
      payload = readJson(path);
    } catch (e) {
      fail(rel(path), `not valid JSON: ${e.message}`);
      continue;
    }
    if (!runFileValidate(payload)) {
      fail(rel(path), `run-file validation: ${ajv.errorsText(runFileValidate.errors)}`);
      continue;
    }
    okExamples += 1;
    info(`runfile  ${rel(path)}`);
  }
  if (localSamples.length === 0) {
    console.log(`(no references/run-samples/ found locally; skipping real-export check)`);
  }
} else {
  fail("contracts/runfile/run.v9.schema.json", "run-file schema didn't compile; skipping run-file examples");
}

// --- 4. Validate OpenAPI document. ---

const openapiPath = join(CONTRACTS, "api", "openapi.yaml");
try {
  const text = readFileSync(openapiPath, "utf8");
  const doc = YAML.parse(text);
  await SwaggerParser.validate(doc);
  okSchemas += 1;
  info(`openapi  ${rel(openapiPath)}`);
} catch (e) {
  fail(rel(openapiPath), `OpenAPI validation: ${e.message}`);
}

// --- Done. ---

function rel(p) {
  return p.startsWith(REPO_ROOT) ? p.slice(REPO_ROOT.length + 1) : p;
}

if (errors > 0) {
  console.error(`\n${errors} error(s).`);
  process.exit(1);
}
console.log(`\nOK: validated ${okSchemas} schemas, ${okExamples} examples.`);
