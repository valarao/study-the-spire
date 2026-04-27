import { auth } from "@clerk/nextjs/server";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type CharacterStat = { characterClass: string | null; runs: number; wins: number };
type AscensionStat = { ascension: number; runs: number; wins: number };
type DeathCauseStat = { cause: string; count: number };

type StatsSummaryRep = {
  totalRuns: number;
  wins: number;
  defeats: number;
  abandoned: number;
  winRate: number | null;
  avgRunTimeSecs: number | null;
  byCharacter: CharacterStat[];
  byAscension: AscensionStat[];
  topDeathCauses: DeathCauseStat[];
};

export default async function StatsPage() {
  const { getToken } = await auth();
  const token = await getToken({ template: "study-the-spire-backend" });

  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/stats/summary`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  if (!res.ok) {
    return (
      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Couldn&apos;t load stats</CardTitle>
          <CardDescription>Backend returned {res.status}.</CardDescription>
        </CardHeader>
      </Card>
    );
  }

  const stats = (await res.json()) as StatsSummaryRep;

  if (stats.totalRuns === 0) {
    return (
      <div className="space-y-4">
        <header className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">Stats</h1>
        </header>
        <Card className="max-w-xl">
          <CardHeader>
            <CardTitle>No runs to summarize yet</CardTitle>
            <CardDescription>
              Once your mod uploads runs, this page shows your win rate and patterns.
            </CardDescription>
          </CardHeader>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">Stats</h1>
        <p className="text-muted-foreground text-sm">
          {stats.totalRuns} run{stats.totalRuns === 1 ? "" : "s"} analyzed.
        </p>
      </header>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard
          label="Total runs"
          value={String(stats.totalRuns)}
          sub={`${stats.wins} W · ${stats.defeats} L · ${stats.abandoned} abandoned`}
        />
        <StatCard
          label="Win rate"
          value={stats.winRate == null ? "—" : `${(stats.winRate * 100).toFixed(1)}%`}
          sub={
            stats.winRate == null ? undefined : (
              <ProgressBar value={stats.winRate} />
            )
          }
        />
        <StatCard
          label="Avg run time"
          value={stats.avgRunTimeSecs == null ? "—" : formatDuration(stats.avgRunTimeSecs)}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">By character</CardTitle>
        </CardHeader>
        <CardContent>
          <BreakdownTable
            rows={stats.byCharacter.map((c) => ({
              key: c.characterClass ?? "—",
              label: prettyCharacter(c.characterClass),
              runs: c.runs,
              wins: c.wins,
            }))}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">By ascension</CardTitle>
        </CardHeader>
        <CardContent>
          <BreakdownTable
            rows={stats.byAscension.map((a) => ({
              key: String(a.ascension),
              label: `A${a.ascension}`,
              runs: a.runs,
              wins: a.wins,
            }))}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Top death causes</CardTitle>
          <CardDescription>Where your non-victorious runs ended.</CardDescription>
        </CardHeader>
        <CardContent>
          {stats.topDeathCauses.length === 0 ? (
            <p className="text-muted-foreground text-sm">All wins so far. Nice.</p>
          ) : (
            <ul className="space-y-1 text-sm">
              {stats.topDeathCauses.map((d) => (
                <li key={d.cause} className="flex items-center justify-between">
                  <span className="font-mono text-xs">{d.cause}</span>
                  <span className="text-muted-foreground">×{d.count}</span>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function StatCard({
  label,
  value,
  sub,
}: {
  label: string;
  value: string;
  sub?: React.ReactNode;
}) {
  return (
    <Card>
      <CardHeader className="space-y-0.5">
        <CardDescription className="text-xs uppercase tracking-wide">{label}</CardDescription>
        <CardTitle className="text-3xl">{value}</CardTitle>
      </CardHeader>
      {sub && <CardContent className="text-muted-foreground text-xs">{sub}</CardContent>}
    </Card>
  );
}

function ProgressBar({ value }: { value: number }) {
  const pct = Math.max(0, Math.min(1, value)) * 100;
  return (
    <div className="bg-muted h-1.5 w-full overflow-hidden rounded-full">
      <div className="h-full bg-emerald-500" style={{ width: `${pct}%` }} />
    </div>
  );
}

function BreakdownTable({
  rows,
}: {
  rows: { key: string; label: string; runs: number; wins: number }[];
}) {
  if (rows.length === 0) {
    return <p className="text-muted-foreground text-sm">No data.</p>;
  }
  const maxRuns = Math.max(...rows.map((r) => r.runs));
  return (
    <table className="w-full text-sm">
      <thead className="text-muted-foreground text-left text-xs uppercase tracking-wide">
        <tr>
          <th className="py-1 font-medium">Name</th>
          <th className="py-1 font-medium">Runs</th>
          <th className="py-1 font-medium">Wins</th>
          <th className="py-1 font-medium">Win %</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((r) => {
          const winPct = r.runs === 0 ? 0 : (r.wins / r.runs) * 100;
          const widthPct = maxRuns === 0 ? 0 : (r.runs / maxRuns) * 100;
          return (
            <tr key={r.key} className="border-t">
              <td className="py-1.5">{r.label}</td>
              <td className="py-1.5">
                <div className="flex items-center gap-2">
                  <span>{r.runs}</span>
                  <div className="bg-muted h-1.5 w-24 overflow-hidden rounded-full">
                    <div className="h-full bg-emerald-500/60" style={{ width: `${widthPct}%` }} />
                  </div>
                </div>
              </td>
              <td className="py-1.5">{r.wins}</td>
              <td className="py-1.5">{winPct.toFixed(1)}%</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

function prettyCharacter(c: string | null): string {
  if (!c) return "Unknown";
  return c.replace(/^CHARACTER\./, "").toLowerCase().replace(/(^|_)([a-z])/g, (_, __, l) => " " + l.toUpperCase()).trim();
}

function formatDuration(secs: number): string {
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}
