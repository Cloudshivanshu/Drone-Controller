package com.example.dronecontroller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.dronecontroller.model.JoystickValue
import kotlin.math.min

@Composable
fun JoystickView(
    modifier: Modifier = Modifier,
    title: String,
    returnToCenterOnRelease: Boolean,
    accent: Color,
    throttleFromBottom: Boolean = false,
    onValueChange: (JoystickValue) -> Unit
) {
    var knobOffset by remember(throttleFromBottom) { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var initialized by remember(throttleFromBottom) { mutableStateOf(false) }

    LaunchedEffect(canvasSize, throttleFromBottom) {
        if (canvasSize != IntSize.Zero && !initialized) {
            val radius = min(canvasSize.width, canvasSize.height) * 0.36f
            knobOffset = restPosition(throttleFromBottom, radius)
            onValueChange(normalize(knobOffset, canvasSize.width, canvasSize.height, throttleFromBottom))
            initialized = true
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(returnToCenterOnRelease, throttleFromBottom) {
                detectDragGestures(
                    onDragStart = { position ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) * 0.36f
                        knobOffset = clampToCircle(position - center, radius)
                        onValueChange(normalize(knobOffset, size.width, size.height, throttleFromBottom))
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val radius = min(size.width, size.height) * 0.36f
                        knobOffset = clampToCircle(knobOffset + dragAmount, radius)
                        onValueChange(normalize(knobOffset, size.width, size.height, throttleFromBottom))
                    },
                    onDragEnd = {
                        val radius = min(size.width, size.height) * 0.36f
                        knobOffset = if (returnToCenterOnRelease) {
                            restPosition(throttleFromBottom, radius)
                        } else {
                            Offset(0f, knobOffset.y)
                        }
                        onValueChange(normalize(knobOffset, size.width, size.height, throttleFromBottom))
                    },
                    onDragCancel = {
                        val radius = min(size.width, size.height) * 0.36f
                        knobOffset = if (returnToCenterOnRelease) {
                            restPosition(throttleFromBottom, radius)
                        } else {
                            Offset(0f, knobOffset.y)
                        }
                        onValueChange(normalize(knobOffset, size.width, size.height, throttleFromBottom))
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) * 0.39f
        drawCircle(Color(0xFF101E27), radius)
        drawCircle(accent.copy(alpha = 0.25f), radius, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        drawCircle(accent.copy(alpha = 0.12f), radius * 0.66f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        drawLine(accent.copy(alpha = 0.18f), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1.dp.toPx())
        drawLine(accent.copy(alpha = 0.18f), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1.dp.toPx())
        drawCircle(accent.copy(alpha = 0.18f), radius * 0.23f, center)
        drawCircle(color = accent, radius = radius * 0.25f, center = center + knobOffset)
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = radius * 0.06f,
            center = center + knobOffset - Offset(radius * 0.07f, radius * 0.07f)
        )
    }
}

private fun restPosition(throttleFromBottom: Boolean, radius: Float): Offset {
    return if (throttleFromBottom) Offset(0f, radius) else Offset.Zero
}

private fun clampToCircle(offset: Offset, radius: Float): Offset {
    val distance = offset.getDistance()
    return if (distance > radius && distance > 0f) offset * (radius / distance) else offset
}

private fun normalize(offset: Offset, width: Int, height: Int, throttleFromBottom: Boolean): JoystickValue {
    val radius = min(width, height) * 0.36f
    val yValue = if (throttleFromBottom) {
        // Full vertical travel: bottom (offset.y = +radius) = 0, top (offset.y = -radius) = 1
        ((radius - offset.y) / (2f * radius)).coerceIn(0f, 1f)
    } else {
        (-offset.y / radius).coerceIn(-1f, 1f)
    }
    return JoystickValue(
        x = (offset.x / radius).coerceIn(-1f, 1f),
        y = yValue
    )
}