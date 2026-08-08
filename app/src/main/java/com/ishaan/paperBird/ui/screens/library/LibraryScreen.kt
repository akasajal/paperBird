package com.ishaan.paperBird.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ishaan.paperBird.domain.model.Letter
import com.ishaan.paperBird.ui.components.DeleteConfirmationDialog
import com.ishaan.paperBird.ui.components.EmptyState
import com.ishaan.paperBird.ui.components.ExportFormat
import com.ishaan.paperBird.ui.components.ExportFormatDialog
import com.ishaan.paperBird.ui.components.LetterCard
import com.ishaan.paperBird.ui.components.SectionHeader
import com.ishaan.paperBird.ui.screens.LetterViewModel
import com.ishaan.paperBird.util.PdfExporter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LetterViewModel,
    onOpenLetter: (Long) -> Unit
) {
    val allLetters by viewModel.allLetters.collectAsState()
    val allCategories by viewModel.settingsRepository.allCategories.collectAsState(initial = emptyList())
    val categoryColors by viewModel.settingsRepository.allCategoryColors.collectAsState(initial = emptyMap())
    val context = LocalContext.current

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    var lettersToExport by remember { mutableStateOf<List<Letter>>(emptyList()) }
    var showExportFormatDialog by remember { mutableStateOf(false) }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val content = if (lettersToExport.size == 1) {
                lettersToExport.first().toJson()
            } else {
                Letter.toJsonArray(lettersToExport)
            }
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(content.toByteArray())
            }
            lettersToExport = emptyList()
            selectedIds = emptySet()
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            val pdfBytes = PdfExporter.exportLettersToPdf(lettersToExport, categoryColors)
            context.contentResolver.openOutputStream(it)?.use { stream ->
                stream.write(pdfBytes)
            }
            lettersToExport = emptyList()
            selectedIds = emptySet()
        }
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var idsToDelete by remember { mutableStateOf(setOf<Long>()) }

    BackHandler(isSelectionMode) {
        selectedIds = emptySet()
    }

    var sortIndex by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("All") }
    var viewMode by remember { mutableStateOf("list") } // list or grid

    val sortOptions = listOf("Newest", "Oldest", "A → Z", "Z → A")
    val categories = listOf("All") + allCategories

    val filtered = remember(allLetters, sortIndex, selectedCategory) {
        var list = allLetters
        if (selectedCategory != "All") list = list.filter { it.category == selectedCategory }
        when (sortIndex) {
            0 -> list.sortedByDescending { it.updatedAt }
            1 -> list.sortedBy { it.updatedAt }
            2 -> list.sortedBy { it.title.lowercase() }
            3 -> list.sortedByDescending { it.title.lowercase() }
            else -> list
        }
    }

    val grouped = remember(filtered) {
        val today = LocalDate.now()
        // Assign each letter a bucket label and a sort key (most recent first)
        data class Bucketed(val label: String, val sortKey: Long, val letter: com.ishaan.paperBird.domain.model.Letter)
        val bucketed = filtered.map { letter ->
            val date = Instant.ofEpochMilli(letter.createdAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val days = ChronoUnit.DAYS.between(date, today)
            val label = when {
                days == 0L -> "Today"
                days == 1L -> "Yesterday"
                days <= 7  -> "This Week"
                date.year == today.year && date.month == today.month -> "This Month"
                date.year == today.year -> date.month.name.lowercase().replaceFirstChar { it.uppercase() }
                else -> "${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.year}"
            }
            // Sort key = most recent createdAt within the bucket, negated so DESC
            Bucketed(label, -letter.createdAt, letter)
        }
        // Group, then sort groups by their earliest (most recent) sort key
        bucketed
            .groupBy { it.label }
            .entries
            .sortedBy { (_, items) -> items.minOf { it.sortKey } }
            .associate { (label, items) -> label to items.map { it.letter } }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val filteredIds = filtered.map { it.id }.toSet()
                            selectedIds = if (selectedIds.size == filteredIds.size) emptySet() else filteredIds
                        }) {
                            Icon(Icons.Default.SelectAll, "Select all")
                        }
                        IconButton(onClick = {
                            lettersToExport = allLetters.filter { it.id in selectedIds }
                            showExportFormatDialog = true
                        }) {
                            Icon(Icons.Default.FileDownload, "Export selected")
                        }
                        IconButton(onClick = {
                            viewModel.toggleFavoriteLetters(selectedIds, true)
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Favorite, "Favorite selected")
                        }
                        IconButton(onClick = {
                            idsToDelete = selectedIds
                            showDeleteConfirmation = true
                        }) {
                            Icon(Icons.Default.Delete, "Delete selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text("Library", style = MaterialTheme.typography.titleLarge)
                            Text("${filtered.size} letters", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter/sort row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category filter
                var catExpanded by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = selectedCategory != "All",
                        onClick = { catExpanded = true },
                        label = { Text(selectedCategory) }
                    )
                    DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { selectedCategory = cat; catExpanded = false }
                            )
                        }
                    }
                }
                // Sort
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = sortIndex != 0,
                        onClick = { sortExpanded = true },
                        label = { Text(sortOptions[sortIndex]) }
                    )
                    DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        sortOptions.forEachIndexed { i, opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = { sortIndex = i; sortExpanded = false }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            if (filtered.isEmpty()) {
                EmptyState(
                    title = "No letters here yet.",
                    subtitle = if (selectedCategory != "All") "Try a different category." else ""
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp, ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    grouped.forEach { (group, letters) ->
                        item(key = "header_$group") {
                            SectionHeader(group)
                        }
                        items(letters, key = { it.id }) { letter ->
                            val isSelected = selectedIds.contains(letter.id)
                            LetterCard(
                                letter = letter,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) selectedIds - letter.id else selectedIds + letter.id
                                    } else {
                                        onOpenLetter(letter.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedIds = setOf(letter.id)
                                    }
                                },
                                onFavoriteToggle = { viewModel.toggleFavoriteFromList(letter) },
                                onDuplicate = { viewModel.duplicateLetter(letter.id) {} },
                                onDelete = {
                                    idsToDelete = setOf(letter.id)
                                    showDeleteConfirmation = true
                                },
                                onExport = {
                                    lettersToExport = listOf(letter)
                                    showExportFormatDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            title = if (idsToDelete.size > 1) "Delete ${idsToDelete.size} letters?" else "Delete letter?",
            onConfirm = {
                if (idsToDelete.size > 1) {
                    viewModel.deleteLetters(idsToDelete) {
                        selectedIds = emptySet()
                    }
                } else {
                    idsToDelete.firstOrNull()?.let { id ->
                        viewModel.deleteLetterById(id)
                    }
                }
            },
            onDismiss = {
                showDeleteConfirmation = false
                idsToDelete = emptySet()
            }
        )
    }

    if (showExportFormatDialog) {
        ExportFormatDialog(
            onDismiss = {
                showExportFormatDialog = false
                lettersToExport = emptyList()
            },
            onChoose = { format ->
                showExportFormatDialog = false
                val defaultName = if (lettersToExport.size == 1) {
                    lettersToExport.first().title.ifBlank { "letter" }
                } else {
                    "exported_letters"
                }

                when (format) {
                    ExportFormat.JSON -> jsonExportLauncher.launch("$defaultName.json")
                    ExportFormat.PDF -> pdfExportLauncher.launch("$defaultName.pdf")
                }
            }
        )
    }
}