import { auth } from "@clerk/nextjs/server";
import { NextResponse } from "next/server";

const BACKEND = process.env.NEXT_PUBLIC_API_BASE_URL!;

export async function DELETE(
  _req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const { id } = await params;
  const { getToken } = await auth();
  const token = await getToken({ template: "study-the-spire-backend" });
  if (!token) return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  const res = await fetch(`${BACKEND}/upload-tokens/${encodeURIComponent(id)}`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${token}` },
  });
  return new NextResponse(res.body, {
    status: res.status,
    headers: { "Content-Type": "application/json" },
  });
}
