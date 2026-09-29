package com.example.articles.ui.composables.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
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
import com.example.articles.models.UILocation
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.ui.composables.utils.ProgressButton
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.utils.ScreenSize
import com.example.articles.utils.copyUriToUri
import com.example.articles.viewmodels.AddNewArticleAction
import com.example.articles.viewmodels.AddNewArticleState
import com.example.articles.viewmodels.AddNewArticleViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.articles.ui.composables.utils.SectionHeader
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddNewArticle(
    onBackClick: () -> Unit,
    onLocationPickerClick: (UILocation?) -> Unit,
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
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .then(
                    when (screenSize) {
                        ScreenSize.NORMAL -> Modifier
                        ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                        ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                    }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AddNewArticleItem(
                state = state,
                onLocationPickerClick = { onLocationPickerClick(state.article.location) },
                onClearLocation = {
                    addNewArticleViewModel.handleAction(AddNewArticleAction.ClearLocation)
                },
                onDeadlinePickerClick = {
                    addNewArticleViewModel.handleAction(AddNewArticleAction.ShowDeadlineDatePicker)
                },
                onClearDateTo = {
                    addNewArticleViewModel.handleAction(AddNewArticleAction.ClearDateTo)
                },
                onPhotoClick = {
                    addNewArticleViewModel.handleAction(AddNewArticleAction.ShowBottomSheet)
                },
                onClearPhoto = {
                    addNewArticleViewModel.handleAction(AddNewArticleAction.ClearPhoto)
                },
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
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .weight(weight = 1f)
                    .heightIn(min = 48.dp)
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
            ProgressButton(
                onButtonClick = {
                    addNewArticleViewModel.handleAction(AddNewArticleAction.SaveArticle)
                },
                isLoading = state.isLoading,
                text = stringResource(id = R.string.save),
                modifier = Modifier.weight(weight = 1f),
            )
        }
        if (state.showDeadlineDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = state.article.dateTo?.time
            )
            DatePickerDialog(
                onDismissRequest = {
                    addNewArticleViewModel.handleAction(AddNewArticleAction.HideDeadlineDatePicker)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                addNewArticleViewModel.handleAction(
                                    AddNewArticleAction.UpdateDateTo(Date(millis))
                                )
                            }
                        }
                    ) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            addNewArticleViewModel.handleAction(AddNewArticleAction.HideDeadlineDatePicker)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
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
    onDeadlinePickerClick: () -> Unit,
    onClearDateTo: () -> Unit,
    showUnitsDropdownMenu: () -> Unit,
    hideUnitsDropdownMenu: () -> Unit,
    onArticleTypeChange: (ArticleUnit) -> Unit,
    onPhotoClick: () -> Unit,
    onClearPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        FormSection(title = stringResource(id = R.string.article_details)) {
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
                        Text(text = stringResource(id = R.string.provide_name))
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
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
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
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    state = state.price,
                    placeholder = {
                        Text(text = stringResource(id = R.string.price))
                    },
                    label = {
                        Text(text = stringResource(id = R.string.price))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        FormSection(title = stringResource(id = R.string.additional_info)) {
            OutlinedTextField(
                state = state.articleWebsiteLink,
                placeholder = {
                    Text(text = stringResource(id = R.string.website_link))
                },
                label = {
                    Text(text = stringResource(id = R.string.website_link))
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Language, contentDescription = null)
                },
                isError = state.articleWebsiteLinkError,
                supportingText =
                    if (state.articleWebsiteLinkError) {
                        { Text(text = stringResource(id = R.string.invalid_website_link)) }
                    } else null,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                state = state.articleDescription,
                placeholder = {
                    Text(text = stringResource(id = R.string.article_description))
                },
                label = {
                    Text(text = stringResource(id = R.string.article_description))
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                lineLimits = TextFieldLineLimits.MultiLine(
                    minHeightInLines = 2,
                    maxHeightInLines = 5
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            DataRow(
                icon = Icons.Default.LocationOn,
                title = stringResource(id = R.string.location),
                labelText = state.locationString,
                buttonText =
                    if (state.article.location != null) stringResource(id = R.string.change_location)
                    else stringResource(id = R.string.add_location),
                onButtonClick = onLocationPickerClick,
                onDeleteClick = {
                    if (state.article.location != null) onClearLocation()
                },
                labelTextLoading = state.isLocationStringLoading,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 68.dp)
            )
            DataRow(
                icon = Icons.Default.Event,
                title = stringResource(id = R.string.deadline),
                labelText = state.article.dateTo?.let { dateTo ->
                    remember(dateTo) {
                        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(dateTo)
                    }
                },
                buttonText =
                    if (state.article.dateTo != null) stringResource(id = R.string.change_deadline)
                    else stringResource(id = R.string.add_deadline),
                onButtonClick = onDeadlinePickerClick,
                onDeleteClick = {
                    if (state.article.dateTo != null) onClearDateTo()
                },
                labelTextLoading = false,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(start = 68.dp)
            )
            DataRow(
                icon = Icons.Default.Image,
                title = stringResource(id = R.string.photo),
                labelText =
                    if (state.article.photoUri != null) stringResource(id = R.string.change_photo)
                    else null,
                buttonText =
                    if (state.article.photoUri != null) stringResource(id = R.string.change_photo)
                    else stringResource(id = R.string.add_photo),
                onButtonClick = onPhotoClick,
                onDeleteClick = {
                    if (state.article.photoUri != null) onClearPhoto()
                },
                labelTextLoading = false,
            )
            state.article.photoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 260.dp)
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
        }
    }

}

@Composable
private fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(text = title)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun DataRow(
    icon: ImageVector,
    title: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    labelText: String?,
    labelTextLoading: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onButtonClick)
            .heightIn(min = 68.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (labelTextLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(16.dp)
                )
            } else {
                Text(
                    text = labelText ?: buttonText,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (labelText != null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (labelText != null && !labelTextLoading) {
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
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = buttonText,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
