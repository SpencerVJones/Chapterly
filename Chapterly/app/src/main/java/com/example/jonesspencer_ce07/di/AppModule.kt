package com.example.jonesspencer_ce07.di

import android.content.Context
import androidx.room.Room
import com.example.jonesspencer_ce07.data.local.BooksDao
import com.example.jonesspencer_ce07.data.local.BooksDatabase
import com.example.jonesspencer_ce07.data.remote.GoogleBooksApi
import com.example.jonesspencer_ce07.data.repository.BooksRepository
import com.example.jonesspencer_ce07.data.repository.OfflineFirstBooksRepository
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        moshi: Moshi,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/books/v1/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleBooksApi(retrofit: Retrofit): GoogleBooksApi {
        return retrofit.create(GoogleBooksApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBooksDatabase(
        @ApplicationContext appContext: Context
    ): BooksDatabase {
        return Room.databaseBuilder(
            appContext,
            BooksDatabase::class.java,
            "books_database"
        ).build()
    }

    @Provides
    fun provideBooksDao(database: BooksDatabase): BooksDao = database.booksDao()

    @Provides
    @Singleton
    fun provideBooksRepository(
        api: GoogleBooksApi,
        database: BooksDatabase,
        booksDao: BooksDao
    ): BooksRepository = OfflineFirstBooksRepository(
        api = api,
        database = database,
        booksDao = booksDao
    )
}
