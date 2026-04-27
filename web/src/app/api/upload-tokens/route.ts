import { auth } from "@clerk/nextjs/server";
import { NextResponse } from "next/server";

const BACKEND = process.env.NEXT_PUBLIC_API_BASE_URL!;

async function bearer(): Promise<string | null> {
  const { getToken } = await auth();
  return getToken({ template: "study-the-spire-backend" });
}

export async function GET() {
  const token = await bearer();
  if (!token) return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  const res = await fetch(`${BACKEND}/upload-tokens`, {
    headers: { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
  return new NextResponse(res.body, {
    status: res.status,
    headers: { "Content-Type": "application/json" },
  });
}

export async function POST(req: Request) {
  const token = await bearer();
  if (!token) return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  const body = await req.text();
  const res = await fetch(`${BACKEND}/upload-tokens`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body,
  });
  return new NextResponse(res.body, {
    status: res.status,
    headers: { "Content-Type": "application/json" },
  });
}
