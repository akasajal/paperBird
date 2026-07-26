package com.ishaan.paperBird.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.ishaan.paperBird.data.local.PaperBirdDatabase
import com.ishaan.paperBird.data.local.dao.AttachmentDao
import com.ishaan.paperBird.data.local.dao.LetterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paperBird_settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PaperBirdDatabase =
        Room.databaseBuilder(context, PaperBirdDatabase::class.java, "paperBird.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLetterDao(db: PaperBirdDatabase): LetterDao = db.letterDao()

    @Provides
    fun provideAttachmentDao(db: PaperBirdDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}
