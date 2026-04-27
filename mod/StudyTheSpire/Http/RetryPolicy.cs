using System;
using System.Threading;
using System.Threading.Tasks;

namespace StudyTheSpire.Http;

internal abstract record RetryDecision
{
    public sealed record Success<T>(T Value) : RetryDecision;
    public sealed record RetryAfter(TimeSpan Delay) : RetryDecision;
    public sealed record Fatal(string Reason) : RetryDecision;
}

internal static class RetryPolicy
{
    /// <summary>
    /// Runs <paramref name="op"/> up to <paramref name="maxAttempts"/> times. The classifier decides
    /// per result whether the call succeeded, should retry (with a delay), or should fail fatally.
    /// </summary>
    public static async Task<T?> ExecuteAsync<T>(
        Func<Task<T>> op,
        Func<T, RetryDecision> classify,
        int maxAttempts = 3,
        TimeSpan? baseDelay = null,
        CancellationToken ct = default)
        where T : class
    {
        var rng = new Random();
        var delay = baseDelay ?? TimeSpan.FromMilliseconds(250);
        for (var attempt = 1; attempt <= maxAttempts; attempt++)
        {
            T result;
            try
            {
                result = await op().ConfigureAwait(false);
            }
            catch (Exception)
            {
                if (attempt == maxAttempts) throw;
                await Task.Delay(WithJitter(delay, rng), ct).ConfigureAwait(false);
                delay = TimeSpan.FromMilliseconds(delay.TotalMilliseconds * 2);
                continue;
            }
            switch (classify(result))
            {
                case RetryDecision.Success<T> success:
                    return success.Value;
                case RetryDecision.Fatal:
                    return null;
                case RetryDecision.RetryAfter ra:
                    if (attempt == maxAttempts) return null;
                    var sleep = ra.Delay > TimeSpan.Zero ? ra.Delay : WithJitter(delay, rng);
                    await Task.Delay(sleep, ct).ConfigureAwait(false);
                    delay = TimeSpan.FromMilliseconds(delay.TotalMilliseconds * 2);
                    continue;
            }
        }
        return null;
    }

    private static TimeSpan WithJitter(TimeSpan d, Random rng)
    {
        var jitterMs = rng.Next(-50, 51);
        var ms = Math.Max(0, d.TotalMilliseconds + jitterMs);
        return TimeSpan.FromMilliseconds(ms);
    }
}
