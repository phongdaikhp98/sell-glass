import api from "@/lib/api";
import { ApiResponse, Customer, TokenResponse } from "@/types";

export interface RegisterData {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
}

export async function loginCustomer(
  email: string,
  password: string
): Promise<TokenResponse> {
  const res = await api.post<ApiResponse<TokenResponse>>("/v1/auth/login", {
    email,
    password,
  });
  return res.data.data;
}

export async function registerCustomer(
  data: RegisterData
): Promise<Customer> {
  const res = await api.post<ApiResponse<Customer>>("/v1/auth/register", data);
  return res.data.data;
}

export async function loginStaff(
  email: string,
  password: string
): Promise<TokenResponse> {
  const res = await api.post<ApiResponse<TokenResponse>>(
    "/v1/auth/staff/login",
    { email, password }
  );
  return res.data.data;
}

export async function forgotPassword(email: string): Promise<void> {
  await api.post("/v1/auth/forgot-password", { email });
}

export async function resetPassword(
  token: string,
  newPassword: string
): Promise<void> {
  await api.post("/v1/auth/reset-password", { token, newPassword });
}
