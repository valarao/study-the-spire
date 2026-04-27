import { auth } from "@clerk/nextjs/server";
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default async function DashboardPage() {
  const { getToken } = await auth();
  const token = await getToken({ template: "study-the-spire-backend" });

  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_BASE_URL}/me`,
    {
      headers: { Authorization: `Bearer ${token}` },
      cache: "no-store",
    },
  );

  if (!res.ok) {
    return (
      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Identity unavailable</CardTitle>
          <CardDescription>
            Could not reach the backend ({res.status}). Check that
            NEXT_PUBLIC_API_BASE_URL and the Clerk JWT template are configured.
          </CardDescription>
        </CardHeader>
      </Card>
    );
  }

  const me = (await res.json()) as { userId: string; email: string | null };

  return (
    <Card className="max-w-xl">
      <CardHeader>
        <CardTitle>Welcome</CardTitle>
        <CardDescription>{me.email ?? me.userId}</CardDescription>
      </CardHeader>
    </Card>
  );
}
