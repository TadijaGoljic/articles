package com.example.articles.ui.composables.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.viewmodels.CreateGroupViewModel
import com.example.articles.viewmodels.GroupAction

@Composable
fun CreateGroup(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    createGroupViewModel: CreateGroupViewModel = hiltViewModel()
) {

    val state by createGroupViewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Toolbar(
                title = stringResource(id = R.string.create_group)
            )
            OutlinedTextField(
                value = state.name,
                onValueChange = { createGroupViewModel.handleAction(GroupAction.UpdateName(it)) },
                label = {
                    Text(text = stringResource(id = R.string.name))
                },
                isError = state.name in state.groupNames
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        fontSize = 16.sp
                    )
                }
                Button(
                    onClick = {
                        createGroupViewModel.handleAction(GroupAction.CreateGroup)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.save_group),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }

}