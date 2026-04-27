"use client";

import Link from "next/link";
import { useState, useTransition } from "react";
import { Button } from "@/components/ui/button";

export type RunRep = {
  id: string;
  status: "victory" | "defeat" | "abandoned";
  characterClass: string | null;
  ascension: number;
  seed: string;
  buildId: string;
  startTime: string;
  runTimeSecs: number;
};

type RunsListRep = { runs: RunRep[]; nextCursor: string | null };

export function RunsTable({
  initialRuns,
  initialNextCursor,
  searchParams,
}: {
  initialRuns: RunRep[];
  initialNextCursor: string | null;
  searchParams: string;
}) {
  const [runs, setRuns] = useState(initialRuns);
  const [cursor, setCursor] = useState(initialNextCursor);
  const [error, setError] = useState<string | null>(null);
  const [isPending, startTransition] = useTransition();

  const loadMore = async () => {
    if (!cursor) return;
    setError(null);
    const sp = new URLSearchParams(searchParams);
    sp.set("cursor", cursor);
    const res = await fetch(`/api/runs?${sp.toString()}`, { cache: "no-store" });
    if (!res.ok) {
      setError(`Couldn't load more runs (${res.status}).`);
      return;
    }
    const data = (await res.json()) as RunsListRep;
    startTransition(() => {
      setRuns((prev) => [...prev, ...data.runs]);
      setCursor(data.nextCursor);
    });
  };

  if (runs.length === 0) {
    return (
      <p className="text-muted-foreground text-sm">No runs match these filters.</p>
    );
  }

  return (
    <div className="space-y-3">
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
            {runs.map((r) => (
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

      {error && <p className="text-destructive text-sm">{error}</p>}

      {cursor && (
        <Button variant="outline" onClick={loadMore} disabled={isPending}>
          {isPending ? "Loading…" : "Load more"}
        </Button>
      )}

      <p className="text-muted-foreground text-xs">
        Showing {runs.length} run{runs.length === 1 ? "" : "s"}
        {cursor ? " (more available)" : " (end of list)"}.
      </p>
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
