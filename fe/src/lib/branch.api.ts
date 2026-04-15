import api from "./api";
import type { ApiResponse, Branch } from "@/types";

export async function getBranches(): Promise<Branch[]> {
  const res = await api.get<ApiResponse<Branch[]>>("/v1/branches");
  return res.data.data;
}
