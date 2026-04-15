"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeftIcon } from "lucide-react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthStore } from "@/store/auth.store";
import { getMyOrder } from "@/lib/order.api";
import type { Order, OrderStatus, PaymentStatus } from "@/types";

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

export default function OrderDetailPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)();
  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }
    getMyOrder(params.id)
      .then(setOrder)
      .catch(() => toast.error("Không thể tải thông tin đơn hàng"))
      .finally(() => setLoading(false));
  }, [isAuthenticated, params.id]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-8">
        <Skeleton className="mb-6 h-7 w-48" />
        <div className="space-y-4">
          <Skeleton className="h-24 w-full rounded-lg" />
          <Skeleton className="h-32 w-full rounded-lg" />
          <Skeleton className="h-40 w-full rounded-lg" />
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col items-center justify-center px-4 py-24 text-muted-foreground">
        <p>Không tìm thấy đơn hàng</p>
        <Button variant="outline" className="mt-4" onClick={() => router.push("/orders")}>
          Quay lại đơn hàng
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <Button
        variant="ghost"
        size="sm"
        className="mb-6 -ml-2 gap-1"
        onClick={() => router.push("/orders")}
      >
        <ArrowLeftIcon className="size-4" />
        Quay lại đơn hàng
      </Button>

      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">
            Đơn hàng #{order.id.slice(-4).toUpperCase()}
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Đặt ngày {new Date(order.createdAt).toLocaleDateString("vi-VN", {
              day: "2-digit",
              month: "2-digit",
              year: "numeric",
              hour: "2-digit",
              minute: "2-digit",
            })}
          </p>
        </div>
        <Badge
          variant="outline"
          className={ORDER_STATUS_CLASS[order.status]}
        >
          {ORDER_STATUS_LABEL[order.status]}
        </Badge>
      </div>

      <div className="space-y-4">
        {/* Branch & delivery info */}
        <div className="rounded-lg border p-4">
          <h2 className="mb-3 font-medium">Thông tin đơn hàng</h2>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Chi nhánh</span>
              <span className="font-medium">{order.branch.name}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Hình thức</span>
              <span className="font-medium">
                {order.orderType === "PICKUP" ? "Nhận tại cửa hàng" : "Giao hàng tận nơi"}
              </span>
            </div>
            {order.orderType === "DELIVERY" && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Địa chỉ</span>
                <span className="max-w-xs text-right font-medium">{order.branch.address}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-muted-foreground">Thanh toán</span>
              <Badge
                variant="outline"
                className={PAYMENT_STATUS_CLASS[order.paymentStatus]}
              >
                {PAYMENT_STATUS_LABEL[order.paymentStatus]}
              </Badge>
            </div>
          </div>
        </div>

        {/* Payment section */}
        {order.paymentStatus === "PAID" ? (
          <div className="flex items-center gap-2 rounded-lg border border-green-200 bg-green-50 px-4 py-3">
            <span className="text-lg">✅</span>
            <span className="font-semibold text-green-800">Đã thanh toán</span>
          </div>
        ) : (
          <div className="rounded-lg border border-dashed border-amber-400 bg-amber-50 p-4">
            <h2 className="mb-3 font-medium text-amber-900">Thanh toán chuyển khoản</h2>
            <div className="flex flex-col gap-4 sm:flex-row sm:gap-6">
              <div className="flex-1 space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Ngân hàng</span>
                  <span className="font-medium">Vietcombank</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Số tài khoản</span>
                  <span className="font-medium">1234567890</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Chủ tài khoản</span>
                  <span className="font-medium">SELL GLASS</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Nội dung CK</span>
                  <button
                    className="font-bold tracking-wide text-amber-900 hover:underline"
                    onClick={() => {
                      navigator.clipboard.writeText(`SG-${order.id.slice(-4).toUpperCase()}`);
                      toast.success("Đã sao chép nội dung chuyển khoản");
                    }}
                  >
                    SG-{order.id.slice(-4).toUpperCase()}
                  </button>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Số tiền</span>
                  <span className="font-semibold">{formatVND(order.total)}</span>
                </div>
                <p className="pt-1 text-xs text-muted-foreground">
                  Sau khi chuyển khoản, đơn hàng sẽ được xác nhận trong vòng 15 phút
                </p>
              </div>
              <div className="flex h-32 w-32 shrink-0 items-center justify-center self-center rounded-md border border-dashed border-amber-400 text-xs text-muted-foreground">
                QR Code ngân hàng
              </div>
            </div>
          </div>
        )}

        {/* Order items */}
        <div className="rounded-lg border p-4">
          <h2 className="mb-3 font-medium">Sản phẩm</h2>
          <div className="space-y-3">
            {order.items.map((item) => (
              <div key={item.id} className="flex items-center justify-between gap-4 text-sm">
                <div className="flex-1">
                  <p className="font-medium">{item.productName}</p>
                  <p className="text-muted-foreground">SKU: {item.variantSku}</p>
                </div>
                <div className="flex items-center gap-4 text-right">
                  <span className="text-muted-foreground">
                    {formatVND(item.unitPrice)} x {item.quantity}
                  </span>
                  <span className="w-24 font-medium">{formatVND(item.subtotal)}</span>
                </div>
              </div>
            ))}
          </div>

          <Separator className="my-4" />

          <div className="space-y-2 text-sm">
            <div className="flex justify-between text-muted-foreground">
              <span>Tạm tính</span>
              <span>{formatVND(order.subtotal)}</span>
            </div>
            <div className="flex justify-between text-muted-foreground">
              <span>Phí vận chuyển</span>
              <span>
                {order.shippingFee === 0 ? "Miễn phí" : formatVND(order.shippingFee)}
              </span>
            </div>
            <Separator />
            <div className="flex justify-between font-semibold">
              <span>Tổng cộng</span>
              <span>{formatVND(order.total)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
