# Run-file examples

Minimal redacted run-file fixtures that validate against
[`contracts/runfile/run.v9.schema.json`](../../runfile/run.v9.schema.json).
Each covers one outcome: `sample_win`, `sample_loss`, `sample_abandoned`.

These were rebuilt in M10 against real game exports (which live under
`references/run-samples/` locally — gitignored because they contain Steam IDs).
The fixtures here keep the same top-level shape but anonymize identifiers and
drop most nested data; they exist purely so CI can verify the schema accepts
each outcome variant.

If you have your own real samples locally, drop them in `references/run-samples/`
and run `pnpm --filter @studythespire/contracts-ci validate` — the validator
auto-detects that directory and runs the same schema against every real file
in addition to the committed fixtures.
