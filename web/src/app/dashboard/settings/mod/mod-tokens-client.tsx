"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";

export type UploadTokenRep = {
  id: string;
  name: string;
  tokenPrefix: string;
  createdAt: string;
  lastUsedAt: string | null;
};

type CreateRep = { token: UploadTokenRep; secret: string };

export function ModTokensClient({ initialTokens }: { initialTokens: UploadTokenRep[] }) {
  const router = useRouter();
  const [isPending, startTransition] = useTransition();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [revealed, setRevealed] = useState<CreateRep | null>(null);

  const handleCreate = async () => {
    setError(null);
    if (!name.trim()) {
      setError("Name is required.");
      return;
    }
    const res = await fetch("/api/upload-tokens", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: name.trim() }),
    });
    if (!res.ok) {
      setError(`Backend returned ${res.status}.`);
      return;
    }
    const created = (await res.json()) as CreateRep;
    setRevealed(created);
    setName("");
    startTransition(() => router.refresh());
  };

  const handleRevoke = async (id: string) => {
    if (!confirm("Revoke this token? The mod will stop working with it immediately.")) return;
    const res = await fetch(`/api/upload-tokens/${id}`, { method: "DELETE" });
    if (!res.ok) {
      setError(`Couldn't revoke token (${res.status}).`);
      return;
    }
    startTransition(() => router.refresh());
  };

  const copySecret = async (secret: string) => {
    try {
      await navigator.clipboard.writeText(secret);
    } catch {
      // ignore — clipboard may be blocked in some browsers
    }
  };

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Create a new token</CardTitle>
          <CardDescription>
            Give it a recognizable name (e.g. &ldquo;Desktop&rdquo;, &ldquo;Steam Deck&rdquo;).
            You can revoke it at any time.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex-1 space-y-1">
            <label htmlFor="token-name" className="text-sm font-medium">
              Name
            </label>
            <Input
              id="token-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Desktop"
              disabled={isPending}
            />
          </div>
          <Button onClick={handleCreate} disabled={isPending || !name.trim()}>
            Create token
          </Button>
        </CardContent>
        {error && (
          <CardContent>
            <p className="text-destructive text-sm">{error}</p>
          </CardContent>
        )}
      </Card>

      {revealed && (
        <Card className="border-amber-500/50 bg-amber-500/5">
          <CardHeader>
            <CardTitle>Copy this token now</CardTitle>
            <CardDescription>
              This is the only time you&apos;ll see the full secret. Save it somewhere
              safe — once you close this banner, only the prefix remains visible.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <code className="bg-muted block w-full overflow-x-auto rounded-md p-3 font-mono text-sm">
              {revealed.secret}
            </code>
            <div className="flex gap-2">
              <Button onClick={() => copySecret(revealed.secret)}>Copy</Button>
              <Button variant="outline" onClick={() => setRevealed(null)}>
                I&apos;ve saved it
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      <section className="space-y-2">
        <h2 className="text-lg font-semibold">Your tokens</h2>
        {initialTokens.length === 0 ? (
          <p className="text-muted-foreground text-sm">No tokens yet.</p>
        ) : (
          <ul className="space-y-2">
            {initialTokens.map((t) => (
              <li key={t.id}>
                <Card>
                  <CardHeader className="flex flex-row items-center justify-between gap-4 space-y-0">
                    <div>
                      <CardTitle className="text-base">{t.name}</CardTitle>
                      <CardDescription className="font-mono text-xs">
                        {t.tokenPrefix}…
                      </CardDescription>
                      <CardDescription className="text-xs">
                        Created {formatDate(t.createdAt)}
                        {t.lastUsedAt
                          ? ` · last used ${formatDate(t.lastUsedAt)}`
                          : " · never used"}
                      </CardDescription>
                    </div>
                    <Button
                      variant="outline"
                      onClick={() => handleRevoke(t.id)}
                      disabled={isPending}
                    >
                      Revoke
                    </Button>
                  </CardHeader>
                </Card>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
