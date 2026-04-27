using StudyTheSpire.Logging;
using Xunit;

namespace StudyTheSpire.Tests;

public class RedactorTests
{
    [Fact]
    public void Redacts_bare_token_in_a_sentence()
    {
        var input = "got token stsa_live_AbCd1234EfGhIjKl-_xyz from server";
        Assert.Equal("got token stsa_live_<redacted> from server", Redactor.Redact(input));
    }

    [Fact]
    public void Redacts_authorization_bearer_header_value()
    {
        var input = "Authorization: Bearer eyJhbGc.something.signature";
        Assert.Equal("Authorization: Bearer <redacted>", Redactor.Redact(input));
    }

    [Fact]
    public void Redacts_authorization_case_insensitive()
    {
        var input = "authorization: bearer eyJhbGc.x.y";
        Assert.Equal("authorization: bearer <redacted>", Redactor.Redact(input));
    }

    [Fact]
    public void Empty_string_passes_through()
    {
        Assert.Equal("", Redactor.Redact(""));
    }

    [Fact]
    public void Redaction_is_idempotent()
    {
        var once = Redactor.Redact("see token stsa_live_abcdefg");
        var twice = Redactor.Redact(once);
        Assert.Equal(once, twice);
    }

    [Fact]
    public void Multiple_tokens_in_one_string_are_all_redacted()
    {
        var input = "first stsa_live_AAA second stsa_live_BBB end";
        Assert.Equal("first stsa_live_<redacted> second stsa_live_<redacted> end", Redactor.Redact(input));
    }

    [Fact]
    public void Non_token_text_is_not_modified()
    {
        var input = "this string mentions stsa but not the prefix";
        Assert.Equal(input, Redactor.Redact(input));
    }
}
