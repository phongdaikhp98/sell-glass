import api from "./api";
import type { ApiResponse, PageResponse, Category, Brand } from "@/types";

export interface AdminProductListItem {
  id: string;
  name: string;
  slug: string;
  category: Category;
  brand: Brand;
  primaryImageUrl?: string;
  minPrice: number;
  gender: "MEN" | "WOMEN" | "UNISEX";
  isActive: boolean;
  createdAt: string;
}

export interface ProductFormData {
  name: string;
  slug: string;
  categoryId: string;
  brandId: string;
  description?: string;
  frameShape?: string;
  material?: string;
  gender: "MEN" | "WOMEN" | "UNISEX";
  isActive: boolean;
}

export async function getAdminProducts(
  page: number,
  size: number,
  search?: string
): Promise<PageResponse<AdminProductListItem>> {
  const params: Record<string, string | number> = { page, size };
  if (search) params.search = search;
  const res = await api.get<ApiResponse<PageResponse<AdminProductListItem>>>(
    "/v1/products",
    { params }
  );
  return res.data.data;
}

export async function createProduct(data: ProductFormData): Promise<void> {
  await api.post("/v1/products", data);
}

export async function updateProduct(
  id: string,
  data: ProductFormData
): Promise<void> {
  await api.put(`/v1/products/${id}`, data);
}

export async function deleteProduct(id: string): Promise<void> {
  await api.delete(`/v1/products/${id}`);
}
