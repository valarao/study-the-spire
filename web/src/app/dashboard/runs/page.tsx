import Link from "next/link";
import { auth } from "@clerk/nextjs/server";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type RunRep = {
  id: string;
  status: "victory" | "defeat" | "abandoned";
  characterClass: string | null;
  ascension: number;
  seed: string;
  buildId: string;
  startTime: string;
  runTimeSecs: number;
  killedByEncounter: string | null;
  killedByEvent: string | null;
};

type RunsListRep = { runs: RunRep[] };

export default async function RunsPage() {
  const { getToken } = await auth();
  const token = await getToken({ template: "study-the-spire-backend" });

  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/runs`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  if (!res.ok) {
    return (
      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Couldn&apos;t load runs</CardTitle>
          <CardDescription>Backend returned {res.status}.</CardDescription>
        </CardHeader>
      </Card>
    );
  }

  const data = (await res.json()) as RunsListRep;

  if (data.runs.length === 0) {
    return (
      <div className="space-y-4">
        <header className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">Runs</h1>
        </header>
        <Card className="max-w-xl">
          <CardHeader>
            <CardTitle>No runs uploaded yet</CardTitle>
            <CardDescription>
              Configure the mod with an upload token and start playing — finished
              runs will show up here automatically. See <code>mod/README.md</code>
              for setup.
            </CardDescription>
          </CardHeader>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">Runs</h1>
        <p className="text-muted-foreground text-sm">
          {data.runs.length} run{data.runs.length === 1 ? "" : "s"} uploaded.
          Newest first.
        </p>
      </header>
      <div className="overflow-x-auto rounded-md border">
        <table className="w-full text-sm">
          <thead className="bg-muted/50 text-left">
            <tr>
              <th className="px-3 py-2 font-medium">Status</th>
              <th className="px-3 py-2 font-medium">Character</th>
              <th className="px-3 py-2 font-medium">Asc</th>
              <th className="px-3 py-2 font-medium">Started</th>
              <th className="px-3 py-2 font-medium">Duration</th>
              <th className="px-3 py-2 font-medium">Build</th>
            </tr>
          </thead>
          <tbody>
            {data.runs.map((r) => (
              <tr key={r.id} className="hover:bg-muted/30 border-t">
                <td className="px-3 py-2">
                  <Link href={`/dashboard/runs/${r.id}`} className="hover:underline">
                    <StatusPill status={r.status} />
                  </Link>
                </td>
                <td className="px-3 py-2">
                  <Link href={`/dashboard/runs/${r.id}`} className="hover:underline">
                    {prettyCharacter(r.characterClass)}
                  </Link>
                </td>
                <td className="px-3 py-2">{r.ascension}</td>
                <td className="px-3 py-2">{formatRelative(r.startTime)}</td>
                <td className="px-3 py-2">{formatDuration(r.runTimeSecs)}</td>
                <td className="text-muted-foreground px-3 py-2 font-mono text-xs">
                  {r.buildId}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatusPill({ status }: { status: RunRep["status"] }) {
  const tone =
    status === "victory"
      ? "bg-emerald-500/15 text-emerald-700 dark:text-emerald-400"
      : status === "defeat"
        ? "bg-rose-500/15 text-rose-700 dark:text-rose-400"
        : "bg-muted text-muted-foreground";
  return (
    <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${tone}`}>
      {status}
    </span>
  );
}

function prettyCharacter(c: string | null): string {
  if (!c) return "—";
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

function formatRelative(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  const ms = Date.now() - d.getTime();
  const days = Math.floor(ms / 86_400_000);
  if (days >= 7) return d.toLocaleDateString();
  if (days >= 1) return `${days}d ago`;
  const hours = Math.floor(ms / 3_600_000);
  if (hours >= 1) return `${hours}h ago`;
  const mins = Math.floor(ms / 60_000);
  if (mins >= 1) return `${mins}m ago`;
  return "just now";
}
