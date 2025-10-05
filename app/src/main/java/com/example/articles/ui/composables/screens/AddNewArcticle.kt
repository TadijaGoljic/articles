package com.example.articles.ui.composables.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlusOne
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.articles.R
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.models.ArticleUnit
import com.example.articles.ui.models.UIArticle
import com.example.articles.viewmodels.AddNewArticleViewModel
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.KEY_INDEX
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.KEY_LOCATION
import com.example.articles.viewmodels.LocationData
import java.io.File

@Composable
fun AddNewArticle(
    onLocationPickerClick: () -> Unit,
    onBackButtonClicked: () -> Unit,
    onSeePhotoClick: (String) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    addNewArticleViewModel: AddNewArticleViewModel = hiltViewModel(),
    navController: NavHostController
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

    val context = LocalContext.current
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            File(context.cacheDir, "article.jpg")
        )
    }
    context.grantUriPermission(context.packageName, photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                val index = savedStateHandle?.get<Int>(KEY_INDEX)
                    ?: return@rememberLauncherForActivityResult
                addNewArticleViewModel.updatePhoto(photoUri.toString(), index)
            }
        }
    )

    LaunchedEffect(state.ok) {
        if (state.ok) {
            onFinish()
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val index =
                savedStateHandle?.get<Int>(KEY_INDEX) ?: return@rememberLauncherForActivityResult
            addNewArticleViewModel.updatePhoto(uri.toString(), index)
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
                    leadingIcon = Icons.Default.ChevronLeft,
                    onLeadingIconClick = onBackButtonClicked,
                    trailingIcon = Icons.Default.PlusOne,
                    onTrailingIconClick = addNewArticleViewModel::addNewArticle,
                )
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 18.dp)
                            .padding(bottom = 60.dp)
                            .widthIn(max = 450.dp)
                            .align(Alignment.TopCenter)
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
                                onOpenCameraClick = {
                                    savedStateHandle?.set("index", index)
                                    cameraLauncher.launch(photoUri)
                                },
                                onChooseImageClick = {
                                    savedStateHandle?.set("index", index)
                                    pickImage.launch("image/*")
                                },
                                onSeePhotoClick = onSeePhotoClick,
                                enableRemoveOption = state.articles.size > 1,
                                modifier = Modifier
                                    .padding(vertical = 5.dp)
                            )
                        }
                        AnimatedVisibility(visible = state.message != null) {
                            state.message?.let { error ->
                                Text(
                                    text = stringResource(error),
                                    color = Color.Red,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            addNewArticleViewModel.save()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.save),
                            color = Color.White
                        )
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
    onOpenCameraClick: () -> Unit,
    onSeePhotoClick: (String) -> Unit,
    onChooseImageClick: () -> Unit,
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
            .padding(bottom = 5.dp)
    ) {
        Column {
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
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    singleLine = true,
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
            AnimatedVisibility(
                visible = article.expanded,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            onLocationPickerClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(id = R.string.choose_location),
                            tint =
                                if (article.location != null) MaterialTheme.colorScheme.primary
                                else if (isSystemInDarkTheme()) Color.White else Color.Black
                        )
                    }
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            textValue = newValue
                            onQuantityChange(newValue.toDoubleOrNull())
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = {
                            Box {
                                TextButton(
                                    onClick = {
                                        isDropDownMenuExpanded = true
                                    }
                                ) {
                                    Text(
                                        text = stringResource(
                                            id = article.unit?.abbreviation
                                                ?: ArticleUnit.default.abbreviation
                                        ),
                                        color = Color.Gray,
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
                                                Text(text = stringResource(id = unit.abbreviation))
                                            },
                                            onClick = {
                                                onArticleTypeChange(unit)
                                                isDropDownMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(
                                    id = article.unit?.label ?: ArticleUnit.default.label
                                )
                            )
                        },
                        singleLine = true,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onOpenCameraClick,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = stringResource(id = R.string.take_a_photo),
                        )
                    }
                    IconButton(
                        onClick = onChooseImageClick,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Photo,
                            contentDescription = stringResource(id = R.string.choose_photo),
                        )
                    }
                }
            }
            if (article.expanded && article.location != null) {
                Text(
                    text = "${stringResource(id = R.string.location)} - (${article.location})",
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (article.expanded && article.photoUri != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = {
                            onSeePhotoClick(article.photoUri)
                        }
                    ) {
                        Text(text = stringResource(id = R.string.tap_to_see_photo))
                    }
                }
            }
        }
    }

}