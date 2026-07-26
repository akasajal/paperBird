package com.ishaan.paperBird.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ishaan.paperBird.data.local.dao.AttachmentDao
import com.ishaan.paperBird.data.local.dao.LetterDao
import com.ishaan.paperBird.data.local.entities.AttachmentEntity
import com.ishaan.paperBird.data.local.entities.LetterEntity

@Database(
    entities = [LetterEntity::class, AttachmentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PaperBirdDatabase : RoomDatabase() {
    abstract fun letterDao(): LetterDao
    abstract fun attachmentDao(): AttachmentDao
}
