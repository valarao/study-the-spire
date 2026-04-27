using System;
using System.IO;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;
using Godot;
using MegaCrit.Sts2.Core.Modding;
using StudyTheSpire.Config;
using StudyTheSpire.Http;
using StudyTheSpire.Logging;
using StudyTheSpire.Saves;

namespace StudyTheSpire;

[ModInitializer(nameof(Initialize))]
public partial class StudyTheSpire : Node
{
    public const string ModId = "StudyTheSpire";
    private const string ModVersion = "0.1.0";

    private static readonly System.Collections.Generic.List<RunFileWatcher> _watchers = new();

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
                    if (config.Capture.RunHistory)
                    {
                        StartRunFileWatcher(client, log);
                    }
                    else
                    {
                        log.Info("Run history capture disabled in config (capture.run_history=false).");
                    }
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

    private static void StartRunFileWatcher(StudyTheSpireClient client, ModLogger log)
    {
        var historyDirs = SaveFinder.FindHistoryDirs(log);
        if (historyDirs.Count == 0) return;

        var modDir = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location)
                     ?? Directory.GetCurrentDirectory();
        var cachePath = Path.Combine(modDir, "uploaded-hashes.txt");
        var cache = new UploadedRunCache(cachePath, log);
        var uploader = new RunFileUploader(client, cache, log);

        log.Info($"Starting run-file watcher across {historyDirs.Count} profile(s).");
        foreach (var historyDir in historyDirs)
        {
            var w = new RunFileWatcher(
                historyDir: historyDir,
                onFileReady: file => uploader.UploadAsync(file),
                log: log);
            w.Start();
            w.EnumerateExisting();
            _watchers.Add(w);
        }
    }
}
