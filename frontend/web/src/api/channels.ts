import client from "./client";
import type { ApiResponse, Channel, PageResponse } from "@/types";

export const channelsApi = {
  list: async (): Promise<Channel[]> => {
    const res =
      await client.get<ApiResponse<PageResponse<Channel>>>("/channels");

    return res.data.data.content;
  },

  create: async (name: string, description?: string): Promise<Channel> => {
    const res = await client.post<ApiResponse<Channel>>("/channels", {
      name,
      description: description ?? null,
    });

    return res.data.data;
  },

  join: async (channelId: string): Promise<void> => {
    await client.post(`/channels/${channelId}/members`);
  },
};
