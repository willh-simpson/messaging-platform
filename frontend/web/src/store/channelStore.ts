import { create } from "zustand";
import { channelsApi } from "@/api/channels";
import type { Channel } from "@/types";

interface ChannelState {
  channels: Channel[];
  activeChannelId: string | null;
  loading: boolean;

  fetchChannels: () => Promise<void>;
  setActiveChannel: (channelId: string) => void;
  addChannel: (channel: Channel) => void;
}

export const useChannelStore = create<ChannelState>((set) => ({
  channels: [],
  activeChannelId: null,
  loading: false,

  fetchChannels: async () => {
    set({ loading: true });

    try {
      const channels = await channelsApi.list();
      set({ channels, loading: false });
    } catch {
      set({ loading: false });
    }
  },

  setActiveChannel: (channelId: string) => set({ activeChannelId: channelId }),

  addChannel: (channel: Channel) =>
    set((state) => ({ channels: [...state.channels, channel] })),
}));
