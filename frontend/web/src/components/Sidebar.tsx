import { useState } from "react";
import { useAuthStore } from "@/store/authStore";
import { useChannelStore } from "@/store/channelStore";
import { channelsApi } from "@/api/channels";
import styles from "./Sidebar.module.css";

export default function Sidebar() {
  const { username, logout } = useAuthStore();
  const {
    channels,
    activeChannelId,
    setActiveChannel,
    addChannel,
    fetchChannels,
  } = useChannelStore();

  const [creating, setCreating] = useState(false);
  const [newChannelName, setNewChannelName] = useState("");
  const [newChannelDesc, setNewChannelDesc] = useState("");
  const [createLoading, setCreateLoading] = useState(false);

  const handleCreateChannel = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!newChannelName.trim()) return;
    setCreateLoading(true);

    try {
      const channel = await channelsApi.create(
        newChannelName.trim(),
        newChannelDesc.trim() || undefined,
      );

      addChannel(channel);
      setActiveChannel(channel.channel_id);
      setNewChannelName("");
      setNewChannelDesc("");

      setCreating(false);
    } catch {
      // channel may already exist, so refreshing to get latest
      await fetchChannels();
    } finally {
      setCreateLoading(false);
    }
  };

  return (
    <aside className={styles.sidebar}>
      {/* brand */}
      <div className={styles.brand}>
        <span className={styles.brandIcon}>⬡</span>
        <span className={styles.brandName}>Messaging</span>
      </div>

      {/* channel list */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <span className={styles.sectionLabel}>Channels</span>
          <button
            className={styles.addBtn}
            onClick={() => setCreating((v) => !v)}
            title="New channel"
          >
            {creating ? "X" : "+"}
          </button>
        </div>

        {/* new channel form */}
        {creating && (
          <form onSubmit={handleCreateChannel} className={styles.createForm}>
            <input
              className={styles.createInput}
              type="text"
              placeholder="channel-name"
              value={newChannelName}
              onChange={(e) => setNewChannelName(e.target.value)}
              autoFocus
            />
            <input
              className={styles.createInput}
              type="text"
              placeholder="Description (optional)"
              value={newChannelDesc}
              onChange={(e) => setNewChannelDesc(e.target.value)}
            />
            <button
              type="submit"
              className={styles.createSubmit}
              disabled={createLoading || !newChannelName.trim()}
            >
              {createLoading ? "Creating..." : "Create"}
            </button>
          </form>
        )}

        {/* channel items */}
        <nav className={styles.channelList}>
          {channels.length === 0 && (
            <p className={styles.emptyChannels}>No channels yet</p>
          )}
          {channels.map((channel) => (
            <button
              key={channel.channel_id}
              className={`${styles.channelItem} ${channel.channel_id === activeChannelId ? styles.active : ""}`}
              onClick={() => setActiveChannel(channel.channel_id)}
            >
              <span className={styles.channelHash}>#</span>
              <span className={styles.channelName}>{channel.name}</span>
            </button>
          ))}
        </nav>

        {/* user bar */}
        <div className={styles.userBar}>
          <div className={styles.avatar}>
            {username?.[0]?.toUpperCase() ?? "?"}
          </div>
          <span className={styles.usernameText}>{username}</span>
          <button
            className={styles.logoutBtn}
            onClick={logout}
            title="Sign out"
          >
            ⎋
          </button>
        </div>
      </div>
    </aside>
  );
}
