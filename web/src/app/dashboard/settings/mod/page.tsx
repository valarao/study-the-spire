import { auth } from "@clerk/nextjs/server";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ModTokensClient, type UploadTokenRep } from "./mod-tokens-client";

export default async function ModSettingsPage() {
  const { getToken } = await auth();
  const token = await getToken({ template: "study-the-spire-backend" });

  const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/upload-tokens`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });

  if (!res.ok) {
    return (
      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Couldn&apos;t load tokens</CardTitle>
          <CardDescription>
            Backend returned {res.status}. Try refreshing.
          </CardDescription>
        </CardHeader>
      </Card>
    );
  }

  const data = (await res.json()) as { tokens: UploadTokenRep[] };

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <h1 className="text-2xl font-semibold tracking-tight">Mod settings</h1>
        <p className="text-muted-foreground text-sm">
          Generate upload tokens for the Study the Spire mod. Each token is a long-lived
          credential — keep it secret, treat it like a password.
        </p>
      </header>
      <ModTokensClient initialTokens={data.tokens} />
    </div>
  );
}
