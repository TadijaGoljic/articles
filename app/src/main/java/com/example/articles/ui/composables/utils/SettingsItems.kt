package com.example.articles.ui.composables.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private val avatarColors = listOf(
    Color(0xFF1E8A52), Color(0xFFE8891C), Color(0xFF3B7DD8),
    Color(0xFFC2477A), Color(0xFF7A5AD1), Color(0xFF0F9AA0)
)

@Composable
fun EmailAvatar(
    email: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    fontSize: TextUnit = 16.sp
) {
    val initial = email.firstOrNull()?.uppercaseChar() ?: '?'
    val avatarColor = avatarColors[email.length % avatarColors.size]

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(avatarColor)
    ) {
        Text(
            text = initial.toString(),
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun UserInfoItem(
    email: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = 56.dp)
            .padding(vertical = 8.dp)
    ) {
        EmailAvatar(email = email)
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

enum class Position {
    TOP, MIDDLE, BOTTOM, SINGLE
}

private fun Position.shape(): Shape = when (this) {
    Position.SINGLE -> RoundedCornerShape(size = 20.dp)
    Position.TOP -> RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Position.MIDDLE -> RoundedCornerShape(size = 0.dp)
    Position.BOTTOM -> RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
}

@Composable
private fun SettingsRow(
    label: String,
    position: Position,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
    trailing: @Composable () -> Unit,
) {
    val accent = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .clip(position.shape())
            .background(color = MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            icon?.let {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            trailing()
        }
        if (position != Position.SINGLE && position != Position.BOTTOM) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = if (icon != null) 66.dp else 16.dp)
            )
        }
    }
}

@Composable
fun SettingsItem(
    label: String,
    onItemClick: () -> Unit,
    position: Position,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDestructive: Boolean = false,
) {
    SettingsRow(
        label = label,
        position = position,
        onClick = onItemClick,
        icon = icon,
        isDestructive = isDestructive,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsItemWithSwitch(
    label: String,
    onItemClick: (Boolean) -> Unit,
    position: Position,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    SettingsRow(
        label = label,
        position = position,
        onClick = { onItemClick(!isChecked) },
        icon = icon,
        modifier = modifier
    ) {
        Switch(
            checked = isChecked,
            onCheckedChange = onItemClick
        )
    }
}
