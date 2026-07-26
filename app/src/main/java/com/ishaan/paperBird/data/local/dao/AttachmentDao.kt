package com.ishaan.paperBird.data.local.dao

import androidx.room.*
import com.ishaan.paperBird.data.local.entities.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity): Long

    @Delete
    suspend fun delete(attachment: AttachmentEntity)

    @Query("SELECT * FROM letter_attachments WHERE letter_id = :letterId ORDER BY created_at ASC")
    fun getAttachmentsForLetter(letterId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM letter_attachments WHERE letter_id = :letterId ORDER BY created_at ASC")
    suspend fun getAttachmentsForLetterOnce(letterId: Long): List<AttachmentEntity>

    @Query("DELETE FROM letter_attachments WHERE letter_id = :letterId")
    suspend fun deleteAllForLetter(letterId: Long)
}
