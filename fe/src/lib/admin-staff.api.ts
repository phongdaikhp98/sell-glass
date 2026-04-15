import api from "./api";
import type { ApiResponse, PageResponse, Branch } from "@/types";

export interface UserResponse {
  id: string;
  branchId: string | null;
  fullName: string;
  email: string;
  role: "SUPER_ADMIN" | "BRANCH_MANAGER" | "STAFF";
  isActive: boolean;
  createdAt: string;
}

export interface StaffFormData {
  fullName: string;
  email: string;
  password: string;
  role: "SUPER_ADMIN" | "BRANCH_MANAGER" | "STAFF";
  branchId?: string;
}

export async function getStaff(
  page: number,
  size: number
): Promise<PageResponse<UserResponse>> {
  const res = await api.get<ApiResponse<PageResponse<UserResponse>>>(
    "/v1/admin/users",
    { params: { page, size } }
  );
  return res.data.data;
}

export async function createStaff(data: StaffFormData): Promise<void> {
  await api.post("/v1/admin/users", data);
}

export async function updateStaff(
  id: string,
  data: StaffFormData
): Promise<void> {
  await api.put(`/v1/admin/users/${id}`, data);
}

export async function deactivateStaff(id: string): Promise<void> {
  await api.patch(`/v1/admin/users/${id}/deactivate`);
}

export async function getBranches(): Promise<Branch[]> {
  const res = await api.get<ApiResponse<Branch[]>>("/v1/branches");
  return res.data.data;
}
