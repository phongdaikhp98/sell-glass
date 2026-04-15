import api from "./api";
import type {
  ProductListItem,
  Product,
  Category,
  Brand,
  ApiResponse,
  PageResponse,
} from "@/types";

export interface ProductFilters {
  search?: string;
  categoryId?: string;
  brandId?: string;
  gender?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export async function getProducts(
  filters: ProductFilters = {}
): Promise<PageResponse<ProductListItem>> {
  const params: Record<string, string | number> = {};
  if (filters.search) params.search = filters.search;
  if (filters.categoryId) params.categoryId = filters.categoryId;
  if (filters.brandId) params.brandId = filters.brandId;
  if (filters.gender) params.gender = filters.gender;
  if (filters.page !== undefined) params.page = filters.page;
  if (filters.size !== undefined) params.size = filters.size;
  if (filters.sort) params.sort = filters.sort;

  const res = await api.get<ApiResponse<PageResponse<ProductListItem>>>(
    "/v1/products",
    { params }
  );
  return res.data.data;
}

export async function getProductBySlug(slug: string): Promise<Product> {
  const res = await api.get<ApiResponse<Product>>(`/v1/products/${slug}`);
  return res.data.data;
}

export async function getCategories(): Promise<Category[]> {
  const res = await api.get<ApiResponse<Category[]>>("/v1/categories");
  return res.data.data;
}

export async function getBrands(): Promise<Brand[]> {
  const res = await api.get<ApiResponse<Brand[]>>("/v1/brands");
  return res.data.data;
}
