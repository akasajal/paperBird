package com.ishaan.paperBird.data.local.dao

import androidx.room.*
import com.ishaan.paperBird.data.local.entities.LetterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(letter: LetterEntity): Long

    @Update
    suspend fun update(letter: LetterEntity)

    @Delete
    suspend fun delete(letter: LetterEntity)

    @Query("SELECT * FROM letters ORDER BY updated_at DESC")
    fun getAllLetters(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM letters ORDER BY updated_at DESC LIMIT 10")
    fun getRecentLetters(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM letters WHERE favorite = 1 ORDER BY updated_at DESC")
    fun getFavoriteLetters(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM letters WHERE id = :id")
    suspend fun getLetterById(id: Long): LetterEntity?

    @Query("""
        SELECT * FROM letters 
        WHERE title LIKE '%' || :query || '%' 
           OR body LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%'
        ORDER BY updated_at DESC
    """)
    fun searchLetters(query: String): Flow<List<LetterEntity>>

    @Query("UPDATE letters SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("SELECT * FROM letters WHERE category = :category ORDER BY updated_at DESC")
    fun getLettersByCategory(category: String): Flow<List<LetterEntity>>

    // For calendar: letters in a given month
    @Query("""
        SELECT * FROM letters 
        WHERE created_at >= :startMs AND created_at < :endMs
        ORDER BY created_at ASC
    """)
    suspend fun getLettersBetween(startMs: Long, endMs: Long): List<LetterEntity>

    // On This Day: letters from same month/day in any year
    @Query("SELECT * FROM letters ORDER BY updated_at DESC")
    suspend fun getAllLettersOnce(): List<LetterEntity>
}
