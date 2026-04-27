using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using StudyTheSpire.Logging;

namespace StudyTheSpire.Config;

/// <summary>
/// Loads <see cref="ModConfig"/> from a hand-rolled INI file at <c>config.ini</c> next
/// to the mod's DLL. No NuGet dependency — the format is small and stable.
/// </summary>
internal static class IniConfigLoader
{
    /// <summary>
    /// Returns null if the file is missing or the upload token isn't valid.
    /// The caller logs a warning and skips the mod's networking for the session.
    /// </summary>
    public static ModConfig? Load(ModLogger log)
    {
        var dllPath = Assembly.GetExecutingAssembly().Location;
        var modDir = Path.GetDirectoryName(dllPath) ?? Directory.GetCurrentDirectory();
        var configPath = Path.Combine(modDir, "config.ini");

        if (!File.Exists(configPath))
        {
            log.Warn($"config.ini not found at {configPath}. Copy config.ini.example to config.ini and add your upload token.");
            return null;
        }

        var sections = Parse(File.ReadAllLines(configPath));

        var sts = sections.GetValueOrDefault("study_the_spire") ?? new();
        var cap = sections.GetValueOrDefault("capture") ?? new();
        var lg = sections.GetValueOrDefault("logging") ?? new();

        var token = sts.GetValueOrDefault("upload_token", "").Trim();
        var endpoint = sts.GetValueOrDefault("endpoint", "").Trim();
        var enabled = ParseBool(sts.GetValueOrDefault("enabled", "true"), true);

        if (!token.StartsWith("stsa_live_"))
        {
            log.Warn("upload_token is missing or malformed. Tokens look like 'stsa_live_…' — create one at studythespire.com/dashboard/settings/mod.");
            return null;
        }

        if (string.IsNullOrEmpty(endpoint))
        {
            endpoint = "https://study-the-spire-api-96468418534.us-central1.run.app";
            log.Info($"endpoint not set; falling back to {endpoint}.");
        }

        return new ModConfig(
            StudyTheSpire: new StudyTheSpireSection(token, endpoint, enabled),
            Capture: new CaptureSection(
                RunHistory: ParseBool(cap.GetValueOrDefault("run_history", "true"), true),
                LiveEvents: ParseBool(cap.GetValueOrDefault("live_events", "false"), false),
                CombatSummaries: ParseBool(cap.GetValueOrDefault("combat_summaries", "false"), false)),
            Logging: new LoggingSection(lg.GetValueOrDefault("level", "info").Trim().ToLowerInvariant()));
    }

    private static Dictionary<string, Dictionary<string, string>> Parse(string[] lines)
    {
        var result = new Dictionary<string, Dictionary<string, string>>();
        Dictionary<string, string>? current = null;
        foreach (var raw in lines)
        {
            var line = raw.Trim();
            if (line.Length == 0 || line.StartsWith("#") || line.StartsWith(";")) continue;
            if (line.StartsWith("[") && line.EndsWith("]"))
            {
                var section = line.Substring(1, line.Length - 2).Trim().ToLowerInvariant();
                current = new Dictionary<string, string>();
                result[section] = current;
                continue;
            }
            var eq = line.IndexOf('=');
            if (eq < 0 || current is null) continue;
            var key = line.Substring(0, eq).Trim().ToLowerInvariant();
            var value = line.Substring(eq + 1).Trim();
            current[key] = value;
        }
        return result;
    }

    private static bool ParseBool(string value, bool fallback) =>
        value.Trim().ToLowerInvariant() switch
        {
            "true" or "1" or "yes" or "on" => true,
            "false" or "0" or "no" or "off" => false,
            _ => fallback,
        };
}
