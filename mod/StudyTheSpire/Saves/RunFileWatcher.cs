using System;
using System.Collections.Concurrent;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using StudyTheSpire.Logging;

namespace StudyTheSpire.Saves;

/// <summary>
/// Watches the StS2 history directory for new/changed *.run files. Coalesces rapid
/// successive events on the same path within a debounce window so we don't try to
/// upload a file that's still being written.
/// </summary>
internal sealed class RunFileWatcher : IDisposable
{
    private readonly string _historyDir;
    private readonly Func<FileInfo, Task> _onFileReady;
    private readonly TimeSpan _debounce;
    private readonly ModLogger _log;
    private readonly ConcurrentDictionary<string, CancellationTokenSource> _pending = new();
    private readonly FileSystemWatcher _fsw;

    public RunFileWatcher(
        string historyDir,
        Func<FileInfo, Task> onFileReady,
        ModLogger log,
        TimeSpan? debounce = null)
    {
        _historyDir = historyDir;
        _onFileReady = onFileReady;
        _log = log;
        _debounce = debounce ?? TimeSpan.FromMilliseconds(500);
        _fsw = new FileSystemWatcher(historyDir, "*.run")
        {
            NotifyFilter = NotifyFilters.FileName | NotifyFilters.LastWrite | NotifyFilters.Size,
            IncludeSubdirectories = false,
        };
        _fsw.Created += OnEvent;
        _fsw.Changed += OnEvent;
        _fsw.Renamed += OnRenamed;
    }

    public void Start()
    {
        _fsw.EnableRaisingEvents = true;
        _log.Info($"Watching {_historyDir} for new run files (debounce {_debounce.TotalMilliseconds:F0}ms).");
    }

    /// <summary>
    /// Enumerates files already present at startup. The downstream handler is
    /// responsible for dedup (sha256 cache + backend idempotency).
    /// </summary>
    public void EnumerateExisting()
    {
        try
        {
            var existing = Directory.GetFiles(_historyDir, "*.run");
            _log.Info($"Found {existing.Length} existing run file(s); checking each for upload.");
            foreach (var path in existing)
            {
                if (ShouldSkip(path)) continue;
                ScheduleReady(path);
            }
        }
        catch (Exception e)
        {
            _log.Error($"Couldn't enumerate {_historyDir}: {e.Message}");
        }
    }

    public void Dispose()
    {
        _fsw.EnableRaisingEvents = false;
        _fsw.Dispose();
        foreach (var cts in _pending.Values) cts.Cancel();
    }

    private void OnEvent(object sender, FileSystemEventArgs e) => ScheduleReady(e.FullPath);
    private void OnRenamed(object sender, RenamedEventArgs e) => ScheduleReady(e.FullPath);

    private void ScheduleReady(string path)
    {
        if (ShouldSkip(path)) return;
        var cts = new CancellationTokenSource();
        if (_pending.TryGetValue(path, out var prev)) prev.Cancel();
        _pending[path] = cts;
        _ = DelayThenInvoke(path, cts.Token);
    }

    private async Task DelayThenInvoke(string path, CancellationToken ct)
    {
        try
        {
            await Task.Delay(_debounce, ct).ConfigureAwait(false);
        }
        catch (TaskCanceledException) { return; }
        _pending.TryRemove(path, out _);
        try
        {
            var fi = new FileInfo(path);
            if (!fi.Exists) return;
            await _onFileReady(fi).ConfigureAwait(false);
        }
        catch (Exception e)
        {
            _log.Error($"Run-file handler crashed for {Path.GetFileName(path)}: {e.Message}");
        }
    }

    private static bool ShouldSkip(string path)
    {
        var name = Path.GetFileName(path);
        if (name.StartsWith(".")) return true;
        if (name.EndsWith(".tmp", StringComparison.OrdinalIgnoreCase)) return true;
        if (name.EndsWith("~")) return true;
        return false;
    }
}
