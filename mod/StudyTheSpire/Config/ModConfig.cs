namespace StudyTheSpire.Config;

internal sealed record ModConfig(
    StudyTheSpireSection StudyTheSpire,
    CaptureSection Capture,
    LoggingSection Logging);

internal sealed record StudyTheSpireSection(
    string UploadToken,
    string Endpoint,
    bool Enabled);

internal sealed record CaptureSection(
    bool RunHistory,
    bool LiveEvents,
    bool CombatSummaries);

internal sealed record LoggingSection(string Level);
