package com.ishaan.paperBird.data.repository

import com.ishaan.paperBird.data.local.dao.AttachmentDao
import com.ishaan.paperBird.data.local.dao.LetterDao
import com.ishaan.paperBird.data.local.entities.AttachmentEntity
import com.ishaan.paperBird.data.local.entities.LetterEntity
import com.ishaan.paperBird.domain.model.Attachment
import com.ishaan.paperBird.domain.model.Letter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LetterRepository @Inject constructor(
    private val letterDao: LetterDao,
    private val attachmentDao: AttachmentDao
) {
    fun getAllLetters(): Flow<List<Letter>> =
        letterDao.getAllLetters().map { it.map(LetterEntity::toDomain) }

    fun getRecentLetters(): Flow<List<Letter>> =
        letterDao.getRecentLetters().map { it.map(LetterEntity::toDomain) }

    fun getFavoriteLetters(): Flow<List<Letter>> =
        letterDao.getFavoriteLetters().map { it.map(LetterEntity::toDomain) }

    fun searchLetters(query: String): Flow<List<Letter>> =
        letterDao.searchLetters(query).map { it.map(LetterEntity::toDomain) }

    suspend fun getLetterById(id: Long): Letter? =
        letterDao.getLetterById(id)?.toDomain()

    suspend fun saveLetter(letter: Letter): Long =
        letterDao.insert(letter.toEntity())

    suspend fun updateLetter(letter: Letter) =
        letterDao.update(letter.toEntity())

    suspend fun deleteLetter(letter: Letter) =
        letterDao.delete(letter.toEntity())

    suspend fun toggleFavorite(letter: Letter) =
        letterDao.setFavorite(letter.id, !letter.favorite)

    suspend fun getLettersBetween(startMs: Long, endMs: Long): List<Letter> =
        letterDao.getLettersBetween(startMs, endMs).map(LetterEntity::toDomain)

    suspend fun getAllLettersOnce(): List<Letter> =
        letterDao.getAllLettersOnce().map(LetterEntity::toDomain)

    // Attachments
    fun getAttachmentsForLetter(letterId: Long): Flow<List<Attachment>> =
        attachmentDao.getAttachmentsForLetter(letterId).map { it.map(AttachmentEntity::toDomain) }

    suspend fun addAttachment(attachment: Attachment): Long =
        attachmentDao.insert(attachment.toEntity())

    suspend fun deleteAttachment(attachment: Attachment) =
        attachmentDao.delete(attachment.toEntity())
}

// Mappers
fun LetterEntity.toDomain() = Letter(id, title, body, category, favorite, createdAt, updatedAt)
fun Letter.toEntity() = LetterEntity(id, title, body, category, favorite, createdAt, updatedAt)
fun AttachmentEntity.toDomain() = Attachment(id, letterId, filename, mimeType, uriPath, createdAt)
fun Attachment.toEntity() = AttachmentEntity(id, letterId, filename, mimeType, uriPath, createdAt)
