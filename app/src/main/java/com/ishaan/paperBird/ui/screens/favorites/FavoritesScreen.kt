package com.ishaan.paperBird.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishaan.paperBird.ui.components.DeleteConfirmationDialog
import com.ishaan.paperBird.ui.components.EmptyState
import com.ishaan.paperBird.ui.components.LetterCard
import com.ishaan.paperBird.ui.screens.LetterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: LetterViewModel,
    onOpenLetter: (Long) -> Unit
) {
    val favorites by viewModel.favoriteLetters.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var letterToDelete by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Favorites", style = MaterialTheme.typography.titleLarge)
                        if (favorites.isNotEmpty()) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text("${favorites.size}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            EmptyState(
                title = "No favorites yet.",
                subtitle = "Heart a letter to keep it close.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favorites, key = { it.id }) { letter ->
                    LetterCard(
                        letter = letter,
                        onClick = { onOpenLetter(letter.id) },
                        onFavoriteToggle = { viewModel.toggleFavoriteFromList(letter) },
                        onDelete = {
                            letterToDelete = letter.id
                            showDeleteConfirmation = true
                        }
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
