package com.example.articles.ui.composables.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.models.ArticleUnit
import com.example.articles.models.Location
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.ui.composables.utils.ProgressButton
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.utils.ScreenSize
import com.example.articles.utils.copyUriToUri
import com.example.articles.viewmodels.AddNewArticleAction
import com.example.articles.viewmodels.AddNewArticleState
import com.example.articles.viewmodels.AddNewArticleViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewArticle(
    onBackClick: () -> Unit,
    onLocationPickerClick: (Location?) -> Unit,
    modifier: Modifier = Modifier,
    addNewArticleViewModel: AddNewArticleViewModel
) {

    val state by addNewArticleViewModel.uiState.collectAsStateWithLifecycle()
    val screenSize = ScreenSize.getScreenSize()
    val context = LocalContext.current

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let {
                addNewArticleViewModel.handleAction(AddNewArticleAction.UpdatePhoto(it.toString()))
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = addNewArticleViewModel.photoUri()
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val dest = addNewArticleViewModel.photoUri()
            val copied = copyUriToUri(sourceUri = uri, destinationUri = dest, context = context)
            if (copied != null) {
                addNewArticleViewModel.handleAction(AddNewArticleAction.UpdatePhoto(copied.toString()))
            }
        }
    }

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.okToSave) {
        if (state.okToSave) onBackClick()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Toolbar(
            title = stringResource(id = if (state.isEditMode) R.string.edit_article else R.string.add_new_article),
            leadingIcon = Icons.Default.ChevronLeft,
            onLeadingIconClick = onBackClick,
            color = MaterialTheme.colorScheme.onBackground,
        )
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .then(
                    when (screenSize) {
                        ScreenSize.NORMAL -> Modifier
                        ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                        ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                    }
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                AddNewArticleItem(
                    state = state,
                    onLocationPickerClick = { onLocationPickerClick(state.article.location) },
                    onClearLocation = {
                        addNewArticleViewModel.handleAction(AddNewArticleAction.ClearLocation)
                    },
                    onPhotoClick = {
                        addNewArticleViewModel.handleAction(AddNewArticleAction.ShowBottomSheet)
                    },
                    onClearPhoto = {
                        addNewArticleViewModel.handleAction(AddNewArticleAction.ClearPhoto)
                    },
                    modifier = Modifier.padding(vertical = 5.dp),
                    showUnitsDropdownMenu = {
                        addNewArticleViewModel.handleAction(AddNewArticleAction.ShowUnitsDropdownMenu)
                    },
                    hideUnitsDropdownMenu = {
                        addNewArticleViewModel.handleAction(AddNewArticleAction.HideUnitsDropdownMenu)
                    },
                    onArticleTypeChange = { unit ->
                        addNewArticleViewModel.handleAction(
                            AddNewArticleAction.UpdateArticleUnit(unit = unit)
                        )
                    }
                )
                Row {
                    Button(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 4.dp)
                            .weight(weight = 1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    ProgressButton(
                        onButtonClick = {
                            addNewArticleViewModel.handleAction(AddNewArticleAction.SaveArticle)
                        },
                        isLoading = state.isLoading,
                        text = stringResource(id = R.string.save),
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 8.dp, start = 4.dp, end = 8.dp)
                            .weight(weight = 1f),
                    )
                }
            }
        }
        if (state.isBottomSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = {
                    coroutineScope
                        .launch { sheetState.hide() }
                        .invokeOnCompletion {
                            addNewArticleViewModel.handleAction(AddNewArticleAction.HideBottomSheet)
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
                        icon = Icons.Default.CameraAlt,
                        contentDescription = stringResource(id = R.string.take_a_photo),
                        text = stringResource(id = R.string.take_a_photo),
                        onClick = {
                            addNewArticleViewModel.handleAction(AddNewArticleAction.HideBottomSheet)
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                    BottomSheetRowItem(
                        icon = Icons.Default.Image,
                        contentDescription = stringResource(id = R.string.add_photo),
                        text = stringResource(id = R.string.add_photo),
                        onClick = {
                            addNewArticleViewModel.handleAction(AddNewArticleAction.HideBottomSheet)
                            galleryLauncher.launch("image/*")
                        }
                    )
                }
            }
        }

    }
}

@Composable
fun AddNewArticleItem(
    state: AddNewArticleState,
    onLocationPickerClick: () -> Unit,
    onClearLocation: () -> Unit,
    showUnitsDropdownMenu: () -> Unit,
    hideUnitsDropdownMenu: () -> Unit,
    onArticleTypeChange: (ArticleUnit) -> Unit,
    onPhotoClick: () -> Unit,
    onClearPhoto: () -> Unit,
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
            OutlinedTextField(
                state = state.articleName,
                placeholder = {
                    Text(text = stringResource(id = R.string.name))
                },
                label = {
                    val errorColor = MaterialTheme.colorScheme.error
                    Text(text = buildAnnotatedString {
                        append(stringResource(id = R.string.name))
                        append(" ")
                        withStyle(SpanStyle(color = errorColor)) { append("*") }
                    })
                },
                isError = state.articleNameError,
                supportingText = {
                    if (state.articleNameError) {
                        Text(
                            text = stringResource(id = R.string.provide_name),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.error)) {
                                    append("*")
                                }
                                append(" ")
                                append(stringResource(id = R.string.required))
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    state = state.articleQuantity,
                    placeholder = {
                        Text(text = stringResource(id = state.article.unit.label))
                    },
                    label = {
                        Text(text = stringResource(id = state.article.unit.label))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Box {
                            TextButton(
                                onClick = showUnitsDropdownMenu
                            ) {
                                Text(
                                    text = stringResource(id = state.article.unit.abbreviation),
                                    color = Color.Gray,
                                )
                            }
                            DropdownMenu(
                                expanded = state.unitsDropdownMenuVisible,
                                onDismissRequest = hideUnitsDropdownMenu
                            ) {
                                ArticleUnit.entries.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = stringResource(id = unit.abbreviation))
                                        },
                                        onClick = {
                                            onArticleTypeChange(unit)
                                            hideUnitsDropdownMenu()
                                        }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    state = state.articlePricePerUnit,
                    placeholder = {
                        Text(text = stringResource(id = R.string.price_per_unit))
                    },
                    label = {
                        Text(text = stringResource(id = R.string.price_per_unit))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                state = state.articleWebsiteLink,
                placeholder = {
                    Text(text = stringResource(id = R.string.website_link))
                },
                label = {
                    Text(text = stringResource(id = R.string.website_link))
                },
                isError = state.articleWebsiteLinkError,
                supportingText = {
                    if (state.articleWebsiteLinkError) {
                        Text(
                            text = stringResource(id = R.string.invalid_website_link),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
            DataRow(
                labelText = state.locationString,
                buttonText =
                    if (state.article.location != null) stringResource(id = R.string.change_location)
                    else stringResource(id = R.string.add_location),
                onButtonClick = onLocationPickerClick,
                onDeleteClick = {
                    if (state.article.location != null) onClearLocation()
                },
                labelTextLoading = state.isLocationStringLoading,
                modifier = Modifier.padding(top = 16.dp)
            )
            DataRow(
                labelText =
                    if (state.article.photoUri != null) stringResource(id = R.string.photo)
                    else null,
                buttonText =
                    if (state.article.photoUri != null) stringResource(id = R.string.change_photo)
                    else stringResource(id = R.string.add_photo),
                onButtonClick = onPhotoClick,
                onDeleteClick = {
                    if (state.article.photoUri != null) onClearPhoto()
                },
                labelTextLoading = false,
                modifier = Modifier.padding(top = 8.dp)
            )
            state.article.photoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 300.dp)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }

}

@Composable
fun DataRow(
    buttonText: String,
    onButtonClick: () -> Unit,
    labelText: String?,
    labelTextLoading: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        if (labelTextLoading) {
            CircularProgressIndicator()
        } else {
            if (labelText != null) {
                Text(
                    text = labelText,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onButtonClick
                ) {
                    Text(text = buttonText)
                }
                IconButton(
                    onClick = onDeleteClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Button(
                    onClick = onButtonClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = buttonText)
                }
            }
        }
    }
}