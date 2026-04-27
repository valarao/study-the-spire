using StudyTheSpire.Config;
using Xunit;

namespace StudyTheSpire.Tests;

public class IniParserTests
{
    [Fact]
    public void Parses_a_simple_section_and_key()
    {
        var lines = new[]
        {
            "[study_the_spire]",
            "upload_token = stsa_live_abc",
            "endpoint = https://example.com",
        };
        var result = IniParser.Parse(lines);
        Assert.Equal("stsa_live_abc", result["study_the_spire"]["upload_token"]);
        Assert.Equal("https://example.com", result["study_the_spire"]["endpoint"]);
    }

    [Fact]
    public void Skips_hash_and_semicolon_comments()
    {
        var lines = new[]
        {
            "# this is a comment",
            "; this too",
            "[s]",
            "k = v",
            "# inside section",
            "  ; indented",
        };
        var result = IniParser.Parse(lines);
        Assert.Single(result["s"]);
        Assert.Equal("v", result["s"]["k"]);
    }

    [Fact]
    public void Trims_whitespace_around_keys_and_values()
    {
        var lines = new[]
        {
            "[s]",
            "   key   =    value with trailing spaces   ",
        };
        var result = IniParser.Parse(lines);
        Assert.Equal("value with trailing spaces", result["s"]["key"]);
    }

    [Fact]
    public void Lowercases_section_and_key_names()
    {
        var lines = new[]
        {
            "[Capture]",
            "Run_History = true",
            "LIVE_EVENTS = false",
        };
        var result = IniParser.Parse(lines);
        Assert.True(result.ContainsKey("capture"));
        Assert.True(result["capture"].ContainsKey("run_history"));
        Assert.True(result["capture"].ContainsKey("live_events"));
    }

    [Fact]
    public void Last_duplicate_key_wins()
    {
        var lines = new[]
        {
            "[s]",
            "k = first",
            "k = second",
        };
        var result = IniParser.Parse(lines);
        Assert.Equal("second", result["s"]["k"]);
    }

    [Fact]
    public void Lines_outside_a_section_are_ignored()
    {
        var lines = new[]
        {
            "orphan = value",
            "[real]",
            "k = v",
        };
        var result = IniParser.Parse(lines);
        Assert.Single(result);
        Assert.Equal("v", result["real"]["k"]);
    }

    [Fact]
    public void Empty_input_yields_empty_map()
    {
        var result = IniParser.Parse(new string[0]);
        Assert.Empty(result);
    }

    [Theory]
    [InlineData("true", true)]
    [InlineData("TRUE", true)]
    [InlineData("True", true)]
    [InlineData("1", true)]
    [InlineData("yes", true)]
    [InlineData("on", true)]
    [InlineData("false", false)]
    [InlineData("0", false)]
    [InlineData("no", false)]
    [InlineData("off", false)]
    public void ParseBool_recognized_values(string input, bool expected)
    {
        Assert.Equal(expected, IniParser.ParseBool(input, !expected));
    }

    [Fact]
    public void ParseBool_unknown_returns_fallback()
    {
        Assert.True(IniParser.ParseBool("garbage", true));
        Assert.False(IniParser.ParseBool("garbage", false));
    }

    [Fact]
    public void ParseBool_trims_whitespace()
    {
        Assert.True(IniParser.ParseBool("  true  ", false));
    }
}
