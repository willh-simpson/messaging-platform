import client from "./client";
import type {
  ApiResponse,
  Message,
  MessageAccepted,
  PageResponse,
} from "@/types";

export const messagesApi = {
  history: async (
    channelId: string,
    page = 0,
    size = 50,
  ): Promise<Message[]> => {
    const res = await client.get<ApiResponse<PageResponse<Message>>>(
      `/channels/${channelId}/messages`,
      { params: { page, size } },
    );

    // history is received newest first. reversing for display
    return res.data.data.content.reverse();
  },

  publish: async (
    channelId: string,
    content: string,
  ): Promise<MessageAccepted> => {
    const rest = await client.post<ApiResponse<MessageAccepted>>("/messages", {
      channel_id: channelId,
      content,
    });

    return rest.data.data;
  },
};
