import api from "./api";
import type { ApiResponse } from "@/types";

export interface ProductImage {
  id: string;
  productId: string;
  url: string;
  sortOrder: number;
  isPrimary: boolean;
}

export async function getProductImages(productId: string): Promise<ProductImage[]> {
  const res = await api.get<ApiResponse<ProductImage[]>>(
    `/v1/admin/products/${productId}/images`
  );
  return res.data.data;
}

export async function uploadProductImage(
  productId: string,
  file: File
): Promise<ProductImage> {
  const formData = new FormData();
  formData.append("file", file);
  const res = await api.post<ApiResponse<ProductImage>>(
    `/v1/admin/products/${productId}/images`,
    formData,
    { headers: { "Content-Type": "multipart/form-data" } }
  );
  return res.data.data;
}

export async function deleteProductImage(
  productId: string,
  imageId: string
): Promise<void> {
  await api.delete(`/v1/admin/products/${productId}/images/${imageId}`);
}

export async function setPrimaryImage(
  productId: string,
  imageId: string
): Promise<ProductImage> {
  const res = await api.patch<ApiResponse<ProductImage>>(
    `/v1/admin/products/${productId}/images/${imageId}/primary`
  );
  return res.data.data;
}

export async function reorderImages(
  productId: string,
  items: { id: string; sortOrder: number }[]
): Promise<ProductImage[]> {
  const res = await api.put<ApiResponse<ProductImage[]>>(
    `/v1/admin/products/${productId}/images/reorder`,
    items
  );
  return res.data.data;
}
