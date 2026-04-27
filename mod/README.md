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
endpoint = https://study-the-spire-api-96468418534.us-central1.run.app
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

## Notes

- **Manifest filename**: the StS2 mod loader expects `<ModName>.json` next to the
  DLL. The build plan mentioned `mod_manifest.json` — the canonical convention
  takes precedence here.
- **Token redaction**: every line that goes through `ModLogger` is run through a
  regex that masks `stsa_live_…` and `Authorization: Bearer …`. Never log via
  `MegaCrit.Sts2.Core.Logging.Logger` directly.
- **Affects gameplay**: `false` for now — this milestone is HTTP-only. Will revisit
  when run-event capture (M11+) adds Harmony patches.
