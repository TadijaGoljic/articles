package com.example.articles.ui.composables.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun Toolbar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    trailingIconBadge: String? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
) {
    val hasIcons = leadingIcon != null || trailingIcon != null
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (hasIcons) 12.dp else 24.dp,
                vertical = if (hasIcons) 8.dp else 20.dp
            )
    ) {
        leadingIcon?.let {
            RoundIconButton(
                icon = it,
                contentDescription = null,
                onClick = { onLeadingIconClick?.invoke() }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (leadingIcon != null) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.headlineSmall,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailingIcon?.let {
            RoundIconButton(
                icon = it,
                contentDescription = null,
                badge = trailingIconBadge.takeIf { value -> !value.equals("0", ignoreCase = true) },
                onClick = { onTrailingIconClick?.invoke() }
            )
        }
    }
}
