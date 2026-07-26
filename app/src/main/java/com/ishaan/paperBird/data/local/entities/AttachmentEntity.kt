package com.ishaan.paperBird.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "letter_attachments",
    foreignKeys = [
        ForeignKey(
            entity = LetterEntity::class,
            parentColumns = ["id"],
            childColumns = ["letter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("letter_id")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "letter_id") val letterId: Long,
    val filename: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    // Store file URI path string instead of BLOB for Android (better for large images)
    @ColumnInfo(name = "uri_path") val uriPath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
