import { useChannelStore } from "@/store/channelStore";
import styles from "./ChannelHeader.module.css";

export default function ChannelHeader() {
  const { channels, activeChannelId } = useChannelStore();

  const channel = channels.find((c) => c.channel_id === activeChannelId);
  if (!channel) return null;

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <span className={styles.hash}>#</span>
        <span className={styles.name}>{channel.name}</span>
        {channel.description && (
          <>
            <span className={styles.divider} />
            <span className={styles.description}>{channel.description}</span>
          </>
        )}
      </div>

      <div className={styles.right}>
        <span className={styles.members}>
          {channel.member_count}{" "}
          {channel.member_count === 1 ? "member" : "members"}
        </span>
      </div>
    </header>
  );
}
