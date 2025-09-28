package com.example.articles.composables.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import com.example.articles.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Toolbar(
    title: String,
    onBackButtonClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (onBackButtonClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = stringResource(id = R.string.back),
                modifier = Modifier
                    .clickable { onBackButtonClick() }
                    .align(Alignment.CenterStart)
            )
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 25.sp
        )
    }
}