package com.messaging.messagingplatform.data.websocket

import android.util.Log
import com.google.gson.Gson
import com.messaging.messagingplatform.data.model.MessageDeliveryPayload
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements STOMP 1.2 protocol over raw OkHttp WebSocket.
 *
 * Raw STOMP is used over a library because
 * Spring's STOMP broker uses 1.2 wire protocol and backend is directly controlled,
 * so this can connect directly to the /ws/websocket endpoint without issue.
 */
@Singleton
class StompClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null

    /**
     * Connects to delivery-service WebSocket and subscribes to a given channel.
     *
     * delivery-service/com.messaging.deliveryservice.security.JwtHandShakeInterceptor reads
     * token via upgrade request URL, so token is passed as a query parameter.
     */
    fun observeChannel(
        wsBaseUrl: String,
        token: String,
        channelId: String,
    ): Flow<MessageDeliveryPayload> = callbackFlow {
        val url = "${wsBaseUrl}ws/websocket?token=${token}"
        val request = Request.Builder().url(url).build()

        val listener = object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened")

                webSocket.send(buildConnectFrame())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.v(TAG, "Frame received: $text")

                val frame = parseFrame(text)
                when (frame.command) {
                    "CONNECTED" -> {
                        Log.d(TAG, "STOMP connected. Subscribing to channel: $channelId")

                        webSocket.send(buildSubscribeFrame(channelId))
                    }

                    "MESSAGE" -> {
                        try {
                            val payload = gson.fromJson(frame.body, MessageDeliveryPayload::class.java)
                            trySend(payload)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse MESSAGE body", e)
                        }
                    }

                    "ERROR" -> {
                        Log.e(TAG, "STOMP ERROR frame: ${frame.body}")
                    }

                    else -> {
                        // RECEIPT, HEARTBEAT, etc. no action needed.
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)

                close(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")

                webSocket.close(1000, null)
                channel.close()
            }
        }

        webSocket = okHttpClient.newWebSocket(request, listener)

        // when Flow connection is canceled, this sends STOMP DISCONNECT and closes WebSocket connection cleanly
        awaitClose {
            Log.d(TAG, "Flow cancelled. Disconnecting WebSocket")

            webSocket?.send(buildDisconnectFrame())
            webSocket?.close(1000, "Screen left")
            webSocket = null
        }
    }

    fun disconnect() {
        webSocket?.send(buildDisconnectFrame())
        webSocket?.close(1000, "Explicit disconnect")
        webSocket = null
    }

    /*
     * STOMP frame builders
     */

    private fun buildConnectFrame(): String =
        "CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\u0000"

    private fun buildSubscribeFrame(channelId: String): String =
        "SUBSCRIBE\nid:sub-0\ndestination:/topic/channels/{$channelId}\n\n\u0000"

    private fun buildDisconnectFrame(): String =
        "DISCONNECT\n\n\u0000"

    /*
     * STOMP frame parsing
     */

    private data class StompFrame(
        val command: String,
        val headers: Map<String, String>,
        val body: String,
    )

    /**
     * Takes incoming raw text from STOMP frame and cleanly separate into
     * command, headers, body.
     *
     * Headers and body are optional in STOMP frames.
     *
     * Because this client uses STOMP 1.2 over a raw OkHttp WebSocket client,
     * incoming frames will be raw text, so frame information needs to be parsed and separated.
     */
    private fun parseFrame(raw: String): StompFrame {
        val text = raw.trimEnd('\u0000')
        val lines = text.split("\n")

        val command = lines.firstOrNull()?.trim() ?: ""
        val headers = mutableMapOf<String, String>()
        var bodyStart = lines.size

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) {
                bodyStart = i + 1

                break
            }

            val colon = line.indexOf(':')
            if (colon > 0) {
                headers[line.substring(0, colon).trim()] = line.substring(colon + 1).trim()
            }
        }

        val body = if (bodyStart < lines.size) {
            lines.subList(bodyStart, lines.size).joinToString("\n").trim()
        } else ""

        return StompFrame(command, headers, body)
    }

    companion object {
        private const val TAG = "StompClient"
    }
}