package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.example.articles.ui.composables.utils.Toolbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.viewmodels.GroupDetailsAction
import com.example.articles.viewmodels.GroupDetailsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsPage(
    onAddArticle: (articleGroup: UIGroup) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    groupDetailsViewModel: GroupDetailsViewModel = hiltViewModel()
) {

    val state by groupDetailsViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    if (state.showEditDeleteBottonSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope
                    .launch { sheetState.hide() }
                    .invokeOnCompletion {
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                    }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomSheetRowItem(
                    icon = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit_article),
                    text = stringResource(id = R.string.edit_article),
                    onClick = {
                        // TODO Claude
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                    }
                )
                BottomSheetRowItem(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.remove_article),
                    text = stringResource(id = R.string.remove_article),
                    onClick = {
                        groupDetailsViewModel.handleAction(GroupDetailsAction.RemoveArticle)
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                    }
                )
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Toolbar(
                title = state.group.name,
                leadingIcon = Icons.Default.ChevronLeft,
                onLeadingIconClick = onBackClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.articles.forEach { article ->
                ArticleItem(
                    article = article,
                    onLongClick = {
                        groupDetailsViewModel.handleAction(
                            GroupDetailsAction.ArticleLongClick(article = article)
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    onAddArticle(state.group)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.add_new_article)
                    )
                    Text(
                        text = stringResource(id = R.string.add_new_article)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}

@Composable
fun ArticleItem(
    article: UIArticle,
    onLongClick: (UIArticle) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongClick(article) }
            )
    ) {
        Column {
            Text("Name: " + article.name.toString())
            Text("Description: " + article.description.toString())
            Text("Website link: " + article.websiteLink.toString())
            Text("Quantity: " + article.quantity.toString())
            Text("Unit: " + article.unit.toString())
            Text("Price per unit: " + article.pricePerUnit.toString())
            article.photoUri?.let { protoUri ->
                AsyncImage(
                    model = protoUri,
                    contentDescription = null
                )
            }
        }
    }
}
