package com.nanami.koishi.feature.tools.color_picker.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.color_picker.engine.FavoriteColor
import java.util.Locale

@Composable
fun ColorFavoritesView(
    favorites: List<FavoriteColor>,
    onSelectDetails: (FavoriteColor) -> Unit,
    onDeleteFavorite: (String) -> Unit,
    onCopyText: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.color_picker_favorites_empty),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.color_picker_favorites_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites, key = { it.id }) { item ->
                FavoriteColorCard(
                    favorite = item,
                    onClick = { onSelectDetails(item) },
                    onDelete = { onDeleteFavorite(item.id) },
                    onCopyHex = {
                        onCopyText(item.hex, R.string.color_picker_copied_hex)
                    }
                )
            }
        }
    }
}

@Composable
private fun FavoriteColorCard(
    favorite: FavoriteColor,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCopyHex: () -> Unit,
    modifier: Modifier = Modifier
) {
    val composeColor = remember(favorite.colorArgb) { Color(favorite.colorArgb) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .background(composeColor)
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = favorite.hex,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "RGB ${favorite.red}, ${favorite.green}, ${favorite.blue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!favorite.coordinates.isNullOrBlank()) {
                    Text(
                        text = favorite.coordinates,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCopyHex,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.color_picker_copy_color),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.color_picker_delete_favorite),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorDetailsDialog(
    favorite: FavoriteColor,
    onDismissRequest: () -> Unit,
    onCopyText: (String, Int) -> Unit
) {
    val composeColor = remember(favorite.colorArgb) { Color(favorite.colorArgb) }

    val hsl = remember(favorite) {
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(favorite.red, favorite.green, favorite.blue, hsv)
        val h = hsv[0].toInt()
        val s = (hsv[1] * 100).toInt()
        val v = (hsv[2] * 100).toInt()
        "hsl($h, $s%, $v%)"
    }

    val argbHex = String.format(Locale.US, "0x%08X", favorite.colorArgb)
    val composeCode = "Color($argbHex)"
    val xmlCode = "<color name=\"picked_color\">${favorite.hex}</color>"
    val rgbCode = "rgb(${favorite.red}, ${favorite.green}, ${favorite.blue})"

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = stringResource(R.string.color_picker_color_details),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = composeColor
                ) {}

                ColorDetailRow(
                    label = stringResource(R.string.color_picker_format_hex),
                    value = favorite.hex,
                    onCopy = { onCopyText(favorite.hex, R.string.color_picker_copied_value) }
                )

                ColorDetailRow(
                    label = stringResource(R.string.color_picker_format_rgb),
                    value = rgbCode,
                    onCopy = { onCopyText(rgbCode, R.string.color_picker_copied_value) }
                )

                ColorDetailRow(
                    label = stringResource(R.string.color_picker_format_hsl),
                    value = hsl,
                    onCopy = { onCopyText(hsl, R.string.color_picker_copied_value) }
                )

                ColorDetailRow(
                    label = stringResource(R.string.color_picker_format_argb),
                    value = argbHex,
                    onCopy = { onCopyText(argbHex, R.string.color_picker_copied_value) }
                )

                ColorDetailRow(
                    label = stringResource(R.string.color_picker_format_compose),
                    value = composeCode,
                    onCopy = { onCopyText(composeCode, R.string.color_picker_copied_value) }
                )

                ColorDetailRow(
                    label = stringResource(R.string.color_picker_format_xml),
                    value = xmlCode,
                    onCopy = { onCopyText(xmlCode, R.string.color_picker_copied_value) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_confirm))
            }
        }
    )
}

@Composable
private fun ColorDetailRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onCopy),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = stringResource(R.string.color_picker_copy_color),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
