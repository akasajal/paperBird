package com.ishaan.paperBird.ui.screens.editor

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ishaan.paperBird.domain.model.Attachment
import com.ishaan.paperBird.domain.model.CATEGORY_COLORS
import com.ishaan.paperBird.ui.components.CategoryBadge
import com.ishaan.paperBird.ui.components.DeleteConfirmationDialog
import com.ishaan.paperBird.ui.screens.LetterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: LetterViewModel,
    letterId: Long?,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onSaved: () -> Unit = onBack
) {
    val context = LocalContext.current

    val letter by viewModel.currentLetter.collectAsState()
    val title by viewModel.editorTitle.collectAsState()
    val body by viewModel.editorBody.collectAsState()
    val category by viewModel.editorCategory.collectAsState()
    val attachments by viewModel.currentAttachments.collectAsState()
    val allCategories by viewModel.settingsRepository.allCategories.collectAsState(initial = emptyList())
    val editorFontSize by viewModel.settingsRepository.editorFontSize.collectAsState(initial = 16)
    val isFavorite by viewModel.editorFavorite.collectAsState()

    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var previewAttachment by remember { mutableStateOf<Attachment?>(null) }

    val titleFocus = remember { FocusRequester() }
    val bodyFocus = remember { FocusRequester() }

    val wordCount = remember(body) {
        if (body.isBlank()) 0 else body.trim().split(Regex("\\s+")).size
    }
    val readingTime = remember(wordCount) { maxOf(1, wordCount / 200) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val filename = uri.lastPathSegment ?: "image"
            val mime = context.contentResolver.getType(uri) ?: "image/*"
            viewModel.addAttachment(
                Attachment(letterId = letter?.id ?: 0, filename = filename, mimeType = mime, uriPath = uri.toString())
            )
        }
    }

    // Autosave on content change for existing letters
    LaunchedEffect(title, body) {
        if (letter != null && hasUnsavedChanges) {
            kotlinx.coroutines.delay(3000)
            viewModel.saveLetter()
        }
    }

    LaunchedEffect(letterId) {
        viewModel.loadLetter(letterId)
        if (letterId == null) {
            kotlinx.coroutines.delay(100)
            titleFocus.requestFocus()
        }
    }

    val catColor = Color((CATEGORY_COLORS[category] ?: 0xFF9A9A9A).toLong())

    // Intercept system back when there are unsaved changes
    val navigateBack: () -> Unit = {
        if (hasUnsavedChanges) showUnsavedDialog = true else onBack()
    }
    BackHandler(enabled = hasUnsavedChanges) { showUnsavedDialog = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { showCategorySheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.18f))
                                .border(1.dp, catColor.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = catColor
                            )
                        }
                    }
                    IconButton(onClick = { imagePicker.launch("image/*") }) {
                        Icon(Icons.Filled.Image, contentDescription = "Attach image")
                    }
                    if (letter != null && hasUnsavedChanges) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { viewModel.saveLetter { onSaved() } }) {
                        Icon(Icons.Filled.Save, contentDescription = "Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    if (attachments.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attachments, key = { it.id }) { att ->
                                Box {
                                    AsyncImage(
                                        model = Uri.parse(att.uriPath),
                                        contentDescription = att.filename,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .height(72.dp)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { previewAttachment = att }
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeAttachment(att) },
                                        modifier = Modifier
                                            .size(22.dp)
                                            .align(Alignment.TopEnd)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "$wordCount words · $readingTime min read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = title,
                onValueChange = viewModel::updateTitle,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 34.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text(
                            "Title",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .focusRequester(titleFocus)
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

            BasicTextField(
                value = body,
                onValueChange = viewModel::updateBody,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = editorFontSize.sp,
                    lineHeight = (editorFontSize * 1.6).sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (body.isEmpty()) Text(
                            "Write your letter\u2026",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                fontSize = editorFontSize.sp
                            )
                        )
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 400.dp)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .focusRequester(bodyFocus)
            )
        }
    }

    previewAttachment?.let { att ->
        Dialog(
            onDismissRequest = { previewAttachment = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { previewAttachment = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = Uri.parse(att.uriPath),
                    contentDescription = att.filename,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { previewAttachment = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close preview", tint = Color.White)
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Save your changes before leaving?") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    viewModel.saveLetter { onSaved() }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Keep editing")
                    }
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        onBack()
                    }) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteLetter { onDeleted() }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }) {
            Text(
                "Category",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            allCategories.forEach { cat ->
                ListItem(
                    headlineContent = { Text(cat) },
                    leadingContent = { CategoryBadge(cat) },
                    trailingContent = if (cat == category) {
                        { Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                    modifier = Modifier.clickable {
                        viewModel.updateCategory(cat)
                        showCategorySheet = false
                    }
                )
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}