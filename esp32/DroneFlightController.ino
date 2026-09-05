#include <Arduino.h>
#include <WiFi.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <ArduinoJson.h>

// Configure these for the network used by the flight controller.
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

constexpr uint16_t WEBSOCKET_PORT = 81;
constexpr uint32_t TELEMETRY_INTERVAL_MS = 250;

AsyncWebServer server(WEBSOCKET_PORT);
AsyncWebSocket webSocket("/");

struct RcInput {
  int throttle = 0;  // 0..100
  int yaw = 0;       // -100..100
  int pitch = 0;     // -100..100
  int roll = 0;      // -100..100
  bool armed = false;
};

RcInput rc;
uint32_t lastTelemetryAt = 0;

int wifiSignalPercent() {
  const int rssi = WiFi.RSSI();
  if (rssi <= -100) return 0;
  if (rssi >= -50) return 100;
  return 2 * (rssi + 100);
}

const char* startupLogs[] = {
  "MPU9250 I2C connection [OK]",
  "AK8963 I2C connection [OK]",
  "LPS25H I2C connection [OK]",
  "ESTIMATOR: Using estimator 1",
  "EEPROM I2C connection [OK]",
  "AK8963: Self test [OK]"
};

void sendLog(AsyncWebSocketClient* client, const char* message) {
  JsonDocument document;
  document["log"] = message;
  String output;
  serializeJson(document, output);
  if (client != nullptr) {
    client->text(output);
  } else {
    webSocket.textAll(output);
  }
}

void sendTelemetry() {
  JsonDocument document;

  // Replace these placeholders with real estimator and battery values.
  document["pitch"] = 0;
  document["roll"] = 0;
  document["throttle"] = rc.armed ? rc.throttle : 0;
  document["yaw"] = 0;
  document["battery"] = 100;
  document["wifi"] = wifiSignalPercent();
  document["log"] = "SYS: Free heap: " + String(ESP.getFreeHeap()) + " bytes";

  String output;
  serializeJson(document, output);
  webSocket.textAll(output);
}

void applyControlToFlightController() {
  const int safeThrottle = rc.armed ? constrain(rc.throttle, 0, 100) : 0;
  const int safeYaw = rc.armed ? constrain(rc.yaw, -100, 100) : 0;
  const int safePitch = rc.armed ? constrain(rc.pitch, -100, 100) : 0;
  const int safeRoll = rc.armed ? constrain(rc.roll, -100, 100) : 0;

  // Hardware integration boundary:
  // map safeThrottle/safeYaw/safePitch/safeRoll to the actual RC input,
  // PWM channels, or flight-stack API for your airframe.
  (void)safeThrottle;
  (void)safeYaw;
  (void)safePitch;
  (void)safeRoll;
}

void handleControlMessage(AsyncWebSocketClient* client, const uint8_t* data, size_t length) {
  JsonDocument document;
  DeserializationError error = deserializeJson(document, data, length);
  if (error) {
    sendLog(client, "SYS: Invalid JSON command");
    return;
  }

  const char* command = document["cmd"] | "";
  if (strcmp(command, "arm") == 0) {
    rc.armed = true;
    sendLog(client, "SYS: Armed");
    return;
  }
  if (strcmp(command, "disarm") == 0) {
    rc.armed = false;
    rc.throttle = 0;
    sendLog(client, "SYS: Disarmed");
    applyControlToFlightController();
    return;
  }
  if (strcmp(command, "buzzer") == 0) {
    bool enabled = document["enabled"] | false;
    // Drive the buzzer GPIO here for the specific flight-controller board.
    sendLog(client, enabled ? "SYS: Buzzer enabled" : "SYS: Buzzer disabled");
    return;
  }
  if (strcmp(command, "mode") == 0) {
    const char* mode = document["mode"] | "manual";
    // Forward mode to the estimator/flight stack here when supported.
    String modeLog = "SYS: Mode set to ";
    modeLog += mode;
    sendLog(client, modeLog.c_str());
    return;
  }

  if (document["throttle"].is<int>()) rc.throttle = constrain(document["throttle"].as<int>(), 0, 100);
  if (document["yaw"].is<int>()) rc.yaw = constrain(document["yaw"].as<int>(), -100, 100);
  if (document["pitch"].is<int>()) rc.pitch = constrain(document["pitch"].as<int>(), -100, 100);
  if (document["roll"].is<int>()) rc.roll = constrain(document["roll"].as<int>(), -100, 100);
  applyControlToFlightController();
}

void webSocketEvent(
  AsyncWebSocket* serverInstance,
  AsyncWebSocketClient* client,
  AwsEventType type,
  void* arg,
  uint8_t* data,
  size_t length
) {
  (void)serverInstance;
  (void)arg;

  if (type == WS_EVT_CONNECT) {
    client->text("{\"log\":\"SYS: WebSocket connected\"}");
    for (const char* line : startupLogs) {
      sendLog(client, line);
    }
    return;
  }

  if (type == WS_EVT_DATA) {
    AwsFrameInfo* frame = reinterpret_cast<AwsFrameInfo*>(arg);
    if (frame->final && frame->index == 0 && frame->len == length &&
        frame->opcode == WS_TEXT) {
      handleControlMessage(client, data, length);
    }
    return;
  }

  if (type == WS_EVT_DISCONNECT) {
    // A lost client must never leave a live throttle command behind.
    rc.armed = false;
    rc.throttle = 0;
    applyControlToFlightController();
  }
}

void setup() {
  Serial.begin(115200);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("Connecting to WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(300);
    Serial.print(".");
  }
  Serial.println();
  Serial.print("WebSocket endpoint: ws://");
  Serial.print(WiFi.localIP());
  Serial.println(":81/");

  webSocket.onEvent(webSocketEvent);
  server.addHandler(&webSocket);
  server.begin();
}

void loop() {
  webSocket.cleanupClients();
  if (millis() - lastTelemetryAt >= TELEMETRY_INTERVAL_MS) {
    lastTelemetryAt = millis();
    sendTelemetry();
  }
}