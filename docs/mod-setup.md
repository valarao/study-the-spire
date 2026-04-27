# Mod setup

- The **Slay the Spire 2** exporter lives under **`mod/`** (C#).
- Build, install, and debugging steps will be documented here after the mod project is initialized (later milestone).

## Verify mod auth from a terminal

Before writing any C# code, you can confirm the upload-token auth path against the
hosted backend with `curl`. This proves the full pipeline `mod token → backend
verifier → /mod/ping` works.

1. Sign in to the dashboard and open **Mod settings**:
   <https://studythespire.com/dashboard/settings/mod>
2. Click **Create token**, give it a name (e.g. `Local curl test`), and copy the
   `stsa_live_…` secret. The secret is shown **once** — save it before closing
   the page.
3. Run the smoke test (production URL):

   ```bash
   curl -X POST https://study-the-spire-api-96468418534.us-central1.run.app/mod/ping \
     -H "Authorization: Bearer stsa_live_..." \
     -H "Content-Type: application/json" \
     -d '{"modVersion":"0.1.0","gameVersion":"dev"}'
   ```

   Expected response:

   ```json
   {"ok":true,"tokenName":"Local curl test","serverVersion":"0.1.0"}
   ```

4. Revoke the token in the dashboard, repeat the curl — expect `401`.
5. Send the request without an `Authorization` header — also `401`.

Keep the test token revoked once you're done; create a fresh one when the mod
itself needs to authenticate.
