import { useEffect, useRef } from "react";
import { useChannelStore } from "@/store/channelStore";
import { useMessageStore } from "@/store/messageStore";
import { useAuthStore } from "@/store/authStore";
import styles from "./MessageList.module.css";

function formatDateAndTime(iso: string): { date: string; time: string } {
  const date = new Date(iso);
  const today = new Date();
  const yesterday = new Date(today.getDate() - 1);

  const time = date.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });

  if (date.toDateString() === today.toDateString())
    return { date: "Today", time: time };
  if (date.toDateString() === yesterday.toDateString())
    return { date: "Yesterday", time: time };

  return {
    date: date.toLocaleDateString([], {
      weekday: "long",
      month: "long",
      day: "numeric",
    }),
    time: time,
  };
}

export default function MessageList() {
  const { activeChannelId } = useChannelStore();
  const { messagesByChannel, loading } = useMessageStore();
  const { userId } = useAuthStore();
  const bottomRef = useRef<HTMLDivElement>(null);

  const messages = activeChannelId
    ? (messagesByChannel[activeChannelId] ?? [])
    : [];

  // auto-scrolls to bottom when new message is sent
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smoooth" });
  }, [messages.length]);

  if (loading) {
    return (
      <div className={styles.loading}>
        <span className={styles.loadingDot} />
        <span className={styles.loadingDot} />
        <span className={styles.loadingDot} />
      </div>
    );
  }

  // groups messages by date
  let lastDate = "";

  return (
    <div className={styles.list}>
      {messages.length === 0 && (
        <div className={styles.empty}>
          <span className={styles.emptyIcon}>#</span>
          <p>No messages yet. Be the first to say something.</p>
        </div>
      )}

      {messages.map((message) => {
        const isSelf = message.author_id === userId;
        const { date, time } = formatDateAndTime(message.created_at);

        const showDateSeparator = date !== lastDate;
        lastDate = date;

        // groups consecutive messages from the same author
        const msgIndex = messages.indexOf(message);
        const prev = messages[msgIndex - 1];
        const isGrouped =
          prev &&
          prev.author_id === message.author_id &&
          formatDateAndTime(prev.created_at).date === date &&
          // group should be broken if messages are older than 5 minutes
          new Date(message.created_at).getTime() -
            new Date(prev.created_at).getTime() <
            5 * 60 * 1000;

        return (
          <div key={message.message_id}>
            {showDateSeparator && (
              <div className={styles.dateSeparator}>
                <span className={styles.dateLabel}>{date}</span>
              </div>
            )}

            <div
              className={`${styles.message} ${isSelf ? styles.self : ""} ${isGrouped ? styles.grouped : ""}`}
              styles={{ animationDelay: `${msgIndex * 0.02}s` }}
            >
              {!isGrouped && (
                <div className={styles.meta}>
                  <span className={styles.author}>
                    {message.author_username}
                  </span>
                  <span className={styles.time}>{time}</span>
                </div>
              )}
              <div className={styles.bubble}>
                <p className={styles.content}>{messages.content}</p>
              </div>
            </div>
          </div>
        );
      })}

      <div ref={bottomRef} />
    </div>
  );
}
