import api from "./api";
import type { ApiResponse, Branch } from "@/types";

// ─── Helpers ───────────────────────────────────────────────────────────────

export function toSlug(str: string): string {
  return str
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d")
    .replace(/[^a-z0-9\s-]/g, "")
    .trim()
    .replace(/\s+/g, "-");
}

// ─── Category ──────────────────────────────────────────────────────────────

export interface CategoryResponse {
  id: string;
  name: string;
  slug: string;
}

export interface CategoryRequest {
  name: string;
  slug: string;
}

export async function getCategories(): Promise<CategoryResponse[]> {
  const res = await api.get<ApiResponse<CategoryResponse[]>>("/v1/categories");
  return res.data.data;
}

export async function createCategory(data: CategoryRequest): Promise<CategoryResponse> {
  const res = await api.post<ApiResponse<CategoryResponse>>("/v1/categories", data);
  return res.data.data;
}

export async function updateCategory(id: string, data: CategoryRequest): Promise<CategoryResponse> {
  const res = await api.put<ApiResponse<CategoryResponse>>(`/v1/categories/${id}`, data);
  return res.data.data;
}

export async function deleteCategory(id: string): Promise<void> {
  await api.delete(`/v1/categories/${id}`);
}

// ─── Brand ─────────────────────────────────────────────────────────────────

export interface BrandResponse {
  id: string;
  name: string;
  slug: string;
  logoUrl?: string;
}

export interface BrandRequest {
  name: string;
  slug: string;
  logoUrl?: string;
}

export async function getBrands(): Promise<BrandResponse[]> {
  const res = await api.get<ApiResponse<BrandResponse[]>>("/v1/brands");
  return res.data.data;
}

export async function createBrand(data: BrandRequest): Promise<BrandResponse> {
  const res = await api.post<ApiResponse<BrandResponse>>("/v1/brands", data);
  return res.data.data;
}

export async function updateBrand(id: string, data: BrandRequest): Promise<BrandResponse> {
  const res = await api.put<ApiResponse<BrandResponse>>(`/v1/brands/${id}`, data);
  return res.data.data;
}

export async function deleteBrand(id: string): Promise<void> {
  await api.delete(`/v1/brands/${id}`);
}

// ─── Branch ────────────────────────────────────────────────────────────────

export interface BranchRequest {
  name: string;
  address: string;
  phone: string;
  openTime: string;
  closeTime: string;
  isActive?: boolean;
}

export async function getBranches(): Promise<Branch[]> {
  const res = await api.get<ApiResponse<Branch[]>>("/v1/branches");
  return res.data.data;
}

export async function createBranch(data: BranchRequest): Promise<Branch> {
  const res = await api.post<ApiResponse<Branch>>("/v1/branches", data);
  return res.data.data;
}

export async function updateBranch(id: string, data: BranchRequest): Promise<Branch> {
  const res = await api.put<ApiResponse<Branch>>(`/v1/branches/${id}`, data);
  return res.data.data;
}

export async function deleteBranch(id: string): Promise<void> {
  await api.delete(`/v1/branches/${id}`);
}
