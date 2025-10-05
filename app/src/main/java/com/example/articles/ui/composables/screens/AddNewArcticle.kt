package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.articles.R
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.models.ArticleUnit
import com.example.articles.ui.models.UIArticle
import com.example.articles.viewmodels.AddNewArticleViewModel
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.KEY_INDEX
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.KEY_LOCATION
import com.example.articles.viewmodels.LocationData

@Composable
fun AddNewArticle(
    onLocationPickerClick: () -> Unit,
    onBackButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    addNewArticleViewModel: AddNewArticleViewModel = hiltViewModel(),
    navController: androidx.navigation.NavHostController
) {

    val state by addNewArticleViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.maxValue) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val pickedLocationFlow = savedStateHandle?.getStateFlow<LocationData?>(KEY_LOCATION, null)
    val pickedLocation = pickedLocationFlow?.collectAsState()?.value

    LaunchedEffect(pickedLocation) {
        pickedLocation?.let { location ->
            val index = savedStateHandle.get<Int>(KEY_INDEX) ?: return@let
            addNewArticleViewModel.revalidateLocations(location, index)
            savedStateHandle.remove<LocationData>(KEY_LOCATION)
            savedStateHandle.remove<Int>(KEY_INDEX)
        }
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
                    onBackButtonClick = onBackButtonClicked
                )
                Column(
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    state.articles.forEachIndexed { index, article ->
                        ArticleItem(
                            article = article,
                            onNameChange = {
                                if (it.length < 30) {
                                    addNewArticleViewModel.updateName(index, it)
                                }
                            },
                            onRemoveClick = {
                                addNewArticleViewModel.remove(index)
                            },
                            onExpandIconClick = {
                                addNewArticleViewModel.toggleState(index)
                            },
                            onArticleTypeChange = {
                                addNewArticleViewModel.updateType(index, it)
                            },
                            onQuantityChange = {
                                addNewArticleViewModel.updateQuantity(index, it)
                            },
                            onLocationPickerClick = {
                                savedStateHandle?.set("index", index)
                                onLocationPickerClick()
                            },
                            enableRemoveOption = state.articles.size > 1,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }

                    Row {
                        Button(onClick = { addNewArticleViewModel.addNewArticle() }) {
                            Text("+")
                        }
                        Button(onClick = { addNewArticleViewModel.save() }) {
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
    article: UIArticle,
    onNameChange: (String) -> Unit,
    onRemoveClick: () -> Unit,
    onExpandIconClick: () -> Unit,
    onArticleTypeChange: (ArticleUnit) -> Unit,
    onQuantityChange: (Double?) -> Unit,
    onLocationPickerClick: () -> Unit,
    enableRemoveOption: Boolean,
    modifier: Modifier = Modifier
) {

    var isDropDownMenuExpanded by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf(article.quantity?.toString() ?: "") }

    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.background)
            .padding(vertical = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onExpandIconClick
                ) {
                    Icon(
                        imageVector =
                            if (article.expanded) Icons.Filled.KeyboardArrowDown
                            else Icons.Filled.ChevronRight,
                        contentDescription =
                            if (article.expanded) stringResource(id = R.string.collapse)
                            else stringResource(id = R.string.expand)
                    )
                }
                OutlinedTextField(
                    value = article.name,
                    onValueChange = onNameChange,
                    label = {
                        Text(text = stringResource(id = R.string.name))
                    },
                    modifier = modifier,
                )
                IconButton(
                    onClick = onRemoveClick,
                    enabled = enableRemoveOption,
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(id = R.string.remove_article)
                    )
                }
            }
            if (article.expanded) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        TextButton(
                            onClick = {
                                isDropDownMenuExpanded = true
                            }
                        ) {
                            Text(
                                text = stringResource(id = article.unit?.label ?: ArticleUnit.default.label),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = isDropDownMenuExpanded,
                            onDismissRequest = {
                                isDropDownMenuExpanded = false
                            }
                        ) {
                            ArticleUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = {
                                        Text(text = stringResource(id = unit.label))
                                    },
                                    onClick = {
                                        onArticleTypeChange(unit)
                                        isDropDownMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            textValue = newValue
                            onQuantityChange(newValue.toDoubleOrNull())
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            Text(text = stringResource(id = article.unit?.abbreviation ?: ArticleUnit.default.abbreviation))
                        },
                        label = {
                            Text(text = stringResource(id = article.unit?.label ?: ArticleUnit.default.label))
                        },
                        singleLine = true,
                        modifier = Modifier.width(200.dp)
                    )
                    IconButton(
                        onClick = {
                            onLocationPickerClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(id = R.string.choose_location),
                            tint = if (article.location != null) Color.Blue else Color.Unspecified
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
            }
            if (article.expanded && article.location != null) {
                Text(
                    text = "${stringResource(id = R.string.location)} - ${article.location}",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }

}