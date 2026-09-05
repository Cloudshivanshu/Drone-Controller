package com.example.dronecontroller.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dronecontroller.model.ConnectionState
import com.example.dronecontroller.model.DroneUiState
import com.example.dronecontroller.viewmodel.DroneViewModel

@Composable
fun MainScreen(
    viewModel: DroneViewModel,
    onSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BoxWithLandscapeGuard {
        DroneControlSurface(
            state = state,
            onConnect = { if (state.connectionState == ConnectionState.CONNECTED) viewModel.disconnect() else viewModel.connect() },
            onSettings = onSettings,
            onArmToggle = viewModel::toggleArmed,
            onMode = viewModel::toggleMode,
            onAltHold = viewModel::toggleAltHold,
            onMenu = viewModel::toggleConsole,
            onBuzzer = viewModel::toggleBuzzer,
            onLeftStick = viewModel::updateLeftJoystick,
            onRightStick = viewModel::updateRightJoystick
        )
    }
}

@Composable
private fun DroneControlSurface(
    state: DroneUiState,
    onConnect: () -> Unit,
    onSettings: () -> Unit,
    onArmToggle: () -> Unit,
    onMode: () -> Unit,
    onAltHold: () -> Unit,
    onMenu: () -> Unit,
    onBuzzer: () -> Unit,
    onLeftStick: (com.example.dronecontroller.model.JoystickValue) -> Unit,
    onRightStick: (com.example.dronecontroller.model.JoystickValue) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(state.connectionState)
            Spacer(Modifier.width(7.dp))
            Text(
                text = state.connectionState.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = state.connectionState.color
            )
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Wifi, "WiFi", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                "${state.telemetry.wifiSignal ?: "--"}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onArmToggle,
                enabled = state.connectionState == ConnectionState.CONNECTED && !state.failsafe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.armed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    contentColor = if (state.armed) Color.White else MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.SportsMotorsports, null, Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (state.armed) "DISARM" else "ARM", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onConnect, modifier = Modifier.height(32.dp)) {
                Icon(
                    if (state.connectionState == ConnectionState.CONNECTED) Icons.Default.WifiOff else Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(if (state.connectionState == ConnectionState.CONNECTED) "DISCONNECT" else "CONNECT", fontSize = 11.sp)
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Settings, "Settings", modifier = Modifier.size(20.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp, start = 10.dp, end = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            StickPane(
                title = "THROTTLE / YAW",
                modifier = Modifier.weight(0.9f).fillMaxHeight(),
                onValueChange = onLeftStick,
                returnToCenterOnRelease = !state.settings.holdThrottleY,
                accent = Color(0xFF66E3FF)
            )
            CenterConsole(
                state = state,
                onMode = onMode,
                onAltHold = onAltHold,
                onMenu = onMenu,
                onBuzzer = onBuzzer,
                modifier = Modifier.weight(1.4f).fillMaxHeight()
            )
            StickPane(
                title = "PITCH / ROLL",
                modifier = Modifier.weight(0.9f).fillMaxHeight(),
                onValueChange = onRightStick,
                returnToCenterOnRelease = true,
                accent = Color(0xFFFFB86B)
            )
        }
    }
}

@Composable
private fun StickPane(
    title: String,
    modifier: Modifier,
    returnToCenterOnRelease: Boolean,
    accent: Color,
    onValueChange: (com.example.dronecontroller.model.JoystickValue) -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
        Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val joystickSize = (maxHeight * 0.82f).coerceAtMost(maxWidth * 0.96f)
                JoystickView(
                    modifier = Modifier.size(joystickSize),
                    title = title,
                    returnToCenterOnRelease = returnToCenterOnRelease,
                    accent = accent,
                    onValueChange = onValueChange
                )
            }
        }
    }
}

@Composable
private fun CenterConsole(
    state: DroneUiState,
    onMode: () -> Unit,
    onAltHold: () -> Unit,
    onMenu: () -> Unit,
    onBuzzer: () -> Unit,
    modifier: Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.consoleLines.size) {
        if (state.consoleLines.isNotEmpty()) listState.animateScrollToItem(state.consoleLines.lastIndex)
    }
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMode, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = "Mode toggle: ${state.mode}",
                    tint = if (state.mode == "STABILIZE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onAltHold, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.Height,
                    contentDescription = "Altitude hold toggle",
                    tint = if (state.altHoldOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onMenu, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Menu, "Menu and log toggle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = onBuzzer, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = "Buzzer toggle",
                    tint = if (state.buzzerOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            "MODE: ${state.mode}" + if (state.altHoldOn) "  •  ALT HOLD" else "",
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (state.mode == "STABILIZE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            "P: ${state.telemetry.pitch}    R: ${state.telemetry.roll}    T: ${state.telemetry.throttle}%    Y: ${state.telemetry.yaw}",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        if (state.failsafe) {
            Text(
                "FAILSAFE — TELEMETRY TIMEOUT",
                color = MaterialTheme.colorScheme.error,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (state.showConsole) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .height(132.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF050B0F)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(state.consoleLines.size) { index ->
                        Text(
                            state.consoleLines[index],
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            color = if (state.consoleLines[index].startsWith("FAILSAFE")) MaterialTheme.colorScheme.error else Color(0xFF9DD3B4)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(connectionState: ConnectionState) {
    Box(
        Modifier
            .size(8.dp)
            .background(connectionState.color, androidx.compose.foundation.shape.CircleShape)
    )
}

@Composable
private fun BoxWithLandscapeGuard(content: @Composable () -> Unit) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
        content()
    } else {
        Box(
            Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Landscape mode required", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

private val ConnectionState.label: String
    get() = when (this) {
        ConnectionState.DISCONNECTED -> "DISCONNECTED"
        ConnectionState.CONNECTING -> "CONNECTING"
        ConnectionState.CONNECTED -> "CONNECTED"
        ConnectionState.RECONNECTING -> "RECONNECTING"
    }

private val ConnectionState.color: Color
    get() = when (this) {
        ConnectionState.CONNECTED -> Color(0xFF66E3A5)
        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> Color(0xFFFFB86B)
        ConnectionState.DISCONNECTED -> Color(0xFFFF7B72)
    }