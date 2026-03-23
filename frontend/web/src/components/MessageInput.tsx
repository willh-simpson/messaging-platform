import { useState, type KeyboardEvent } from "react";
import { useChannelStore } from "@/store/channelStore";
import { messagesApi } from "@/api/messages";
import styles from "./MessageInput.module.css";

export default function MessageInput() {
  const { activeChannelId, channels } = useChannelStore();
  const [content, setContent] = useState("");
  const [sending, setSending] = useState(false);

  const channel = channels.find((c) => c.channel_id === activeChannelId);

  const send = async () => {
    const trimmedContent = content.trim();
    if (!trimmedContent || !activeChannelId || sending) return;

    setSending(true);

    try {
      await messagesApi.publish(activeChannelId, trimmedContent);
      setContent("");

      // message will arrive via WebSocket, so manually adding it to the store will create duplicates.
    } catch (err) {
      console.error("Failed to send message", err);
    } finally {
      setSending(false);
    }
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    /*
     * Enter = send
     * Shift + Enter = new line
     */
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();

      send();
    }
  };

  // textarea needs to be resized as content grows
  const handleContentChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setContent(e.target.value);

    e.target.style.height = "auto";
    e.target.style.height = `${Math.min(e.target.scrollHeight, 160)}px`;
  };

  if (!activeChannelId) return null;

  return (
    <div className={styles.wrapper}>
      <div className={styles.inputRow}>
        <textarea
          className={styles.textarea}
          placeholder={`Message #${channel?.name ?? "..."}`}
          value={content}
          onChange={handleContentChange}
          onKeyDown={handleKeyDown}
          rows={1}
          disabled={sending}
        />
        <button
          className={styles.sendBtn}
          onClick={send}
          disabled={!content.trim() || sending}
          aria-label="Send message"
        >
          {sending ? (
            <span className={styles.spinner} />
          ) : (
            <svg
              width="16"
              height="16"
              viewBox="0 0 16 16"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M14 8L2 2L5 8L2 14L14 8Z"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinejoin="round"
              />
            </svg>
          )}
        </button>
      </div>

      <p className={styles.hint}>
        <kbd>Enter</kbd> to send &nbsp;·&nbsp; <kbd>Shift + Enter</kbd> for new
        line
      </p>
    </div>
  );
}
