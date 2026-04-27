using System.Collections.Generic;
using System.IO;
using StudyTheSpire.Logging;

namespace StudyTheSpire.Saves;

/// <summary>
/// File-backed set of sha256 hashes the backend has already accepted. Prevents
/// the watcher from re-uploading the same run after a game restart (the backend
/// also dedupes server-side, but skipping pointless network calls is cheap).
/// </summary>
internal sealed class UploadedRunCache
{
    private readonly string _path;
    private readonly HashSet<string> _set = new();
    private readonly object _gate = new();
    private readonly ModLogger _log;

    public UploadedRunCache(string path, ModLogger log)
    {
        _path = path;
        _log = log;
        Load();
    }

    public bool Contains(string sha256)
    {
        lock (_gate) return _set.Contains(sha256);
    }

    public void Add(string sha256)
    {
        lock (_gate)
        {
            if (!_set.Add(sha256)) return;
            try { File.AppendAllText(_path, sha256 + "\n"); }
            catch (IOException e) { _log.Warn($"Couldn't append to upload cache: {e.Message}"); }
        }
    }

    private void Load()
    {
        if (!File.Exists(_path))
        {
            _log.Info($"Upload cache (new): {_path}");
            return;
        }
        try
        {
            foreach (var line in File.ReadAllLines(_path))
            {
                var t = line.Trim();
                if (t.Length > 0) _set.Add(t);
            }
            _log.Info($"Upload cache loaded {_set.Count} entries from {_path}");
        }
        catch (IOException e)
        {
            _log.Warn($"Couldn't read upload cache: {e.Message}");
        }
    }
}
