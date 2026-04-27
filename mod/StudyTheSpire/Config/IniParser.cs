using System.Collections.Generic;

namespace StudyTheSpire.Config;

/// <summary>
/// Pure INI parsing: section headers, key=value lines, comments, no IO or logging.
/// Lives in its own file so the test project can include it without dragging
/// <see cref="IniConfigLoader"/>'s ModLogger dependency (and through it the StS2
/// game DLLs) into the test compile.
/// </summary>
internal static class IniParser
{
    /// <summary>
    /// Parses INI-style lines into a section→key→value map. Section and key
    /// names are lowercased; values keep their case but are trimmed. `#` and
    /// `;` start a comment line. Lines outside any section are ignored.
    /// Last write wins for duplicate keys.
    /// </summary>
    public static Dictionary<string, Dictionary<string, string>> Parse(string[] lines)
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

    /// <summary>
    /// Tolerant boolean parsing: <c>true/false/1/0/yes/no/on/off</c> (case-insensitive),
    /// anything else returns <paramref name="fallback"/>.
    /// </summary>
    public static bool ParseBool(string value, bool fallback) =>
        value.Trim().ToLowerInvariant() switch
        {
            "true" or "1" or "yes" or "on" => true,
            "false" or "0" or "no" or "off" => false,
            _ => fallback,
        };
}
