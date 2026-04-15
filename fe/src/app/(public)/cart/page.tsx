"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Trash2Icon, MinusIcon, PlusIcon, ShoppingCartIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthStore } from "@/store/auth.store";
import { useCartStore } from "@/store/cart.store";
import { getCart, updateCartItem, removeCartItem, clearCart as clearCartApi } from "@/lib/cart.api";

const formatVND = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(price);

export default function CartPage() {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)();
  const { cart, setCart, clearCart } = useCartStore();
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }
    getCart()
      .then(setCart)
      .catch(() => toast.error("Không thể tải giỏ hàng"))
      .finally(() => setLoading(false));
  }, [isAuthenticated]); // eslint-disable-line react-hooks/exhaustive-deps

  async function handleUpdateQuantity(itemId: string, quantity: number) {
    if (quantity < 1) return;
    setUpdatingId(itemId);
    try {
      const updated = await updateCartItem(itemId, quantity);
      setCart(updated);
    } catch {
      toast.error("Không thể cập nhật số lượng");
    } finally {
      setUpdatingId(null);
    }
  }

  async function handleRemoveItem(itemId: string) {
    setUpdatingId(itemId);
    try {
      await removeCartItem(itemId);
      const updated = await getCart();
      setCart(updated);
    } catch {
      toast.error("Không thể xóa sản phẩm");
    } finally {
      setUpdatingId(null);
    }
  }

  async function handleClearCart() {
    try {
      await clearCartApi();
      clearCart();
      toast.success("Đã xóa giỏ hàng");
    } catch {
      toast.error("Không thể xóa giỏ hàng");
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-4xl px-4 py-8">
        <h1 className="mb-6 text-xl font-semibold">Giỏ hàng</h1>
        <div className="flex flex-col gap-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="flex gap-4 rounded-lg border p-4">
              <Skeleton className="h-20 w-20 shrink-0 rounded-md" />
              <div className="flex flex-1 flex-col gap-2">
                <Skeleton className="h-4 w-2/3" />
                <Skeleton className="h-3 w-1/3" />
                <Skeleton className="h-4 w-1/4" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  const items = cart?.items ?? [];

  if (items.length === 0) {
    return (
      <div className="mx-auto flex max-w-4xl flex-col items-center justify-center px-4 py-24 text-muted-foreground">
        <ShoppingCartIcon className="mb-4 size-16 opacity-30" />
        <p className="text-base font-medium">Giỏ hàng của bạn đang trống</p>
        <Button
          variant="outline"
          className="mt-6"
          onClick={() => router.push("/products")}
        >
          Tiếp tục mua sắm
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold">Giỏ hàng</h1>
        <Button variant="ghost" size="sm" className="text-destructive hover:text-destructive" onClick={handleClearCart}>
          <Trash2Icon className="mr-1 size-4" />
          Xóa giỏ hàng
        </Button>
      </div>

      <div className="flex flex-col gap-3">
        {items.map((item) => {
          const isUpdating = updatingId === item.id;
          return (
            <div key={item.id} className="flex gap-4 rounded-lg border p-4">
              {item.variant.primaryImageUrl ? (
                <img
                  src={item.variant.primaryImageUrl}
                  alt={item.variant.productName}
                  className="h-20 w-20 shrink-0 rounded-md object-cover"
                />
              ) : (
                <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-md bg-muted text-xs text-muted-foreground">
                  No image
                </div>
              )}

              <div className="flex flex-1 flex-col justify-between">
                <div>
                  <p className="font-medium">{item.variant.productName}</p>
                  <p className="text-sm text-muted-foreground">
                    SKU: {item.variant.sku} · {item.variant.color} · {item.variant.size}
                  </p>
                  <p className="mt-1 text-sm font-semibold">
                    {formatVND(item.variant.price)}
                  </p>
                </div>

                <div className="mt-2 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-7 w-7"
                      disabled={isUpdating || item.quantity <= 1}
                      onClick={() => handleUpdateQuantity(item.id, item.quantity - 1)}
                    >
                      <MinusIcon className="size-3" />
                    </Button>
                    <span className="w-6 text-center text-sm font-medium">
                      {item.quantity}
                    </span>
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-7 w-7"
                      disabled={isUpdating}
                      onClick={() => handleUpdateQuantity(item.id, item.quantity + 1)}
                    >
                      <PlusIcon className="size-3" />
                    </Button>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className="text-sm font-semibold">
                      {formatVND(item.variant.price * item.quantity)}
                    </span>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7 text-muted-foreground hover:text-destructive"
                      disabled={isUpdating}
                      onClick={() => handleRemoveItem(item.id)}
                    >
                      <Trash2Icon className="size-4" />
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <Separator className="my-6" />

      <div className="flex flex-col items-end gap-4">
        <div className="flex w-full max-w-xs flex-col gap-1">
          <div className="flex justify-between text-sm text-muted-foreground">
            <span>{items.reduce((s, i) => s + i.quantity, 0)} sản phẩm</span>
          </div>
          <div className="flex justify-between font-semibold">
            <span>Tổng giỏ hàng</span>
            <span>{formatVND(cart?.total ?? 0)}</span>
          </div>
        </div>

        <Button className="w-full max-w-xs" onClick={() => router.push("/checkout")}>
          Thanh toán
        </Button>
      </div>
    </div>
  );
}
