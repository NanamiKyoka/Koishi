package com.nanami.koishi.feature.tools.ruler.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.ruler.RulerTargetMode

@Composable
fun RulerBottomBar(
    currentMode: RulerTargetMode,
    onModeSelected: (RulerTargetMode) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        NavigationBarItem(
            selected = currentMode == RulerTargetMode.RULER,
            onClick = { onModeSelected(RulerTargetMode.RULER) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Straighten,
                    contentDescription = null
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.ruler_straight_title),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )
        NavigationBarItem(
            selected = currentMode == RulerTargetMode.PROTRACTOR,
            onClick = { onModeSelected(RulerTargetMode.PROTRACTOR) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.SquareFoot,
                    contentDescription = null
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.ruler_protractor_title),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )
    }
}
