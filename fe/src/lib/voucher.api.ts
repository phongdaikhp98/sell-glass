import api from "./api";
import type { ApiResponse, PageResponse, Voucher } from "@/types";

export interface ApplyVoucherResult {
  discountAmount: number;
  description: string;
}

export interface VoucherFormData {
  code: string;
  type: "PERCENTAGE" | "FIXED_AMOUNT";
  value: number;
  maxDiscountAmount?: number | null;
  minOrderAmount: number;
  usageLimit?: number | null;
  expiresAt?: string | null;
  isActive: boolean;
}

export async function applyVoucher(
  code: string,
  orderTotal: number
): Promise<ApplyVoucherResult> {
  const res = await api.post<ApiResponse<ApplyVoucherResult>>("/v1/vouchers/apply", {
    code,
    orderTotal,
  });
  return res.data.data;
}

export async function getAdminVouchers(
  page: number,
  size: number,
  search?: string
): Promise<PageResponse<Voucher>> {
  const params: Record<string, string | number> = { page, size };
  if (search) params.search = search;
  const res = await api.get<ApiResponse<PageResponse<Voucher>>>("/v1/admin/vouchers", { params });
  return res.data.data;
}

export async function createVoucher(data: VoucherFormData): Promise<Voucher> {
  const res = await api.post<ApiResponse<Voucher>>("/v1/admin/vouchers", data);
  return res.data.data;
}

export async function updateVoucher(id: string, data: VoucherFormData): Promise<Voucher> {
  const res = await api.put<ApiResponse<Voucher>>(`/v1/admin/vouchers/${id}`, data);
  return res.data.data;
}

export async function deleteVoucher(id: string): Promise<void> {
  await api.delete(`/v1/admin/vouchers/${id}`);
}
