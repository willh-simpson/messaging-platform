import { useEffect } from "react";
import { useAuthStore } from "@/store/authStore";
import { useChannelStore } from "@/store/channelStore";
import { useMessageStore } from "@/store/messageStore";
import { useWebSocket } from "@/hooks/useWebSocket";
import Sidebar from "@/components/Sidebar";
import MessageList from "@/components/MessageList";
import MessageInput from "@/components/MessageInput";
import ChannelHeader from "@/components/ChannelHeader";
import styles from "./ChatPage.module.css";

export default function ChatPage() {
  const { token } = useAuthStore();
  const { activeChannelId, fetchChannels } = useChannelStore();
  const { fetchHistory } = useMessageStore();

  useWebSocket(token, activeChannelId);

  useEffect(() => {
    fetchChannels();
  }, [fetchChannels]);

  useEffect(() => {
    if (activeChannelId) {
      fetchHistory(activeChannelId);
    }
  }, [activeChannelId, fetchHistory]);

  return (
    <div className={styles.root}>
      <Sidebar />

      <div className={styles.main}>
        {activeChannelId ? (
          <>
            <ChannelHeader />
            <MessageList />
            <MessageInput />
          </>
        ) : (
          <div className={styles.empty}>
            <span className={styles.emptyIcon}>⬡</span>
            <p className={styles.emptyText}>
              Select a channel to start messaging
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
