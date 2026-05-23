package com.sdd.marketplace.core.di

import com.sdd.marketplace.data.repository.*
import com.sdd.marketplace.domain.repository.*
import com.sdd.marketplace.data.repository.BoostRepositoryImpl
import com.sdd.marketplace.domain.repository.BoostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository
    @Binds @Singleton abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds @Singleton abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository
    @Binds @Singleton abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository
    @Binds @Singleton abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
    @Binds @Singleton abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository
    @Binds @Singleton abstract fun bindBlockRepository(impl: BlockRepositoryImpl): BlockRepository
    @Binds @Singleton abstract fun bindKycRepository(impl: KycRepositoryImpl): KycRepository
    @Binds @Singleton abstract fun bindModerationRepository(impl: ModerationRepositoryImpl): ModerationRepository
    @Binds @Singleton abstract fun bindCouponRepository(impl: CouponRepositoryImpl): CouponRepository
    @Binds @Singleton abstract fun bindBoostRepository(impl: BoostRepositoryImpl): BoostRepository
}
