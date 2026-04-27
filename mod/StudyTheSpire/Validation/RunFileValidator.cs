using System.Text.Json;

namespace StudyTheSpire.Validation;

/// <summary>
/// Cheap pre-flight check on a run-file payload. Confirms the bytes are
/// well-formed JSON with at least one of the v9 schema's required top-level
/// fields. Full schema validation is deferred to the backend (and to the
/// `tools/contracts-ci/validate.mjs` validator on every PR), so we don't ship
/// a JSON-Schema library inside the mod.
/// </summary>
internal static class RunFileValidator
{
    public sealed record Result(bool Valid, string? Reason);

    public static Result Validate(string rawJson)
    {
        try
        {
            using var doc = JsonDocument.Parse(rawJson);
            if (doc.RootElement.ValueKind != JsonValueKind.Object)
                return new Result(false, "top-level value is not a JSON object");
            // A real run-file always has these — cheap sanity check.
            foreach (var key in new[] { "build_id", "schema_version", "seed", "start_time" })
            {
                if (!doc.RootElement.TryGetProperty(key, out _))
                    return new Result(false, $"missing required field '{key}'");
            }
            return new Result(true, null);
        }
        catch (JsonException e)
        {
            return new Result(false, $"not valid JSON: {e.Message}");
        }
    }
}
