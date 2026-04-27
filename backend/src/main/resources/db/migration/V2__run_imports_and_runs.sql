CREATE TABLE run_imports (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  sha256       TEXT NOT NULL,
  file_name    TEXT,
  raw_json     TEXT NOT NULL,
  received_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (user_id, sha256)
);

CREATE INDEX idx_run_imports_user_received ON run_imports(user_id, received_at DESC);

CREATE TABLE runs (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  import_id            UUID NOT NULL REFERENCES run_imports(id) ON DELETE CASCADE,
  user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status               TEXT NOT NULL,
  character_class      TEXT,
  ascension            INTEGER NOT NULL,
  seed                 TEXT NOT NULL,
  build_id             TEXT NOT NULL,
  game_mode            TEXT NOT NULL,
  platform_type        TEXT NOT NULL,
  start_time           TIMESTAMPTZ NOT NULL,
  run_time_secs        INTEGER NOT NULL,
  killed_by_encounter  TEXT,
  killed_by_event      TEXT,
  schema_version       INTEGER NOT NULL,
  acts                 TEXT[] NOT NULL DEFAULT '{}',
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_runs_user_start ON runs(user_id, start_time DESC);
