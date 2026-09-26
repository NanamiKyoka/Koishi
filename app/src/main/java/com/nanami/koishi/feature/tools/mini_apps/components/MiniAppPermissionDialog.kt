package com.nanami.koishi.feature.tools.mini_apps.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppWebPermission

@Composable
fun MiniAppPermissionDialog(
    permissions: List<MiniAppWebPermission>,
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDeny,
        icon = {
            Icon(
                imageVector = Icons.Rounded.PrivacyTip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text(stringResource(R.string.mini_apps_permission_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.mini_apps_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(10.dp))
                permissions.forEach { permission ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = permission.icon(),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(permission.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.mini_apps_permission_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDeny) {
                Text(stringResource(R.string.mini_apps_permission_deny))
            }
        }
    )
}

private fun MiniAppWebPermission.icon(): ImageVector = when (this) {
    MiniAppWebPermission.CAMERA -> Icons.Rounded.PhotoCamera
    MiniAppWebPermission.MICROPHONE -> Icons.Rounded.Mic
    MiniAppWebPermission.LOCATION -> Icons.Rounded.MyLocation
}
