"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { MoreHorizontal } from "lucide-react";
import {
  getAdminOrders,
  getAdminOrder,
  updateOrderStatus,
  type OrderResponse,
} from "@/lib/admin.api";
import type { OrderStatus, PaymentStatus, PageResponse } from "@/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";

const PAGE_SIZE = 10;

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

const PAYMENT_STATUS_LABEL: Record<PaymentStatus, string> = {
  UNPAID: "Chưa thanh toán",
  PENDING_VERIFY: "Chờ xác nhận",
  PAID: "Đã thanh toán",
};

const PAYMENT_STATUS_CLASS: Record<PaymentStatus, string> = {
  UNPAID: "bg-red-100 text-red-800 border-red-200",
  PENDING_VERIFY: "bg-yellow-100 text-yellow-800 border-yellow-200",
  PAID: "bg-green-100 text-green-800 border-green-200",
};

const ALL_STATUSES: OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "PROCESSING",
  "READY",
  "DELIVERING",
  "COMPLETED",
  "CANCELLED",
];

const ORDER_TYPE_LABEL: Record<string, string> = {
  PICKUP: "Nhận tại cửa hàng",
  DELIVERY: "Giao hàng",
};

export default function AdminOrdersPage() {
  const [result, setResult] = useState<PageResponse<OrderResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  // Detail dialog
  const [detailOrder, setDetailOrder] = useState<OrderResponse | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);

  // Update status dialog
  const [updateOrder, setUpdateOrder] = useState<OrderResponse | null>(null);
  const [updateOpen, setUpdateOpen] = useState(false);
  const [newStatus, setNewStatus] = useState<string>("");
  const [cancelReason, setCancelReason] = useState("");
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    setLoading(true);
    getAdminOrders(page, PAGE_SIZE)
      .then(setResult)
      .catch(() => toast.error("Không thể tải danh sách đơn hàng"))
      .finally(() => setLoading(false));
  }, [page]);

  function openDetail(order: OrderResponse) {
    setDetailOpen(true);
    setDetailOrder(null);
    setDetailLoading(true);
    getAdminOrder(order.id)
      .then(setDetailOrder)
      .catch(() => toast.error("Không thể tải chi tiết đơn hàng"))
      .finally(() => setDetailLoading(false));
  }

  function openUpdateStatus(order: OrderResponse) {
    setUpdateOrder(order);
    setNewStatus(order.status);
    setCancelReason("");
    setUpdateOpen(true);
  }

  async function handleUpdateStatus() {
    if (!updateOrder) return;
    setUpdating(true);
    try {
      const updated = await updateOrderStatus(
        updateOrder.id,
        newStatus,
        newStatus === "CANCELLED" ? cancelReason : undefined
      );
      toast.success("Cập nhật trạng thái thành công");
      setUpdateOpen(false);
      // Refresh list
      setResult((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          content: prev.content.map((o) =>
            o.id === updated.id ? { ...o, status: updated.status, cancelledReason: updated.cancelledReason } : o
          ),
        };
      });
    } catch {
      toast.error("Cập nhật trạng thái thất bại");
    } finally {
      setUpdating(false);
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Quản lý đơn hàng</h1>

      {loading ? (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full rounded-md" />
          ))}
        </div>
      ) : !result || result.content.length === 0 ? (
        <div className="flex items-center justify-center py-24 text-muted-foreground">
          <p className="text-base">Không có đơn hàng nào</p>
        </div>
      ) : (
        <>
          <div className="rounded-lg border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã đơn</TableHead>
                  <TableHead>Khách hàng</TableHead>
                  <TableHead>Chi nhánh</TableHead>
                  <TableHead>Loại</TableHead>
                  <TableHead>Tổng tiền</TableHead>
                  <TableHead>Trạng thái</TableHead>
                  <TableHead>Thanh toán</TableHead>
                  <TableHead>Ngày</TableHead>
                  <TableHead className="text-right">Hành động</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {result.content.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="font-mono font-medium">
                      #{order.id.slice(-4).toUpperCase()}
                    </TableCell>
                    <TableCell className="font-mono text-muted-foreground">
                      #{order.customerId.slice(-4).toUpperCase()}
                    </TableCell>
                    <TableCell className="max-w-32 truncate text-sm">{order.branchName}</TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {ORDER_TYPE_LABEL[order.orderType] ?? order.orderType}
                    </TableCell>
                    <TableCell className="font-medium">{formatVND(order.total)}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={ORDER_STATUS_CLASS[order.status as OrderStatus]}
                      >
                        {ORDER_STATUS_LABEL[order.status as OrderStatus] ?? order.status}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={PAYMENT_STATUS_CLASS[order.paymentStatus as PaymentStatus]}
                      >
                        {PAYMENT_STATUS_LABEL[order.paymentStatus as PaymentStatus] ?? order.paymentStatus}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(order.createdAt).toLocaleDateString("vi-VN")}
                    </TableCell>
                    <TableCell className="text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger
                          render={
                            <Button variant="ghost" size="icon-sm" aria-label="Hành động" />
                          }
                        >
                          <MoreHorizontal className="size-4" />
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => openDetail(order)}>
                            Xem chi tiết
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => openUpdateStatus(order)}>
                            Cập nhật trạng thái
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="flex flex-col items-center gap-3 sm:flex-row sm:justify-between">
            <p className="text-sm text-muted-foreground">
              Trang {result.number + 1} / {result.totalPages} — {result.totalElements} đơn hàng
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

      {/* Detail dialog */}
      <Dialog open={detailOpen} onOpenChange={setDetailOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>
              Chi tiết đơn hàng{detailOrder ? ` #${detailOrder.id.slice(-4).toUpperCase()}` : ""}
            </DialogTitle>
          </DialogHeader>
          {detailLoading ? (
            <div className="flex flex-col gap-3 py-4">
              <Skeleton className="h-5 w-full" />
              <Skeleton className="h-5 w-3/4" />
              <Skeleton className="h-5 w-1/2" />
            </div>
          ) : detailOrder ? (
            <div className="flex flex-col gap-4 text-sm">
              <div className="grid grid-cols-2 gap-x-4 gap-y-2">
                <div>
                  <p className="text-muted-foreground">Chi nhánh</p>
                  <p className="font-medium">{detailOrder.branchName}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Loại đơn</p>
                  <p className="font-medium">{ORDER_TYPE_LABEL[detailOrder.orderType] ?? detailOrder.orderType}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Người nhận</p>
                  <p className="font-medium">{detailOrder.receiverName}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">SĐT</p>
                  <p className="font-medium">{detailOrder.receiverPhone}</p>
                </div>
                {detailOrder.deliveryAddress && (
                  <div className="col-span-2">
                    <p className="text-muted-foreground">Địa chỉ giao</p>
                    <p className="font-medium">{detailOrder.deliveryAddress}</p>
                  </div>
                )}
                {detailOrder.note && (
                  <div className="col-span-2">
                    <p className="text-muted-foreground">Ghi chú</p>
                    <p className="font-medium">{detailOrder.note}</p>
                  </div>
                )}
                {detailOrder.cancelledReason && (
                  <div className="col-span-2">
                    <p className="text-muted-foreground">Lý do hủy</p>
                    <p className="font-medium text-destructive">{detailOrder.cancelledReason}</p>
                  </div>
                )}
              </div>

              <Separator />

              <div>
                <p className="mb-2 font-medium">Sản phẩm</p>
                <div className="flex flex-col gap-2">
                  {detailOrder.items.map((item) => (
                    <div key={item.id} className="flex items-center justify-between text-sm">
                      <div>
                        <p>{item.productName}</p>
                        <p className="text-muted-foreground">{item.variantSku} x {item.quantity}</p>
                      </div>
                      <p className="font-medium">{formatVND(item.subtotal)}</p>
                    </div>
                  ))}
                </div>
              </div>

              <Separator />

              <div className="flex flex-col gap-1 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Tạm tính</span>
                  <span>{formatVND(detailOrder.subtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Phí vận chuyển</span>
                  <span>{formatVND(detailOrder.shippingFee)}</span>
                </div>
                <div className="flex justify-between font-semibold">
                  <span>Tổng cộng</span>
                  <span>{formatVND(detailOrder.total)}</span>
                </div>
              </div>
            </div>
          ) : null}
          <DialogFooter showCloseButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Update status dialog */}
      <Dialog open={updateOpen} onOpenChange={setUpdateOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>
              Cập nhật trạng thái{updateOrder ? ` #${updateOrder.id.slice(-4).toUpperCase()}` : ""}
            </DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="status-select">Trạng thái mới</Label>
              <Select value={newStatus} onValueChange={(v) => { if (v !== null) setNewStatus(v); }}>
                <SelectTrigger id="status-select" className="w-full">
                  <SelectValue placeholder="Chọn trạng thái" />
                </SelectTrigger>
                <SelectContent>
                  {ALL_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {ORDER_STATUS_LABEL[s]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {newStatus === "CANCELLED" && (
              <div className="flex flex-col gap-2">
                <Label htmlFor="cancel-reason">Lý do hủy</Label>
                <Textarea
                  id="cancel-reason"
                  placeholder="Nhập lý do hủy đơn..."
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  rows={3}
                />
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setUpdateOpen(false)} disabled={updating}>
              Hủy
            </Button>
            <Button onClick={handleUpdateStatus} disabled={updating}>
              {updating ? "Đang lưu..." : "Lưu"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
