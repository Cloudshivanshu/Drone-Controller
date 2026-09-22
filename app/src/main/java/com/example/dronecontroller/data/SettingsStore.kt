package com.example.dronecontroller.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dronecontroller.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(
    name = "drone_settings"
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val host = stringPreferencesKey("esp32_host")
        val port = intPreferencesKey("esp32_port")

        val holdThrottleY =
            booleanPreferencesKey("hold_throttle_y")

        val throttleFromBottom =
            booleanPreferencesKey("throttle_from_bottom")

        val autoReconnect =
            booleanPreferencesKey("auto_reconnect")
    }

    val settings: Flow<AppSettings> =
        context.settingsDataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->

                AppSettings(
                    esp32Host =
                        preferences[Keys.host]
                            ?: AppSettings().esp32Host,

                    esp32Port =
                        (preferences[Keys.port]
                            ?: AppSettings().esp32Port)
                            .coerceIn(1, 65535),

                    holdThrottleY =
                        preferences[Keys.holdThrottleY]
                            ?: AppSettings().holdThrottleY,

                    throttleFromBottom =
                        preferences[Keys.throttleFromBottom]
                            ?: AppSettings().throttleFromBottom,

                    autoReconnect =
                        preferences[Keys.autoReconnect]
                            ?: AppSettings().autoReconnect
                )
            }

    suspend fun save(settings: AppSettings) {

        context.settingsDataStore.edit { preferences ->

            preferences[Keys.host] =
                settings.esp32Host.trim()

            preferences[Keys.port] =
                settings.esp32Port.coerceIn(1, 65535)

            preferences[Keys.holdThrottleY] =
                settings.holdThrottleY

            preferences[Keys.throttleFromBottom] =
                settings.throttleFromBottom

            preferences[Keys.autoReconnect] =
                settings.autoReconnect
        }
    }
}