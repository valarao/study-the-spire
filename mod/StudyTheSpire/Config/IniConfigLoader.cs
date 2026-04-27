using System.IO;
using System.Reflection;
using StudyTheSpire.Logging;

namespace StudyTheSpire.Config;

/// <summary>
/// Loads <see cref="ModConfig"/> from a hand-rolled INI file at <c>config.ini</c> next
/// to the mod's DLL. Pure parsing lives in <see cref="IniParser"/>; this class layers
/// IO, logging, and field validation on top.
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

        var sections = IniParser.Parse(File.ReadAllLines(configPath));

        var sts = sections.GetValueOrDefault("study_the_spire") ?? new();
        var cap = sections.GetValueOrDefault("capture") ?? new();
        var lg = sections.GetValueOrDefault("logging") ?? new();

        var token = sts.GetValueOrDefault("upload_token", "").Trim();
        var endpoint = sts.GetValueOrDefault("endpoint", "").Trim();
        var enabled = IniParser.ParseBool(sts.GetValueOrDefault("enabled", "true"), true);

        if (!token.StartsWith("stsa_live_"))
        {
            log.Warn("upload_token is missing or malformed. Tokens look like 'stsa_live_…' — create one at studythespire.com/dashboard/settings/mod.");
            return null;
        }

        if (string.IsNullOrEmpty(endpoint))
        {
            endpoint = "https://api.studythespire.com";
            log.Info($"endpoint not set; falling back to {endpoint}.");
        }

        return new ModConfig(
            StudyTheSpire: new StudyTheSpireSection(token, endpoint, enabled),
            Capture: new CaptureSection(
                RunHistory: IniParser.ParseBool(cap.GetValueOrDefault("run_history", "true"), true),
                LiveEvents: IniParser.ParseBool(cap.GetValueOrDefault("live_events", "false"), false),
                CombatSummaries: IniParser.ParseBool(cap.GetValueOrDefault("combat_summaries", "false"), false)),
            Logging: new LoggingSection(lg.GetValueOrDefault("level", "info").Trim().ToLowerInvariant()));
    }
}
