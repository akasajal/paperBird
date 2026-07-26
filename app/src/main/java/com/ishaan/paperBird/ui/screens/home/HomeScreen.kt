package com.ishaan.paperBird.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ishaan.paperBird.ui.components.DeleteConfirmationDialog
import com.ishaan.paperBird.ui.components.EmptyState
import com.ishaan.paperBird.ui.components.LetterCard
import com.ishaan.paperBird.ui.components.SectionHeader
import com.ishaan.paperBird.ui.screens.LetterViewModel

private val GREETINGS = listOf(
    "What deserves a page today?",
    "What's on your mind?",
    "How are you feeling today?",
    "What's today's story?",
    "Write whatever feels important.",
    "What's been occupying your thoughts lately?",
    "What happened today?",
    "What's worth remembering today?",
    "Did anything surprise you today?",
    "What's something you don't want to forget?",
    "What's one moment that stayed with you?",
    "What's been bringing you joy lately?",
    "What's been challenging you today?",
    "How has today treated you?",
    "What's been keeping you busy?",
    "What's something you're grateful for today?",
    "What's one thing you're proud of today?",
    "What's been on your heart lately?",
    "What's something you'd like to let out?",
    "Anything you'd like to unpack today?",
    "Capture today's thoughts.",
    "This page is yours.",
    "Take your time.",
    "No rush. Start wherever you'd like.",
    "Whenever you're ready, begin.",
    "Write freely. There's no right way.",
    "Big thoughts or little moments—everything belongs here.",
    "Even ordinary days are worth remembering.",
    "Let's make sense of today together.",
    "What's one thought you keep coming back to?",
    "If today had a title, what would it be?",
    "If today became a memory, what would stand out?",
    "What's something you learned today?",
    "What's something you wish to remember years from now?",
    "What's something you haven't said out loud yet?",
    "Leave today's thoughts here.",
    "Whatever today looked like, this page is ready.",
    "Every story starts with a single sentence.",
    "Where would you like to begin?",
    "Your words are welcome here."
)

private val SUBTITLES = listOf(
    "Every letter becomes a memory. Every memory stays.",
    "Some thoughts deserve a home.",
    "The smallest moments are worth keeping.",
    "A page today. A memory forever.",
    "Write freely. Keep forever.",
    "One more page of your story.",
    "Some conversations never truly end.",
    "Your words matter.",
    "A quiet place for everything unsaid.",
    "The heart remembers what the mind forgets."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: LetterViewModel,
    onNewLetter: () -> Unit,
    onOpenLetter: (Long) -> Unit
) {
    val recentLetters by viewModel.recentLetters.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val randomLetter by viewModel.randomLetter.collectAsState()

    val greeting = remember { GREETINGS.random() }
    val subtitle = remember { SUBTITLES.random() }
    val focusManager = LocalFocusManager.current

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var letterToDelete by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadRandomLetter()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewLetter,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Letter")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search letters\u2026") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear")
                            }
                        }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.height(4.dp))
            }

            if (searchQuery.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No letters match \u201c$searchQuery\u201d",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    item { SectionHeader("${searchResults.size} result${if (searchResults.size == 1) "" else "s"}") }
                    items(searchResults, key = { it.id }) { letter ->
                        LetterCard(
                            letter = letter,
                            onClick = { onOpenLetter(letter.id) },
                            onFavoriteToggle = { viewModel.toggleFavoriteFromList(letter) },
                            onDuplicate = { viewModel.duplicateLetter(letter.id) {} },
                            onDelete = {
                                letterToDelete = letter.id
                                showDeleteConfirmation = true
                            },
                            searchQuery = searchQuery
                        )
                    }
                }
                return@LazyColumn
            }

            randomLetter?.let { letter ->
                item { SectionHeader("A Letter to Revisit") }
                item(key = "random_${letter.id}") {
                    LetterCard(
                        letter = letter,
                        onClick = { onOpenLetter(letter.id) },
                        onFavoriteToggle = { viewModel.toggleFavoriteFromList(letter) },
                        onDuplicate = { viewModel.duplicateLetter(letter.id) {} },
                        onDelete = {
                            letterToDelete = letter.id
                            showDeleteConfirmation = true
                        }
                    )
                }
            }

            if (recentLetters.isNotEmpty()) {
                item { SectionHeader("Recent") }
                items(recentLetters, key = { it.id }) { letter ->
                    LetterCard(
                        letter = letter,
                        onClick = { onOpenLetter(letter.id) },
                        onFavoriteToggle = { viewModel.toggleFavoriteFromList(letter) },
                        onDuplicate = { viewModel.duplicateLetter(letter.id) {} },
                        onDelete = {
                            letterToDelete = letter.id
                            showDeleteConfirmation = true
                        }
                    )
                }
            } else {
                item {
                    EmptyState(
                        title = "No letters yet.",
                        subtitle = "Tap + to write your first letter.",
                        modifier = Modifier.height(300.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = {
                letterToDelete?.let { viewModel.deleteLetterById(it) }
            },
            onDismiss = {
                showDeleteConfirmation = false
                letterToDelete = null
            }
        )
    }
}
