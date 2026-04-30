package com.example.articles.ui.composables.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.models.ArticleUnit
import com.example.articles.models.UIArticle
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.utils.ScreenSize
import com.example.articles.ui.utils.copyUriToUri
import com.example.articles.ui.utils.isInteger
import com.example.articles.viewmodels.AddNewArticleViewModel
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.KEY_INDEX
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.KEY_LOCATION
import com.example.articles.models.Location


@Composable
fun AddNewArticle(
    onLocationPickerClick: () -> Unit,
    onBackButtonClicked: () -> Unit,
    onFinish: () -> Unit,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    addNewArticleViewModel: AddNewArticleViewModel = hiltViewModel()
) {

    val state by addNewArticleViewModel.uiState.collectAsState()
    val screenSize = ScreenSize.getScreenSize()

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val pickedLocationFlow = savedStateHandle?.getStateFlow<Location?>(KEY_LOCATION, null)
    val pickedLocation = pickedLocationFlow?.collectAsState()?.value

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.ok) {
        if (state.ok) onFinish()
    }

    LaunchedEffect(pickedLocation) {
        pickedLocation?.let { location ->
            addNewArticleViewModel.revalidateLocations(location)
            savedStateHandle.remove<Location>(KEY_LOCATION)
            savedStateHandle.remove<Int>(KEY_INDEX)
        }
    }

    fun savePhoto(uri: Uri) {
        addNewArticleViewModel.updatePhoto(uri.toString())
    }

    var uri by remember { mutableStateOf(addNewArticleViewModel.photoUri()) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                savePhoto(uri)
            }
        }
    )

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val photoUri = copyUriToUri(
                sourceUri = uri,
                destinationUri = addNewArticleViewModel.photoUri(),
                context = context
            ) ?: return@rememberLauncherForActivityResult
            savePhoto(photoUri)
        }
    }

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
            onLeadingIconClick = onBackButtonClicked,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = if (keyboardController != null) Modifier.padding() else Modifier
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
                    article = state.article,
                    onNameChange = {
                        if (it.isEmpty()) {
                            addNewArticleViewModel.updateName(null)
                        } else if (it.length < 30) {
                            addNewArticleViewModel.updateName(it)
                        }
                    },
                    onExpandIconClick = {
                        addNewArticleViewModel.toggleExpanded()
                    },
                    onArticleTypeChange = {
                        addNewArticleViewModel.updateType(it)
                    },
                    onQuantityChange = {
                        addNewArticleViewModel.updateQuantity(it)
                    },
                    onLocationPickerClick = {
                        onLocationPickerClick()
                    },
                    onOpenCameraClick = {
                        uri = addNewArticleViewModel.photoUri()
                        cameraLauncher.launch(uri)
                    },
                    onChooseImageClick = {
                        pickImage.launch("image/*")
                    },
                    onSeePhotoClick = {
                        addNewArticleViewModel.togglePhotoVisibility()
                    },
                    modifier = Modifier
                        .padding(vertical = 5.dp)
                )
            }
            Button(
                onClick = { addNewArticleViewModel.save() },
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
    article: UIArticle,
    onNameChange: (String) -> Unit,
    onExpandIconClick: () -> Unit,
    onArticleTypeChange: (ArticleUnit) -> Unit,
    onQuantityChange: (Double?) -> Unit,
    onLocationPickerClick: () -> Unit,
    onOpenCameraClick: () -> Unit,
    onSeePhotoClick: () -> Unit,
    onChooseImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    var isDropDownMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var textValue by rememberSaveable { mutableStateOf(article.quantity?.toString() ?: "") }

    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.surface)
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
                            else stringResource(id = R.string.expand),
                        tint = if (isSystemInDarkTheme()) Color.White else Color.Black
                    )
                }
                OutlinedTextField(
                    value = article.name ?: "",
                    onValueChange = onNameChange,
                    label = {
                        Text(text = stringResource(id = R.string.name))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    maxLines = 1,
                    singleLine = true,
                    isError = article.isError
                )
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
                    val isValueInteger = textValue.toDoubleOrNull()?.isInteger() ?: false
                    OutlinedTextField(
                        value = if (isValueInteger) textValue.toDouble().toInt()
                            .toString() else textValue,
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
                                        text = stringResource(id = article.unit.abbreviation),
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
                                    id = article.unit.label
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
                            tint = if (article.isError) Color.Red else if (isSystemInDarkTheme()) Color.White else Color.Black
                        )
                    }
                    IconButton(
                        onClick = onChooseImageClick,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Photo,
                            contentDescription = stringResource(id = R.string.choose_photo),
                            tint = if (article.isError) Color.Red else if (isSystemInDarkTheme()) Color.White else Color.Black
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
                        onClick = onSeePhotoClick
                    ) {
                        Text(text = stringResource(id = R.string.tap_to_see_photo))
                    }
                }
                if (article.photoVisible) {
                    AsyncImage(
                        model = article.photoUri,
                        contentDescription = article.name,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

}