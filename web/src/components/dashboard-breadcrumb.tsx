"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb";

const PAGE_TITLES: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/dashboard/runs": "Runs",
  "/dashboard/stats": "Stats",
  "/dashboard/settings/mod": "Mod settings",
};

export function DashboardBreadcrumb() {
  const pathname = usePathname();
  const title = resolveTitle(pathname);

  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem className="hidden md:block">
          <BreadcrumbLink render={<Link href="/dashboard" />}>
            Study the Spire
          </BreadcrumbLink>
        </BreadcrumbItem>
        <BreadcrumbSeparator className="hidden md:block" />
        <BreadcrumbItem>
          <BreadcrumbPage>{title}</BreadcrumbPage>
        </BreadcrumbItem>
      </BreadcrumbList>
    </Breadcrumb>
  );
}

function resolveTitle(pathname: string): string {
  if (PAGE_TITLES[pathname]) return PAGE_TITLES[pathname];
  // Run detail: /dashboard/runs/<uuid>
  if (pathname.startsWith("/dashboard/runs/")) return "Run detail";
  // Fallback to last path segment, prettified
  const last = pathname.split("/").filter(Boolean).pop() ?? "Dashboard";
  return last.charAt(0).toUpperCase() + last.slice(1);
}
