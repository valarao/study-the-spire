"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

const STATUSES = ["victory", "defeat", "abandoned"] as const;

export function RunsFilterBar({
  characters,
  ascensions,
}: {
  characters: string[];
  ascensions: number[];
}) {
  const router = useRouter();
  const params = useSearchParams();
  const [isPending, startTransition] = useTransition();

  const character = params.get("character") ?? "";
  const ascension = params.get("ascension") ?? "";
  const status = params.get("status") ?? "";
  const from = params.get("from") ?? "";
  const to = params.get("to") ?? "";

  const update = (next: Record<string, string>) => {
    const sp = new URLSearchParams(params.toString());
    for (const [k, v] of Object.entries(next)) {
      if (v) sp.set(k, v);
      else sp.delete(k);
    }
    const qs = sp.toString();
    startTransition(() => router.push(qs ? `/dashboard/runs?${qs}` : `/dashboard/runs`));
  };

  const clearAll = () =>
    startTransition(() => router.push("/dashboard/runs"));

  const hasFilters = !!(character || ascension || status || from || to);

  return (
    <div className="flex flex-wrap items-end gap-3 rounded-md border p-3">
      <div className="space-y-1">
        <label className="text-muted-foreground text-xs uppercase tracking-wide">Character</label>
        <select
          className="border-input bg-background h-9 rounded-md border px-2 text-sm"
          value={character}
          onChange={(e) => update({ character: e.target.value })}
          disabled={isPending}
        >
          <option value="">Any</option>
          {characters.map((c) => (
            <option key={c} value={c}>{prettyCharacter(c)}</option>
          ))}
        </select>
      </div>

      <div className="space-y-1">
        <label className="text-muted-foreground text-xs uppercase tracking-wide">Ascension</label>
        <select
          className="border-input bg-background h-9 rounded-md border px-2 text-sm"
          value={ascension}
          onChange={(e) => update({ ascension: e.target.value })}
          disabled={isPending}
        >
          <option value="">Any</option>
          {ascensions.map((a) => (
            <option key={a} value={String(a)}>A{a}</option>
          ))}
        </select>
      </div>

      <div className="space-y-1">
        <label className="text-muted-foreground text-xs uppercase tracking-wide">Status</label>
        <div className="flex gap-1">
          {STATUSES.map((s) => (
            <button
              key={s}
              type="button"
              disabled={isPending}
              onClick={() => update({ status: status === s ? "" : s })}
              className={`h-9 rounded-md border px-3 text-xs capitalize ${
                status === s ? "bg-foreground text-background" : "bg-background"
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-1">
        <label htmlFor="from" className="text-muted-foreground text-xs uppercase tracking-wide">From</label>
        <Input
          id="from"
          type="date"
          value={from.slice(0, 10)}
          onChange={(e) => update({ from: e.target.value ? `${e.target.value}T00:00:00Z` : "" })}
          disabled={isPending}
          className="h-9 w-40"
        />
      </div>

      <div className="space-y-1">
        <label htmlFor="to" className="text-muted-foreground text-xs uppercase tracking-wide">To</label>
        <Input
          id="to"
          type="date"
          value={to.slice(0, 10)}
          onChange={(e) => update({ to: e.target.value ? `${e.target.value}T00:00:00Z` : "" })}
          disabled={isPending}
          className="h-9 w-40"
        />
      </div>

      {hasFilters && (
        <Button variant="outline" onClick={clearAll} disabled={isPending} className="h-9">
          Clear
        </Button>
      )}
    </div>
  );
}

function prettyCharacter(c: string): string {
  return c.replace(/^CHARACTER\./, "").toLowerCase().replace(/(^|_)([a-z])/g, (_, __, l) => " " + l.toUpperCase()).trim();
}
