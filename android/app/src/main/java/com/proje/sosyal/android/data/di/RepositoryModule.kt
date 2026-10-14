package com.proje.sosyal.android.data.di

import com.proje.sosyal.android.core.session.EncryptedTokenStore
import com.proje.sosyal.android.core.session.KeystoreTokenCipher
import com.proje.sosyal.android.core.session.TokenCipher
import com.proje.sosyal.android.core.session.TokenStore
import com.proje.sosyal.android.data.repository.AuthRepositoryImpl
import com.proje.sosyal.android.data.repository.SystemRepositoryImpl
import com.proje.sosyal.android.data.repository.UserRepositoryImpl
import com.proje.sosyal.android.domain.repository.AuthRepository
import com.proje.sosyal.android.domain.repository.SystemRepository
import com.proje.sosyal.android.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSystemRepository(impl: SystemRepositoryImpl): SystemRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore

    @Binds
    abstract fun bindTokenCipher(impl: KeystoreTokenCipher): TokenCipher
}
