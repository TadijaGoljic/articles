package com.example.articles.ui.composables.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


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
