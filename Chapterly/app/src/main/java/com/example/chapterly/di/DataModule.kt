package com.example.chapterly.di

import android.content.Context
import com.example.chapterly.BuildConfig
import androidx.room.Room
import com.example.chapterly.data.cloud.CloudSyncDataSource
import com.example.chapterly.data.local.BookDao
import com.example.chapterly.data.local.BookDatabase
import com.example.chapterly.data.remote.ChapterlyBackendApi
import com.example.chapterly.data.remote.GoogleBooksApi
import com.example.chapterly.data.repository.BookRepository
import com.example.chapterly.data.repository.OfflineFirstBookRepository
import com.example.chapterly.data.repository.NetworkProductRepository
import com.example.chapterly.data.repository.ProductRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi =
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    @Provides
    @Singleton
    @Named("books")
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        val apiKey = BuildConfig.BOOKS_API_KEY.trim()

        return OkHttpClient.Builder().apply {
            // Log before the API key is added so the secret never appears in logs.
            addInterceptor(loggingInterceptor)
            if (apiKey.isNotEmpty()) {
                addInterceptor { chain ->
                    val originalRequest = chain.request()
                    val updatedRequest =
                        originalRequest.withQueryParameter(
                            name = "key",
                            value = apiKey,
                        )
                    chain.proceed(updatedRequest)
                }
            }
        }.build()
    }

    @Provides
    @Singleton
    @Named("backend")
    fun provideBackendOkHttpClient(firebaseAuth: FirebaseAuth?): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val currentUser = firebaseAuth?.currentUser
                val token =
                    currentUser?.let { user ->
                        runCatching {
                            Tasks.await(user.getIdToken(false)).token
                        }.getOrNull()
                    }.orEmpty()

                if (token.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                requestBuilder.header(
                    "X-Debug-Uid",
                    currentUser?.uid?.ifBlank { null } ?: LOCAL_DEBUG_UID,
                )

                chain.proceed(requestBuilder.build())
            }.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        moshi: Moshi,
        @Named("books") okHttpClient: OkHttpClient,
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
    @Named("backend")
    fun provideBackendRetrofit(
        moshi: Moshi,
        @Named("backend") backendOkHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(backendOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideChapterlyBackendApi(
        @Named("backend") retrofit: Retrofit,
    ): ChapterlyBackendApi {
        return retrofit.create(ChapterlyBackendApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBookDatabase(
        @ApplicationContext appContext: Context,
    ): BookDatabase {
        return Room.databaseBuilder(
            appContext,
            BookDatabase::class.java,
            "books_database",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBookDao(database: BookDatabase): BookDao = database.bookDao()

    @Provides
    @Singleton
    fun provideBookRepository(
        api: GoogleBooksApi,
        database: BookDatabase,
        bookDao: BookDao,
        cloudSyncDataSource: CloudSyncDataSource,
    ): BookRepository =
        OfflineFirstBookRepository(
            api = api,
            database = database,
            bookDao = bookDao,
            cloudSyncDataSource = cloudSyncDataSource,
        )

    @Provides
    @Singleton
    fun provideProductRepository(
        backendApi: ChapterlyBackendApi,
    ): ProductRepository = NetworkProductRepository(backendApi)
}

private fun Request.withQueryParameter(
    name: String,
    value: String,
): Request =
    newBuilder()
        .url(
            url.newBuilder()
                .setQueryParameter(name, value)
                .build(),
        )
        .build()

private const val LOCAL_DEBUG_UID = "chapterly-local-debug"
