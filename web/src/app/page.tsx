import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { auth } from "@clerk/nextjs/server";
import { redirect } from "next/navigation";
import { Button } from "@/components/ui/button";

export default async function Home() {
  const { userId } = await auth();
  if (userId) redirect("/dashboard");

  return (
    <div className="flex min-h-screen flex-col">
      <main className="flex flex-1 items-center justify-center px-6 py-24">
        <div className="flex max-w-xl flex-col items-center gap-6 text-center">
          <h1 className="text-5xl font-semibold tracking-tight sm:text-6xl">
            Study the Spire
          </h1>
          <p className="text-lg text-muted-foreground sm:text-xl">
            Turn every run into a lesson.
          </p>
          <div className="mt-2 flex flex-col items-center gap-3 sm:flex-row">
            <Button size="lg" render={<Link href="/sign-up" />}>
              Get started
            </Button>
            <Button
              variant="ghost"
              size="lg"
              render={<Link href="/sign-in" />}
            >
              Sign in
              <ArrowRight className="size-4" />
            </Button>
          </div>
        </div>
      </main>
      <footer className="px-6 pb-6 text-center text-sm text-muted-foreground">
        Out of love (addiction) for Slay the Spire 2 –{" "}
        <Link
          href="https://github.com/valarao/study-the-spire"
          target="_blank"
          rel="noreferrer"
          className="underline underline-offset-4 hover:text-foreground"
        >
          GitHub (Work in Progress)
        </Link>
      </footer>
    </div>
  );
}
