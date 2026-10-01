package com.meuscanais.di

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import com.meuscanais.IptvApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    @OptIn(UnstableApi::class)
    fun provideBandwidthMeter(@ApplicationContext context: Context): DefaultBandwidthMeter {
        return DefaultBandwidthMeter.Builder(context).build()
    }

    @Provides
    @Singleton
    @OptIn(UnstableApi::class)
    @DownloadCache
    fun provideDownloadCache(@ApplicationContext context: Context): SimpleCache {
        return (context as IptvApplication).downloadCache
    }

    @Provides
    @Singleton
    @OptIn(UnstableApi::class)
    @PlaybackCache
    fun providePlaybackCache(@ApplicationContext context: Context): SimpleCache {
        return (context as IptvApplication).playbackCache
    }

    @Provides
    @Singleton
    @OptIn(UnstableApi::class)
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        @PlayerOkHttpClient okHttpClient: OkHttpClient,
        bandwidthMeter: DefaultBandwidthMeter,
        @PlaybackCache playbackCache: SimpleCache
    ): ExoPlayer {
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("IPTVSmarters")

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(playbackCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Optimize TS extractors for IPTV
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)
            .setTsExtractorTimestampSearchBytes(TsExtractor.DEFAULT_TIMESTAMP_SEARCH_BYTES * 2)

        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(cacheDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2500,  // minBufferMs (Lower for faster start)
                30000, // maxBufferMs
                1000,  // bufferForPlaybackMs
                2000   // bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(10000, true)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .build()
    }
}
