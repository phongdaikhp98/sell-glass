import api from "./api";
import type { ApiResponse, Order, PageResponse } from "@/types";

export interface PrescriptionData {
  odSph?: number | null;
  odCyl?: number | null;
  odAxis?: number | null;
  osSph?: number | null;
  osCyl?: number | null;
  osAxis?: number | null;
  pd?: number | null;
  note?: string | null;
}

export interface CreateOrderData {
  branchId: string;
  orderType: "PICKUP" | "DELIVERY";
  receiverName?: string;
  receiverPhone?: string;
  deliveryAddress?: string;
  items: { productVariantId: string; quantity: number }[];
  note?: string;
  voucherCode?: string;
  prescription?: PrescriptionData | null;
}

export async function createOrder(data: CreateOrderData): Promise<Order> {
  const res = await api.post<ApiResponse<Order>>("/v1/orders", data);
  return res.data.data;
}

export async function getMyOrders(
  page: number,
  size: number
): Promise<PageResponse<Order>> {
  const res = await api.get<ApiResponse<PageResponse<Order>>>("/v1/orders", {
    params: { page, size },
  });
  return res.data.data;
}

export async function getMyOrder(id: string): Promise<Order> {
  const res = await api.get<ApiResponse<Order>>(`/v1/orders/${id}`);
  return res.data.data;
}
