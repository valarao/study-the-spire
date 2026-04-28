using System;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using StudyTheSpire.Logging;

namespace StudyTheSpire.Http;

/// <summary>
/// Talks to the Study the Spire backend. <see cref="PingAsync"/> proves auth on startup.
/// Future milestones add <c>UploadRunFileAsync</c>, <c>UploadEventAsync</c>, etc. on top of
/// the same retry/disable scaffolding here.
/// </summary>
internal sealed class StudyTheSpireClient
{
    private static readonly JsonSerializerOptions Json = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    };

    private readonly HttpClient _http;
    private readonly string _token;
    private readonly ModLogger _log;
    private bool _disabled;

    public StudyTheSpireClient(string endpoint, string uploadToken, ModLogger log)
    {
        _http = new HttpClient { BaseAddress = new Uri(endpoint.TrimEnd('/') + "/") };
        _http.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", uploadToken);
        _token = uploadToken;
        _log = log;
    }

    public bool Disabled => _disabled;

    public async Task<ModPingResponse?> PingAsync(
        string modVersion,
        string gameVersion,
        CancellationToken ct = default)
    {
        if (_disabled) return null;

        var body = new ModPingRequest(modVersion, gameVersion);

        return await RetryPolicy.ExecuteAsync<HttpResponseMessage>(
            op: () => _http.PostAsJsonAsync("mod/ping", body, Json, ct),
            classify: ClassifyPing,
            ct: ct).ContinueWith(async t =>
        {
            var resp = t.Result;
            if (resp is null) return null;
            try
            {
                return await resp.Content.ReadFromJsonAsync<ModPingResponse>(Json, ct);
            }
            catch (Exception e)
            {
                _log.Warn($"Couldn't parse /mod/ping response: {e.Message}");
                return null;
            }
        }).Unwrap();
    }

    public async Task<ImportRunFileResponse?> UploadRunFileAsync(
        string rawJson,
        string fileName,
        string sha256,
        string localPlayerId,
        CancellationToken ct = default)
    {
        if (_disabled) return null;

        return await RetryPolicy.ExecuteAsync<HttpResponseMessage>(
            op: () =>
            {
                var content = new StringContent(rawJson, System.Text.Encoding.UTF8, "application/json");
                var req = new HttpRequestMessage(HttpMethod.Post, "imports/run-file") { Content = content };
                req.Headers.TryAddWithoutValidation("X-Run-File-Name", fileName);
                req.Headers.TryAddWithoutValidation("X-Local-Player-Id", localPlayerId);
                return _http.SendAsync(req, ct);
            },
            classify: r => ClassifyImport(r, fileName),
            ct: ct).ContinueWith(async t =>
        {
            var resp = t.Result;
            if (resp is null) return null;
            try
            {
                return await resp.Content.ReadFromJsonAsync<ImportRunFileResponse>(Json, ct);
            }
            catch (Exception e)
            {
                _log.Warn($"Couldn't parse /imports/run-file response for {fileName}: {e.Message}");
                return null;
            }
        }).Unwrap();
    }

    private RetryDecision ClassifyImport(HttpResponseMessage resp, string fileName)
    {
        switch ((int)resp.StatusCode)
        {
            case 200:
                return new RetryDecision.Success<HttpResponseMessage>(resp);
            case 401:
                _disabled = true;
                _log.Warn($"401 on /imports/run-file ({fileName}) — token rejected. Uploads disabled for this session.");
                return new RetryDecision.Fatal("401");
            case 400:
                _log.Warn($"400 on /imports/run-file ({fileName}) — rejected: {SafeReadBody(resp)}");
                return new RetryDecision.Fatal("400");
            case 429:
                var delay = resp.Headers.RetryAfter?.Delta ?? TimeSpan.FromSeconds(5);
                _log.Info($"429 on /imports/run-file ({fileName}) — backing off for {delay.TotalSeconds:F1}s.");
                return new RetryDecision.RetryAfter(delay);
            default:
                if ((int)resp.StatusCode >= 500)
                {
                    _log.Info($"{(int)resp.StatusCode} on /imports/run-file ({fileName}) — retrying.");
                    return new RetryDecision.RetryAfter(TimeSpan.Zero);
                }
                _log.Warn($"Unexpected status on /imports/run-file ({fileName}): {(int)resp.StatusCode}");
                return new RetryDecision.Fatal($"status={(int)resp.StatusCode}");
        }
    }

    private RetryDecision ClassifyPing(HttpResponseMessage resp)
    {
        switch ((int)resp.StatusCode)
        {
            case 200:
                return new RetryDecision.Success<HttpResponseMessage>(resp);
            case 401:
                _disabled = true;
                _log.Warn("401 on /mod/ping — upload token rejected. Uploads disabled for this session. Create a new token at studythespire.com/dashboard/settings/mod.");
                return new RetryDecision.Fatal("401");
            case 400:
                _log.Warn($"400 on /mod/ping — request rejected: {SafeReadBody(resp)}");
                return new RetryDecision.Fatal("400");
            case 429:
                var delay = resp.Headers.RetryAfter?.Delta ?? TimeSpan.FromSeconds(5);
                _log.Info($"429 on /mod/ping — backing off for {delay.TotalSeconds:F1}s.");
                return new RetryDecision.RetryAfter(delay);
            default:
                if ((int)resp.StatusCode >= 500)
                {
                    _log.Info($"{(int)resp.StatusCode} on /mod/ping — retrying.");
                    return new RetryDecision.RetryAfter(TimeSpan.Zero);
                }
                _log.Warn($"Unexpected status on /mod/ping: {(int)resp.StatusCode}");
                return new RetryDecision.Fatal($"status={(int)resp.StatusCode}");
        }
    }

    private static string SafeReadBody(HttpResponseMessage resp)
    {
        try { return resp.Content.ReadAsStringAsync().GetAwaiter().GetResult(); }
        catch { return "<body unavailable>"; }
    }
}
