package com.example.articles.ui.composables.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Toolbar(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    trailingIconBadge: String? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        leadingIcon?.let {
            ToolbarIcon(
                icon = it,
                modifier = Modifier.align(Alignment.CenterStart),
                color = color,
                onClick = onLeadingIconClick
            )
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 25.sp,
            color = color
        )
        trailingIcon?.let {
            ToolbarIcon(
                icon = it,
                modifier = Modifier.align(Alignment.CenterEnd),
                color = color,
                badge = trailingIconBadge.takeIf { value -> !value.equals("0", ignoreCase = true) },
                onClick = onTrailingIconClick
            )
        }
    }
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color,
    badge: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val content = @Composable {
        IconButton(
            onClick = { onClick?.invoke() },
            modifier = Modifier.size(24.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = color
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }

    Box(
        modifier = modifier
    ) {
        if (badge != null) {
            BadgedBox(
                badge = {
                    Badge {
                        Text(badge)
                    }
                }
            ) {
                content()
            }
        } else {
            content()
        }
    }
}