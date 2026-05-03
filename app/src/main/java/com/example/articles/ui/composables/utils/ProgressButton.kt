package com.example.articles.ui.composables.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        modifier = modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .size(48.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Text(
                text = text,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}