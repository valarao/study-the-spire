using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using StudyTheSpire.Http;
using StudyTheSpire.Logging;
using StudyTheSpire.Validation;

namespace StudyTheSpire.Saves;

/// <summary>
/// Reads a run file, hashes it, validates against the embedded v9 schema, and
/// uploads to <c>/imports/run-file</c>. Updates the local cache on success so
/// subsequent launches don't re-upload the same content.
/// </summary>
internal sealed class RunFileUploader
{
    private readonly StudyTheSpireClient _client;
    private readonly UploadedRunCache _cache;
    private readonly ModLogger _log;

    public RunFileUploader(StudyTheSpireClient client, UploadedRunCache cache, ModLogger log)
    {
        _client = client;
        _cache = cache;
        _log = log;
    }

    public async Task UploadAsync(FileInfo file, CancellationToken ct = default)
    {
        string text;
        try
        {
            text = await File.ReadAllTextAsync(file.FullName, ct).ConfigureAwait(false);
        }
        catch (IOException e)
        {
            _log.Warn($"Couldn't read {file.Name}: {e.Message}");
            return;
        }

        var sha = Sha256Hex.Of(text);
        if (_cache.Contains(sha))
        {
            _log.Debug($"Skipping {file.Name}: already uploaded (sha={sha[..8]}…).");
            return;
        }

        var validation = RunFileValidator.Validate(text);
        if (!validation.Valid)
        {
            _log.Warn($"Skipping {file.Name}: schema validation failed — {validation.Reason}");
            return;
        }

        var result = await _client.UploadRunFileAsync(text, file.Name, sha, ct).ConfigureAwait(false);
        if (result is null)
        {
            // Client logged the failure (401/4xx/exhausted retries). Leave cache alone — retry next session.
            return;
        }
        _cache.Add(sha);
        _log.Info(
            result.Imported
                ? $"Imported {file.Name} (sha={sha[..8]}… runId={result.RunId})."
                : $"Server already had {file.Name} (sha={sha[..8]}… runId={result.RunId}); cached locally.");
    }

}
