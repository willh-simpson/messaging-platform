import client from "./client";
import type { ApiResponse, AuthResponse } from "@/types";

export const authApi = {
  login: async (username: string, password: string): Promise<AuthResponse> => {
    const res = await client.post<ApiResponse<AuthResponse>>("/auth/login", {
      username,
      password,
    });

    return res.data.data;
  },

  register: async (
    username: string,
    email: string,
    password: string,
  ): Promise<AuthResponse> => {
    const res = await client.post<ApiResponse<AuthResponse>>("/auth/register", {
      username,
      email,
      password,
    });

    return res.data.data;
  },
};
