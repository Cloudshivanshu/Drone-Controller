package com.example.dronecontroller.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dronecontroller.data.SettingsStore
import com.example.dronecontroller.model.AltHoldCommandPacket
import com.example.dronecontroller.model.AppSettings
import com.example.dronecontroller.model.BuzzerCommandPacket
import com.example.dronecontroller.model.CommandPacket
import com.example.dronecontroller.model.ConnectionState
import com.example.dronecontroller.model.ControlInput
import com.example.dronecontroller.model.ControlPacket
import com.example.dronecontroller.model.DroneUiState
import com.example.dronecontroller.model.JoystickValue
import com.example.dronecontroller.model.Telemetry
import com.example.dronecontroller.model.TelemetryPacket
import com.example.dronecontroller.model.ModeCommandPacket
import com.example.dronecontroller.network.SocketEvent
import com.example.dronecontroller.network.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class DroneViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val socketManager = WebSocketManager()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val _uiState = MutableStateFlow(DroneUiState())
    private var connectJob: Job? = null
    private var controlJob: Job? = null
    private var manualDisconnect = false

    val uiState: StateFlow<DroneUiState> = _uiState.asStateFlow()

    val altHoldOn: Boolean = false
    val storedSettings = settingsStore.settings
        .catch { emit(AppSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    init {
        viewModelScope.launch {
            storedSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            socketManager.events.collect(::handleSocketEvent)
        }
        controlJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                sendControlFrame()
                checkFailsafe()
                delay(50L)
            }
        }
    }

    fun connect() {
        if (connectJob?.isActive == true) return
        manualDisconnect = false
        connectJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val url = settingsStore.settings.first().websocketUrl
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(connectionState = if (it.connectionState == ConnectionState.DISCONNECTED) ConnectionState.CONNECTING else ConnectionState.RECONNECTING) }
                }
                socketManager.connectAndAwait(url)
                if (!isActive || manualDisconnect) break
                if (!settingsStore.settings.first().autoReconnect) break
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(connectionState = ConnectionState.RECONNECTING) }
                }
                delay(1_500L)
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(connectionState = ConnectionState.DISCONNECTED, armed = false) }
            }
        }
    }

    fun disconnect() {
        manualDisconnect = true
        connectJob?.cancel()
        connectJob = null
        socketManager.disconnect()
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.DISCONNECTED,
                armed = false,
                control = it.control.copy(throttle = 0f)
            )
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            settingsStore.save(settings)
            disconnect()
        }
    }

    fun updateLeftJoystick(value: JoystickValue) {
        _uiState.update {
            it.copy(control = it.control.copy(throttle = value.y.coerceIn(0f, 1f), yaw = value.x.coerceIn(-1f, 1f)))
        }
    }

    fun updateRightJoystick(value: JoystickValue) {
        _uiState.update {
            it.copy(control = it.control.copy(pitch = value.y.coerceIn(-1f, 1f), roll = value.x.coerceIn(-1f, 1f)))
        }
    }

    fun toggleArmed() {
        val state = _uiState.value
        if (state.connectionState != ConnectionState.CONNECTED || state.failsafe) return
        val armed = !state.armed
        _uiState.update { it.copy(armed = armed) }
        socketManager.send(json.encodeToString(CommandPacket(if (armed) "arm" else "disarm")))
        appendConsole(if (armed) "SYS: ARM command sent" else "SYS: DISARM command sent")
    }

    fun toggleMode() {
        val nextMode = if (_uiState.value.mode == "MANUAL") "STABILIZE" else "MANUAL"
        _uiState.update { it.copy(mode = nextMode) }
        if (_uiState.value.connectionState == ConnectionState.CONNECTED) {
            socketManager.send(json.encodeToString(ModeCommandPacket("mode", nextMode.lowercase())))
            appendConsole("SYS: Mode set to $nextMode")
        }
    }

    fun toggleConsole() {
        _uiState.update { it.copy(showConsole = !it.showConsole) }
    }

    fun toggleBuzzer() {
        val enabled = !_uiState.value.buzzerOn
        _uiState.update { it.copy(buzzerOn = enabled) }
        if (_uiState.value.connectionState == ConnectionState.CONNECTED) {
            socketManager.send(json.encodeToString(BuzzerCommandPacket("buzzer", enabled)))
            appendConsole("SYS: Buzzer ${if (enabled) "enabled" else "disabled"}")
        }
    }

    fun toggleAltHold() {
        val enabled = !_uiState.value.altHoldOn
        _uiState.update { it.copy(altHoldOn = enabled) }
        if (_uiState.value.connectionState == ConnectionState.CONNECTED) {
            socketManager.send(json.encodeToString(AltHoldCommandPacket("althold", enabled)))
            appendConsole("SYS: Altitude hold ${if (enabled) "enabled" else "disabled"}")
        }
    }

    private suspend fun sendControlFrame() {
        val state = _uiState.value
        if (state.connectionState != ConnectionState.CONNECTED) return
        val input = state.control
        val packet = ControlPacket(
            throttle = if (state.armed && !state.failsafe) (input.throttle * 100f).toInt().coerceIn(0, 100) else 0,
            yaw = if (state.armed && !state.failsafe) (input.yaw * 100f).toInt().coerceIn(-100, 100) else 0,
            pitch = if (state.armed && !state.failsafe) (input.pitch * 100f).toInt().coerceIn(-100, 100) else 0,
            roll = if (state.armed && !state.failsafe) (input.roll * 100f).toInt().coerceIn(-100, 100) else 0
        )
        socketManager.send(json.encodeToString(packet))
    }

    private fun checkFailsafe() {
        val state = _uiState.value
        val lastTelemetryAt = state.lastTelemetryAt ?: return
        if (state.connectionState == ConnectionState.CONNECTED &&
            state.armed &&
            !state.failsafe &&
            System.currentTimeMillis() - lastTelemetryAt > 1_000L
        ) {
            _uiState.update { it.copy(failsafe = true, armed = false, control = it.control.copy(throttle = 0f)) }
            socketManager.send(json.encodeToString(CommandPacket("disarm")))
            appendConsole("FAILSAFE: telemetry timeout — throttle zeroed")
        }
    }

    private fun handleSocketEvent(event: SocketEvent) {
        when (event) {
            SocketEvent.Open -> _uiState.update {
                it.copy(connectionState = ConnectionState.CONNECTED, failsafe = false, lastTelemetryAt = System.currentTimeMillis())
            }
            is SocketEvent.Message -> parseIncoming(event.text)
            is SocketEvent.Closed -> {
                _uiState.update { it.copy(connectionState = ConnectionState.RECONNECTING, armed = false) }
                appendConsole("SYS: Link closed — ${event.reason}")
            }
            is SocketEvent.Failure -> {
                _uiState.update { it.copy(connectionState = ConnectionState.RECONNECTING, armed = false) }
                appendConsole("SYS: Link error — ${event.message}")
            }
        }
    }

    private fun parseIncoming(text: String) {
        runCatching {
            val packet = json.decodeFromString<TelemetryPacket>(text)
            val telemetry = Telemetry(
                pitch = packet.pitch ?: packet.p ?: _uiState.value.telemetry.pitch,
                roll = packet.roll ?: packet.r ?: _uiState.value.telemetry.roll,
                throttle = packet.throttle ?: packet.t ?: _uiState.value.telemetry.throttle,
                yaw = packet.yaw ?: packet.y ?: _uiState.value.telemetry.yaw,
                battery = packet.battery ?: _uiState.value.telemetry.battery,
                wifiSignal = packet.wifi ?: _uiState.value.telemetry.wifiSignal
            )
            _uiState.update {
                it.copy(
                    telemetry = telemetry,
                    lastTelemetryAt = System.currentTimeMillis(),
                    failsafe = false
                )
            }
            packet.log?.let(::appendConsole)
            packet.logs.orEmpty().forEach(::appendConsole)
        }.onFailure {
            appendConsole("SYS: Ignored malformed telemetry packet")
        }
    }

    private fun appendConsole(line: String) {
        _uiState.update { state ->
            state.copy(consoleLines = (state.consoleLines + line).takeLast(80))
        }
    }

    override fun onCleared() {
        controlJob?.cancel()
        connectJob?.cancel()
        socketManager.close()
        super.onCleared()
    }
}