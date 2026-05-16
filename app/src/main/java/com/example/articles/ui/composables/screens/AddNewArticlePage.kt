package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.utils.ScreenSize
import com.example.articles.viewmodels.AddNewArticleAction
import com.example.articles.viewmodels.AddNewArticleViewModel
import com.example.articles.viewmodels.AddNewArticleState


@Composable
fun AddNewArticle(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    addNewArticleViewModel: AddNewArticleViewModel = hiltViewModel()
) {

    val state by addNewArticleViewModel.uiState.collectAsStateWithLifecycle()
    val screenSize = ScreenSize.getScreenSize()

//    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
//    val pickedLocationFlow = savedStateHandle?.getStateFlow<Location?>(KEY_LOCATION, null)
//    val pickedLocation = pickedLocationFlow?.collectAsState()?.value
//
//    val context = LocalContext.current
//    val keyboardController = LocalSoftwareKeyboardController.current
//
//    LaunchedEffect(state.ok) {
//        if (state.ok) onFinish()
//    }
//
//    LaunchedEffect(pickedLocation) {
//        pickedLocation?.let { location ->
//            addNewArticleViewModel.handleAction(AddNewArticleAction.RevalidateLocation(location))
//            savedStateHandle.remove<Location>(KEY_LOCATION)
//            savedStateHandle.remove<Int>(KEY_INDEX)
//        }
//    }
//
//    fun savePhoto(uri: Uri) {
//        addNewArticleViewModel.handleAction(AddNewArticleAction.UpdatePhoto(uri.toString()))
//    }
//
//    var uri by remember { mutableStateOf(addNewArticleViewModel.photoUri()) }
//    val cameraLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.TakePicture(),
//        onResult = { success ->
//            if (success) {
//                savePhoto(uri)
//            }
//        }
//    )
//
//    val pickImage = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        if (uri != null) {
//            val photoUri = copyUriToUri(
//                sourceUri = uri,
//                destinationUri = addNewArticleViewModel.photoUri(),
//                context = context
//            ) ?: return@rememberLauncherForActivityResult
//            savePhoto(photoUri)
//        }
//    }
//
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Toolbar(
            title = stringResource(id = R.string.add_new_article),
            leadingIcon = Icons.Default.ChevronLeft,
            onLeadingIconClick = onBackClick,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when (screenSize) {
                        ScreenSize.NORMAL -> Modifier
                        ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                        ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AddNewArticleItem(
                    state = state,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Button(
                onClick = {
                    addNewArticleViewModel.handleAction(AddNewArticleAction.SaveArticle)
                    onBackClick()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.save),
                    color = Color.White
                )
            }
        }
    }

}

@Composable
fun AddNewArticleItem(
    state: AddNewArticleState,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(top = 5.dp, bottom = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
//            Row {
//                Text(
//                    "Select Group of your article: "
//                )
//                DropdownMenu(
//                    expanded = true,
//                    onDismissRequest = { }
//                ) {
//                    state.groupNames.forEach { groupName ->
//                        DropdownMenuItem(
//                            text = {
//                                Text(text = groupName)
//                            },
//                            onClick = { }
//                        )
//                    }
//                }
//            }
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//            ) {
//                IconButton(
//                    onClick = onExpandIconClick
//                ) {
//                    Icon(
//                        imageVector =
//                            if (state.article.expanded) Icons.Filled.KeyboardArrowDown
//                            else Icons.Filled.ChevronRight,
//                        contentDescription =
//                            if (state.article.expanded) stringResource(id = R.string.collapse)
//                            else stringResource(id = R.string.expand),
//                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black
//                    )
//                }
            OutlinedTextField(
                state = state.articleName,
                placeholder = {
                    Text(text = stringResource(id = R.string.name))
                },
                label = {
                    Text(text = stringResource(id = R.string.name))
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                state = state.articleDescription,
                placeholder = {
                    Text(text = stringResource(id = R.string.article_description))
                },
                label = {
                    Text(text = stringResource(id = R.string.article_description))
                },
                modifier = Modifier.fillMaxWidth(),
                lineLimits = TextFieldLineLimits.MultiLine(
                    minHeightInLines = 2,
                    maxHeightInLines = 5
                )
            )
//            }
//            AnimatedVisibility(
//                visible = state.article.expanded,
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//                    IconButton(
//                        onClick = {
//                            onLocationPickerClick()
//                        }
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.LocationOn,
//                            contentDescription = stringResource(id = R.string.choose_location),
//                            tint =
//                                if (state.article.location != null) MaterialTheme.colorScheme.primary
//                                else if (isSystemInDarkTheme()) Color.White else Color.Black
//                        )
//                    }
//                    val isValueInteger = textValue.toDoubleOrNull()?.isInteger() ?: false
//                    OutlinedTextField(
//                        value = if (isValueInteger) textValue.toDouble().toInt()
//                            .toString() else textValue,
//                        onValueChange = { newValue ->
//                            textValue = newValue
//                            onQuantityChange(newValue.toDoubleOrNull())
//                        },
//                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//                        trailingIcon = {
//                            Box {
//                                TextButton(
//                                    onClick = { isDropDownMenuExpanded = true }
//                                ) {
//                                    Text(
//                                        text = stringResource(id = state.article.unit.abbreviation),
//                                        color = Color.Gray,
//                                    )
//                                }
//                                DropdownMenu(
//                                    expanded = isDropDownMenuExpanded,
//                                    onDismissRequest = {
//                                        isDropDownMenuExpanded = false
//                                    }
//                                ) {
//                                    ArticleUnit.entries.forEach { unit ->
//                                        DropdownMenuItem(
//                                            text = {
//                                                Text(text = stringResource(id = unit.abbreviation))
//                                            },
//                                            onClick = {
//                                                onArticleTypeChange(unit)
//                                                isDropDownMenuExpanded = false
//                                            }
//                                        )
//                                    }
//                                }
//                            }
//                        },
//                        label = {
//                            Text(text = stringResource(id = state.article.unit.label))
//                        },
//                        singleLine = true,
//                        maxLines = 1,
//                        modifier = Modifier.weight(1f)
//                    )
//                    IconButton(
//                        onClick = onOpenCameraClick,
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.CameraAlt,
//                            contentDescription = stringResource(id = R.string.take_a_photo),
//                            tint = if (state.article.isError) Color.Red else if (isSystemInDarkTheme()) Color.White else Color.Black
//                        )
//                    }
//                    IconButton(
//                        onClick = onChooseImageClick,
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.Photo,
//                            contentDescription = stringResource(id = R.string.choose_photo),
//                            tint = if (state.article.isError) Color.Red else if (isSystemInDarkTheme()) Color.White else Color.Black
//                        )
//                    }
//                }
//            }
//            if (state.article.expanded && state.article.location != null) {
//                Text(
//                    text = "${stringResource(id = R.string.location)} - (${state.article.location})",
//                    modifier = Modifier
//                        .padding(top = 12.dp)
//                        .align(Alignment.CenterHorizontally),
//                    color = MaterialTheme.colorScheme.primary,
//                )
//            }
//            if (state.article.expanded && state.article.photoUri != null) {
//                Column(
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    TextButton(
//                        onClick = onSeePhotoClick
//                    ) {
//                        Text(text = stringResource(id = R.string.tap_to_see_photo))
//                    }
//                }
//                if (state.article.photoVisible) {
//                    AsyncImage(
//                        model = state.article.photoUri,
//                        contentDescription = state.article.name,
//                        modifier = Modifier
//                            .fillMaxWidth(0.8f)
//                            .align(Alignment.CenterHorizontally)
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                }
//            }
        }
    }

}