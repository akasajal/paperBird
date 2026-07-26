package com.ishaan.paperBird.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ishaan.paperBird.domain.model.CATEGORY_COLORS
import com.ishaan.paperBird.domain.model.Letter
import com.ishaan.paperBird.ui.components.DeleteConfirmationDialog
import com.ishaan.paperBird.ui.components.LetterCard
import com.ishaan.paperBird.ui.screens.LetterViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: LetterViewModel,
    onOpenLetter: (Long) -> Unit
) {
    val calendarMonth by viewModel.calendarMonth.collectAsState()
    val calendarLetters by viewModel.calendarLetters.collectAsState()
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var letterToDelete by remember { mutableStateOf<Long?>(null) }

    val selectedLetters = remember(selectedDay, calendarLetters) {
        selectedDay?.let { calendarLetters[it] } ?: emptyList()
    }

    LaunchedEffect(Unit) { viewModel.setCalendarMonth(YearMonth.now()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Month navigation
        TopAppBar(
            title = {
                Text(
                    calendarMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${calendarMonth.year}",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.setCalendarMonth(calendarMonth.minusMonths(1))
                    selectedDay = null
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
            },
            actions = {
                IconButton(onClick = {
                    viewModel.setCalendarMonth(calendarMonth.plusMonths(1))
                    selectedDay = null
                }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Calendar grid — original spacious layout
        val firstDay = calendarMonth.atDay(1)
        val startOffset = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        val daysInMonth = calendarMonth.lengthOfMonth()
        val today = LocalDate.now()

        val cells = buildList {
            repeat(startOffset) { add(null) }
            for (d in 1..daysInMonth) add(d)
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            cells.chunked(7).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { day ->
                        Box(modifier = Modifier.weight(1f).aspectRatio(0.9f)) {
                            if (day != null) {
                                val isToday = today.year == calendarMonth.year &&
                                        today.monthValue == calendarMonth.monthValue &&
                                        today.dayOfMonth == day
                                val isSelected = selectedDay == day
                                val letters = calendarLetters[day] ?: emptyList()
                                CalendarDayCell(
                                    day = day,
                                    letters = letters,
                                    isToday = isToday,
                                    isSelected = isSelected,
                                    onClick = { selectedDay = if (selectedDay == day) null else day }
                                )
                            }
                        }
                    }
                    repeat(7 - row.size) { Box(modifier = Modifier.weight(1f)) }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Letters list for selected day
        when {
            selectedDay == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Tap a day to see letters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            selectedLetters.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No letters on this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(selectedLetters, key = { it.id }) { letter ->
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

@Composable
private fun CalendarDayCell(
    day: Int,
    letters: List<Letter>,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            ),
            color = textColor
        )
        Spacer(Modifier.height(2.dp))
        // Show letter count if > 0, else dots by category (max 3)
        if (letters.isNotEmpty()) {
            if (letters.size == 1) {
                // Single dot in category colour
                val dotColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else Color((CATEGORY_COLORS[letters[0].category] ?: 0xFF9A9A9A).toLong())
                Box(Modifier.size(5.dp).clip(CircleShape).background(dotColor))
            } else {
                // Show count badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${letters.size}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}