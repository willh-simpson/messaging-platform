import { create } from "zustand";
import { messagesApi } from "@/api/messages";
import type { Message } from "@/types";

interface MessageState {
  messagesByChannel: Record<string, Message[]>;
  loading: boolean;

  fetchHistory: (channelId: string) => Promise<void>;
  addMessage: (message: Message) => void;
  clearChannel: (channelId: string) => void;
}

export const useMessageStore = create<MessageState>((set, get) => ({
  messagesByChannel: {},
  loading: false,

  fetchHistory: async (channelId: string) => {
    set({ loading: true });

    try {
      const messages = await messagesApi.history(channelId);
      set((state: MessageState) => ({
        messagesByChannel: {
          ...state.messagesByChannel,
          [channelId]: messages,
        },
        loading: false,
      }));
    } catch {
      set({ loading: false });
    }
  },

  addMessage: (message: Message) => {
    const { messagesByChannel } = get();
    const existingMessages = messagesByChannel[message.channel_id] ?? [];

    /*
     * ignores messages if message_id already exists in the list.
     * this can happen if REST history load and websocket both deliver the same message,
     * so enforcing de-duplication is important at this step.
     */
    if (
      existingMessages.some((m: Message) => m.message_id === message.message_id)
    )
      return;

    set((state: MessageState) => ({
      messagesByChannel: {
        ...state.messagesByChannel,
        [message.message_id]: [...existingMessages, message],
      },
    }));
  },

  clearChannel: (channelId: string) =>
    set((state: MessageState) => {
      const next = { ...state.messagesByChannel };
      delete next[channelId];

      return { messagesByChannel: next };
    }),
}));
