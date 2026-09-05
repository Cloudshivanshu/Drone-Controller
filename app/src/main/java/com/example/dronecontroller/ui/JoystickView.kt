package com.example.dronecontroller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.dronecontroller.model.JoystickValue
import kotlin.math.min

@Composable
fun JoystickView(
    modifier: Modifier = Modifier,
    title: String,
    returnToCenterOnRelease: Boolean,
    accent: Color,
    onValueChange: (JoystickValue) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(returnToCenterOnRelease) {
                detectDragGestures(
                    onDragStart = { position ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        knobOffset = clampToCircle(position - center, min(size.width, size.height) * 0.36f)
                        onValueChange(normalize(knobOffset, size.width, size.height))
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val radius = min(size.width, size.height) * 0.36f
                        knobOffset = clampToCircle(knobOffset + dragAmount, radius)
                        onValueChange(normalize(knobOffset, size.width, size.height))
                    },
                    onDragEnd = {
                        knobOffset = if (returnToCenterOnRelease) {
                            Offset.Zero
                        } else {
                            Offset(0f, knobOffset.y)
                        }
                        onValueChange(normalize(knobOffset, size.width, size.height))
                    },
                    onDragCancel = {
                        knobOffset = if (returnToCenterOnRelease) Offset.Zero else Offset(0f, knobOffset.y)
                        onValueChange(normalize(knobOffset, size.width, size.height))
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

private fun clampToCircle(offset: Offset, radius: Float): Offset {
    val distance = offset.getDistance()
    return if (distance > radius && distance > 0f) offset * (radius / distance) else offset
}

private fun normalize(offset: Offset, width: Int, height: Int): JoystickValue {
    val radius = min(width, height) * 0.36f
    return JoystickValue(
        x = (offset.x / radius).coerceIn(-1f, 1f),
        y = (-offset.y / radius).coerceIn(-1f, 1f)
    )
}