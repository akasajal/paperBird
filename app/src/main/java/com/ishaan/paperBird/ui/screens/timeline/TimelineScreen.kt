package com.ishaan.paperBird.ui.screens.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishaan.paperBird.domain.model.CATEGORY_COLORS
import com.ishaan.paperBird.domain.model.Letter
import com.ishaan.paperBird.ui.screens.LetterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: LetterViewModel,
    onOpenLetter: (Long) -> Unit
) {
    val letters by viewModel.timelineLetters.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        if (letters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Your story starts here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                // Group by year, show year headers
                var currentYear = -1
                itemsIndexed(letters, key = { _, l -> l.id }) { index, letter ->
                    val year = java.time.Instant.ofEpochMilli(letter.createdAt)
                        .atZone(java.time.ZoneId.systemDefault()).year
                    val isLast = index == letters.size - 1

                    if (year != currentYear) {
                        currentYear = year
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 56.dp)
                        )
                    }

                    TimelineEntry(
                        letter = letter,
                        isLast = isLast,
                        onClick = { onOpenLetter(letter.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEntry(
    letter: Letter,
    isLast: Boolean,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(letter.createdAt))
    val catColor = Color((CATEGORY_COLORS[letter.category] ?: 0xFF9A9A9A).toLong())

    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        // Date column
        Column(
            modifier = Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateStr.split(" ").getOrElse(0) { "" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = dateStr.split(" ").getOrElse(1) { "" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Spine (dot + line)
        Column(
            modifier = Modifier.width(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(catColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .defaultMinSize(minHeight = 40.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        // Content
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, bottom = 12.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = letter.title.ifBlank { "(Untitled)" },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val preview = letter.body.replace("\n", " ").let {
                    if (it.length > 80) it.take(80).trimEnd() + "…" else it
                }
                if (preview.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
