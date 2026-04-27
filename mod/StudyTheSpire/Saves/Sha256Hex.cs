using System;
using System.Security.Cryptography;
using System.Text;

namespace StudyTheSpire.Saves;

/// <summary>
/// Pure, dependency-free SHA-256 helper. Lives in its own file so the test
/// project can include it without dragging in StudyTheSpireClient / ModLogger
/// (which transitively need StS2 game DLLs).
/// </summary>
internal static class Sha256Hex
{
    public static string Of(string s)
    {
        var bytes = Encoding.UTF8.GetBytes(s);
        var digest = SHA256.HashData(bytes);
        return Convert.ToHexString(digest).ToLowerInvariant();
    }
}
