#!/usr/bin/env node
/**
 * Mock backend for Study the Spire mod development.
 *
 * Implements the four mod-facing endpoints with canned responses so the mod
 * can be exercised end-to-end without a Cloud Run deploy or local Postgres.
 *
 *   POST /mod/ping           → { ok, tokenName, serverVersion }
 *   POST /events             → 200, no body
 *   POST /events/batch       → { accepted, duplicates: 0, rejected: [] }
 *   POST /imports/run-file   → 200, no body
 *
 * Auth: any token starting with `stsa_live_`. No revocation, no rate limits,
 * no persistence. See README.md for what is and isn't simulated.
 *
 * Usage:
 *   node tools/mock-backend/server.mjs              # 127.0.0.1:8081
 *   node tools/mock-backend/server.mjs --port 9090
 *   node tools/mock-backend/server.mjs --host 0.0.0.0 --port 8081
 */

import http from "node:http";

const args = parseArgs(process.argv.slice(2));
const HOST = args.host ?? "127.0.0.1";
const PORT = Number(args.port ?? 8081);

const SERVER_VERSION = "mock-0.1.0";
const TOKEN_NAME = "MockToken";

const server = http.createServer(async (req, res) => {
  const route = `${req.method} ${req.url}`;
  const auth = req.headers["authorization"] ?? "";
  const tokenPrefix = auth.startsWith("Bearer ")
    ? auth.slice("Bearer ".length, "Bearer ".length + 14)
    : "";

  let body;
  try {
    body = await readJsonBody(req);
  } catch (e) {
    log(route, tokenPrefix, 0, "400 (bad JSON)");
    return send(res, 400, { error: "bad_json", detail: e.message });
  }

  const bodyBytes = body.raw.length;

  if (!isAuthorized(auth)) {
    log(route, tokenPrefix, bodyBytes, "401");
    return send(res, 401, { error: "unauthorized" });
  }

  switch (route) {
    case "POST /mod/ping":
      log(route, tokenPrefix, bodyBytes, "200");
      return send(res, 200, {
        ok: true,
        tokenName: TOKEN_NAME,
        serverVersion: SERVER_VERSION,
      });

    case "POST /events":
      log(route, tokenPrefix, bodyBytes, "200");
      return send(res, 200, null);

    case "POST /events/batch": {
      const n = Array.isArray(body.parsed) ? body.parsed.length : 1;
      log(route, tokenPrefix, bodyBytes, `200 (accepted=${n})`);
      return send(res, 200, { accepted: n, duplicates: 0, rejected: [] });
    }

    case "POST /imports/run-file":
      log(route, tokenPrefix, bodyBytes, "200");
      return send(res, 200, null);

    default:
      log(route, tokenPrefix, bodyBytes, "404");
      return send(res, 404, { error: "not_found" });
  }
});

server.listen(PORT, HOST, () => {
  console.log(`mock-backend listening on http://${HOST}:${PORT}`);
});

process.on("SIGINT", () => {
  console.log("\nshutting down");
  server.close(() => process.exit(0));
});

function isAuthorized(authHeader) {
  if (!authHeader.startsWith("Bearer ")) return false;
  const token = authHeader.slice("Bearer ".length).trim();
  return token.startsWith("stsa_live_") && token.length > "stsa_live_".length;
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", (c) => chunks.push(c));
    req.on("error", reject);
    req.on("end", () => {
      const raw = Buffer.concat(chunks);
      if (raw.length === 0) return resolve({ raw, parsed: null });
      try {
        resolve({ raw, parsed: JSON.parse(raw.toString("utf8")) });
      } catch (e) {
        reject(e);
      }
    });
  });
}

function send(res, status, body) {
  if (body === null) {
    res.writeHead(status);
    res.end();
    return;
  }
  const json = JSON.stringify(body);
  res.writeHead(status, {
    "content-type": "application/json",
    "content-length": Buffer.byteLength(json),
  });
  res.end(json);
}

function log(route, tokenPrefix, bytes, outcome) {
  const ts = new Date().toISOString();
  const tok = tokenPrefix ? ` token=${tokenPrefix}` : "";
  console.log(`${ts} ${route} bytes=${bytes}${tok} → ${outcome}`);
}

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--port") out.port = argv[++i];
    else if (a === "--host") out.host = argv[++i];
  }
  return out;
}
