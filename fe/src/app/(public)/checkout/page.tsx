"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useAuthStore } from "@/store/auth.store";
import { useCartStore } from "@/store/cart.store";
import { getCart } from "@/lib/cart.api";
import { createOrder } from "@/lib/order.api";
import { getBranches } from "@/lib/branch.api";
import type { Branch, Cart } from "@/types";

const formatVND = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

const SHIPPING_FEE = 30_000;

export default function CheckoutPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)();
  const { cart, setCart, clearCart } = useCartStore();

  const [localCart, setLocalCart] = useState<Cart | null>(cart);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [orderType, setOrderType] = useState<"PICKUP" | "DELIVERY">("PICKUP");
  const [branchId, setBranchId] = useState("");
  const [receiverName, setReceiverName] = useState("");
  const [receiverPhone, setReceiverPhone] = useState("");
  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }
    const loadData = async () => {
      const [cartData, branchData] = await Promise.all([
        getCart().catch(() => null),
        getBranches().catch(() => []),
      ]);
      if (cartData) {
        setLocalCart(cartData);
        setCart(cartData);
      }
      setBranches(branchData.filter((b) => b.isActive));
    };
    loadData();
  }, [isAuthenticated]); // eslint-disable-line react-hooks/exhaustive-deps

  const items = localCart?.items ?? [];
  const subtotal = localCart?.total ?? 0;
  const shippingFee = orderType === "DELIVERY" ? SHIPPING_FEE : 0;
  const total = subtotal + shippingFee;

  async function handleSubmit() {
    if (!branchId) {
      toast.error("Vui lòng chọn chi nhánh");
      return;
    }
    if (orderType === "DELIVERY") {
      if (!receiverName.trim()) {
        toast.error("Vui lòng nhập tên người nhận");
        return;
      }
      if (!receiverPhone.trim()) {
        toast.error("Vui lòng nhập số điện thoại");
        return;
      }
      if (!deliveryAddress.trim()) {
        toast.error("Vui lòng nhập địa chỉ giao hàng");
        return;
      }
    }
    if (items.length === 0) {
      toast.error("Giỏ hàng trống");
      return;
    }

    setSubmitting(true);
    try {
      const order = await createOrder({
        branchId,
        orderType,
        receiverName: orderType === "DELIVERY" ? receiverName : undefined,
        receiverPhone: orderType === "DELIVERY" ? receiverPhone : undefined,
        deliveryAddress: orderType === "DELIVERY" ? deliveryAddress : undefined,
        items: items.map((i) => ({
          productVariantId: i.variant.id,
          quantity: i.quantity,
        })),
        note: note.trim() || undefined,
      });
      clearCart();
      toast.success("Đặt hàng thành công!");
      router.push(`/orders/${order.id}`);
    } catch {
      toast.error("Đặt hàng thất bại. Vui lòng thử lại.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold">Thanh toán</h1>

      <div className="flex flex-col gap-8 lg:flex-row lg:gap-10">
        {/* Left: form */}
        <div className="flex-1 space-y-6">
          {/* Order type */}
          <section>
            <h2 className="mb-3 font-medium">Hình thức nhận hàng</h2>
            <div className="flex gap-3">
              <label className="flex cursor-pointer items-center gap-2 rounded-lg border px-4 py-3 has-[:checked]:border-primary has-[:checked]:bg-primary/5">
                <input
                  type="radio"
                  name="orderType"
                  value="PICKUP"
                  checked={orderType === "PICKUP"}
                  onChange={() => setOrderType("PICKUP")}
                  className="accent-primary"
                />
                <span className="text-sm font-medium">Nhận tại cửa hàng</span>
              </label>
              <label className="flex cursor-pointer items-center gap-2 rounded-lg border px-4 py-3 has-[:checked]:border-primary has-[:checked]:bg-primary/5">
                <input
                  type="radio"
                  name="orderType"
                  value="DELIVERY"
                  checked={orderType === "DELIVERY"}
                  onChange={() => setOrderType("DELIVERY")}
                  className="accent-primary"
                />
                <span className="text-sm font-medium">Giao hàng tận nơi</span>
              </label>
            </div>
          </section>

          {/* Branch selection */}
          <section>
            <Label className="mb-2 block">Chi nhánh</Label>
            <Select value={branchId} onValueChange={setBranchId}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="Chọn chi nhánh" />
              </SelectTrigger>
              <SelectContent>
                {branches.map((b) => (
                  <SelectItem key={b.id} value={b.id}>
                    {b.name} — {b.address}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </section>

          {/* Delivery info */}
          {orderType === "DELIVERY" && (
            <section className="space-y-3">
              <h2 className="font-medium">Thông tin giao hàng</h2>
              <div>
                <Label className="mb-1 block text-sm">Tên người nhận</Label>
                <Input
                  placeholder="Nguyễn Văn A"
                  value={receiverName}
                  onChange={(e) => setReceiverName(e.target.value)}
                />
              </div>
              <div>
                <Label className="mb-1 block text-sm">Số điện thoại</Label>
                <Input
                  placeholder="0901234567"
                  value={receiverPhone}
                  onChange={(e) => setReceiverPhone(e.target.value)}
                />
              </div>
              <div>
                <Label className="mb-1 block text-sm">Địa chỉ giao hàng</Label>
                <Input
                  placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
                  value={deliveryAddress}
                  onChange={(e) => setDeliveryAddress(e.target.value)}
                />
              </div>
            </section>
          )}

          {/* Note */}
          <section>
            <Label className="mb-2 block">Ghi chú (tùy chọn)</Label>
            <textarea
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              rows={3}
              placeholder="Ghi chú cho đơn hàng..."
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </section>

        </div>

        {/* Right: order summary */}
        <div className="w-full lg:w-80 lg:shrink-0">
          <div className="rounded-lg border p-4">
            <h2 className="mb-4 font-medium">Tóm tắt đơn hàng</h2>

            <div className="flex flex-col gap-3">
              {items.map((item) => (
                <div key={item.id} className="flex items-start gap-3">
                  {item.variant.primaryImageUrl ? (
                    <img
                      src={item.variant.primaryImageUrl}
                      alt={item.variant.productName}
                      className="h-12 w-12 shrink-0 rounded-md object-cover"
                    />
                  ) : (
                    <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-md bg-muted text-xs text-muted-foreground">
                      No img
                    </div>
                  )}
                  <div className="flex-1 text-sm">
                    <p className="font-medium leading-tight">{item.variant.productName}</p>
                    <p className="text-muted-foreground">{item.variant.color} · {item.variant.size}</p>
                    <p className="text-muted-foreground">x{item.quantity}</p>
                  </div>
                  <span className="shrink-0 text-sm font-medium">
                    {formatVND(item.variant.price * item.quantity)}
                  </span>
                </div>
              ))}
            </div>

            <Separator className="my-4" />

            <div className="space-y-2 text-sm">
              <div className="flex justify-between text-muted-foreground">
                <span>Tạm tính</span>
                <span>{formatVND(subtotal)}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Phí vận chuyển</span>
                <span>{shippingFee === 0 ? "Miễn phí" : formatVND(shippingFee)}</span>
              </div>
              <Separator />
              <div className="flex justify-between font-semibold">
                <span>Tổng cộng</span>
                <span>{formatVND(total)}</span>
              </div>
            </div>

            <Button
              className="mt-6 w-full"
              onClick={handleSubmit}
              disabled={submitting || items.length === 0}
            >
              {submitting ? "Đang đặt hàng..." : "Đặt hàng"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
