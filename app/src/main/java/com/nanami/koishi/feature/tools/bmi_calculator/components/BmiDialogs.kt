package com.nanami.koishi.feature.tools.bmi_calculator.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nanami.koishi.R

@Composable
fun BmiClearAllDialog(
    recordCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.bmi_clear_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.bmi_clear_message, recordCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.bmi_clear_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.bmi_clear_cancel))
            }
        }
    )
}
