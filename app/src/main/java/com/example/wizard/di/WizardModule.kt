package com.example.wizard.di

import com.example.wizard.data.WizardRepositoryImpl
import com.example.wizard.domain.repository.WizardRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module(includes = [RepositoryModule::class])
@InstallIn(SingletonComponent::class)
object WizardModule {

    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }
}

@Suppress("UNUSED")
@Module
@InstallIn(SingletonComponent::class)
private interface RepositoryModule {

    @Binds
    fun bindRepository(repository: WizardRepositoryImpl): WizardRepository
}
