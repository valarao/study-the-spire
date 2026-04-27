import { auth } from "@clerk/nextjs/server";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { RunsFilterBar } from "./runs-filter-bar";
import { RunsTable, type RunRep } from "./runs-table";

type RunsListRep = { runs: RunRep[]; nextCursor: string | null };

type SearchParams = {
  character?: string;
  ascension?: string;
  status?: string;
  from?: string;
  to?: string;
};

type StatsSummaryRep = {
  byCharacter: { characterClass: string | null; runs: number }[];
  byAscension: { ascension: number; runs: number }[];
};

export default async function RunsPage({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const sp = await searchParams;
  const { getToken } = await auth();
  const token = await getToken({ template: "study-the-spire-backend" });

  const qs = buildQueryString(sp);
  const runsRes = await fetch(
    `${process.env.NEXT_PUBLIC_API_BASE_URL}/runs${qs ? `?${qs}` : ""}`,
    { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" },
  );
  if (!runsRes.ok) {
    return (
      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Couldn&apos;t load runs</CardTitle>
          <CardDescription>Backend returned {runsRes.status}.</CardDescription>
        </CardHeader>
      </Card>
    );
  }
  const data = (await runsRes.json()) as RunsListRep;

  // Pull facet values for the filter dropdowns from the stats endpoint so the
  // user only ever sees options that actually have runs.
  const statsRes = await fetch(
    `${process.env.NEXT_PUBLIC_API_BASE_URL}/stats/summary`,
    { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" },
  );
  const stats: StatsSummaryRep | null = statsRes.ok ? await statsRes.json() : null;
  const characters = stats?.byCharacter
    .filter((c) => c.characterClass != null)
    .map((c) => c.characterClass as string) ?? [];
  const ascensions = stats?.byAscension.map((a) => a.ascension) ?? [];

  if (data.runs.length === 0 && !hasAnyFilter(sp)) {
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
              runs will show up here automatically.
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
          Filter by character, ascension, status, or date. Click a row to open
          the full run.
        </p>
      </header>
      <RunsFilterBar characters={characters} ascensions={ascensions} />
      <RunsTable
        initialRuns={data.runs}
        initialNextCursor={data.nextCursor}
        searchParams={qs}
      />
    </div>
  );
}

function buildQueryString(sp: SearchParams): string {
  const params = new URLSearchParams();
  if (sp.character) params.set("character", sp.character);
  if (sp.ascension) params.set("ascension", sp.ascension);
  if (sp.status) params.set("status", sp.status);
  if (sp.from) params.set("from", sp.from);
  if (sp.to) params.set("to", sp.to);
  return params.toString();
}

function hasAnyFilter(sp: SearchParams): boolean {
  return !!(sp.character || sp.ascension || sp.status || sp.from || sp.to);
}
