"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { getCustomers, type CustomerResponse } from "@/lib/admin.api";
import type { PageResponse } from "@/types";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

const PAGE_SIZE = 10;

export default function AdminCustomersPage() {
  const [result, setResult] = useState<PageResponse<CustomerResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    getCustomers(page, PAGE_SIZE)
      .then(setResult)
      .catch(() => toast.error("Không thể tải danh sách khách hàng"))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Quản lý khách hàng</h1>

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}
        </div>
      ) : !result || result.content.length === 0 ? (
        <div className="flex items-center justify-center py-24 text-muted-foreground">
          <p className="text-base">Không có khách hàng nào</p>
        </div>
      ) : (
        <>
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Họ tên</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>SĐT</TableHead>
                  <TableHead>Ngày tạo</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {result.content.map((customer) => (
                  <TableRow key={customer.id}>
                    <TableCell className="font-medium">{customer.fullName}</TableCell>
                    <TableCell className="text-muted-foreground">{customer.email}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {customer.phone ?? "—"}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {new Date(customer.createdAt).toLocaleDateString("vi-VN")}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Trang {result.number + 1} / {result.totalPages} — {result.totalElements} khách hàng
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={result.number === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                Trước
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={result.number + 1 >= result.totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Tiếp
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
