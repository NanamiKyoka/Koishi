package com.nanami.koishi.feature.tools.color_picker.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.R

@Composable
fun ColorPreviewCard(
    color: Color,
    hexString: String,
    rgbString: String,
    coordinateString: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCopyHex: () -> Unit,
    onCopyRgb: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBgColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 150),
        label = "color_preview_bg"
    )

    val isLight = animatedBgColor.luminance() > 0.5f
    val contentColor = if (isLight) Color(0xFF1B1B1F) else Color(0xFFFFFFFF)
    val secondaryContentColor = contentColor.copy(alpha = 0.82f)
    val pillBackground = if (isLight) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.22f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = animatedBgColor,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBackground)
                    .clickable(onClick = onCopyHex)
                    .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) {
                            if (isLight) Color(0xFFE53935) else Color(0xFFFF5252)
                        } else {
                            contentColor
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = hexString,
                    color = contentColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clickable(onClick = onCopyRgb)
            ) {
                Text(
                    text = "RGB $rgbString",
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Text(
                    text = stringResource(R.string.color_picker_pixel_at, coordinateString),
                    color = secondaryContentColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
