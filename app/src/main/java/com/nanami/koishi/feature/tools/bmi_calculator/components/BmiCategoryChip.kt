package com.nanami.koishi.feature.tools.bmi_calculator.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCategory

@Composable
fun BmiCategoryChip(
    category: BmiCategory,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val bandColors = rememberBmiBandColors()
    val color = bandColors.of(category)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = if (emphasized) 0.24f else 0.16f)
    ) {
        Text(
            text = stringResource(category.labelRes),
            style = if (emphasized) {
                MaterialTheme.typography.labelLarge
            } else {
                MaterialTheme.typography.labelMedium
            },
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(
                horizontal = if (emphasized) 10.dp else 7.dp,
                vertical = if (emphasized) 4.dp else 2.dp
            )
        )
    }
}
