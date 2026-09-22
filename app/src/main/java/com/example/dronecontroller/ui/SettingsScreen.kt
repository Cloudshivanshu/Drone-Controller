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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dronecontroller.model.AppSettings
import com.example.dronecontroller.viewmodel.DroneViewModel

@Composable
fun SettingsScreen(
    viewModel: DroneViewModel,
    onBack: () -> Unit
) {
    val stored by viewModel.storedSettings.collectAsStateWithLifecycle()
    var host by remember(stored.esp32Host) { mutableStateOf(stored.esp32Host) }
    var port by remember(stored.esp32Port) { mutableStateOf(stored.esp32Port.toString()) }
    var holdThrottleY by remember(stored.holdThrottleY) { mutableStateOf(stored.holdThrottleY) }
    var autoReconnect by remember(stored.autoReconnect) { mutableStateOf(stored.autoReconnect) }
    var throttleFromBottom by remember(stored.throttleFromBottom) { mutableStateOf(stored.throttleFromBottom) }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "LINK SETTINGS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("ESP32 IP address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Router, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                label = { Text("WebSocket port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            SettingToggle("Hold throttle Y on release", holdThrottleY) { holdThrottleY = it }
            SettingToggle("Throttle starts from bottom (full range)", throttleFromBottom) { throttleFromBottom = it }
            SettingToggle("Auto-reconnect link", autoReconnect) { autoReconnect = it }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    viewModel.updateSettings(
                        AppSettings(
                            esp32Host = host,
                            esp32Port = port.toIntOrNull()?.coerceIn(1, 65535) ?: 81,
                            holdThrottleY = holdThrottleY,
                            autoReconnect = autoReconnect,
                            throttleFromBottom = throttleFromBottom
                        )
                    )
                    onBack()
                },
                enabled = host.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("SAVE & CLOSE")
            }
            Spacer(Modifier.height(16.dp))
        }
        Card(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("CONTROL LINK", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "The controller sends a compact control frame 20 times per second while the link is active. Throttle is always forced to zero until the ARM command is accepted locally.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Failsafe behavior",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "If no telemetry arrives for one second, the app disarms locally, zeros throttle, and shows a visible warning. Reconnect is available from the main screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Endpoint: ws://${host.ifBlank { "…" }}:${port.ifBlank { "81" }}/",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9DD3B4)
                )
            }
        }
    }
}
@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}