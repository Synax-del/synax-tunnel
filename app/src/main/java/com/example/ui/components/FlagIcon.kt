package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CircularFlagIcon(
    flagCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    if (flagCode.equals("CUSTOM", ignoreCase = true)) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Public,
                contentDescription = "Custom server",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.62f),
            )
        }
        return
    }

    Canvas(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(1.5.dp, Color(0xFFD6DCDA), CircleShape),
    ) {
        val w = this.size.width
        val h = this.size.height

        when (flagCode.uppercase()) {
            "SG" -> {
                drawRect(Color(0xFFEF3340), Offset.Zero, Size(w, h * 0.5f))
                drawRect(Color.White, Offset(0f, h * 0.5f), Size(w, h * 0.5f))
                // Crescent + stars cluster in upper-left quadrant
                drawCircle(Color.White, radius = w * 0.14f, center = Offset(w * 0.30f, h * 0.25f))
                drawCircle(Color(0xFFEF3340), radius = w * 0.115f, center = Offset(w * 0.34f, h * 0.25f))
                val stars = listOf(
                    Offset(w * 0.37f, h * 0.17f),
                    Offset(w * 0.43f, h * 0.22f),
                    Offset(w * 0.41f, h * 0.31f),
                    Offset(w * 0.33f, h * 0.31f),
                    Offset(w * 0.31f, h * 0.22f),
                )
                stars.forEach { pt ->
                    drawCircle(Color.White, radius = w * 0.022f, center = pt)
                }
            }

            "US" -> {
                val stripeH = h / 7f
                for (i in 0 until 7) {
                    val color = if (i % 2 == 0) Color(0xFFB22234) else Color.White
                    drawRect(color, Offset(0f, i * stripeH), Size(w, stripeH + 0.5f))
                }
                drawRect(Color(0xFF3C3B6E), Offset.Zero, Size(w * 0.48f, h * 0.52f))
                for (r in 0..2) {
                    for (c in 0..2) {
                        drawCircle(
                            Color.White,
                            radius = w * 0.025f,
                            center = Offset(w * (0.11f + c * 0.13f), h * (0.12f + r * 0.13f)),
                        )
                    }
                }
            }

            "GB" -> {
                drawRect(Color(0xFF012169), Offset.Zero, Size(w, h))
                drawLine(Color.White, Offset(0f, 0f), Offset(w, h), strokeWidth = w * 0.18f)
                drawLine(Color.White, Offset(w, 0f), Offset(0f, h), strokeWidth = w * 0.18f)
                drawLine(Color(0xFFC8102E), Offset(0f, 0f), Offset(w, h), strokeWidth = w * 0.08f)
                drawLine(Color(0xFFC8102E), Offset(w, 0f), Offset(0f, h), strokeWidth = w * 0.08f)
                drawRect(Color.White, Offset(w * 0.38f, 0f), Size(w * 0.24f, h))
                drawRect(Color.White, Offset(0f, h * 0.38f), Size(w, h * 0.24f))
                drawRect(Color(0xFFC8102E), Offset(w * 0.43f, 0f), Size(w * 0.14f, h))
                drawRect(Color(0xFFC8102E), Offset(0f, h * 0.43f), Size(w, h * 0.14f))
            }

            "DE" -> {
                drawRect(Color(0xFF111111), Offset.Zero, Size(w, h / 3f))
                drawRect(Color(0xFFDD0000), Offset(0f, h / 3f), Size(w, h / 3f))
                drawRect(Color(0xFFFFCE00), Offset(0f, 2f * h / 3f), Size(w, h / 3f + 1f))
            }

            "JP" -> {
                drawRect(Color.White, Offset.Zero, Size(w, h))
                drawCircle(Color(0xFFBC002D), radius = w * 0.27f, center = Offset(w * 0.5f, h * 0.5f))
            }

            "NL" -> {
                drawRect(Color(0xFFAE1C28), Offset.Zero, Size(w, h / 3f))
                drawRect(Color.White, Offset(0f, h / 3f), Size(w, h / 3f))
                drawRect(Color(0xFF21468B), Offset(0f, 2f * h / 3f), Size(w, h / 3f + 1f))
            }

            "BR" -> {
                drawRect(Color(0xFF009C3B), Offset.Zero, Size(w, h))
                val diamond = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.86f, h * 0.5f)
                    lineTo(w * 0.5f, h * 0.86f)
                    lineTo(w * 0.14f, h * 0.5f)
                    close()
                }
                drawPath(diamond, Color(0xFFFFDF00))
                drawCircle(Color(0xFF002776), radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.5f))
            }

            "ZA" -> {
                drawRect(Color(0xFFE03C31), Offset.Zero, Size(w, h * 0.5f))
                drawRect(Color(0xFF001489), Offset(0f, h * 0.5f), Size(w, h * 0.5f))
                drawRect(Color.White, Offset(0f, h * 0.36f), Size(w, h * 0.28f))
                drawRect(Color(0xFF007749), Offset(0f, h * 0.41f), Size(w, h * 0.18f))
                val triangle = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.52f, h * 0.5f)
                    lineTo(0f, h)
                    close()
                }
                drawPath(triangle, Color(0xFF007749))
                val inner = Path().apply {
                    moveTo(0f, h * 0.18f)
                    lineTo(w * 0.34f, h * 0.5f)
                    lineTo(0f, h * 0.82f)
                    close()
                }
                drawPath(inner, Color(0xFFFFB81C))
                val blackTri = Path().apply {
                    moveTo(0f, h * 0.26f)
                    lineTo(w * 0.24f, h * 0.5f)
                    lineTo(0f, h * 0.74f)
                    close()
                }
                drawPath(blackTri, Color(0xFF111111))
            }

            else -> {
                drawRect(Color(0xFF005A56), Offset.Zero, Size(w, h))
                drawCircle(Color(0xFF14B8A6), radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.5f))
            }
        }
    }
}
