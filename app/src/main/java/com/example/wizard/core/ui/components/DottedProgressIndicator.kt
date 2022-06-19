package com.example.wizard.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
fun DottedProgressIndicator(
    modifier: Modifier = Modifier,
    numberOfCircles: Int = 3,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val x = numberOfCircles * 2 - 1
    val transition = rememberInfiniteTransition()
    val scales = List(numberOfCircles) { i ->
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(i * 100)
            )
        )
    }
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(48.dp)
            .then(modifier)
    ) {
        val preferredHeight = maxWidth / x
        val size = when {
            preferredHeight <= maxHeight -> DpSize(maxWidth, preferredHeight)
            else -> DpSize(maxHeight * x, maxHeight)
        }
        Canvas(Modifier.size(size)) {
            val circleSize = this.size.width / x
            val maxCircleRadius = circleSize / 2
            val circleRadius = maxCircleRadius / 1.5f
            repeat(numberOfCircles) { i ->
                drawCircle(
                    color = color,
                    radius = circleRadius * scales[i].value,
                    center = Offset((2 * i * circleSize) + maxCircleRadius, maxCircleRadius)
                )
            }
        }
    }
}
