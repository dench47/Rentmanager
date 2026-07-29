package com.rentmanager.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Здесь можно добавить общие зависимости приложения
    // (например, SharedPreferences, Room Database и т.д.)
}