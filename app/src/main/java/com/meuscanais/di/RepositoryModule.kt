package com.meuscanais.di

import android.content.Context
import com.meuscanais.data.api.XtreamService
import com.meuscanais.data.local.dao.IptvDao
import com.meuscanais.data.network.EpgService
import com.meuscanais.data.repository.SettingsRepository
import com.meuscanais.core.data.repository.CatalogRepository
import com.meuscanais.core.data.repository.EpgRepository
import com.meuscanais.core.data.repository.StreamRepository
import com.meuscanais.core.data.repository.UserRepository
import com.meuscanais.core.domain.interactor.DnsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideCatalogRepository(
        xtreamService: XtreamService,
        iptvDao: IptvDao,
        epgRepository: EpgRepository
    ): CatalogRepository {
        return CatalogRepository(xtreamService, iptvDao, epgRepository)
    }

    @Provides
    @Singleton
    fun provideUserRepository(iptvDao: IptvDao): UserRepository {
        return UserRepository(iptvDao)
    }

    @Provides
    @Singleton
    fun provideEpgRepository(
        xtreamService: XtreamService,
        epgService: EpgService,
        iptvDao: IptvDao
    ): EpgRepository {
        return EpgRepository(xtreamService, epgService, iptvDao)
    }

    @Provides
    @Singleton
    fun provideStreamRepository(): StreamRepository {
        return StreamRepository()
    }

    @Provides
    @Singleton
    fun provideDnsManager(@ApiOkHttpClient okHttpClient: OkHttpClient): DnsManager {
        return DnsManager(okHttpClient)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}
