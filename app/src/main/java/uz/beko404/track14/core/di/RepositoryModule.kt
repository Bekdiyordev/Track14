package uz.beko404.track14.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.beko404.track14.data.apps.FirestoreAppsRepository
import uz.beko404.track14.data.auth.FirebaseAuthRepository
import uz.beko404.track14.data.profile.FirestoreProfileRepository
import uz.beko404.track14.data.tests.FirestoreTestsRepository
import uz.beko404.track14.data.owner.FirestoreOwnerRepository
import uz.beko404.track14.data.economy.FirestoreEconomyRepository
import uz.beko404.track14.data.config.FirestoreConfigRepository
import uz.beko404.track14.data.published.FirestorePublishedRepository
import uz.beko404.track14.data.social.FirestoreFeedbackRepository
import uz.beko404.track14.data.social.FirestoreNotificationsRepository
import uz.beko404.track14.data.social.FirestoreReportRepository
import uz.beko404.track14.data.wallet.FirestoreWalletRepository
import uz.beko404.track14.domain.repository.AppsRepository
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.ProfileRepository
import uz.beko404.track14.domain.repository.TestsRepository
import uz.beko404.track14.domain.repository.OwnerRepository
import uz.beko404.track14.domain.repository.EconomyRepository
import uz.beko404.track14.domain.repository.ConfigRepository
import uz.beko404.track14.domain.repository.PublishedRepository
import uz.beko404.track14.domain.repository.FeedbackRepository
import uz.beko404.track14.domain.repository.NotificationsRepository
import uz.beko404.track14.domain.repository.ReportRepository
import uz.beko404.track14.domain.repository.WalletRepository
import javax.inject.Singleton

/** Binds the domain contracts to their Firebase implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun auth(impl: FirebaseAuthRepository): AuthRepository

    @Binds @Singleton
    abstract fun profile(impl: FirestoreProfileRepository): ProfileRepository

    @Binds @Singleton
    abstract fun wallet(impl: FirestoreWalletRepository): WalletRepository

    @Binds @Singleton
    abstract fun tests(impl: FirestoreTestsRepository): TestsRepository

    @Binds @Singleton
    abstract fun owner(impl: FirestoreOwnerRepository): OwnerRepository

    @Binds @Singleton
    abstract fun economy(impl: FirestoreEconomyRepository): EconomyRepository

    @Binds @Singleton
    abstract fun feedback(impl: FirestoreFeedbackRepository): FeedbackRepository

    @Binds @Singleton
    abstract fun reports(impl: FirestoreReportRepository): ReportRepository

    @Binds @Singleton
    abstract fun notifications(impl: FirestoreNotificationsRepository): NotificationsRepository

    @Binds @Singleton
    abstract fun config(impl: FirestoreConfigRepository): ConfigRepository

    @Binds @Singleton
    abstract fun published(impl: FirestorePublishedRepository): PublishedRepository

    @Binds @Singleton
    abstract fun apps(impl: FirestoreAppsRepository): AppsRepository
}
