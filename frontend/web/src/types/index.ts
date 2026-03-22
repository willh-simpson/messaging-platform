/*
 * all fields are in snake case to match Jackson SNAKE_CASE strategy in APIs.
 */

export interface User {
  user_id: string;
  username: string;
  email: string;
  created_at: string;
}

export interface AuthResponse {
  token: string;
  user_id: string;
  username: string;
}

export interface Channel {
  channel_id: string;
  name: string;
  description: string | null;
  created_by: string;
  created_at: string;
  member_count: number;
}

export interface Message {
  message_id: string;
  channel_id: string;
  author_id: string;
  author_username: string;
  content: string;
  created_at: string;
}

export interface MessageAccepted {
  message_id: string;
  channel_id: string;
  accepted_at: string;
  status: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  total_elements: number;
  total_pages: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}

// WebSocket STOMP payload from delivery-service
export interface DeliveryPayload {
  message_id: string;
  channel_id: string;
  author_id: string;
  author_username: string;
  content: string;
  created_at: string;
}
