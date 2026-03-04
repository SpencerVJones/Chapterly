package com.example.chapterly.di

import android.content.Context
import com.example.chapterly.data.auth.AuthRepository
import com.example.chapterly.data.auth.FirebaseAuthRepository
import com.example.chapterly.data.cloud.CloudSyncDataSource
import com.example.chapterly.data.cloud.FirestoreCloudSyncDataSource
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseApp(
        @ApplicationContext appContext: Context,
    ): FirebaseApp? {
        val existing = FirebaseApp.getApps(appContext).firstOrNull()
        return existing ?: FirebaseApp.initializeApp(appContext)
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(firebaseApp: FirebaseApp?): FirebaseAuth? {
        return firebaseApp?.let { app -> FirebaseAuth.getInstance(app) }
    }

    @Provides
    @Singleton
    fun provideFirestore(firebaseApp: FirebaseApp?): FirebaseFirestore? {
        return firebaseApp?.let { app -> FirebaseFirestore.getInstance(app) }
    }

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth?): AuthRepository {
        return FirebaseAuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideCloudSyncDataSource(
        firebaseAuth: FirebaseAuth?,
        firestore: FirebaseFirestore?,
    ): CloudSyncDataSource {
        return FirestoreCloudSyncDataSource(firebaseAuth, firestore)
    }
}
