import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function DashboardPage() {
  return (
    <Card className="max-w-xl">
      <CardHeader>
        <CardTitle>No runs yet</CardTitle>
        <CardDescription>
          Upload a run from the mod to see it appear here.
        </CardDescription>
      </CardHeader>
    </Card>
  );
}
