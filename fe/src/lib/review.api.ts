import api from "./api";
import type { ApiResponse, PageResponse, Review } from "@/types";

export async function getProductReviews(
  productId: string,
  page: number,
  size: number
): Promise<PageResponse<Review>> {
  const res = await api.get<ApiResponse<PageResponse<Review>>>(
    `/v1/products/${productId}/reviews`,
    { params: { page, size } }
  );
  return res.data.data;
}

export async function createReview(
  productId: string,
  data: { rating: number; comment?: string }
): Promise<Review> {
  const res = await api.post<ApiResponse<Review>>(
    `/v1/products/${productId}/reviews`,
    data
  );
  return res.data.data;
}
