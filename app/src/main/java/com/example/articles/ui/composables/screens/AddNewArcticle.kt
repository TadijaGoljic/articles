package com.example.articles.ui.composables.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.articles.R
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.viewmodels.AddNewArticleViewModel

@Composable
fun AddNewArticle(
    onBackButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    addNewArticleViewModel: AddNewArticleViewModel = hiltViewModel()
) {

    val state by addNewArticleViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.maxValue) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Scaffold { innerPadding ->
        Surface(
            modifier = modifier.padding(innerPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Toolbar(
                    title = stringResource(id = R.string.add_new_article),
                    onBackButtonClick = {
                        onBackButtonClicked()
                    }
                )
                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    state.articles.forEachIndexed { index, article ->
                        Row {
                            ArticleItem(
                                articleName = article.name,
                                onNameChange = {
                                    addNewArticleViewModel.updateName(index, it)
                                },
                                onRemoveClick = {
                                    addNewArticleViewModel.remove(index)
                                },
                                showRemoveOption = state.articles.size > 1,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                    Row {
                        Button(
                            onClick = {
                                addNewArticleViewModel.addNewArticle()
                            }
                        ) {
                            Text("+")
                        }
                        Button(
                            onClick = {
                                addNewArticleViewModel.save()
                            }
                        ) {
                            Text(text = stringResource(id = R.string.save))
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun ArticleItem(
    articleName: String,
    onNameChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
    showRemoveOption: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = articleName,
            onValueChange = {
                onNameChange(it)
            },
            placeholder = {
                Text(text = stringResource(id = R.string.name))
            },
            trailingIcon = {
                Row {
                    IconButton(
                        onClick = {

                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(id = R.string.choose_location)
                        )
                    }
                    IconButton(
                        onClick = {

                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(id = R.string.take_a_photo)
                        )
                    }
                }
            },
            modifier = modifier
        )
        IconButton(
            onClick = {
                onRemoveClick()
            },
            enabled = showRemoveOption,
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = stringResource(id = R.string.remove_article)
            )
        }
    }
}