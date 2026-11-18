package uz.beko404.track14.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.beko404.track14.domain.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun clock(): Clock = Clock { System.currentTimeMillis() }
}
