package com.meuscanais.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import com.meuscanais.core.data.repository.CatalogRepository
import com.meuscanais.core.data.repository.StreamRepository
import com.meuscanais.core.domain.interactor.PlaybackManager
import com.meuscanais.core.domain.interactor.PlaybackWatchdog
import com.meuscanais.data.model.EpgProgramme
import com.meuscanais.data.model.XtreamStream
import com.meuscanais.R
import com.meuscanais.data.repository.SettingsRepository
import com.meuscanais.domain.usecase.GetPlayerEpgUseCase
import com.meuscanais.domain.usecase.GetQuickSwitchUseCase
import com.meuscanais.domain.usecase.PerformDnsFailoverUseCase
import com.meuscanais.domain.usecase.PreparePlaybackSessionUseCase
import com.meuscanais.domain.usecase.SmartDownloadUseCase
import com.meuscanais.domain.usecase.SyncWatchProgressUseCase
import com.meuscanais.domain.usecase.ToggleFavoriteUseCase
import com.meuscanais.appfunctions.AppFunctionActionBus
import com.meuscanais.domain.model.ContentType
import com.meuscanais.domain.model.IptvItem
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import androidx.core.content.ContextCompat
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import kotlin.time.Duration.Companion.seconds

sealed class PlayerUiState(
    open val position: Long = 0L,
    open val duration: Long = 0L,
    open val name: String? = null
) {
    data class Loading(override val name: String? = null) : PlayerUiState(name = name)
    data class Playing(
        override val position: Long = 0L,
        override val duration: Long = 0L,
        override val name: String? = null,
        val streamIcon: String? = null,
        val isPlaying: Boolean = true,
        val nextEpisodeStreamId: Int? = null,
        val nextEpisodeName: String? = null,
        val contentType: ContentType = ContentType.LIVE, 
        val isLive: Boolean = false,
        val savedPosition: Long? = null,
        val tracks: Tracks? = null,
        val isFavorite: Boolean = false,
        val bandwidthMbps: Double = 0.0,
        val bufferDelaySeconds: Long = 0,
        val bufferedPosition: Long = 0L,
        val availableQualities: Map<String, Int> = emptyMap(),
        val epgId: String? = null,
        val streamId: Int? = null,
        val resolution: String? = null,
        val frameRate: Float? = null,
        val videoCodec: String? = null,
        val audioCodec: String? = null,
        val epgListings: List<com.meuscanais.data.model.EpgListing>? = null,
        val signalHealth: Float = 1.0f // 0.0 to 1.0
    ) : PlayerUiState(position, duration)
    data class Error(
        val message: String,
        override val position: Long = 0L,
        override val duration: Long = 0L
    ) : PlayerUiState(position, duration)
}

@HiltViewModel
@OptIn(UnstableApi::class)
class PlayerViewModel @Inject constructor(
    private val application: Application,
    private val catalogRepository: CatalogRepository,
    private val streamRepository: StreamRepository,
    private val settingsRepository: SettingsRepository,
    private val syncWatchProgressUseCase: SyncWatchProgressUseCase,
    private val getPlayerEpgUseCase: GetPlayerEpgUseCase,
    private val getQuickSwitchUseCase: GetQuickSwitchUseCase,
    private val performDnsFailoverUseCase: PerformDnsFailoverUseCase,
    private val preparePlaybackSessionUseCase: PreparePlaybackSessionUseCase,
    private val smartDownloadUseCase: SmartDownloadUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val actionBus: AppFunctionActionBus,
    private val playbackWatchdog: PlaybackWatchdog,
    private val playbackManager: PlaybackManager,
    private val bandwidthMeter: DefaultBandwidthMeter
) : ViewModel() {

    val currentPlayer: StateFlow<Player?> = playbackManager.playerState

    private val _zappingSessionId = MutableStateFlow(0)
    val zappingSessionId: StateFlow<Int> = _zappingSessionId.asStateFlow()

    init {
        initializeController()
        
        viewModelScope.launch {
            actionBus.actions.collect { action ->
                when (action) {
                    is AppFunctionActionBus.Action.Pause -> {
                        playbackManager.pause()
                    }
                    is AppFunctionActionBus.Action.Resume -> {
                        playbackManager.play()
                    }
                    is AppFunctionActionBus.Action.Stop -> {
                        stopPlayback()
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            playbackManager.events.collect { event ->
                handlePlaybackEvent(event)
            }
        }
    }

    private fun initializeController() {
        playbackManager.connect { 
            // Controller initialized and listener already registered in PlaybackManager
        }
    }

    private suspend fun ensurePlayer(): Player {
        if (playbackManager.player == null) {
            initializeController()
        }
        while (playbackManager.player == null) {
            delay(100)
        }
        return playbackManager.player!!
    }

    private fun updatePlayingState(p: Player) {
        val isLive = currentType == ContentType.LIVE
        val currentState = _uiState.value
        
        val videoFormat = p.currentTracks.groups.find { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }?.getTrackFormat(0)
        val audioFormat = p.currentTracks.groups.find { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }?.getTrackFormat(0)

        if (currentState is PlayerUiState.Playing) {
            _uiState.value = currentState.copy(
                position = p.currentPosition,
                duration = if (p.duration > 0) p.duration else 0L,
                isPlaying = p.playWhenReady,
                contentType = currentType,
                isLive = isLive,
                name = currentState.name,
                nextEpisodeStreamId = nextEpisodeStreamId,
                nextEpisodeName = nextEpisodeName,
                tracks = p.currentTracks,
                availableQualities = _availableQualities.value,
                epgId = currentEpgId,
                streamId = currentStreamId,
                resolution = videoFormat?.let { "${it.width}x${it.height}" },
                frameRate = videoFormat?.frameRate,
                videoCodec = videoFormat?.sampleMimeType,
                audioCodec = audioFormat?.sampleMimeType
            )
        } else {
            _uiState.value = PlayerUiState.Playing(
                position = p.currentPosition,
                duration = if (p.duration > 0) p.duration else 0L,
                isPlaying = p.playWhenReady,
                contentType = currentType,
                isLive = isLive,
                name = currentDisplayName ?: currentState.name,
                nextEpisodeStreamId = nextEpisodeStreamId,
                nextEpisodeName = nextEpisodeName,
                tracks = p.currentTracks,
                availableQualities = _availableQualities.value,
                epgId = currentEpgId,
                streamId = currentStreamId,
                resolution = videoFormat?.let { "${it.width}x${it.height}" },
                frameRate = videoFormat?.frameRate,
                videoCodec = videoFormat?.sampleMimeType,
                audioCodec = audioFormat?.sampleMimeType
            )
        }
    }

    private fun handlePlaybackEvent(event: PlaybackManager.Event) {
        val p = playbackManager.player ?: return
        
        when (event) {
            is PlaybackManager.Event.PlaybackStateChanged -> {
                if (event.state == Player.STATE_BUFFERING) {
                    rebufferingCount++
                    val currentName = (_uiState.value as? PlayerUiState.Playing)?.name ?: _uiState.value.name
                    _uiState.value = PlayerUiState.Loading(name = currentName)
                    startWatchdog()
                }
                when (event.state) {
                    Player.STATE_READY -> {
                        stopWatchdog()
                        // Reset recovery on successful playback
                        if (p.currentPosition > 1000) {
                            recoveryCount = 0
                        }
                        
                        _nextEpisodeCountdown.value = null
                        updatePlayingState(p)
                    }
                    Player.STATE_ENDED -> {
                        viewModelScope.launch {
                            val settings = settingsRepository.settingsFlow.first()
                            if (settings.autoPlayEnabled && currentType == ContentType.SERIES && nextEpisodeStreamId != null) {
                                startNextEpisodeCountdown()
                            }

                            // Smart Download Trigger
                            if (currentType == ContentType.SERIES && currentSeriesId != null && currentSeasonNumber != null && currentStreamId != null) {
                                smartDownloadUseCase(currentSeriesId!!, currentSeasonNumber!!, currentStreamId!!)
                            }
                        }
                    }
                    else -> {}
                }
            }
            is PlaybackManager.Event.TracksChanged -> {
                val currentState = _uiState.value
                if (currentState is PlayerUiState.Playing) {
                    _uiState.value = currentState.copy(tracks = event.tracks)
                }
                // Auto-select based on preferences
                applyTrackPreferences(event.tracks)
            }
            is PlaybackManager.Event.IsPlayingChanged -> {
                val currentState = _uiState.value
                if (currentState is PlayerUiState.Playing) {
                    _uiState.value = currentState.copy(isPlaying = event.isPlaying)
                }
            }
            is PlaybackManager.Event.PlayerError -> {
                stopWatchdog()
                val error = event.error
                val cause = error.cause
                val currentSession = _zappingSessionId.value
                val message = when {
                    error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                        "Erro de servidor (HTTP ${cause?.message ?: "Desconhecido"}). Verifique se sua lista permite conexões simultâneas."
                    }
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> {
                        "Falha na conexão. O servidor IPTV pode estar indisponível ou você está offline."
                    }
                    else -> application.getString(R.string.stream_playback_error)
                }
                Timber.e(error, "Erro do Player: $message")

                // Auto-Failover Logic
                if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED || 
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT) {
                    
                    if (dnsRetryCount < MAX_DNS_RETRIES) {
                        dnsRetryCount++
                        attemptDnsFailover(currentSession)
                        return
                    }
                }

                _uiState.value = PlayerUiState.Error(message)
            }
        }
    }

    private fun attemptDnsFailover(sessionIdAtError: Int) {
        viewModelScope.launch {
            if (_zappingSessionId.value != sessionIdAtError) return@launch
            
            val nextDns = performDnsFailoverUseCase()
            if (nextDns != null) {
                Timber.i("Failover [$sessionIdAtError]: Servidor alterado para $nextDns")
                _uiState.value = PlayerUiState.Error("Conexão instável. Trocando rota...")
                delay(1000)
                reloadStream()
            }
        }
    }

    private fun startWatchdog() {
        val currentSession = _zappingSessionId.value
        val p = playbackManager.player ?: return

        playbackWatchdog.startMonitoring(
            player = p,
            contentType = currentType,
            sessionId = currentSession,
            scope = viewModelScope
        ) {
            performRecovery(currentSession)
        }
    }

    private fun stopWatchdog() {
        playbackWatchdog.stop()
    }

    private fun performRecovery(sessionId: Int) {
        if (_zappingSessionId.value != sessionId) return
        
        viewModelScope.launch {
            recoveryCount++
            when {
                recoveryCount == 1 -> {
                    Timber.i("Watchdog: Tentativa 1 - Recarregando stream...")
                    reloadStream()
                }
                recoveryCount == 2 -> {
                    Timber.i("Watchdog: Tentativa 2 - Failover de rota...")
                    attemptDnsFailover(sessionId)
                }
                else -> {
                    Timber.e("Watchdog: Falha crítica após múltiplas tentativas.")
                    _uiState.value = PlayerUiState.Error("O stream parou de responder. Tente novamente mais tarde.")
                }
            }
        }
    }

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _nextEpisodeCountdown = MutableStateFlow<Int?>(null)
    val nextEpisodeCountdown: StateFlow<Int?> = _nextEpisodeCountdown.asStateFlow()

    private val _currentProgram = MutableStateFlow<EpgProgramme?>(null)
    val currentProgram: StateFlow<EpgProgramme?> = _currentProgram.asStateFlow()

    private val _nextPrograms = MutableStateFlow<List<EpgProgramme>>(emptyList())
    val nextPrograms: StateFlow<List<EpgProgramme>> = _nextPrograms.asStateFlow()

    private val _quickSwitchStreams = MutableStateFlow<List<XtreamStream>>(emptyList())
    val quickSwitchStreams: StateFlow<List<XtreamStream>> = _quickSwitchStreams.asStateFlow()

    private val _zappingEpg = MutableStateFlow<Map<Int, EpgProgramme>>(emptyMap())
    val zappingEpg: StateFlow<Map<Int, EpgProgramme>> = _zappingEpg.asStateFlow()

    private val _recentChannels = MutableStateFlow<List<XtreamStream>>(emptyList())
    val recentChannels: StateFlow<List<XtreamStream>> = _recentChannels.asStateFlow()

    private val _sleepTimer = MutableStateFlow<Int?>(null)
    val sleepTimer: StateFlow<Int?> = _sleepTimer.asStateFlow()
    private var sleepTimerJob: Job? = null

    private val _subtitleSize = MutableStateFlow(1f) // 1.0f is normal
    val subtitleSize: StateFlow<Float> = _subtitleSize.asStateFlow()

    val appSettings: StateFlow<SettingsRepository.AppSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsRepository.AppSettings(null, lastSyncTimestamp = 0L, syncIntervalHours = 24)
    )

    private val _resizeMode = MutableStateFlow(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT)
    val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

    private val _availableQualities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val availableQualities: StateFlow<Map<String, Int>> = _availableQualities.asStateFlow()

    private var recoveryCount = 0
    
    private var positionUpdateJob: Job? = null
    private var currentType: ContentType = ContentType.LIVE
    private var currentContainer: String? = null
    private var currentSeriesId: Int? = null
    private var currentSeasonNumber: Int? = null
    private var currentStreamId: Int? = null
    private var currentEpgId: String? = null
    private var currentCategoryId: String? = null
    private var nextEpisodeStreamId: Int? = null
    private var nextEpisodeName: String? = null
    private var savedProgressPosition: Long? = null
    private var lastQualities: Map<String, Int> = emptyMap()

    private var rebufferingCount = 0
    private var lastSignalCheck = System.currentTimeMillis()
    private var dnsRetryCount = 0
    private val MAX_DNS_RETRIES = 3

    sealed class NavigationEvent {
        data class NavigateToPlayer(
            val streamId: Int,
            val name: String,
            val type: ContentType,
            val container: String?
        ) : NavigationEvent()

        data class OpenExternalPlayer(
            val url: String,
            val title: String
        ) : NavigationEvent()
    }

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private var playJob: Job? = null

    private var currentDisplayName: String? = null

    fun playStream(
        streamId: Int, 
        type: ContentType, 
        container: String?, 
        epgId: String?,
        seriesId: Int? = null,
        seasonNumber: Int? = null,
        qualities: Map<String, Int> = emptyMap(),
        categoryId: String? = null,
        displayName: String? = null,
        streamIcon: String? = null
    ) {
        currentDisplayName = displayName
        // 1. LIMPEZA IMEDIATA (Síncrona)
        // Paramos o player na hora para remover o frame do canal anterior
        playbackManager.stop()
        playbackManager.clearMediaItems()
        
        // Incrementar sessão para limpar a UI
        _zappingSessionId.value += 1

        // Cancel previous play job immediately
        playJob?.cancel()
        stopWatchdog()
        recoveryCount = 0
        
        currentStreamId = streamId
        currentType = type
        currentContainer = container
        currentEpgId = epgId
        currentSeriesId = seriesId
        currentSeasonNumber = seasonNumber
        currentCategoryId = categoryId
        nextEpisodeStreamId = null
        nextEpisodeName = null
        lastQualities = qualities
        _availableQualities.value = qualities
        dnsRetryCount = 0 // Reset retries on new stream

        // Update UI State instantly with metadata even before loading starts
        if (type == ContentType.LIVE) {
            val currentState = _uiState.value
            val isFav = if (currentState is PlayerUiState.Playing) currentState.isFavorite else false
            
            _uiState.value = PlayerUiState.Playing(
                isPlaying = false,
                contentType = type,
                isLive = true,
                epgId = epgId,
                streamId = streamId,
                isFavorite = isFav,
                name = displayName ?: "Carregando...",
                streamIcon = streamIcon
            )
            
            // Clear current EPG to avoid showing old info
            _currentProgram.value = null
            _nextPrograms.value = emptyList()
        } else {
            _uiState.value = PlayerUiState.Loading(name = displayName)
        }

        playJob = viewModelScope.launch {
            try {
                val settings = settingsRepository.settingsFlow.first()
                val credentials = settings.credentials ?: run {
                    _uiState.value = PlayerUiState.Error("Credenciais não encontradas")
                    return@launch
                }
                
                // Fetch basic stream info if not provided for UI consistency
                val streams = catalogRepository.getStreamsByIds(listOf(streamId))
                val stream = streams.firstOrNull { it.streamType == type.toString().uppercase() }
                val isFav = stream?.isFavorite ?: false
                val actualName = stream?.name ?: displayName ?: "Carregando..."
                val actualIcon = stream?.logo ?: streamIcon

                // Update UI again with actual name/logo from DB
                val currentState = _uiState.value
                if (currentState is PlayerUiState.Playing) {
                    _uiState.value = currentState.copy(
                        isFavorite = isFav,
                        name = actualName,
                        streamIcon = actualIcon
                    )
                }

                Timber.d("Iniciando reprodução: $streamId ($actualName)")
                
                // Get URL immediately (fast)
                val url = streamRepository.getStreamUrl(credentials, streamId, type.toString(), container)
                
                // Optimized Login Check via UseCase
                val sessionResult = preparePlaybackSessionUseCase()
                if (sessionResult is PreparePlaybackSessionUseCase.Result.Error) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = PlayerUiState.Error(sessionResult.message)
                    }
                    return@launch
                }

                ensurePlayer()
                
                withContext(Dispatchers.Main) {
                    val metadataBuilder = MediaMetadata.Builder()
                        .setTitle(actualName)
                        .setDisplayTitle(actualName)
                        .setArtist(when (type) {
                            ContentType.LIVE -> application.getString(R.string.live_tv_title)
                            ContentType.SERIES -> application.getString(R.string.series_title)
                            ContentType.MOVIE -> application.getString(R.string.movies_title)
                            else -> "Meus Canais"
                        })
                        .setIsPlayable(true)

                    if (!actualIcon.isNullOrBlank()) {
                        try {
                            metadataBuilder.setArtworkUri(Uri.parse(actualIcon))
                        } catch (_: Exception) {}
                    }

                    val mediaItem = MediaItem.Builder()
                        .setUri(url)
                        .setMediaId(streamId.toString())
                        .setMediaMetadata(metadataBuilder.build())
                        .setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(if (type == ContentType.LIVE) 2000L else 5000L) 
                                .build()
                        )
                        .build()
                    
                    playbackManager.setMediaItem(mediaItem)
                    playbackManager.prepare()
                    playbackManager.play()
                }

                // Background tasks after starting playback
                launch(Dispatchers.IO) {
                    if (type == ContentType.LIVE) {
                        if (stream != null) {
                            addToRecentChannels(XtreamStream(
                                streamId = stream.id,
                                name = stream.name,
                                streamIcon = stream.logo,
                                epgChannelId = stream.url,
                                categoryId = stream.categoryId,
                                streamType = stream.streamType
                            ))
                        }
                        loadEpg(streamId)
                        loadQuickSwitchStreams(categoryId)
                    }
                }
                
                if (type == ContentType.SERIES && seriesId != null && seasonNumber != null) {
                    checkNextEpisode(seriesId, seasonNumber, streamId)
                }

                startPositionUpdates()
            } catch (e: CancellationException) {
                // Ignore cancellation as it means a new stream is being played
            } catch (e: Exception) {
                Timber.e(e, "Erro ao iniciar stream")
                
                if (type == ContentType.LIVE) {
                    _uiState.value = PlayerUiState.Error("Conexão falhou. Tentando reconectar...")
                    delay(2000)
                    playStream(streamId, type, container, epgId, seriesId, seasonNumber, qualities, categoryId)
                } else {
                    _uiState.value = PlayerUiState.Error("Erro ao reproduzir: ${e.localizedMessage}")
                }
            }
        }
    }

    fun reloadStream() {
        val streamId = currentStreamId ?: return
        playStream(
            streamId = streamId,
            type = currentType,
            container = currentContainer,
            epgId = currentEpgId,
            seriesId = currentSeriesId,
            seasonNumber = currentSeasonNumber,
            qualities = lastQualities,
            categoryId = currentCategoryId
        )
    }

    private fun addToRecentChannels(stream: XtreamStream) {
        val currentList = _recentChannels.value.toMutableList()
        currentList.removeAll { it.streamId == stream.streamId }
        currentList.add(0, stream)
        _recentChannels.value = currentList.take(10)
    }

    fun setSleepTimer(minutes: Int?) {
        _sleepTimer.value = minutes
        sleepTimerJob?.cancel()
        if (minutes != null && minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                var remaining = minutes
                while (remaining > 0) {
                    delay(60.seconds)
                    remaining--
                    _sleepTimer.value = remaining
                }
                stopPlayback()
                _sleepTimer.value = null
            }
        }
    }

    fun setSubtitleSize(size: Float) {
        _subtitleSize.value = size
    }

    private fun checkNextEpisode(seriesId: Int, seasonNumber: Int, streamId: Int) {
        viewModelScope.launch {
            try {
                val credentials = settingsRepository.settingsFlow.first().credentials ?: return@launch
                val (_, episodesMap) = catalogRepository.getSeriesInfo(credentials, seriesId)
                val episodes = episodesMap[seasonNumber] ?: emptyList()
                val currentIndex = episodes.indexOfFirst { it.streamId == streamId }
                val nextEpisode = if (currentIndex != -1 && currentIndex < episodes.size - 1) {
                    episodes[currentIndex + 1]
                } else null
                
                nextEpisodeStreamId = nextEpisode?.streamId
                nextEpisodeName = nextEpisode?.title

                val currentState = _uiState.value
                if (currentState is PlayerUiState.Playing) {
                    _uiState.value = currentState.copy(
                        nextEpisodeStreamId = nextEpisodeStreamId,
                        nextEpisodeName = nextEpisodeName
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                val p = playbackManager.player ?: break
                if (p.isPlaying) {
                    val currentPos = p.currentPosition
                    val duration = p.duration
                    if (currentType != ContentType.LIVE && currentStreamId != null && duration > 0) {
                        syncWatchProgressUseCase(
                            streamId = currentStreamId!!,
                            type = currentType.toString(),
                            position = currentPos,
                            duration = duration,
                            seriesId = currentSeriesId
                        )
                    }
                    val currentState = _uiState.value
                    if (currentState is PlayerUiState.Playing) {
                        val bandwidth = bandwidthMeter.bitrateEstimate / 1_000_000.0
                        val bufferedPos = p.bufferedPosition
                        val delay = if (p.isCurrentMediaItemLive) (p.duration - currentPos) / 1000 else (bufferedPos - currentPos) / 1000
                        
                        // Signal Health Logic
                        val now = System.currentTimeMillis()
                        if (now - lastSignalCheck > 30000) { // Every 30s check stability
                            rebufferingCount = 0
                            lastSignalCheck = now
                        }
                        val health = (1.0f - (rebufferingCount * 0.15f)).coerceIn(0.1f, 1.0f)

                        val videoFormat = p.currentTracks.groups.find { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }?.getTrackFormat(0)
                        val audioFormat = p.currentTracks.groups.find { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }?.getTrackFormat(0)

                        _uiState.value = currentState.copy(
                            position = currentPos,
                            duration = if (duration > 0) duration else 0L,
                            savedPosition = if (currentPos < 1000) savedProgressPosition else currentState.savedPosition,
                            bandwidthMbps = bandwidth,
                            bufferDelaySeconds = delay.coerceAtLeast(0),
                            bufferedPosition = bufferedPos,
                            resolution = videoFormat?.let { "${it.width}x${it.height}" },
                            frameRate = videoFormat?.frameRate,
                            videoCodec = videoFormat?.sampleMimeType,
                            audioCodec = audioFormat?.sampleMimeType,
                            signalHealth = health
                        )
                    }
                }
                delay(2000)
            }
        }
    }

    fun togglePlayPause() {
        if (playbackManager.player?.isPlaying == true) {
            playbackManager.pause()
        } else {
            playbackManager.play()
        }
    }

    fun seekTo(position: Long) {
        playbackManager.seekTo(position)
    }

    fun seekForward() {
        playbackManager.player?.let { p ->
            playbackManager.seekTo((p.currentPosition + 10000).coerceAtMost(p.duration))
        }
    }

    fun seekBack() {
        playbackManager.player?.let { p ->
            playbackManager.seekTo((p.currentPosition - 10000).coerceAtLeast(0L))
        }
    }

    fun playByNumber(number: String) {
        viewModelScope.launch {
            try {
                val num = number.toIntOrNull() ?: return@launch
                val streams = _quickSwitchStreams.value
                if (streams.isEmpty()) return@launch
                
                // Try to find by 'num' property first, then by index
                val target = streams.find { it.num == num } 
                    ?: if (num > 0 && num <= streams.size) streams[num - 1] else null
                
                target?.let {
                    playStream(
                        streamId = it.streamId ?: 0,
                        type = ContentType.LIVE,
                        container = null,
                        epgId = it.epgChannelId,
                        categoryId = currentCategoryId,
                        displayName = it.name,
                        streamIcon = it.streamIcon
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao reproduzir por número")
            }
        }
    }

    fun playNextChannel() {
        val streams = _quickSwitchStreams.value
        val currentId = currentStreamId ?: return
        if (streams.isEmpty()) return
        
        val currentIndex = streams.indexOfFirst { it.streamId == currentId }
        val nextIndex = (currentIndex + 1) % streams.size
        val target = streams[nextIndex]
        
        Timber.i("Zapping: Próximo Canal (${target.name})")
        
        // Instant play call with displayName for immediate UI update
        playStream(
            streamId = target.streamId ?: 0,
            type = ContentType.LIVE,
            container = null,
            epgId = target.epgChannelId,
            categoryId = currentCategoryId,
            displayName = target.name,
            streamIcon = target.streamIcon
        )
    }

    fun playPreviousChannel() {
        val streams = _quickSwitchStreams.value
        val currentId = currentStreamId ?: return
        if (streams.isEmpty()) return
        
        val currentIndex = streams.indexOfFirst { it.streamId == currentId }
        val prevIndex = if (currentIndex <= 0) streams.size - 1 else currentIndex - 1
        val target = streams[prevIndex]
        
        Timber.i("Zapping: Canal Anterior (${target.name})")
        
        // Instant play call with displayName for immediate UI update
        playStream(
            streamId = target.streamId ?: 0,
            type = ContentType.LIVE,
            container = null,
            epgId = target.epgChannelId,
            categoryId = currentCategoryId,
            displayName = target.name,
            streamIcon = target.streamIcon
        )
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackManager.setPlaybackSpeed(speed)
    }

    fun toggleResizeMode() {
        _resizeMode.value = when (_resizeMode.value) {
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    private fun startNextEpisodeCountdown() {
        viewModelScope.launch {
            for (i in 5 downTo 1) {
                _nextEpisodeCountdown.value = i
                delay(1000)
            }
            _nextEpisodeCountdown.value = null
            playNextEpisode()
        }
    }

    fun cancelCountdown() {
        _nextEpisodeCountdown.value = null
    }

    fun playNextEpisode() {
        val currentState = _uiState.value as? PlayerUiState.Playing
        val nextId = currentState?.nextEpisodeStreamId ?: return
        val nextName = currentState.nextEpisodeName ?: "Next Episode"
        viewModelScope.launch {
            _navigationEvent.emit(NavigationEvent.NavigateToPlayer(nextId, nextName, currentType, currentContainer))
        }
    }

    fun toggleFavorite() {
        val streamId = currentStreamId ?: return
        val currentState = _uiState.value as? PlayerUiState.Playing ?: return
        val newFav = !currentState.isFavorite
        viewModelScope.launch {
            try {
                val streams = catalogRepository.getStreamsByIds(listOf(streamId))
                val stream = streams.firstOrNull { it.streamType == currentType.toString().uppercase() }
                if (stream != null) {
                    val item = IptvItem(
                        id = stream.id.toString(),
                        name = stream.name,
                        icon = stream.logo,
                        type = currentType,
                        epgId = stream.url,
                        rating = stream.rating,
                        releaseDate = stream.releaseDate,
                        containerExtension = stream.containerExtension,
                        categoryId = stream.categoryId
                    )
                    
                    val settings = settingsRepository.settingsFlow.first()
                    val profileId = settings.activeProfileId
                    val hash = settings.credentials?.providerHash
                    
                    toggleFavoriteUseCase(item, newFav, profileId, hash)
                    _uiState.value = currentState.copy(isFavorite = newFav)
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao favoritar")
            }
        }
    }

    private fun loadEpg(streamId: Int) {
        viewModelScope.launch {
            try {
                val result = getPlayerEpgUseCase(streamId)
                _currentProgram.value = result.current
                _nextPrograms.value = result.next
                
                val currentState = _uiState.value
                if (currentState is PlayerUiState.Playing) {
                    _uiState.value = currentState.copy(epgListings = result.allListings)
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar EPG")
                _currentProgram.value = null
            }
        }
    }

    fun stopPlayback() {
        syncFinalProgress()
        stopWatchdog()
        playbackManager.stopAndDisconnect()
        positionUpdateJob?.cancel()
    }

    private fun syncFinalProgress() {
        val p = playbackManager.player ?: return
        val currentPos = p.currentPosition
        val duration = p.duration
        val streamId = currentStreamId
        if (currentType != ContentType.LIVE && streamId != null && duration > 0) {
            viewModelScope.launch {
                syncWatchProgressUseCase(
                    streamId = streamId,
                    type = currentType.toString(),
                    position = currentPos,
                    duration = duration,
                    seriesId = currentSeriesId,
                    forceSync = true
                )
            }
        }
    }

    fun selectTrack(groupId: Int, trackIndex: Int, trackType: Int) {
        playbackManager.player?.let { p ->
            val tracks = p.currentTracks
            val groups = tracks.groups.filter { it.type == trackType }
            if (groupId < groups.size) {
                val trackGroup = groups[groupId]
                val format = trackGroup.getTrackFormat(trackIndex)
                
                // Save preference
                viewModelScope.launch {
                    if (trackType == C.TRACK_TYPE_AUDIO) {
                        settingsRepository.updatePreferredAudioLang(format.language)
                    } else if (trackType == C.TRACK_TYPE_TEXT) {
                        settingsRepository.updatePreferredSubtitleLang(format.language)
                    }
                }

                playbackManager.setTrackSelectionParameters(
                    p.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(TrackSelectionOverride(trackGroup.mediaTrackGroup, trackIndex))
                        .build()
                )
            }
        }
    }

    private fun applyTrackPreferences(tracks: Tracks) {
        val player = playbackManager.player ?: return
        val settings = appSettings.value
        
        var paramsBuilder = player.trackSelectionParameters.buildUpon()
        var changed = false

        // Audio
        if (settings.preferredAudioLang != null) {
            val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            audioGroups.forEach { group ->
                for (i in 0 until group.length) {
                    if (group.getTrackFormat(i).language == settings.preferredAudioLang) {
                        paramsBuilder = paramsBuilder.setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, i)
                        )
                        changed = true
                        break
                    }
                }
            }
        }

        // Subtitles
        if (settings.preferredSubtitleLang != null) {
            val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            textGroups.forEach { group ->
                for (i in 0 until group.length) {
                    if (group.getTrackFormat(i).language == settings.preferredSubtitleLang) {
                        paramsBuilder = paramsBuilder.setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, i)
                        )
                        changed = true
                        break
                    }
                }
            }
        }

        if (changed) {
            playbackManager.setTrackSelectionParameters(paramsBuilder.build())
        }
    }

    fun clearTrackOverride(@Suppress("UNUSED_PARAMETER") trackType: Int) {
        playbackManager.clearTrackOverrides()
    }

    private fun loadQuickSwitchStreams(categoryId: String? = null) {
        viewModelScope.launch {
            try {
                val streams = getQuickSwitchUseCase.getStreams(categoryId)
                _quickSwitchStreams.value = streams
                
                getQuickSwitchUseCase.loadZappingEpg(streams).collect { (streamId, programme) ->
                    _zappingEpg.update { current ->
                        val next = current + (streamId to programme)
                        if (next.size > 50) next.toList().takeLast(50).toMap() else next
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Erro ao carregar streams de zapping")
            }
        }
    }

    override fun onCleared() {
        stopWatchdog()
        stopPlayback()
    }
}
