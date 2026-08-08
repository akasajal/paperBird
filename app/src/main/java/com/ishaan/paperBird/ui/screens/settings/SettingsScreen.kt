package com.ishaan.paperBird.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishaan.paperBird.ui.components.PinSetupDialog
import com.ishaan.paperBird.ui.components.PinVerifyDialog
import com.ishaan.paperBird.ui.screens.LetterViewModel
import com.ishaan.paperBird.ui.theme.AccentColors
import com.ishaan.paperBird.ui.theme.CategoryPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LetterViewModel,
    onThemeChange: (Boolean) -> Unit,
    onAccentChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings = viewModel.settingsRepository

    val darkThemeOverride by settings.darkTheme.collectAsState(initial = null)
    val accentColor by settings.accentColor.collectAsState(initial = "Rose")
    val editorFontSize by settings.editorFontSize.collectAsState(initial = 16)
    val defaultCategory by settings.defaultCategory.collectAsState(initial = "Today")
    val customCategories by settings.customCategories.collectAsState(initial = emptyList())
    val customCategoryColors by settings.customCategoryColors.collectAsState(initial = emptyMap())
    val allCategories by settings.allCategories.collectAsState(initial = emptyList())
    val importStatus by viewModel.importStatus.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val appLockEnabled by settings.appLockEnabled.collectAsState(initial = false)
    val useBiometrics by settings.useBiometrics.collectAsState(initial = false)
    val usePin by settings.usePin.collectAsState(initial = false)
    val instantLock by settings.instantLock.collectAsState(initial = false)
    val appPinHash by settings.appPinHash.collectAsState(initial = null)

    var showPinSetup by remember { mutableStateOf(false) }
    var showPinVerifyToDisable by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                // ✅ Resolve actual filename via ContentResolver, not URI string
                val mimeType = context.contentResolver.getType(it)
                val fileName = context.contentResolver
                    .query(it, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }

                context.contentResolver.openInputStream(it)?.use { stream ->
                    val isZip = mimeType == "application/zip" ||
                            mimeType == "application/octet-stream" ||
                            fileName?.endsWith(".zip", ignoreCase = true) == true
                    if (isZip) {
                        viewModel.importLettersFromZip(stream.readBytes())
                    } else {
                        val json = stream.bufferedReader().use { r -> r.readText() }
                        viewModel.importLetterFromJson(json)
                    }
                }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Failed to read file: ${e.localizedMessage}") }
            }
        }
    }

    LaunchedEffect(importStatus) {
        importStatus?.let { result ->
            when (result) {
                is LetterViewModel.ImportResult.Success -> {
                    snackbarHostState.showSnackbar("${result.count} letter(s) imported successfully")
                }
                is LetterViewModel.ImportResult.Error -> {
                    snackbarHostState.showSnackbar(result.message)
                }
            }
            viewModel.clearImportStatus()
        }
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(CategoryPalette.first()) }

    // Theme options: null = System, true = Dark, false = Light
    val themeOptions = listOf<Boolean?>(null, true, false)
    val themeLabels = listOf("System", "Dark", "Light")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            // ── Appearance ────────────────────────────────────────────────
            item { SettingsSection("Appearance") }

            item {
                SettingsRow(title = "Theme") {
                    SingleChoiceSegmentedButtonRow {
                        themeOptions.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = darkThemeOverride == option,
                                onClick = {
                                    scope.launch {
                                        if (option == null) {
                                            settings.setSystemTheme()
                                        } else {
                                            settings.setDarkTheme(option)
                                            onThemeChange(option)
                                        }
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                                label = { Text(themeLabels[index], style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            item {
                SettingsRow(title = "Accent Color") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentColors.all.forEach { (name, color) ->
                            val selected = name == accentColor
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (selected) Modifier.border(2.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        scope.launch { settings.setAccentColor(name) }
                                        onAccentChange(name)
                                    }
                            )
                        }
                    }
                }
            }

            // ── Editor ────────────────────────────────────────────────────
            item { SettingsSection("Editor") }

            item {
                SettingsRow(title = "Font Size", subtitle = "${editorFontSize}sp") {
                    Slider(
                        value = editorFontSize.toFloat(),
                        onValueChange = { scope.launch { settings.setEditorFontSize(it.toInt()) } },
                        valueRange = 12f..24f,
                        steps = 11,
                        modifier = Modifier.width(160.dp)
                    )
                }
            }

            // ── Writing ───────────────────────────────────────────────────
            item { SettingsSection("Writing") }

            item {
                SettingsRow(
                    title = "Import Letters",
                    subtitle = "Import from .json or .zip files",
                    onClick = { importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream", "*/*")) }
                )
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    SettingsRow(
                        title = "Default Category",
                        subtitle = defaultCategory,
                        onClick = { expanded = true }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        allCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    scope.launch { settings.setDefaultCategory(cat) }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Custom Categories ─────────────────────────────────────────
            item { SettingsSection("Custom Categories") }

            items(customCategories, key = { it }) { category ->
                val catColorHex = customCategoryColors[category]
                SettingsRow(
                    title = category,
                    leading = if (catColorHex != null) ({
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(catColorHex))
                        )
                    }) else null,
                    trailing = {
                        IconButton(onClick = {
                            scope.launch { settings.removeCustomCategory(category) }
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove",
                                modifier = Modifier.size(16.dp))
                        }
                    }
                )
            }

            item {
                TextButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Category")
                }
            }

            // ── Privacy ───────────────────────────────────────────────────
            item { SettingsSection("Privacy") }

            // App Lock
            item {
                SettingsRow(
                    title = "App Lock",
                    subtitle = if (appLockEnabled) "Tap to disable" else "Secure app with a PIN",
                    trailing = {
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { turningOn ->
                                if (turningOn) {
                                    showPinSetup = true
                                } else {
                                    showPinVerifyToDisable = true
                                }
                            }
                        )
                    }
                )
            }

            // Biometric
            item {
                AnimatedVisibility(
                    visible = appLockEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SettingsRow(
                        title = "Biometric",
                        subtitle = "Unlock with Fingerprint or Face",
                        onClick = { scope.launch { settings.setUseBiometrics(!useBiometrics) } },
                        trailing = {
                            Switch(
                                checked = useBiometrics,
                                onCheckedChange = { scope.launch { settings.setUseBiometrics(it) } },
                            )
                        }
                    )
                }
            }

            // Instant Lock
            item {
                AnimatedVisibility(
                    visible = appLockEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    SettingsRow(
                        title = "Instant Lock",
                        subtitle = "Lock immediately when app goes to background",
                        onClick = { scope.launch { settings.setInstantLock(!instantLock) } },
                        trailing = {
                            Switch(
                                checked = instantLock,
                                onCheckedChange = { scope.launch { settings.setInstantLock(it) } },
                            )
                        }
                    )
                }
            }

            // ── About ─────────────────────────────────────────────────────
            item { SettingsSection("About") }
            item {
                SettingsRow(
                    title = "To Her",
                    subtitle = "v1.3 · A place for everything I never got to say."
                )
            }
        }
    }

    // Turning App Lock ON: set a new PIN (entered twice) then enable
    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onPinSet = { pin ->
                scope.launch {
                    settings.enableAppLock(pin)
                    showPinSetup = false
                    snackbarHostState.showSnackbar("App Lock enabled")
                }
            }
        )
    }

    // Turning App Lock OFF: verify current PIN first
    if (showPinVerifyToDisable) {
        PinVerifyDialog(
            title = "Disable App Lock",
            subtitle = "Enter your PIN to confirm",
            storedHash = appPinHash,
            verifyPin = settings::verifyPin,
            onDismiss = { showPinVerifyToDisable = false },
            onVerified = {
                scope.launch {
                    settings.disableAppLock()
                    showPinVerifyToDisable = false
                    snackbarHostState.showSnackbar("App Lock disabled")
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryName = ""
                selectedColor = CategoryPalette.first()
            },
            title = { Text("New Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select Color", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Showing first 6 colors, then another row for the rest
                            CategoryPalette.take(6).forEach { colorHex ->
                                ColorOption(
                                    color = Color(colorHex),
                                    selected = selectedColor == colorHex,
                                    onClick = { selectedColor = colorHex }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CategoryPalette.drop(6).forEach { colorHex ->
                                ColorOption(
                                    color = Color(colorHex),
                                    selected = selectedColor == colorHex,
                                    onClick = { selectedColor = colorHex }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        val nameToAdd = newCategoryName.trim()
                        val colorToSave = selectedColor
                        showAddCategoryDialog = false
                        newCategoryName = ""
                        selectedColor = CategoryPalette.first()
                        scope.launch {
                            try {
                                settings.addCustomCategory(nameToAdd, colorToSave)
                                snackbarHostState.showSnackbar("Added $nameToAdd")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                            }
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCategoryDialog = false
                    newCategoryName = ""
                    selectedColor = CategoryPalette.first()
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ColorOption(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String = "",
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick)
    else Modifier.fillMaxWidth()

    Row(
        modifier = modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
    HorizontalDivider(
        modifier = Modifier.alpha(0.3f),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}