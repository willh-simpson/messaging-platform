import { create } from "zustand";
import { authApi } from "@/api/auth";

interface AuthState {
  token: string | null;
  userId: string | null;
  username: string | null;
  isAuthenticated: boolean;

  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  hydrate: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  userId: null,
  username: null,
  isAuthenticated: false,

  hydrate: () => {
    const token = localStorage.getItem("token");
    const userId = localStorage.getItem("userId");
    const username = localStorage.getItem("username");

    if (token && userId && username) {
      set({ token, userId, username, isAuthenticated: true });
    }
  },

  login: async (username: string, password: string) => {
    const auth = await authApi.login(username, password);

    localStorage.setItem("token", auth.token);
    localStorage.setItem("userId", auth.user_id);
    localStorage.setItem("username", auth.username);

    set({
      token: auth.token,
      userId: auth.user_id,
      username: auth.user_id,
      isAuthenticated: true,
    });
  },

  register: async (username: string, email: string, password: string) => {
    const auth = await authApi.register(username, email, password);

    localStorage.setItem("token", auth.token);
    localStorage.setItem("userId", auth.user_id);
    localStorage.setItem("username", auth.username);

    set({
      token: auth.token,
      userId: auth.user_id,
      username: auth.username,
      isAuthenticated: true,
    });
  },

  logout: () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("username");

    set({
      token: null,
      userId: null,
      username: null,
      isAuthenticated: false,
    });
  },
}));
