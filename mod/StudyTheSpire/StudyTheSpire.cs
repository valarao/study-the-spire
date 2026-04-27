using System;
using System.Threading;
using System.Threading.Tasks;
using Godot;
using MegaCrit.Sts2.Core.Modding;
using StudyTheSpire.Config;
using StudyTheSpire.Http;
using StudyTheSpire.Logging;

namespace StudyTheSpire;

[ModInitializer(nameof(Initialize))]
public partial class StudyTheSpire : Node
{
    public const string ModId = "StudyTheSpire";
    private const string ModVersion = "0.1.0";

    public static void Initialize()
    {
        var bootLogger = new ModLogger(ModId, level: "info");
        bootLogger.Info($"{ModId} {ModVersion} initializing.");

        var config = IniConfigLoader.Load(bootLogger);
        if (config is null)
        {
            bootLogger.Warn("Mod inactive: config missing or invalid.");
            return;
        }

        var log = new ModLogger(ModId, config.Logging.Level);

        if (!config.StudyTheSpire.Enabled)
        {
            log.Info("Mod disabled in config (study_the_spire.enabled=false).");
            return;
        }

        var client = new StudyTheSpireClient(
            endpoint: config.StudyTheSpire.Endpoint,
            uploadToken: config.StudyTheSpire.UploadToken,
            log: log);

        // Fire-and-forget: never block game startup on a network call.
        _ = Task.Run(async () =>
        {
            try
            {
                var resp = await client.PingAsync(
                    modVersion: ModVersion,
                    gameVersion: "unknown",
                    ct: CancellationToken.None);
                if (resp is { Ok: true })
                {
                    log.Info($"Ping success: tokenName={resp.TokenName}, serverVersion={resp.ServerVersion}.");
                }
                else if (!client.Disabled)
                {
                    log.Warn("Ping failed; uploads will be skipped.");
                }
            }
            catch (Exception e)
            {
                log.Error($"Ping crashed: {e.Message}");
            }
        });
    }
}
