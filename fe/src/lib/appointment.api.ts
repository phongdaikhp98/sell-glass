import api from "@/lib/api";
import type { ApiResponse, PageResponse } from "@/types";

export interface AppointmentResponse {
  id: string;
  customerId: string;
  branchId: string;
  branchName: string;
  staffId?: string;
  scheduledAt: string;
  status: "PENDING" | "CONFIRMED" | "DONE" | "CANCELLED";
  note?: string;
  resultNote?: string;
  createdAt: string;
}

export async function getMyAppointments(
  page: number,
  size: number
): Promise<PageResponse<AppointmentResponse>> {
  const res = await api.get<ApiResponse<PageResponse<AppointmentResponse>>>(
    "/v1/appointments",
    { params: { page, size } }
  );
  return res.data.data;
}

export async function createAppointment(data: {
  branchId: string;
  scheduledAt: string;
  note?: string;
}): Promise<AppointmentResponse> {
  const res = await api.post<ApiResponse<AppointmentResponse>>(
    "/v1/appointments",
    data
  );
  return res.data.data;
}

export async function cancelAppointment(id: string): Promise<void> {
  await api.patch(`/v1/appointments/${id}/cancel`);
}

// Admin
export async function getAdminAppointments(
  page: number,
  size: number
): Promise<PageResponse<AppointmentResponse>> {
  const res = await api.get<ApiResponse<PageResponse<AppointmentResponse>>>(
    "/v1/admin/appointments",
    { params: { page, size } }
  );
  return res.data.data;
}

export async function updateAppointmentStatus(
  id: string,
  status: string,
  resultNote?: string
): Promise<AppointmentResponse> {
  const res = await api.patch<ApiResponse<AppointmentResponse>>(
    `/v1/admin/appointments/${id}/status`,
    null,
    { params: { status, ...(resultNote ? { resultNote } : {}) } }
  );
  return res.data.data;
}
