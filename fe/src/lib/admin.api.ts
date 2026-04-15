import api from "@/lib/api";
import type { ApiResponse, PageResponse } from "@/types";

export interface OrderItem {
  id: string;
  productName: string;
  variantSku: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderResponse {
  id: string;
  customerId: string;
  branchId: string;
  branchName: string;
  orderType: "PICKUP" | "DELIVERY";
  status: string;
  paymentStatus: string;
  receiverName: string;
  receiverPhone: string;
  deliveryAddress?: string;
  subtotal: number;
  shippingFee: number;
  total: number;
  note?: string;
  cancelledReason?: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CustomerResponse {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  createdAt: string;
}

export async function getAdminOrders(page: number, size: number): Promise<PageResponse<OrderResponse>> {
  const res = await api.get<ApiResponse<PageResponse<OrderResponse>>>("/v1/admin/orders", {
    params: { page, size },
  });
  return res.data.data;
}

export async function getAdminOrder(id: string): Promise<OrderResponse> {
  const res = await api.get<ApiResponse<OrderResponse>>(`/v1/admin/orders/${id}`);
  return res.data.data;
}

export async function updateOrderStatus(
  id: string,
  status: string,
  cancelledReason?: string
): Promise<OrderResponse> {
  const body: { status: string; cancelledReason?: string } = { status };
  if (cancelledReason) body.cancelledReason = cancelledReason;
  const res = await api.patch<ApiResponse<OrderResponse>>(`/v1/admin/orders/${id}/status`, body);
  return res.data.data;
}

export async function getCustomers(page: number, size: number): Promise<PageResponse<CustomerResponse>> {
  const res = await api.get<ApiResponse<PageResponse<CustomerResponse>>>("/v1/customers", {
    params: { page, size },
  });
  return res.data.data;
}

export async function getCustomer(id: string): Promise<CustomerResponse> {
  const res = await api.get<ApiResponse<CustomerResponse>>(`/v1/customers/${id}`);
  return res.data.data;
}
