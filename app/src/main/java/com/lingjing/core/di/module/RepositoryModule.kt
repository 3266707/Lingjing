package com.lingjing.core.di.module

import com.lingjing.data.repository.impl.AttributeRepositoryImpl
import com.lingjing.data.repository.impl.DailyStateRepositoryImpl
import com.lingjing.data.repository.impl.PlanRepositoryImpl
import com.lingjing.domain.repository.AttributeRepository
import com.lingjing.domain.repository.DailyStateRepository
import com.lingjing.domain.repository.PlanRepository
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
    abstract fun bindPlanRepository(impl: PlanRepositoryImpl): PlanRepository

    @Binds
    @Singleton
    abstract fun bindAttributeRepository(impl: AttributeRepositoryImpl): AttributeRepository

    @Binds
    @Singleton
    abstract fun bindDailyStateRepository(impl: DailyStateRepositoryImpl): DailyStateRepository
}
