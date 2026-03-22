import { useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useMessageStore } from "@/store/messageStore";
import type { DeliveryPayload, Message } from "@/types";

/**
 * Connects to delivery-service STOMP broker.
 * Subscribes to /topic/channels/{channel_id} when a channel is active.
 *
 * Connection is only established once on mount using JWT passed as query parameter,
 * which is how backend validates the WebSocket connection.
 *
 * On channel change the previous subscription is unsubscribed before the new one is created.
 * The STOMP client itself stays connected.
 */
export function useWebSocket(
  token: string | null,
  activeChannelId: string | null,
) {
  const clientRef = useRef<Client | null>(null);
  const subscriptionRef = useRef<{ unsubscribe: () => void } | null>(null);
  const addMessage = useMessageStore((s) => s.addMessage);

  useEffect(() => {
    if (!token) return;

    // establish STOMP connection once token is received
    const stompClient = new Client({
      // token is used as query parameter since WebSocket upgrade requests can't carry Authorization headers
      webSocketFactory: () =>
        new SockJS(`/ws?token=${encodeURIComponent(token)}`),

      reconnectDelay: 3000,

      onConnect: () => {
        console.log("[WebSocket] Connected to delivery-service");
      },

      onDisconnect: () => {
        console.log("[WebSocket] Disconnected");
      },

      onStompError: (frame) => {
        console.error("[WebSocket] STOMP error", frame);
      },
    });

    stompClient.activate();
    clientRef.current = stompClient;

    return () => {
      stompClient.deactivate();
      clientRef.current = null;
    };
  }, [token]);

  // subscribe/unsubscribe when active channel changes
  useEffect(() => {
    const client = clientRef.current;
    if (!client || !activeChannelId) return;

    const waitAndSubscribe = () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }

      const subscription = client.unsubscribe(
        `/topic/channels/${activeChannelId}`,
        (frame) => {
          try {
            const payload: DeliveryPayload = JSON.parse(frame.body);

            const message: Message = {
              message_id: payload.message_id,
              channel_id: payload.channel_id,
              author_id: payload.author_id,
              author_username: payload.author_username,
              content: payload.content,
              created_at: payload.created_at,
            };

            addMessage(message);
          } catch (e) {
            console.error("[WebSocket] Failed to parse message", e);
          }
        },
      );

      subscription.current = subscription;
    };

    if (client.connected) {
      waitAndSubscribe();
    } else {
      const originalOnConnect = client.onConnect;
      client.onConnect = (frame) => {
        originalOnConnect?.(frame);
        waitAndSubscribe();
      };
    }

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
    };
  }, [activeChannelId, addMessage]);
}
