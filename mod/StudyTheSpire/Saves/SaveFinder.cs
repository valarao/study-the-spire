using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using StudyTheSpire.Logging;

namespace StudyTheSpire.Saves;

/// <summary>
/// Locates StS2 run-file history directories across platforms.
/// Layout (observed):
///   macOS:   ~/Library/Application Support/SlayTheSpire2/steam/&lt;steamId&gt;/profile&lt;n&gt;/saves/history/
///   Windows: %APPDATA%/SlayTheSpire2/steam/&lt;steamId&gt;/profile&lt;n&gt;/saves/history/
///   Linux:   ~/.local/share/SlayTheSpire2/steam/&lt;steamId&gt;/profile&lt;n&gt;/saves/history/
/// A user can have multiple profiles; we return every profile that contains a
/// saves/history dir so the watcher can monitor all of them. The Steam ID parsed
/// from the path is returned alongside each dir — the importer needs it to pick
/// the local player out of co-op runs whose `players[]` arrays contain everyone
/// in the lobby.
/// </summary>
internal static class SaveFinder
{
    internal readonly record struct HistoryDir(string Path, string SteamId);

    public static IReadOnlyList<HistoryDir> FindHistoryDirs(ModLogger log)
    {
        var root = ResolveAppDataRoot();
        if (root is null || !Directory.Exists(root))
        {
            log.Warn($"StS2 application support directory not found at expected path; run-file watcher disabled.");
            return Array.Empty<HistoryDir>();
        }

        var steamDir = Path.Combine(root, "steam");
        if (!Directory.Exists(steamDir))
        {
            log.Warn($"No 'steam' subdirectory under '{root}'; run-file watcher disabled.");
            return Array.Empty<HistoryDir>();
        }

        var dirs = new List<HistoryDir>();
        foreach (var steamUser in Directory.GetDirectories(steamDir))
        {
            var steamId = Path.GetFileName(steamUser);
            foreach (var profile in Directory.GetDirectories(steamUser, "profile*"))
            {
                var history = Path.Combine(profile, "saves", "history");
                if (Directory.Exists(history)) dirs.Add(new HistoryDir(history, steamId));
            }
        }

        if (dirs.Count == 0)
        {
            log.Warn($"No profile under '{steamDir}' has a 'saves/history' directory; run-file watcher disabled.");
            return Array.Empty<HistoryDir>();
        }

        // Sort by file count descending so logs show "biggest" profile first; the
        // watcher monitors all of them regardless.
        dirs.Sort((a, b) =>
            Directory.GetFiles(b.Path, "*.run").Length.CompareTo(Directory.GetFiles(a.Path, "*.run").Length));
        return dirs;
    }

    private static string? ResolveAppDataRoot()
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
        {
            var home = Environment.GetEnvironmentVariable("HOME");
            return home is null ? null : Path.Combine(home, "Library", "Application Support", "SlayTheSpire2");
        }
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            var appData = Environment.GetEnvironmentVariable("APPDATA");
            return appData is null ? null : Path.Combine(appData, "SlayTheSpire2");
        }
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux))
        {
            var xdg = Environment.GetEnvironmentVariable("XDG_DATA_HOME");
            var home = Environment.GetEnvironmentVariable("HOME");
            var root = xdg ?? (home is null ? null : Path.Combine(home, ".local", "share"));
            return root is null ? null : Path.Combine(root, "SlayTheSpire2");
        }
        return null;
    }
}
