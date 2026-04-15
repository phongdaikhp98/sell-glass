"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
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
import { useAuthStore } from "@/store/auth.store";
import { getMyOrders } from "@/lib/order.api";
import type { Order, OrderStatus, PageResponse } from "@/types";

const formatVND = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "Chờ xác nhận",
  CONFIRMED: "Đã xác nhận",
  PROCESSING: "Đang xử lý",
  READY: "Sẵn sàng",
  DELIVERING: "Đang giao",
  COMPLETED: "Hoàn thành",
  CANCELLED: "Đã hủy",
};

const ORDER_STATUS_CLASS: Record<OrderStatus, string> = {
  PENDING: "bg-yellow-100 text-yellow-800 border-yellow-200",
  CONFIRMED: "bg-blue-100 text-blue-800 border-blue-200",
  PROCESSING: "bg-indigo-100 text-indigo-800 border-indigo-200",
  READY: "bg-purple-100 text-purple-800 border-purple-200",
  DELIVERING: "bg-orange-100 text-orange-800 border-orange-200",
  COMPLETED: "bg-green-100 text-green-800 border-green-200",
  CANCELLED: "bg-red-100 text-red-800 border-red-200",
};

const PAGE_SIZE = 10;

export default function OrdersPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)();
  const [result, setResult] = useState<PageResponse<Order> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }
    setLoading(true);
    getMyOrders(page, PAGE_SIZE)
      .then(setResult)
      .catch(() => toast.error("Không thể tải danh sách đơn hàng"))
      .finally(() => setLoading(false));
  }, [isAuthenticated, page]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold">Đơn hàng của tôi</h1>

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}
        </div>
      ) : !result || result.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-24 text-muted-foreground">
          <p className="text-base">Bạn chưa có đơn hàng nào</p>
          <Button
            variant="outline"
            className="mt-6"
            onClick={() => router.push("/products")}
          >
            Mua sắm ngay
          </Button>
        </div>
      ) : (
        <>
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã đơn</TableHead>
                  <TableHead>Ngày đặt</TableHead>
                  <TableHead>Tổng tiền</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {result.content.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="font-mono font-medium">
                      #{order.id.slice(-4).toUpperCase()}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {new Date(order.createdAt).toLocaleDateString("vi-VN")}
                    </TableCell>
                    <TableCell className="font-medium">
                      {formatVND(order.total)}
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={ORDER_STATUS_CLASS[order.status]}
                      >
                        {ORDER_STATUS_LABEL[order.status]}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => router.push(`/orders/${order.id}`)}
                      >
                        Xem
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="mt-6 flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Trang {result.number + 1} / {result.totalPages} —{" "}
              {result.totalElements} đơn hàng
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
