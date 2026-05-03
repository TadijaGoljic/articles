package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.models.UIArticle
import com.example.articles.ui.composables.utils.ProgressButton
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.viewmodels.CreateGroupViewModel
import com.example.articles.viewmodels.GroupAction


@Composable
fun CreateGroup(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    createGroupViewModel: CreateGroupViewModel = hiltViewModel()
) {

    val state by createGroupViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            Toolbar(
                title = stringResource(id = R.string.create_group),
                leadingIcon = Icons.Default.ChevronLeft,
                onLeadingIconClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(shape = RoundedCornerShape(10.dp))
                    .background(color = MaterialTheme.colorScheme.surface)
            ) {
                OutlinedTextField(
                    state = state.name,
                    label = {
                        Text(text = stringResource(id = R.string.name))
                    },
                    isError = state.name.text.toString() in state.groupNames,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .padding(bottom = 8.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.group_screen_message),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.ungroupedArticles) { article ->
                    NewArticleItem(
                        article = article,
                        onClick = { checked ->
                            createGroupViewModel.handleAction(GroupAction.ArticleClicked(article, checked))
                        },
                        checked = article in state.articlesToAdd,
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(shape = RoundedCornerShape(size = 10.dp))
                    )
                }
            }
            ProgressButton(
                text = stringResource(id = R.string.save),
                onButtonClick = {
                    createGroupViewModel.handleAction(
                        GroupAction.CreateGroup(state.name.text.toString())
                    )
                },
                isLoading = state.isLoading,
                enabled = state.name.text.isNotEmpty() &&
                          state.name.text.toString() !in state.groupNames,
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp),
            )
        }
    }

}

@Composable
private fun NewArticleItem(
    article: UIArticle,
    onClick: (Boolean) -> Unit,
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .clickable { onClick(!checked) }
                .background(
                    color =
                        if (checked) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                )
        ) {
            Text(
                text = article.name ?: "",
                textAlign = TextAlign.Center,
                color =
                    if (checked) Color.White
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxSize()
            )
            AsyncImage(
                model = article.photoUri,
                contentDescription = article.name,
                modifier = Modifier
                    .size(100.dp),
                error = painterResource(id = R.drawable.image_unavailable)
            )
        }
    }
}