package com.example.articles.ui.composables.utils

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressButton(
    onButtonClick: () -> Unit,
    isLoading: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onButtonClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
