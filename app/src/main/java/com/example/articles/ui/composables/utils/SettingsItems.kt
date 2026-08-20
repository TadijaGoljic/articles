package com.example.articles.ui.composables.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Badge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private val avatarColors = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350),
    Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFFF7043)
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
            .clip(RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp)
            .height(56.dp)
    ) {
        EmailAvatar(email = email)
        Text(
            text = email,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

enum class Position {
    TOP, MIDDLE, BOTTOM, SINGLE
}


@Composable
fun SettingsItem(
    label: String,
    onItemClick: () -> Unit,
    position: Position,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(
                when (position) {
                    Position.SINGLE -> RoundedCornerShape(size = 10.dp)
                    Position.TOP -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    Position.MIDDLE -> RoundedCornerShape(size = 0.dp)
                    Position.BOTTOM -> RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                }
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .clickable {
                onItemClick()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .height(height = 48.dp)
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        if (position != Position.SINGLE && position != Position.BOTTOM) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SettingsItemWithSwitch(
    label: String,
    onItemClick: (Boolean) -> Unit,
    position: Position,
    isChecked: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(
                when (position) {
                    Position.SINGLE -> RoundedCornerShape(size = 10.dp)
                    Position.TOP -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    Position.MIDDLE -> RoundedCornerShape(size = 0.dp)
                    Position.BOTTOM -> RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                }
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .clickable {
                onItemClick(!isChecked)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .height(height = 48.dp)
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isChecked,
                onCheckedChange = onItemClick
            )
        }
        if (position != Position.SINGLE && position != Position.BOTTOM) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SettingsItemWithBadge(
    label: String,
    onItemClick: () -> Unit,
    badgeValue: String?,
    position: Position,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(
                when (position) {
                    Position.SINGLE -> RoundedCornerShape(size = 10.dp)
                    Position.TOP -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    Position.MIDDLE -> RoundedCornerShape(size = 0.dp)
                    Position.BOTTOM -> RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                }
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .clickable {
                onItemClick()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .height(height = 48.dp)
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (badgeValue != null && !badgeValue.equals("0", ignoreCase = true)) {
                Badge(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp),
                ) {
                    Text(text = badgeValue)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        if (position != Position.SINGLE && position != Position.BOTTOM) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}