import Link from "next/link";
import { ArrowRight } from "lucide-react";

export default function Home() {
  return (
    <main className="flex flex-1 items-center justify-center px-6 py-24">
      <div className="flex max-w-xl flex-col items-center gap-6 text-center">
        <h1 className="text-5xl font-semibold tracking-tight sm:text-6xl">
          Study the Spire
        </h1>
        <p className="text-lg text-muted-foreground sm:text-xl">
          Turn every run into a lesson.
        </p>
        <Link
          href="/dashboard"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
        >
          Open dashboard
          <ArrowRight className="size-4" />
        </Link>
      </div>
    </main>
  );
}
