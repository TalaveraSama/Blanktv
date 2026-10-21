package app.blanktv.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * The D-pad primitive every interactive element in blankTV is built on.
 *
 * Compose's [clickable] answers DPAD_CENTER once the node holds focus, but it draws nothing to say
 * so; on a TV an invisible focus is an unusable app. This wrapper adds the two things a remote
 * needs: a visible focus ring and a scale-up, so the focused tile reads from across the room.
 */
@Composable
fun FocusBox(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val ring = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = if (focused) FOCUS_SCALE else 1f
                scaleY = if (focused) FOCUS_SCALE else 1f
            }
            .clip(shape)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) ring else Color.Transparent,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .focusable(),
    ) {
        content(focused)
    }
}

private const val FOCUS_SCALE = 1.05f
