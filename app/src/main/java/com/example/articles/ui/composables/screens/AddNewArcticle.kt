package com.example.articles.composables.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.articles.R
import com.example.articles.composables.utils.Toolbar
import com.example.articles.viewmodels.AddNewArticleViewModel

@Composable
fun AddNewArticle(
    onBackButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    addNewArticleViewModel: AddNewArticleViewModel = hiltViewModel()
) {

    Surface(
        modifier = modifier
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
        }
    }

}