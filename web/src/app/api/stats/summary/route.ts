import { auth } from "@clerk/nextjs/server";
import { NextResponse } from "next/server";

const BACKEND = process.env.NEXT_PUBLIC_API_BASE_URL!;

export async function GET() {
  const { getToken } = await auth();
  const token = await getToken({ template: "study-the-spire-backend" });
  if (!token) return NextResponse.json({ error: "unauthorized" }, { status: 401 });

  const res = await fetch(`${BACKEND}/stats/summary`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  return new NextResponse(res.body, {
    status: res.status,
    headers: { "Content-Type": "application/json" },
  });
}
