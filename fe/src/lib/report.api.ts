import api from "@/lib/api";
import type { ApiResponse } from "@/types";

export interface SummaryReport {
  totalOrders: number;
  totalRevenue: number;
  totalCustomers: number;
  totalProducts: number;
}

export async function getSummary(): Promise<SummaryReport> {
  const res = await api.get<ApiResponse<SummaryReport>>(
    "/v1/admin/reports/summary"
  );
  return res.data.data;
}

export async function getRevenue(
  period: string
): Promise<{ date: string; revenue: number }[]> {
  const res = await api.get<ApiResponse<{ date: string; revenue: number }[]>>(
    "/v1/admin/reports/revenue",
    { params: { period } }
  );
  return res.data.data;
}

export async function getTopProducts(
  limit: number
): Promise<{ productName: string; totalQuantity: number; totalRevenue: number }[]> {
  const res = await api.get<
    ApiResponse<{ productName: string; totalQuantity: number; totalRevenue: number }[]>
  >("/v1/admin/reports/top-products", { params: { limit } });
  return res.data.data;
}

export async function getOrdersByStatus(): Promise<
  { status: string; count: number }[]
> {
  const res = await api.get<ApiResponse<{ status: string; count: number }[]>>(
    "/v1/admin/reports/orders-by-status"
  );
  return res.data.data;
}
