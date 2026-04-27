# Study the Spire — Slay the Spire 2 mod

Captures runs from Slay the Spire 2 and ships them to the Study the Spire backend
for analysis at <https://studythespire.com>.

## Prerequisites

- **.NET 9 SDK** (or newer — the SDK is forward-compatible with the `net9.0` target).
- **Slay the Spire 2** installed via Steam. The build needs the game's `0Harmony.dll`
  and `sts2.dll`, which are referenced via `Sts2PathDiscovery.props`.

## Build and install

```bash
cd mod
dotnet build StudyTheSpire.sln
```

The post-build target copies the `.dll`, `.json` manifest, and `config.ini.example`
into your StS2 install:

- macOS: `~/Library/Application Support/Steam/steamapps/common/Slay the Spire 2/SlayTheSpire2.app/Contents/MacOS/mods/StudyTheSpire/`
- Windows: `…/steamapps/common/Slay the Spire 2/mods/StudyTheSpire/`
- Linux: `~/.local/share/Steam/steamapps/common/Slay the Spire 2/mods/StudyTheSpire/`

If the build fails with `StS2 data not found at …`, the discovery script couldn't
find your install. Override with `dotnet build /p:Sts2Path="…/Slay the Spire 2"`.

## Configure

1. In the mod folder (where the `.dll` lives), copy `config.ini.example` to `config.ini`.
2. Create an upload token at <https://studythespire.com/dashboard/settings/mod>
   and paste it into `upload_token`. Tokens look like `stsa_live_…` and are shown
   only once on creation.
3. (Optional) Override `endpoint` for local dev or staging.

```ini
[study_the_spire]
upload_token = stsa_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
endpoint = https://api.studythespire.com
enabled = true
```

## Verify

Launch Slay the Spire 2. The mod runs `POST /mod/ping` on startup. Check the log:

- macOS: `~/Library/Application Support/SlayTheSpire2/logs/`
- Windows: `%appdata%\SlayTheSpire2\logs\`
- Linux: `~/.local/share/SlayTheSpire2/logs/`

Or open the in-game log window: **Mod Configurations → BaseLib → "Open log window
on startup"**, or backtick → `showlog`.

Look for `Ping success: tokenName=…, serverVersion=0.1.0`. If you see
`401 — upload token rejected`, the token is invalid or revoked; create a fresh
one in the dashboard.

For backend smoke tests with `curl`, see [`docs/mod-setup.md`](../docs/mod-setup.md).

## Tests

```bash
dotnet test StudyTheSpire.Tests/StudyTheSpire.Tests.csproj
```

The test project (`StudyTheSpire.Tests/`) covers pure-logic units —
`Redactor` and `IniParser` — by including their source files directly via
`<Compile Include>` rather than referencing the main project. That avoids
pulling in the StS2 game DLL references the main project needs to compile.
Both the mod and the tests target `net9.0` (the runtime StS2 hosts), so you
need the **.NET 9 runtime** installed locally to run tests — even if your
SDK is newer. Grab it from
[dotnet.microsoft.com/download/dotnet/9.0](https://dotnet.microsoft.com/download/dotnet/9.0).

CI runs only `dotnet test` on this project — see
[`.github/workflows/mod-tests.yml`](../.github/workflows/mod-tests.yml).

## Local-only mock backend

When iterating on the mod, point `endpoint` at the local mock so you don't need
the real Cloud Run deploy:

```bash
node tools/mock-backend/server.mjs --port 8081
# then in config.ini: endpoint = http://localhost:8081
```

See [`tools/mock-backend/README.md`](../tools/mock-backend/README.md) for what
the mock does and doesn't simulate.

## Known limitations

- **No CI build of the main mod project.** `dotnet build` on
  `StudyTheSpire.csproj` requires `sts2.dll` and `0Harmony.dll` from a local
  StS2 install (the `CheckDependencyPaths` MSBuild target enforces this). CI
  doesn't have StS2, so it can only run unit tests. Lifting this either means
  committing stub DLLs or splitting the mod into a `StudyTheSpire.Core`
  project that's free of game references — both are deferred.

## Notes

- **Manifest filename**: the StS2 mod loader expects `<ModName>.json` next to the
  DLL. The build plan mentioned `mod_manifest.json` — the canonical convention
  takes precedence here.
- **Token redaction**: every line that goes through `ModLogger` is run through a
  regex that masks `stsa_live_…` and `Authorization: Bearer …`. Never log via
  `MegaCrit.Sts2.Core.Logging.Logger` directly.
- **Affects gameplay**: `false` for now — this milestone is HTTP-only. Will revisit
  when run-event capture (M11+) adds Harmony patches.
