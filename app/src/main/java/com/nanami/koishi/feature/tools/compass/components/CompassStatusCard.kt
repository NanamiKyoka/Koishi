package com.nanami.koishi.feature.tools.compass.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.core.sensor.SensorAccuracy
import com.nanami.koishi.feature.tools.compass.CompassUiState

@Composable
fun CompassStatusCard(
    state: CompassUiState,
    modifier: Modifier = Modifier
) {
    val (icon, titleRes, messageRes, containerColor) = statusContent(state)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun statusContent(state: CompassUiState): StatusContent {
    val containerColor = MaterialTheme.colorScheme.surfaceContainer
    return when {
        state.errorRes != null -> StatusContent(
            icon = Icons.Rounded.ErrorOutline,
            titleRes = R.string.compass_status_error,
            messageRes = state.errorRes,
            containerColor = containerColor
        )

        state.frame.tiltDeg > 25.0 -> StatusContent(
            icon = Icons.Rounded.WarningAmber,
            titleRes = R.string.compass_status_tilted,
            messageRes = R.string.compass_status_tilted_desc,
            containerColor = containerColor
        )

        state.frame.accuracy == SensorAccuracy.LOW ||
            state.frame.accuracy == SensorAccuracy.UNRELIABLE -> StatusContent(
            icon = Icons.Rounded.WarningAmber,
            titleRes = R.string.compass_status_calibrate,
            messageRes = R.string.compass_status_calibrate_desc,
            containerColor = containerColor
        )

        else -> StatusContent(
            icon = if (state.frame.accuracy == SensorAccuracy.HIGH) {
                Icons.Rounded.CheckCircle
            } else {
                Icons.Rounded.Explore
            },
            titleRes = R.string.compass_status_ready,
            messageRes = R.string.compass_status_ready_desc,
            containerColor = containerColor
        )
    }
}

private data class StatusContent(
    val icon: ImageVector,
    val titleRes: Int,
    val messageRes: Int,
    val containerColor: androidx.compose.ui.graphics.Color
)
