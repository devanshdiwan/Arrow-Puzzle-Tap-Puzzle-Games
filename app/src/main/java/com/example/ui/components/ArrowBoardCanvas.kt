package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.models.Arrow
import com.example.models.Direction
import com.example.models.GridPoint
import com.example.models.LevelDefinition
import com.example.ui.theme.ArrowBlue
import com.example.ui.theme.ArrowNavy
import com.example.ui.theme.DangerShakeRed
import com.example.ui.theme.StarGold
import kotlin.math.hypot

@Composable
fun ArrowBoardCanvas(
    level: LevelDefinition,
    activeArrows: List<Arrow>,
    escapingArrow: Arrow?,
    escapeProgress: Float,
    shakingArrowId: String?,
    hintArrowId: String?,
    blockedCollisionPoints: List<GridPoint>?,
    isZoomMode: Boolean,
    onArrowTapped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing animation for hints
    val hintPulse = remember { Animatable(1f) }
    LaunchedEffect(hintArrowId) {
        if (hintArrowId != null) {
            hintPulse.animateTo(
                targetValue = 1.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            hintPulse.snapTo(1f)
        }
    }

    // Shake animation for error taps
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(shakingArrowId) {
        if (shakingArrowId != null) {
            repeat(3) {
                shakeOffset.animateTo(12f, tween(35, easing = LinearEasing))
                shakeOffset.animateTo(-12f, tween(35, easing = LinearEasing))
            }
            shakeOffset.snapTo(0f)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = constraints.maxWidth.toFloat()
        val availableHeight = constraints.maxHeight.toFloat()

        val cols = level.cols
        val rows = level.rows

        // Cell size calculation ensuring grid fits comfortably with margin
        val maxCellW = availableWidth / (cols + 1.2f)
        val maxCellH = availableHeight / (rows + 1.2f)
        val baseCellSize = minOf(maxCellW, maxCellH)
        val cellSize = if (isZoomMode) baseCellSize * 1.35f else baseCellSize

        val boardWidth = cols * cellSize
        val boardHeight = rows * cellSize
        val startX = (availableWidth - boardWidth) / 2f
        val startY = (availableHeight - boardHeight) / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("arrow_board_canvas")
                .pointerInput(activeArrows, cols, rows, cellSize, startX, startY) {
                    detectTapGestures { tapOffset ->
                        // Hit test all active arrows to find the closest arrow
                        var closestArrow: Arrow? = null
                        var minDistance = Float.MAX_VALUE
                        val hitThreshold = cellSize * 0.75f

                        for (arrow in activeArrows) {
                            for (p in arrow.allOccupiedPoints()) {
                                val cx = startX + p.x * cellSize + cellSize / 2f
                                val cy = startY + p.y * cellSize + cellSize / 2f
                                val dist = hypot((tapOffset.x - cx).toDouble(), (tapOffset.y - cy).toDouble()).toFloat()
                                if (dist < hitThreshold && dist < minDistance) {
                                    minDistance = dist
                                    closestArrow = arrow
                                }
                            }
                        }

                        closestArrow?.let { onArrowTapped(it.id) }
                    }
                }
        ) {
            // Draw subtle background grid dots for alignment aesthetics
            val dotRadius = (cellSize * 0.05f).coerceIn(1.5f, 3.5f)
            for (gx in 0 until cols) {
                for (gy in 0 until rows) {
                    val dotX = startX + gx * cellSize + cellSize / 2f
                    val dotY = startY + gy * cellSize + cellSize / 2f
                    drawCircle(
                        color = Color(0xFFDCE2ED).copy(alpha = 0.6f),
                        radius = dotRadius,
                        center = Offset(dotX, dotY)
                    )
                }
            }

            // Draw blocked collision line if active
            blockedCollisionPoints?.let { points ->
                if (points.isNotEmpty()) {
                    for (pt in points) {
                        val cx = startX + pt.x * cellSize + cellSize / 2f
                        val cy = startY + pt.y * cellSize + cellSize / 2f
                        drawCircle(
                            color = DangerShakeRed.copy(alpha = 0.35f),
                            radius = cellSize * 0.35f,
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            val strokeWidth = (cellSize * 0.22f).coerceIn(5.dp.toPx(), 14.dp.toPx())
            val arrowHeadSize = (cellSize * 0.38f).coerceIn(9.dp.toPx(), 22.dp.toPx())

            // Draw all active arrows
            for (arrow in activeArrows) {
                val isShaking = arrow.id == shakingArrowId
                val isHint = arrow.id == hintArrowId
                val arrowShake = if (isShaking) shakeOffset.value else 0f

                val color = when {
                    isShaking -> DangerShakeRed
                    isHint -> StarGold
                    else -> ArrowNavy
                }

                drawArrow(
                    arrow = arrow,
                    startX = startX + arrowShake,
                    startY = startY,
                    cellSize = cellSize,
                    strokeWidth = strokeWidth,
                    arrowHeadSize = arrowHeadSize,
                    color = color,
                    isHint = isHint,
                    hintGlowMultiplier = hintPulse.value
                )
            }

            // Draw escaping arrow with projectile translation
            escapingArrow?.let { arrow ->
                val travelDistance = maxOf(boardWidth, boardHeight) * 1.2f * escapeProgress
                val offsetX = arrow.direction.dx * travelDistance
                val offsetY = arrow.direction.dy * travelDistance
                val alpha = (1f - escapeProgress).coerceIn(0f, 1f)

                drawArrow(
                    arrow = arrow,
                    startX = startX + offsetX,
                    startY = startY + offsetY,
                    cellSize = cellSize,
                    strokeWidth = strokeWidth,
                    arrowHeadSize = arrowHeadSize,
                    color = ArrowBlue.copy(alpha = alpha),
                    isHint = false,
                    hintGlowMultiplier = 1f
                )
            }
        }
    }
}

private fun DrawScope.drawArrow(
    arrow: Arrow,
    startX: Float,
    startY: Float,
    cellSize: Float,
    strokeWidth: Float,
    arrowHeadSize: Float,
    color: Color,
    isHint: Boolean,
    hintGlowMultiplier: Float
) {
    if (arrow.points.isEmpty()) return

    val pixelPoints = arrow.points.map { pt ->
        Offset(
            x = startX + pt.x * cellSize + cellSize / 2f,
            y = startY + pt.y * cellSize + cellSize / 2f
        )
    }

    val path = Path().apply {
        moveTo(pixelPoints.first().x, pixelPoints.first().y)
        for (i in 1 until pixelPoints.size) {
            lineTo(pixelPoints[i].x, pixelPoints[i].y)
        }
    }

    // Hint halo glow
    if (isHint) {
        drawPath(
            path = path,
            color = StarGold.copy(alpha = 0.4f),
            style = Stroke(
                width = strokeWidth * 1.8f * hintGlowMultiplier,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }

    // Main arrow shaft
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Draw arrowhead at headPoint pointing in arrow.direction
    val headOffset = pixelPoints.last()
    drawArrowHead(
        center = headOffset,
        direction = arrow.direction,
        headSize = arrowHeadSize,
        color = color
    )
}

private fun DrawScope.drawArrowHead(
    center: Offset,
    direction: Direction,
    headSize: Float,
    color: Color
) {
    val halfBase = headSize * 0.7f
    val tipOffset = headSize * 0.6f

    val tip: Offset
    val leftCorner: Offset
    val rightCorner: Offset

    when (direction) {
        Direction.UP -> {
            tip = Offset(center.x, center.y - tipOffset)
            leftCorner = Offset(center.x - halfBase, center.y + halfBase * 0.6f)
            rightCorner = Offset(center.x + halfBase, center.y + halfBase * 0.6f)
        }
        Direction.DOWN -> {
            tip = Offset(center.x, center.y + tipOffset)
            leftCorner = Offset(center.x - halfBase, center.y - halfBase * 0.6f)
            rightCorner = Offset(center.x + halfBase, center.y - halfBase * 0.6f)
        }
        Direction.LEFT -> {
            tip = Offset(center.x - tipOffset, center.y)
            leftCorner = Offset(center.x + halfBase * 0.6f, center.y - halfBase)
            rightCorner = Offset(center.x + halfBase * 0.6f, center.y + halfBase)
        }
        Direction.RIGHT -> {
            tip = Offset(center.x + tipOffset, center.y)
            leftCorner = Offset(center.x - halfBase * 0.6f, center.y - halfBase)
            rightCorner = Offset(center.x - halfBase * 0.6f, center.y + halfBase)
        }
    }

    val headPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftCorner.x, leftCorner.y)
        lineTo(rightCorner.x, rightCorner.y)
        close()
    }

    drawPath(path = headPath, color = color)
}
