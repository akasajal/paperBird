package com.ishaan.paperBird.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishaan.paperBird.data.repository.LetterRepository
import com.ishaan.paperBird.data.repository.SettingsRepository
import com.ishaan.paperBird.domain.model.Attachment
import com.ishaan.paperBird.domain.model.Letter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.time.*
import java.util.zip.ZipInputStream
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class LetterViewModel @Inject constructor(
    private val letterRepository: LetterRepository,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    // ── Search ─────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<Letter>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList())
            else letterRepository.searchLetters(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    // ── Recent ─────────────────────────────────────────────────────────────
    val recentLetters: StateFlow<List<Letter>> = letterRepository.getRecentLetters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── All letters ────────────────────────────────────────────────────────
    val allLetters: StateFlow<List<Letter>> = letterRepository.getAllLetters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Favorites ──────────────────────────────────────────────────────────
    val favoriteLetters: StateFlow<List<Letter>> = letterRepository.getFavoriteLetters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Editor state ───────────────────────────────────────────────────────
    private val _currentLetter = MutableStateFlow<Letter?>(null)
    val currentLetter = _currentLetter.asStateFlow()

    private val _currentAttachments = MutableStateFlow<List<Attachment>>(emptyList())
    val currentAttachments = _currentAttachments.asStateFlow()

    // Pending attachments for unsaved (new) letters — flushed on first save
    private val _pendingAttachments = MutableStateFlow<List<Attachment>>(emptyList())

    private val _editorTitle = MutableStateFlow("")
    val editorTitle = _editorTitle.asStateFlow()

    private val _editorBody = MutableStateFlow("")
    val editorBody = _editorBody.asStateFlow()

    private val _editorCategory = MutableStateFlow("Life")
    val editorCategory = _editorCategory.asStateFlow()

    // Unified favourite flag — works for both new and saved letters
    private val _editorFavorite = MutableStateFlow(false)
    val editorFavorite = _editorFavorite.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges = _hasUnsavedChanges.asStateFlow()

    fun loadLetter(letterId: Long?) {
        viewModelScope.launch {
            if (letterId != null && letterId > 0) {
                val letter = letterRepository.getLetterById(letterId)
                _currentLetter.value = letter
                _editorTitle.value = letter?.title ?: ""
                _editorBody.value = letter?.body ?: ""
                _editorCategory.value = letter?.category ?: "Life"
                _editorFavorite.value = letter?.favorite ?: false
                _pendingAttachments.value = emptyList()
                _hasUnsavedChanges.value = false
                if (letter != null) {
                    letterRepository.getAttachmentsForLetter(letter.id)
                        .collect { _currentAttachments.value = it }
                }
            } else {
                val defaultCat = settingsRepository.defaultCategory.first()
                _currentLetter.value = null
                _editorTitle.value = ""
                _editorBody.value = ""
                _editorCategory.value = defaultCat
                _editorFavorite.value = false
                _currentAttachments.value = emptyList()
                _pendingAttachments.value = emptyList()
                _hasUnsavedChanges.value = false
            }
        }
    }

    fun updateTitle(t: String) { _editorTitle.value = t; _hasUnsavedChanges.value = true }
    fun updateBody(b: String) { _editorBody.value = b; _hasUnsavedChanges.value = true }
    fun updateCategory(c: String) { _editorCategory.value = c; _hasUnsavedChanges.value = true }

    fun saveLetter(onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val existing = _currentLetter.value
            val now = System.currentTimeMillis()
            val letter = Letter(
                id = existing?.id ?: 0,
                title = _editorTitle.value.trim(),
                body = _editorBody.value.trim(),
                category = _editorCategory.value,
                favorite = existing?.favorite ?: _editorFavorite.value,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            val id = if (existing == null) {
                letterRepository.saveLetter(letter)
            } else {
                letterRepository.updateLetter(letter)
                letter.id
            }
            val savedLetter = letter.copy(id = id)
            _currentLetter.value = savedLetter

            // Flush pending attachments now that we have a real letter id
            val pending = _pendingAttachments.value
            if (pending.isNotEmpty()) {
                pending.forEach { att ->
                    val attId = letterRepository.addAttachment(att.copy(letterId = id))
                    _currentAttachments.value = _currentAttachments.value + att.copy(id = attId, letterId = id)
                }
                _pendingAttachments.value = emptyList()
            }

            _isSaving.value = false
            _hasUnsavedChanges.value = false
            onSaved(id)
        }
    }

    fun deleteLetter(onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _currentLetter.value?.let { letter ->
                val isRandom = _randomLetter.value?.id == letter.id
                letterRepository.deleteLetter(letter)
                if (isRandom) {
                    _randomLetter.value = null
                    _randomLetterLoaded = false
                    loadRandomLetter()
                }
                onDeleted()
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val existing = _currentLetter.value
            if (existing != null) {
                letterRepository.toggleFavorite(existing)
                _currentLetter.value = existing.copy(favorite = !existing.favorite)
                _editorFavorite.value = !existing.favorite
            } else {
                _editorFavorite.value = !_editorFavorite.value
            }
        }
    }

    fun toggleFavoriteFromList(letter: Letter) {
        viewModelScope.launch {
            letterRepository.toggleFavorite(letter)
        }
    }

    fun duplicateLetter(onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            val letter = _currentLetter.value ?: return@launch
            val now = System.currentTimeMillis()
            val id = letterRepository.saveLetter(
                letter.copy(id = 0, title = "${letter.title} (copy)", createdAt = now, updatedAt = now)
            )
            onDuplicated(id)
        }
    }

    fun duplicateLetter(letterId: Long, onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            val letter = letterRepository.getLetterById(letterId) ?: return@launch
            val now = System.currentTimeMillis()
            val id = letterRepository.saveLetter(
                letter.copy(id = 0, title = "${letter.title} (copy)", createdAt = now, updatedAt = now)
            )
            onDuplicated(id)
        }
    }

    fun deleteLetterById(letterId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val isRandom = _randomLetter.value?.id == letterId
            val letter = letterRepository.getLetterById(letterId) ?: return@launch
            letterRepository.deleteLetter(letter)
            if (isRandom) {
                _randomLetter.value = null
                _randomLetterLoaded = false
                loadRandomLetter()
            }
            onDeleted()
        }
    }

    fun addAttachment(attachment: Attachment) {
        viewModelScope.launch {
            val letterId = _currentLetter.value?.id
            if (letterId != null && letterId > 0) {
                val id = letterRepository.addAttachment(attachment.copy(letterId = letterId))
                _currentAttachments.value = _currentAttachments.value + attachment.copy(id = id, letterId = letterId)
            } else {
                val tempId = -(System.currentTimeMillis())
                val pending = attachment.copy(id = tempId, letterId = 0)
                _pendingAttachments.value = _pendingAttachments.value + pending
                _currentAttachments.value = _currentAttachments.value + pending
            }
            _hasUnsavedChanges.value = true
        }
    }

    fun removeAttachment(attachment: Attachment) {
        viewModelScope.launch {
            if (attachment.letterId > 0 && attachment.id > 0) {
                letterRepository.deleteAttachment(attachment)
            }
            _pendingAttachments.value = _pendingAttachments.value.filter { it.id != attachment.id }
            _currentAttachments.value = _currentAttachments.value.filter { it.id != attachment.id }
            _hasUnsavedChanges.value = true
        }
    }

    // ── Calendar data ──────────────────────────────────────────────────────
    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth = _calendarMonth.asStateFlow()

    private val _calendarLetters = MutableStateFlow<Map<Int, List<Letter>>>(emptyMap())
    val calendarLetters = _calendarLetters.asStateFlow()

    fun setCalendarMonth(month: YearMonth) {
        _calendarMonth.value = month
        loadCalendarLetters(month)
    }

    private fun loadCalendarLetters(month: YearMonth) {
        viewModelScope.launch {
            val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = month.atEndOfMonth().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val letters = letterRepository.getLettersBetween(start, end)
            val map = mutableMapOf<Int, MutableList<Letter>>()
            letters.forEach { letter ->
                val day = Instant.ofEpochMilli(letter.createdAt)
                    .atZone(ZoneId.systemDefault()).dayOfMonth
                map.getOrPut(day) { mutableListOf() }.add(letter)
            }
            _calendarLetters.value = map
        }
    }

    // ── Timeline data ──────────────────────────────────────────────────────
    val timelineLetters: StateFlow<List<Letter>> = letterRepository.getAllLetters()
        .map { it.sortedBy { l -> l.createdAt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Random letter ──────────────────────────────────────────────────────
    private val _randomLetter = MutableStateFlow<Letter?>(null)
    val randomLetter = _randomLetter.asStateFlow()

    private var _randomLetterLoaded = false

    fun loadRandomLetter() {
        if (_randomLetterLoaded) return
        viewModelScope.launch {
            val all = letterRepository.getAllLettersOnce()
            _randomLetter.value = if (all.isEmpty()) null else all.random()
            _randomLetterLoaded = true
        }
    }

    private val _importStatus = MutableStateFlow<ImportResult?>(null)
    val importStatus = _importStatus.asStateFlow()

    fun clearImportStatus() { _importStatus.value = null }

    fun importLetterFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val trimmed = jsonString.trim()
                if (trimmed.startsWith("[")) {
                    val letters = Letter.fromJsonArray(trimmed)
                    var count = 0
                    letters.forEach {
                        if (it.title.isNotBlank() || it.body.isNotBlank()) {
                            letterRepository.saveLetter(it.copy(id = 0))
                            count++
                        }
                    }
                    _importStatus.value = ImportResult.Success(count.toLong())
                } else {
                    val importedLetter = Letter.fromJson(trimmed)
                    if (importedLetter.body.isBlank() && importedLetter.title.isBlank()) {
                        _importStatus.value = ImportResult.Error("Letter content is empty")
                        return@launch
                    }
                    letterRepository.saveLetter(importedLetter.copy(id = 0))
                    _importStatus.value = ImportResult.Success(1)
                }
            } catch (e: Exception) {
                _importStatus.value = ImportResult.Error("Invalid letter format: ${e.localizedMessage}")
            }
        }
    }

    fun importLettersFromZip(zipBytes: ByteArray) {
        viewModelScope.launch {
            try {
                var count = 0
                val zis = ZipInputStream(ByteArrayInputStream(zipBytes))
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".json")) {
                        val jsonString = zis.bufferedReader().readText()
                        val trimmed = jsonString.trim()
                        if (trimmed.startsWith("[")) {
                            val letters = Letter.fromJsonArray(trimmed)
                            letters.forEach {
                                if (it.title.isNotBlank() || it.body.isNotBlank()) {
                                    letterRepository.saveLetter(it.copy(id = 0))
                                    count++
                                }
                            }
                        } else {
                            val letter = Letter.fromJson(trimmed)
                            if (letter.title.isNotBlank() || letter.body.isNotBlank()) {
                                letterRepository.saveLetter(letter.copy(id = 0))
                                count++
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
                zis.close()
                if (count > 0) {
                    _importStatus.value = ImportResult.Success(count.toLong())
                } else {
                    _importStatus.value = ImportResult.Error("No valid letters found in zip")
                }
            } catch (e: Exception) {
                _importStatus.value = ImportResult.Error("Error processing zip: ${e.localizedMessage}")
            }
        }
    }

    fun deleteLetters(ids: Set<Long>, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val isRandomInBatch = ids.contains(_randomLetter.value?.id)
            ids.forEach { id ->
                val letter = letterRepository.getLetterById(id)
                if (letter != null) letterRepository.deleteLetter(letter)
            }
            if (isRandomInBatch) {
                _randomLetter.value = null
                _randomLetterLoaded = false
                loadRandomLetter()
            }
            onDeleted()
        }
    }

    fun toggleFavoriteLetters(ids: Set<Long>, favorite: Boolean) {
        viewModelScope.launch {
            ids.forEach { id ->
                val letter = letterRepository.getLetterById(id)
                if (letter != null && letter.favorite != favorite) {
                    letterRepository.toggleFavorite(letter)
                }
            }
        }
    }

    fun duplicateLetters(ids: Set<Long>, onDuplicated: () -> Unit = {}) {
        viewModelScope.launch {
            ids.forEach { id ->
                val letter = letterRepository.getLetterById(id)
                if (letter != null) {
                    val now = System.currentTimeMillis()
                    letterRepository.saveLetter(
                        letter.copy(id = 0, title = "${letter.title} (copy)", createdAt = now, updatedAt = now)
                    )
                }
            }
            onDuplicated()
        }
    }

    sealed class ImportResult {
        data class Success(val count: Long) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}