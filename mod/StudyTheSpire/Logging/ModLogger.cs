using MegaCrit.Sts2.Core.Logging;

namespace StudyTheSpire.Logging;

/// <summary>
/// Wraps the StS2 logger with token redaction. Every line passed through here is
/// run through <see cref="Redactor"/> before reaching the underlying logger, so
/// accidental string interpolation that includes a token can't leak it into log
/// files. The whole mod must log via this class — never call MegaCrit's Logger directly.
/// </summary>
internal sealed class ModLogger
{
    private readonly Logger _inner;
    private readonly bool _debugEnabled;

    public ModLogger(string modId, string level)
    {
        _inner = new Logger(modId, LogType.Generic);
        _debugEnabled = string.Equals(level, "debug", System.StringComparison.OrdinalIgnoreCase);
    }

    public void Info(string message) => _inner.Info(Redactor.Redact(message));
    public void Warn(string message) => _inner.Warn(Redactor.Redact(message));
    public void Error(string message) => _inner.Error(Redactor.Redact(message));

    public void Debug(string message)
    {
        if (!_debugEnabled) return;
        _inner.Debug(Redactor.Redact(message));
    }
}
