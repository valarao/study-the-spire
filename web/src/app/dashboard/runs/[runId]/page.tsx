import Link from "next/link";
import { auth } from "@clerk/nextjs/server";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

type RunRep = {
  id: string;
  status: "victory" | "defeat" | "abandoned";
  characterClass: string | null;
  ascension: number;
  seed: string;
  buildId: string;
  gameMode: string;
  platformType: string;
  startTime: string;
  runTimeSecs: number;
  killedByEncounter: string | null;
  killedByEvent: string | null;
  schemaVersion: number;
  acts: string[];
};

type RunDetailRep = { run: RunRep; rawJson: string; fileName: string | null };

export default async function RunDetailPage({
  params,
}: {
  params: Promise<{ runId: string }>;
}) {
  const { runId } = await params;
  const { getToken } = await auth();
  const token = await getToken({ template: "study-the-spire-backend" });

  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_BASE_URL}/runs/${encodeURIComponent(runId)}`,
    { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" },
  );

  if (!res.ok) {
    return (
      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Run not found</CardTitle>
          <CardDescription>Backend returned {res.status}.</CardDescription>
        </CardHeader>
      </Card>
    );
  }

  const { run, rawJson, fileName } = (await res.json()) as RunDetailRep;

  return (
    <div className="space-y-6">
      <nav className="text-sm">
        <Link href="/dashboard/runs" className="text-muted-foreground hover:underline">
          ← All runs
        </Link>
      </nav>

      <Card>
        <CardHeader className="space-y-2">
          <CardTitle className="flex items-center gap-3">
            <StatusPill status={run.status} />
            <span>{prettyCharacter(run.characterClass)}</span>
            <span className="text-muted-foreground text-sm font-normal">
              · A{run.ascension} · {run.gameMode}
            </span>
          </CardTitle>
          <CardDescription>
            Started {formatAbsolute(run.startTime)} · Lasted {formatDuration(run.runTimeSecs)}
          </CardDescription>
        </CardHeader>
        <CardContent className="grid grid-cols-1 gap-4 text-sm sm:grid-cols-2">
          <Field label="Seed" value={<code className="font-mono">{run.seed}</code>} />
          <Field label="Build" value={<code className="font-mono">{run.buildId}</code>} />
          <Field label="Platform" value={run.platformType} />
          <Field label="Schema version" value={String(run.schemaVersion)} />
          <Field label="Acts" value={run.acts.length === 0 ? "—" : run.acts.join(" → ")} />
          <Field
            label="Ended by"
            value={endedBy(run.killedByEncounter, run.killedByEvent, run.status)}
          />
          {fileName && <Field label="Original file" value={<code className="font-mono">{fileName}</code>} />}
          <Field label="Run id" value={<code className="font-mono text-xs">{run.id}</code>} />
        </CardContent>
      </Card>

      <details className="rounded-md border">
        <summary className="cursor-pointer px-3 py-2 text-sm font-medium">
          Raw run-file JSON
        </summary>
        <pre className="bg-muted/30 max-h-[60vh] overflow-auto px-3 py-2 text-xs leading-relaxed">
          <code>{rawJson}</code>
        </pre>
      </details>
    </div>
  );
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="space-y-0.5">
      <div className="text-muted-foreground text-xs uppercase tracking-wide">{label}</div>
      <div>{value}</div>
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
  if (!c) return "Unknown character";
  return c.replace(/^CHARACTER\./, "").toLowerCase().replace(/(^|_)([a-z])/g, (_, __, l) => " " + l.toUpperCase()).trim();
}

function endedBy(encounter: string | null, event: string | null, status: RunRep["status"]): string {
  if (status === "victory") return "—";
  if (encounter && encounter !== "NONE.NONE") return encounter;
  if (event && event !== "NONE.NONE") return event;
  return status === "abandoned" ? "Player abandoned" : "—";
}

function formatDuration(secs: number): string {
  const h = Math.floor(secs / 3600);
  const m = Math.floor((secs % 3600) / 60);
  const s = secs % 60;
  if (h > 0) return `${h}h ${m}m ${s}s`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}

function formatAbsolute(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString();
}

