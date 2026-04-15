"use client";

import { useEffect, useState } from "react";
import { MapPinIcon, PhoneIcon, ClockIcon } from "lucide-react";
import { toast } from "sonner";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { getBranches } from "@/lib/branch.api";
import type { Branch } from "@/types";

export default function BranchesPage() {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getBranches()
      .then((data) => setBranches(data.filter((b) => b.isActive)))
      .catch(() => toast.error("Không thể tải danh sách chi nhánh"))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold">Chi nhánh</h1>

      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="flex flex-col gap-3 rounded-lg border p-4">
              <Skeleton className="h-5 w-2/3" />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-1/2" />
              <Skeleton className="h-4 w-1/3" />
            </div>
          ))}
        </div>
      ) : branches.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
          <p>Không có chi nhánh nào</p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {branches.map((branch) => (
            <Card key={branch.id}>
              <CardHeader className="pb-3">
                <CardTitle className="text-base">{branch.name}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2 text-sm text-muted-foreground">
                <div className="flex items-start gap-2">
                  <MapPinIcon className="mt-0.5 size-4 shrink-0 text-foreground/50" />
                  <span>{branch.address}</span>
                </div>
                <div className="flex items-center gap-2">
                  <PhoneIcon className="size-4 shrink-0 text-foreground/50" />
                  <span>{branch.phone}</span>
                </div>
                <div className="flex items-center gap-2">
                  <ClockIcon className="size-4 shrink-0 text-foreground/50" />
                  <span>
                    {branch.openTime} – {branch.closeTime}
                  </span>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
