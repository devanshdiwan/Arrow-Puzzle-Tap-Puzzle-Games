package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArrowBlue
import com.example.ui.theme.GameSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary

@Composable
fun ActionControls(
    hintsRemaining: Int,
    canUndo: Boolean,
    isZoomMode: Boolean,
    onHintClicked: () -> Unit,
    onZoomClicked: () -> Unit,
    onUndoClicked: () -> Unit,
    onResetClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset Button
        CircularActionButton(
            icon = Icons.Default.Refresh,
            contentDescription = "Restart Level",
            testTag = "action_reset_button",
            tint = TextPrimary,
            onClick = onResetClicked
        )

        // Hint Button with badge count (matching screenshot)
        Box(contentAlignment = Alignment.TopEnd) {
            CircularActionButton(
                icon = Icons.Default.Lightbulb,
                contentDescription = "Hint",
                testTag = "action_hint_button",
                tint = ArrowBlue,
                size = 64.dp,
                onClick = onHintClicked
            )
            // Badge
            Surface(
                shape = CircleShape,
                color = ArrowBlue,
                modifier = Modifier
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$hintsRemaining",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Zoom / Grid mode button (matching "#" grid button in screenshot)
        CircularActionButton(
            icon = if (isZoomMode) Icons.Default.ZoomOut else Icons.Default.GridOn,
            contentDescription = "Toggle Grid Zoom",
            testTag = "action_zoom_button",
            tint = ArrowBlue,
            size = 64.dp,
            onClick = onZoomClicked
        )

        // Undo Button
        CircularActionButton(
            icon = Icons.AutoMirrored.Filled.Undo,
            contentDescription = "Undo Move",
            testTag = "action_undo_button",
            tint = if (canUndo) TextPrimary else TextTertiary,
            enabled = canUndo,
            onClick = onUndoClicked
        )
    }
}

@Composable
fun CircularActionButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = GameSurface,
        shadowElevation = if (enabled) 6.dp else 1.dp,
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}
