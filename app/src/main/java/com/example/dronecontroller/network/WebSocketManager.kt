package com.example.dronecontroller.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import kotlin.coroutines.resume

sealed interface SocketEvent {
    data object Open : SocketEvent
    data class Message(val text: String) : SocketEvent
    data class Closed(val reason: String) : SocketEvent
    data class Failure(val message: String) : SocketEvent
}

class WebSocketManager {
    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    private var socket: WebSocket? = null
    private var completion: (() -> Unit)? = null

    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    suspend fun connectAndAwait(url: String) {
        disconnect()
        suspendCancellableCoroutine { continuation ->
            var finished = false
            fun finish() {
                if (!finished) {
                    finished = true
                    completion = null
                    continuation.resume(Unit)
                }
            }

            completion = ::finish
            val request = Request.Builder().url(url).build()
            socket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    scope.launch { _events.emit(SocketEvent.Open) }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    scope.launch { _events.emit(SocketEvent.Message(text)) }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    onMessage(webSocket, bytes.utf8())
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    scope.launch { _events.emit(SocketEvent.Closed(reason.ifBlank { "Closed ($code)" })) }
                    finish()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    scope.launch { _events.emit(SocketEvent.Failure(t.message ?: "WebSocket failure")) }
                    finish()
                }
            })

            continuation.invokeOnCancellation {
                socket?.cancel()
                finish()
            }
        }
    }

    fun send(text: String): Boolean = socket?.send(text) == true

    fun disconnect() {
        completion?.invoke()
        completion = null
        socket?.close(1000, "Client disconnected")
        socket = null
    }

    fun close() {
        disconnect()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
        client.cache?.close()
    }
}