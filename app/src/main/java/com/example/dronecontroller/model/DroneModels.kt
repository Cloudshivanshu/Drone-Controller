package com.example.dronecontroller.model

import kotlinx.serialization.Serializable

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class AppSettings(
    val esp32Host: String = "192.168.4.1",
    val esp32Port: Int = 81,
    val holdThrottleY: Boolean = true,
    val autoReconnect: Boolean = true
) {
    val websocketUrl: String
        get() = "ws://$esp32Host:$esp32Port/"
}

data class JoystickValue(
    val x: Float = 0f,
    val y: Float = 0f
)

data class ControlInput(
    val throttle: Float = 0f,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f
)

data class Telemetry(
    val pitch: Int = 0,
    val roll: Int = 0,
    val throttle: Int = 0,
    val yaw: Int = 0,
    val battery: Int? = null,
    val wifiSignal: Int? = null
)

data class DroneUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val settings: AppSettings = AppSettings(),
    val telemetry: Telemetry = Telemetry(),
    val control: ControlInput = ControlInput(),
    val consoleLines: List<String> = listOf(
        "MPU9250 I2C connection [OK]",
        "AK8963 I2C connection [OK]",
        "LPS25H I2C connection [OK]",
        "ESTIMATOR: Using estimator 1",
        "EEPROM I2C connection [OK]",
        "AK8963: Self test [OK]"
    ),
    val mode: String = "MANUAL",
    val showConsole: Boolean = true,
    val buzzerOn: Boolean = false,
    val armed: Boolean = false,
    val failsafe: Boolean = false,
    val lastTelemetryAt: Long? = null,
    val altHoldOn: Boolean = false
)

@Serializable
data class ControlPacket(
    val throttle: Int,
    val yaw: Int,
    val pitch: Int,
    val roll: Int
)

@Serializable
data class CommandPacket(
    val cmd: String
)

@Serializable
data class BuzzerCommandPacket(
    val cmd: String,
    val enabled: Boolean
)

@Serializable
data class AltHoldCommandPacket(val cmd: String, val enabled: Boolean)
@Serializable
data class ModeCommandPacket(
    val cmd: String,
    val mode: String
)

@Serializable
data class TelemetryPacket(
    val pitch: Int? = null,
    val roll: Int? = null,
    val throttle: Int? = null,
    val yaw: Int? = null,
    val p: Int? = null,
    val r: Int? = null,
    val t: Int? = null,
    val y: Int? = null,
    val battery: Int? = null,
    val wifi: Int? = null,
    val log: String? = null,
    val logs: List<String>? = null
)