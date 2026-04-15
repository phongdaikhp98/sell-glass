import api from "./api";
import type { ApiResponse, Cart } from "@/types";

interface CartApiResponse {
  cartId: string;
  items: {
    itemId: string;
    productVariantId: string;
    productName: string;
    primaryImageUrl?: string;
    sku: string;
    color: string;
    size: string;
    price: number;
    quantity: number;
    subtotal: number;
  }[];
  total: number;
}

function mapToCart(raw: CartApiResponse): Cart {
  return {
    id: raw.cartId,
    items: raw.items.map((item) => ({
      id: item.itemId,
      variant: {
        id: item.productVariantId,
        sku: item.sku,
        color: item.color,
        size: item.size,
        price: item.price,
        isActive: true,
        productName: item.productName,
        primaryImageUrl: item.primaryImageUrl,
      },
      quantity: item.quantity,
    })),
    total: raw.total,
  };
}

export async function getCart(): Promise<Cart> {
  const res = await api.get<ApiResponse<CartApiResponse>>("/v1/cart");
  return mapToCart(res.data.data);
}

export async function addToCart(
  productVariantId: string,
  quantity: number
): Promise<Cart> {
  const res = await api.post<ApiResponse<CartApiResponse>>("/v1/cart/items", {
    productVariantId,
    quantity,
  });
  return mapToCart(res.data.data);
}

export async function updateCartItem(
  itemId: string,
  quantity: number
): Promise<Cart> {
  const res = await api.put<ApiResponse<CartApiResponse>>(
    `/v1/cart/items/${itemId}`,
    { quantity }
  );
  return mapToCart(res.data.data);
}

export async function removeCartItem(itemId: string): Promise<void> {
  await api.delete(`/v1/cart/items/${itemId}`);
}

export async function clearCart(): Promise<void> {
  await api.delete("/v1/cart");
}
