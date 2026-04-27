using System.Text.RegularExpressions;

namespace StudyTheSpire.Logging;

internal static class Redactor
{
    private static readonly Regex TokenPattern = new(
        @"stsa_live_[A-Za-z0-9_\-]+",
        RegexOptions.Compiled);

    private static readonly Regex AuthorizationHeaderPattern = new(
        @"(Authorization\s*:\s*Bearer\s+)\S+",
        RegexOptions.Compiled | RegexOptions.IgnoreCase);

    public static string Redact(string message)
    {
        if (string.IsNullOrEmpty(message)) return message;
        var step1 = TokenPattern.Replace(message, "stsa_live_<redacted>");
        return AuthorizationHeaderPattern.Replace(step1, "$1<redacted>");
    }
}
