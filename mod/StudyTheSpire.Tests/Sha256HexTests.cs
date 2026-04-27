using StudyTheSpire.Saves;
using Xunit;

namespace StudyTheSpire.Tests;

public class Sha256HexTests
{
    [Fact]
    public void Empty_string_known_digest()
    {
        // Well-known SHA-256 of empty string.
        Assert.Equal(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256Hex.Of(""));
    }

    [Fact]
    public void Hex_is_lowercase()
    {
        var h = Sha256Hex.Of("hello");
        Assert.Equal(h.ToLowerInvariant(), h);
    }

    [Fact]
    public void Hashing_is_deterministic()
    {
        var a = Sha256Hex.Of("the spire awaits");
        var b = Sha256Hex.Of("the spire awaits");
        Assert.Equal(a, b);
    }

    [Fact]
    public void Different_input_different_output()
    {
        Assert.NotEqual(Sha256Hex.Of("a"), Sha256Hex.Of("b"));
    }

    [Fact]
    public void Output_is_64_hex_chars()
    {
        var h = Sha256Hex.Of("anything");
        Assert.Equal(64, h.Length);
        Assert.Matches("^[0-9a-f]{64}$", h);
    }
}
