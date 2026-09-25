package com.nanami.koishi.feature.tools.ruler.components

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.ruler.engine.RulerPreferences

@Composable
fun RulerCalibrationScreen(
    onDismiss: () -> Unit,
    onCalibrationSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val basePxPerMm = remember { RulerPreferences.getBasePxPerMm(context) }
    var targetMm by remember { mutableFloatStateOf(RulerPreferences.getLastTargetMm(context)) }
    var currentBarX by remember { mutableFloatStateOf(targetMm * RulerPreferences.getCalibratedPxPerMm(context)) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf(targetMm.toInt().toString()) }

    LaunchedEffect(targetMm) {
        currentBarX = targetMm * RulerPreferences.getCalibratedPxPerMm(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    currentBarX = offset.x.coerceIn(50f, size.width.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentBarX = (currentBarX + dragAmount.x).coerceIn(50f, size.width.toFloat())
                }
            }
    ) {
        val lineColor = Color(0xFF66BFA8)
        val thumbColor = Color(0xFF555E5B)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height * 0.5f

            drawLine(
                color = lineColor,
                start = Offset(0f, centerY),
                end = Offset(currentBarX, centerY),
                strokeWidth = 4f,
                cap = StrokeCap.Square
            )

            val thumbWidth = 14f
            val thumbHeight = 64f
            val thumbPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = currentBarX - thumbWidth / 2,
                        top = centerY - thumbHeight / 2,
                        right = currentBarX + thumbWidth / 2,
                        bottom = centerY + thumbHeight / 2,
                        cornerRadius = CornerRadius(7f, 7f)
                    )
                )
            }
            drawPath(path = thumbPath, color = thumbColor)
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = lineColor
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.ruler_calibration_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = lineColor
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 24.dp, top = 16.dp)
                .width(360.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = stringResource(R.string.ruler_calibration_card_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ruler_calibration_method),
                        style = MaterialTheme.typography.bodyMedium,
                        color = lineColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        TextButton(
                            onClick = { dropdownExpanded = true }
                        ) {
                            val label = when (targetMm) {
                                RulerPreferences.ID1_CARD_SHORT_MM -> stringResource(R.string.ruler_calibration_id1_short)
                                RulerPreferences.ID1_CARD_LONG_MM -> stringResource(R.string.ruler_calibration_id1_long)
                                else -> stringResource(R.string.ruler_calibration_custom) + " (${targetMm.toInt()}mm)"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ruler_calibration_id1_short)) },
                                onClick = {
                                    targetMm = RulerPreferences.ID1_CARD_SHORT_MM
                                    dropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ruler_calibration_id1_long)) },
                                onClick = {
                                    targetMm = RulerPreferences.ID1_CARD_LONG_MM
                                    dropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.ruler_calibration_custom)) },
                                onClick = {
                                    dropdownExpanded = false
                                    showCustomDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val calibratedPxPerMm = (currentBarX / targetMm).coerceAtLeast(1.0f)
                        val newScale = calibratedPxPerMm / basePxPerMm
                        RulerPreferences.setCalibrationScale(context, newScale)
                        RulerPreferences.setLastTargetMm(context, targetMm)
                        Toast.makeText(context, context.getString(R.string.ruler_calibration_success), Toast.LENGTH_SHORT).show()
                        onCalibrationSaved()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = lineColor,
                        contentColor = Color(0xFF00382E)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.ruler_calibration_finish),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = {
                        RulerPreferences.resetCalibration(context)
                        currentBarX = targetMm * basePxPerMm
                        Toast.makeText(context, context.getString(R.string.ruler_calibration_reset_success), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = stringResource(R.string.ruler_calibration_reset),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showCustomDialog) {
            AlertDialog(
                onDismissRequest = { showCustomDialog = false },
                title = { Text(stringResource(R.string.ruler_calibration_custom_title)) },
                text = {
                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(R.string.ruler_calibration_custom_hint)) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsed = customInputText.toFloatOrNull()
                            if (parsed != null && parsed > 5.0f && parsed < 600.0f) {
                                targetMm = parsed
                            }
                            showCustomDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.ruler_calibration_finish))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }
    }
}
