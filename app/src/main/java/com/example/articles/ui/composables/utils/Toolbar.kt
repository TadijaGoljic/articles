package com.example.articles.ui.composables.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.articles.R

@Composable
fun Toolbar(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onLeadingIconClick: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    trailingIconEnabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        if (leadingIcon != null) {
            IconButton(
                onClick = {
                    onLeadingIconClick?.invoke()
                },
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterStart),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = color
                )
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 25.sp,
            color = color
        )
        if (trailingIcon != null) {
            IconButton(
                onClick = {
                    onTrailingIconClick?.invoke()
                },
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterEnd),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = color
                ),
                enabled = trailingIconEnabled
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = stringResource(id = R.string.add)
                )
            }
        }
    }
}