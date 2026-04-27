namespace StudyTheSpire.Http;

internal sealed record ModPingRequest(string ModVersion, string GameVersion);

internal sealed record ModPingResponse(bool Ok, string TokenName, string ServerVersion);

internal sealed record ImportRunFileResponse(bool Imported, string RunId);
