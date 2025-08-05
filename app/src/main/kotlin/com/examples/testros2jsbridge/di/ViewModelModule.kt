package com.examples.testros2jsbridge.di

/*
Proper ViewModel injection instead of manual factory pattern
 */

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {
    // No manual ViewModel providers needed with @HiltViewModel.
}