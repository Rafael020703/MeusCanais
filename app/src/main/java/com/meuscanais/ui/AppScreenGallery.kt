@file:OptIn(UnstableApi::class)
package com.meuscanais.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.meuscanais.R
import com.meuscanais.data.local.entities.*
import com.meuscanais.data.model.*
import com.meuscanais.data.repository.SettingsRepository
import com.meuscanais.data.model.XtreamCredentials
import com.meuscanais.domain.model.*
import com.meuscanais.ui.account.AccountContent
import com.meuscanais.ui.content.*
import com.meuscanais.ui.content.components.CategoryListItem
import com.meuscanais.ui.content.components.SortMenuDialog
import com.meuscanais.ui.content.components.PinUnlockDialog
import com.meuscanais.ui.content.components.ContentGrid
import com.meuscanais.core.ui.components.navigation.AppHeader
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.meuscanais.ui.content.player.*
import com.meuscanais.ui.dashboard.*
import com.meuscanais.ui.epg.EpgGridContent
import com.meuscanais.ui.login.LoginContent
import com.meuscanais.ui.login.LoginViewModel
import com.meuscanais.ui.login.SyncItem
import com.meuscanais.core.domain.state.AppSyncProgress
import com.meuscanais.core.domain.state.AppSyncStatus
import com.meuscanais.core.domain.interactor.SyncManager
import com.meuscanais.core.navigation.AppLockScreen
import com.meuscanais.core.navigation.ExitConfirmDialog
import com.meuscanais.ui.search.SearchScreenContent
import com.meuscanais.ui.series.SeriesDetailContent
import com.meuscanais.ui.settings.SettingsCategory
import com.meuscanais.ui.settings.SettingsScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.meuscanais.core.ui.components.settings.SettingsGridCard
import com.meuscanais.core.ui.components.badges.*
import com.meuscanais.core.ui.components.buttons.*
import com.meuscanais.core.ui.components.cards.*
import com.meuscanais.core.ui.components.cards.CinematicActionCard
import com.meuscanais.core.ui.components.states.*
import com.meuscanais.core.ui.components.inputs.*
import com.meuscanais.core.ui.components.common.*
import com.meuscanais.core.ui.theme.AppDesignSystem
import com.meuscanais.core.ui.theme.FocusGlowCyan
import com.meuscanais.core.ui.theme.MeusCanaisTheme
import com.meuscanais.core.ui.theme.PrimaryCyan
import com.meuscanais.core.ui.theme.Spacing
import com.meuscanais.core.ui.theme.ThemeLive
import com.meuscanais.core.ui.theme.ThemeMovies
import com.meuscanais.core.ui.theme.ThemeSeries
import com.meuscanais.core.ui.theme.ThemeSecondary
import com.meuscanais.core.ui.theme.ThemeEPG
import com.meuscanais.core.ui.theme.ThemeMultiView
import com.meuscanais.core.ui.theme.ThemeDownloads
import com.meuscanais.core.ui.theme.ThemeFavorites
import com.meuscanais.core.ui.responsive.ResponsiveLayout
import com.meuscanais.ui.viewmodel.MainViewModel
import com.meuscanais.ui.viewmodel.SearchResult
import com.meuscanais.ui.viewmodel.PlayerUiState
import com.meuscanais.util.DeviceType
import com.meuscanais.util.WindowInfo
import com.meuscanais.util.WindowSize
import androidx.compose.ui.focus.FocusRequester

/**
 * APP SCREEN GALLERY — CLEAN CATALOG (ONLINE FOCUS)
 * 
 * Este catálogo foca nos estados principais de sucesso (Happy Path) e funcionamento online.
 * Todas as telas mantêm o padrão horizontal 16:9.
 */

// --- MOCK DATA ---

@UnstableApi
private object GalleryMocks {
    val windowLandscape = WindowInfo(
        widthSize = WindowSize.EXPANDED,
        heightSize = WindowSize.COMPACT,
        screenWidth = 1280.dp,
        screenHeight = 720.dp,
        deviceType = DeviceType.PHONE
    )

    val windowTV = WindowInfo(
        widthSize = WindowSize.EXPANDED,
        heightSize = WindowSize.EXPANDED,
        screenWidth = 1920.dp,
        screenHeight = 1080.dp,
        deviceType = DeviceType.TV
    )

    val syncProgressDefault = AppSyncProgress(
        accountStatus = AppSyncStatus.Success,
        liveStatus = AppSyncStatus.Done(500),
        vodStatus = AppSyncStatus.Done(1200),
        seriesStatus = AppSyncStatus.Done(300),
        isComplete = true
    )

    val userProfile = UserProfile(id = "1", name = "Visitante", isChild = false)
    
    val userInfo = UserInfo(
        username = "premium_user",
        status = "Active",
        expDate = (System.currentTimeMillis() / 1000 + 86400 * 30).toString(),
        activeCons = "1",
        maxConnections = "2"
    )

    val categories = listOf(
        XtreamCategory("1", "Esportes"),
        XtreamCategory("2", "Notícias"),
        XtreamCategory("3", "Filmes 2024"),
        XtreamCategory("4", "Documentários")
    )

    val channels = listOf(
        IptvItem("101", "ESPN Brasil", "https://logodownload.org/wp-content/uploads/2021/06/espn-logo.png", ContentType.LIVE),
        IptvItem("102", "Globo News", null, ContentType.LIVE),
        IptvItem("103", "Discovery", null, ContentType.LIVE)
    )

    val movies = listOf(
        IptvItem("201", "Oppenheimer", "https://image.tmdb.org/t/p/w500/8Gxv2mYnrnvrM3Q4QvxQoffQHDs.jpg", ContentType.MOVIE, rating = "8.5"),
        IptvItem("202", "Duna: Parte 2", null, ContentType.MOVIE, rating = "9.0")
    )

    val vodInfo = XtreamVodInfo(
        info = VodDetails(
            name = "Oppenheimer",
            plot = "A história do físico americano J. Robert Oppenheimer e seu papel no desenvolvimento da bomba atômica. Um filme épico de Christopher Nolan.",
            cast = "Cillian Murphy, Emily Blunt, Matt Damon",
            director = "Christopher Nolan",
            genre = "Drama, História",
            releaseDate = "2023",
            rating = "8.6",
            duration = "3h 0min",
            youtubeTrailer = "uYPbbksJxIg"
        )
    )

    val seriesInfo = XtreamSeries(
        seriesId = 301,
        name = "Succession",
        cover = "https://image.tmdb.org/t/p/w500/7y97hl9czqjP7tT961UCJyqQC68.jpg",
        plot = "A saga da família Roy, que controla um dos maiores impérios de mídia e entretenimento do mundo.",
        cast = "Brian Cox, Jeremy Strong, Sarah Snook",
        director = "Jesse Armstrong",
        genre = "Drama",
        rating = "8.8"
    )

    val seasons = listOf(
        SeasonEntity(301, 1, "Temporada 1", null, "SERIES"),
        SeasonEntity(301, 2, "Temporada 2", null, "SERIES")
    )

    val episodes = mapOf(
        1 to listOf(
            EpisodeEntity(301, 1, 401, 1, "Celebração", 401, "mp4", null, "O aniversário de Logan Roy.", "60min", "8.5", "SERIES"),
            EpisodeEntity(301, 1, 402, 2, "Merda Show na Prefeitura", 402, "mp4", null, "Kendall tenta lidar com a crise.", "60min", "8.5", "SERIES")
        )
    )

    val epgPrograms = mapOf(
        "101" to listOf(
            EpgProgramEntity("101", System.currentTimeMillis()/1000, System.currentTimeMillis()/1000 + 3600, "Jogo Aberto", "Debate esportivo"),
            EpgProgramEntity("101", System.currentTimeMillis()/1000 + 3600, System.currentTimeMillis()/1000 + 7200, "Os Donos da Bola", "Neto e convidados")
        )
    )
    
    val playerMockEpg = EpgProgramme(
        start = "20241030100000", 
        stop = "20241030110000", 
        channelId = "101", 
        title = "Jogo Aberto", 
        description = "Debate esportivo"
    )

    val appSettings = SettingsRepository.AppSettings(
        credentials = null,
        accounts = listOf(XtreamCredentials("user", "pass", "https://server.tv")),
        currentAccountIndex = 0,
        lastSyncTimestamp = 0L,
        syncIntervalHours = 24
    )
}

// --- GALLERY WRAPPER COMPONENT ---

@Composable
private fun GalleryIndividualFrame(
    title: String,
    deviceInfo: String,
    isTv: Boolean = true,
    content: @Composable () -> Unit
) {
    val tokens = AppDesignSystem
    MeusCanaisTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.colors.background)
                .padding(tokens.spacing.large)
        ) {
            Row(
                modifier = Modifier.padding(bottom = tokens.spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isTv) Icons.Rounded.Tv else Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = tokens.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(tokens.spacing.small))
                Text(
                    text = title.uppercase(),
                    style = tokens.typography.title,
                    fontWeight = FontWeight.Black,
                    color = tokens.colors.textPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(tokens.spacing.large))
                Text(
                    text = deviceInfo,
                    style = tokens.typography.caption,
                    color = tokens.colors.textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize().aspectRatio(16/9f),
                shape = tokens.shapes.large,
                color = tokens.colors.surface.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, tokens.colors.border.copy(alpha = 0.2f)),
                shadowElevation = 12.dp
            ) {
                content()
            }
        }
    }
}

// --- PREVIEWS: DESIGN SYSTEM ---

@Preview(name = "Design System • Componentes Base", widthDp = 1200, heightDp = 800)
@Composable
fun PreviewDesignSystemGallery() {
    MeusCanaisTheme {
        val tokens = AppDesignSystem
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tokens.colors.background)
                .padding(tokens.spacing.huge)
                .verticalScroll(rememberScrollState())
        ) {
            Text("DESIGN SYSTEM • COMPONENTES BASE", style = tokens.typography.display, color = tokens.colors.primary, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(tokens.spacing.huge))

            // BADGES
            Text("BADGES", style = tokens.typography.title, color = tokens.colors.textPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(tokens.spacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.large), verticalAlignment = Alignment.CenterVertically) {
                AgeBadge("18")
                AgeBadge("L")
                RatingBadge("8.5")
                GenreBadge("Ação", onClick = {})
                GenreBadge("Comédia", onClick = {})
            }

            Spacer(modifier = Modifier.height(tokens.spacing.huge))

            // BUTTONS
            Text("BUTTONS", style = tokens.typography.title, color = tokens.colors.textPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(tokens.spacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
                AppButton("Play", onClick = {}, icon = Icons.Rounded.PlayArrow)
                AppSecondaryButton("Trailer", onClick = {}, icon = Icons.Rounded.Movie)
                AppIconButton(Icons.Rounded.Favorite, onClick = {})
                AppIconButton(Icons.Rounded.Search, onClick = {}, tint = tokens.colors.primary)
            }

            Spacer(modifier = Modifier.height(tokens.spacing.huge))

            // CARDS
            Text("CARDS", style = tokens.typography.title, color = tokens.colors.textPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(tokens.spacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraLarge), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(150.dp)) {
                    PosterCard(
                        item = IptvItem("1", "Oppenheimer", null, ContentType.MOVIE, isFavorite = true),
                        progress = 0.5f,
                        onClick = {}
                    )
                }
                Box(modifier = Modifier.width(240.dp)) {
                    ChannelCard(
                        item = IptvItem("101", "ESPN Brasil", null, ContentType.LIVE),
                        epg = "Jogo Aberto",
                        onClick = {}
                    )
                }
                CategoryCard(title = "Filmes", icon = Icons.Rounded.Movie, onClick = {})
                
                // Cinematic Cards Preview
                CinematicActionCard(
                    title = "TV AO VIVO",
                    description = "Canais em tempo real",
                    icon = Icons.Rounded.Monitor,
                    backgroundImage = "https://images.unsplash.com/photo-1781707328305-5bdff1e3d72b?auto=format&fit=crop&q=80&w=600",
                    themeColor = ThemeLive,
                    modifier = Modifier.width(200.dp).height(120.dp),
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(tokens.spacing.huge))

            // STATES
            Text("STATES (SMALL PREVIEWS)", style = tokens.typography.title, color = tokens.colors.textPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(tokens.spacing.large))
            Row(modifier = Modifier.height(200.dp), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraLarge)) {
                Box(modifier = Modifier.weight(1f).border(1.dp, tokens.colors.divider)) { LoadingState(message = "Carregando...") }
                Box(modifier = Modifier.weight(1f).border(1.dp, tokens.colors.divider)) { ErrorState(message = "Erro de conexão", onRetry = {}) }
            }
            
            Spacer(modifier = Modifier.height(tokens.spacing.huge))
            
            // INPUTS
            Text("INPUTS", style = tokens.typography.title, color = tokens.colors.textPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(tokens.spacing.large))
            Column(modifier = Modifier.width(400.dp), verticalArrangement = Arrangement.spacedBy(tokens.spacing.large)) {
                AppTextField(value = "", onValueChange = {}, placeholder = "Usuário")
                AppTextField(value = "password", onValueChange = {}, placeholder = "Senha", isPassword = true)
                SearchField(value = "", onValueChange = {}, placeholder = "Buscar filmes...")
            }
            
            Spacer(modifier = Modifier.height(tokens.spacing.giant))
        }
    }
}

// --- PREVIEWS: AUTHENTICATION ---

@Preview(name = "Auth • Login • Premium Redesign", widthDp = 1280, heightDp = 720)
@Composable
fun PreviewAuthLoginRedesign() {
    val fr = remember { FocusRequester() }
    ResponsiveLayout {
        LoginContent(
            username = "usuario_teste", 
            password = "test_password", 
            isLoading = false, 
            errorMessage = null,
            onUsernameChange = {}, 
            onPasswordChange = {}, 
            onRestoreClick = {}, 
            onLoginClick = {}, 
            usernameFocusRequester = fr, 
            passwordFocusRequester = fr
        )
    }
}

@Preview(name = "Lab • Login • Phone Landscape", widthDp = 800, heightDp = 360)
@Composable
fun PreviewLabLoginPhoneLandscape() {
    val fr = remember { FocusRequester() }
    MeusCanaisTheme {
        ResponsiveLayout {
            LoginContent(
                username = "usuario_teste", 
                password = "test_password", 
                isLoading = false, 
                errorMessage = null,
                onUsernameChange = {}, 
                onPasswordChange = {}, 
                onRestoreClick = {}, 
                onLoginClick = {}, 
                usernameFocusRequester = fr, 
                passwordFocusRequester = fr
            )
        }
    }
}

@Preview(name = "Lab • Login • Phone Portrait (360x800)", widthDp = 360, heightDp = 800)
@Composable
fun PreviewLabLoginPhonePortrait() {
    val fr = remember { FocusRequester() }
    MeusCanaisTheme {
        ResponsiveLayout {
            LoginContent(
                username = "usuario_teste", 
                password = "test_password", 
                isLoading = false, 
                errorMessage = null,
                onUsernameChange = {}, 
                onPasswordChange = {}, 
                onRestoreClick = {}, 
                onLoginClick = {}, 
                usernameFocusRequester = fr, 
                passwordFocusRequester = fr
            )
        }
    }
}

@Preview(name = "Lab • Login • Small Phone (360x740)", widthDp = 360, heightDp = 740)
@Composable
fun PreviewLabLoginSmallPhone() {
    val fr = remember { FocusRequester() }
    MeusCanaisTheme {
        ResponsiveLayout {
            LoginContent(
                username = "usuario_teste", 
                password = "test_password", 
                isLoading = false, 
                errorMessage = null,
                onUsernameChange = {}, 
                onPasswordChange = {}, 
                onRestoreClick = {}, 
                onLoginClick = {}, 
                usernameFocusRequester = fr, 
                passwordFocusRequester = fr
            )
        }
    }
}

@Preview(name = "Auth • Sync • Progresso", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewAuthSyncProgress() {
    GalleryIndividualFrame("Sincronização • Progresso", "TV Style") {
        SyncLayoutMock(GalleryMocks.syncProgressDefault.copy(vodStatus = AppSyncStatus.Syncing, isComplete = false), true)
    }
}

@Preview(name = "Auth • Security • App Lock", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewAuthAppLock() {
    GalleryIndividualFrame("App Lock • PIN", "Widescreen") {
        AppLockScreen(onUnlock = { false })
    }
}

// --- PREVIEWS: DASHBOARDS & RESPONSIVENESS LAB ---

@Preview(name = "TV • Dashboard • 1080p", widthDp = 1280, heightDp = 720)
@Composable
fun PreviewDashboardTV1080p() {
    GalleryIndividualFrame("Dashboard TV • 1080p", "Reference HD View") {
        DashboardContent(
            windowInfo = GalleryMocks.windowTV,
            sessionStatus = SessionStatus.VALID,
            syncProgress = GalleryMocks.syncProgressDefault,
            newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
            watchProgress = emptyMap(),
            activeProfile = GalleryMocks.userProfile,
            credentials = null,
            expDate = "30 de Outubro, 2024", 
            actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
        )
    }
}

@Preview(name = "Lab • Phone • Portrait (360x800)", widthDp = 360, heightDp = 800)
@Composable
fun PreviewLabPhonePortrait() {
    MeusCanaisTheme {
        ResponsiveLayout {
            DashboardContent(
                windowInfo = GalleryMocks.windowLandscape.copy(screenWidth = 360.dp, screenHeight = 800.dp, deviceType = DeviceType.PHONE),
                sessionStatus = SessionStatus.VALID,
                syncProgress = GalleryMocks.syncProgressDefault,
                newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
                watchProgress = emptyMap(),
                activeProfile = GalleryMocks.userProfile,
                credentials = null,
                expDate = "30/10/2024", 
                actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
            )
        }
    }
}

@Preview(name = "Lab • Phone • Portrait (390x844)", widthDp = 390, heightDp = 844)
@Composable
fun PreviewLabPhonePortraitLarge() {
    MeusCanaisTheme {
        ResponsiveLayout {
            DashboardContent(
                windowInfo = GalleryMocks.windowLandscape.copy(screenWidth = 390.dp, screenHeight = 844.dp, deviceType = DeviceType.PHONE),
                sessionStatus = SessionStatus.VALID,
                syncProgress = GalleryMocks.syncProgressDefault,
                newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
                watchProgress = emptyMap(),
                activeProfile = GalleryMocks.userProfile,
                credentials = null,
                expDate = "30/10/2024", 
                actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
            )
        }
    }
}

@Preview(name = "Lab • Phone • Landscape (800x360)", widthDp = 800, heightDp = 360)
@Composable
fun PreviewLabPhoneLandscape() {
    MeusCanaisTheme {
        ResponsiveLayout {
            DashboardContent(
                windowInfo = GalleryMocks.windowLandscape.copy(screenWidth = 800.dp, screenHeight = 360.dp, deviceType = DeviceType.PHONE),
                sessionStatus = SessionStatus.VALID,
                syncProgress = GalleryMocks.syncProgressDefault,
                newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
                watchProgress = emptyMap(),
                activeProfile = GalleryMocks.userProfile,
                credentials = null,
                expDate = "30/10/2024", 
                actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
            )
        }
    }
}

@Preview(name = "Lab • Tablet • Landscape (1024x768)", widthDp = 1024, heightDp = 768)
@Composable
fun PreviewLabTabletLandscape() {
    MeusCanaisTheme {
        ResponsiveLayout {
            DashboardContent(
                windowInfo = GalleryMocks.windowLandscape.copy(screenWidth = 1024.dp, screenHeight = 768.dp, deviceType = DeviceType.PHONE),
                sessionStatus = SessionStatus.VALID,
                syncProgress = GalleryMocks.syncProgressDefault,
                newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
                watchProgress = emptyMap(),
                activeProfile = GalleryMocks.userProfile,
                credentials = null,
                expDate = "30/10/2024", 
                actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
            )
        }
    }
}

@Preview(name = "Lab • TV • 720p (1280x720)", widthDp = 1280, heightDp = 720)
@Composable
fun PreviewLabTV720p() {
    MeusCanaisTheme {
        ResponsiveLayout {
            DashboardContent(
                windowInfo = GalleryMocks.windowTV.copy(screenWidth = 1280.dp, screenHeight = 720.dp),
                sessionStatus = SessionStatus.VALID,
                syncProgress = GalleryMocks.syncProgressDefault,
                newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
                watchProgress = emptyMap(),
                activeProfile = GalleryMocks.userProfile,
                credentials = null,
                expDate = "30/10/2024", 
                actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
            )
        }
    }
}

@Preview(name = "Lab • TV • 1080p (1920x1080)", widthDp = 1920, heightDp = 1080)
@Composable
fun PreviewLabTV1080p() {
    MeusCanaisTheme {
        ResponsiveLayout {
            DashboardContent(
                windowInfo = GalleryMocks.windowTV.copy(screenWidth = 1920.dp, screenHeight = 1080.dp),
                sessionStatus = SessionStatus.VALID,
                syncProgress = GalleryMocks.syncProgressDefault,
                newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
                watchProgress = emptyMap(),
                activeProfile = GalleryMocks.userProfile,
                credentials = null,
                expDate = "30/10/2024", 
                actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
            )
        }
    }
}

@Preview(name = "Lab • TV • 4K (Scaled)", widthDp = 2560, heightDp = 1440)
@Composable
fun PreviewLabTV4K() {
    MeusCanaisTheme {
        ResponsiveLayout {
            DashboardContent(
                windowInfo = GalleryMocks.windowTV.copy(screenWidth = 3840.dp, screenHeight = 2160.dp),
                sessionStatus = SessionStatus.VALID,
                syncProgress = GalleryMocks.syncProgressDefault,
                newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
                watchProgress = emptyMap(),
                activeProfile = GalleryMocks.userProfile,
                credentials = null,
                expDate = "30/10/2024", 
                actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
            )
        }
    }
}

@Preview(name = "Lab • UltraWide (2560x1080)", widthDp = 2560, heightDp = 1080)
@Composable
fun PreviewLabUltraWide() {
    MeusCanaisTheme {
        ResponsiveLayout {
            DashboardContent(
                windowInfo = GalleryMocks.windowTV.copy(screenWidth = 2560.dp, screenHeight = 1080.dp),
                sessionStatus = SessionStatus.VALID,
                syncProgress = GalleryMocks.syncProgressDefault,
                newlyAdded = mapOf(ContentType.MOVIE to GalleryMocks.movies, ContentType.SERIES to emptyList()),
                watchProgress = emptyMap(),
                activeProfile = GalleryMocks.userProfile,
                credentials = null,
                expDate = "30/10/2024", 
                actions = DashboardActions({_,_ ->}, {}, {}, {}, {}, {}, {}, {})
            )
        }
    }
}

// --- PREVIEWS: CONTENT & SEARCH ---

@Preview(name = "Content • Grid • Canais", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewContentGrid() {
    GalleryIndividualFrame("Listagem • Canais e Filmes", "Landscape Catalog") {
        PortalBackground {
            Row(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.width(260.dp).fillMaxHeight(), 
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        GalleryMocks.categories.forEach { 
                            CategoryListItem(it.categoryName ?: "", it.categoryId == "1", onClick = {}) 
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).padding(Spacing.xl)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4), 
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xl), 
                        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
                    ) {
                        items(GalleryMocks.channels) { ChannelCard(it, onClick = {}) }
                        items(GalleryMocks.movies) { PosterCard(it, onClick = {}) }
                    }
                }
            }
        }
    }
}

@Preview(name = "Content • Grid • Filmes", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewContentMovies() {
    GalleryIndividualFrame("Listagem • Filmes", "Landscape Grid") {
        PortalBackground {
            Box(modifier = Modifier.fillMaxSize().padding(Spacing.xl)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6), 
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xl), 
                    verticalArrangement = Arrangement.spacedBy(Spacing.xl)
                ) {
                    items(List(12) { GalleryMocks.movies[0] }) { PosterCard(it, onClick = {}) }
                }
            }
        }
    }
}

@Preview(name = "Content • List • Empty", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewContentEmpty() {
    GalleryIndividualFrame("Listagem • Vazia", "State Example") {
        PortalBackground {
            EmptyState(
                title = "Nenhum resultado encontrado",
                description = "Tente mudar os filtros ou pesquisar outro termo."
            )
        }
    }
}

@Preview(name = "Content • Search • Resultados", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewSearchContent() {
    GalleryIndividualFrame("Busca • Resultados", "Discovery") {
        SearchScreenContent(
            query = "ESPN", 
            selectedType = "ALL", 
            results = listOf(SearchResult.Stream(IptvStreamEntity(101, "ESPN Brasil", "1", "live", null, null))),
            isLoading = false, 
            onQueryChanged = {}, 
            onTypeChanged = {}, 
            onNavigateBack = {}, 
            onItemClick = {_,_,_,_,_,_,_ ->}, 
            onVoiceSearch = {},
            windowInfo = GalleryMocks.windowTV
        )
    }
}

@Preview(name = "Content • EPG • Grade", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewEpgGridContent() {
    GalleryIndividualFrame("EPG Grid • Programação", "TV Experience") {
        EpgGridContent(
            GalleryMocks.channels, GalleryMocks.epgPrograms,
            System.currentTimeMillis()/1000 to System.currentTimeMillis()/1000 + 3600*6,
            GalleryMocks.categories, null, {}, {}, {_,_,_ ->}, {}, true, FocusRequester()
        )
    }
}

// --- PREVIEWS: DETAILS ---

@Preview(name = "Detail • Movie • Oppenheimer", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewDetailMovie() {
    GalleryIndividualFrame("Detalhes • Filme", "Media Info Landscape") {
        VodDetailContent(
            vodId = 201, 
            vodName = "Oppenheimer", 
            icon = "https://image.tmdb.org/t/p/w500/8Gxv2mYnrnvrM3Q4QvxQoffQHDs.jpg", 
            vodInfo = GalleryMocks.vodInfo, 
            isLoading = false, 
            isFavorite = false, 
            currentProgress = 0.4f,
            onBack = {}, 
            onPlay = {_,_,_ ->}, 
            onActorClick = {}, 
            onGenreClick = {}, 
            onDirectorClick = {}, 
            onToggleFavorite = {}, 
            onDownload = {}, 
            similarContent = emptyList(), 
            watchProgress = emptyMap(),
            windowInfo = GalleryMocks.windowTV, 
            playButtonFocusRequester = FocusRequester(), 
            onViewTrailer = {}
        )
    }
}

@Preview(name = "Detail • Series • Succession", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewDetailSeries() {
    GalleryIndividualFrame("Detalhes • Série", "Media Info Landscape") {
        SeriesDetailContent(
            seriesId = 301, 
            seriesName = "Succession", 
            cover = "https://image.tmdb.org/t/p/w500/7y97hl9czqjP7tT961UCJyqQC68.jpg", 
            seasons = GalleryMocks.seasons, 
            episodesMap = GalleryMocks.episodes,
            watchProgress = emptyMap(), 
            selectedSeason = 1, 
            isLoading = false, 
            isFavorite = true, 
            credentials = null, 
            onBack = {}, 
            onSeasonSelect = {}, 
            onPlayEpisode = {_,_,_ ->}, 
            onActorClick = {}, 
            onGenreClick = {}, 
            onDirectorClick = {}, 
            onToggleFavorite = {}, 
            onDownloadSeason = {_,_ ->}, 
            onDownloadEpisode = {_,_,_ ->}, 
            getSeriesMetadata = { _, _ -> GalleryMocks.seriesInfo }, 
            similarContent = emptyList(), 
            isExpanded = true,
            isTv = true
        )
    }
}

@Preview(name = "Content • Grid • Séries", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewContentSeries() {
    GalleryIndividualFrame("Listagem • Séries", "Landscape Grid") {
        PortalBackground {
            Box(modifier = Modifier.fillMaxSize().padding(Spacing.xl)) {
                val seriesItems = listOf(
                    IptvItem("301", "Succession", "https://image.tmdb.org/t/p/w500/7y97hl9czqjP7tT961UCJyqQC68.jpg", ContentType.SERIES, rating = "8.8"),
                    IptvItem("302", "The Bear", null, ContentType.SERIES, rating = "8.6")
                )
                ContentGrid(
                    items = List(12) { if (it % 2 == 0) seriesItems[0] else seriesItems[1] },
                    gridFocusRequester = remember { FocusRequester() },
                    sidebarFocusRequester = remember { FocusRequester() },
                    onItemClick = {}
                )
            }
        }
    }
}

@Preview(name = "Content • Actor Detail • Obras", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewActorDetailContent() {
    val tokens = AppDesignSystem
    GalleryIndividualFrame("Ator • Filmografia", "Biography Layout") {
        PortalBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(
                    title = "Cillian Murphy",
                    subtitle = "2 obras encontradas",
                    onBack = {}
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 64.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = tokens.spacing.extraLarge, vertical = tokens.spacing.large),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(100.dp),
                                shape = CircleShape,
                                color = tokens.colors.primary,
                                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("C", style = tokens.typography.display, color = Color.Black, fontWeight = FontWeight.Black)
                                }
                            }
                            Spacer(modifier = Modifier.width(tokens.spacing.extraLarge))
                            Text(
                                text = "Explore todos os filmes e séries relacionados a Cillian Murphy disponíveis na biblioteca.",
                                style = tokens.typography.body,
                                color = tokens.colors.textSecondary
                            )
                        }
                    }
                    item {
                        ContentRow(
                            title = "FILMES",
                            items = GalleryMocks.movies,
                            watchProgress = emptyMap(),
                            onItemClick = {}
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Content • Library • Favoritos", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewLibraryFavorites() {
    GalleryIndividualFrame("Biblioteca • Favoritos", "Grid View") {
        PortalBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(
                    title = "MEUS FAVORITOS",
                    onBack = {}
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    ContentGrid(
                        items = GalleryMocks.channels + GalleryMocks.movies,
                        gridFocusRequester = remember { FocusRequester() },
                        sidebarFocusRequester = remember { FocusRequester() },
                        favorites = GalleryMocks.channels + GalleryMocks.movies,
                        onItemClick = {}
                    )
                }
            }
        }
    }
}

// --- PREVIEWS: PLAYER ---

@Preview(name = "Player • TV • Live", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewPlayerLive() {
    GalleryIndividualFrame("Player • Live TV", "1080p Experience") {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            LiveBottomOverlay(
                streamName = "ESPN BRASIL",
                streamIcon = "https://logodownload.org/wp-content/uploads/2021/06/espn-logo.png",
                currentProgram = GalleryMocks.playerMockEpg,
                isControlsVisible = true,
                onShowSettings = {},
                onToggleResize = {},
                onShowAudio = {},
                onShowSubtitles = {}
            )
        }
    }
}

@Preview(name = "Player • TV • VOD Controls", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewPlayerVod() {
    GalleryIndividualFrame("Player • VOD Controls", "TV Overlay") {
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
            PlayerControlOverlay(
                streamName = "OPPENHEIMER",
                uiState = PlayerUiState.Playing(
                    position = 3600_000,
                    duration = 7200_000,
                    bufferedPosition = 4500_000,
                    isPlaying = true,
                    contentType = ContentType.MOVIE
                ),
                currentProgram = null,
                nextPrograms = emptyList(),
                recentChannels = emptyList(),
                focusRequester = FocusRequester(),
                isExpanded = true,
                isTv = true,
                onBack = {},
                onTogglePlayPause = {},
                onSeek = {},
                onSeekForward = {},
                onSeekBack = {},
                onNextEpisode = {},
                onToggleFavorite = {},
                onReload = {},
                onEnterPip = {},
                onShowSettings = {},
                onShowChannels = {},
                onShowQuality = {},
                onShowSpeed = {},
                onToggleResizeMode = {},
                onToggleLock = {},
                onSwitchStream = {}
            )
        }
    }
}

@Preview(name = "Player • Zapping • Banner", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewPlayerZapping() {
    GalleryIndividualFrame("Player • Zapping Banner", "Channel Change") {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            ZappingBanner(
                isVisible = true,
                channel = XtreamStream(name = "Globo News", streamIcon = null, num = 12),
                program = GalleryMocks.playerMockEpg,
                isExpanded = true
            )
        }
    }
}

// --- PREVIEWS: SYSTEM & OVERLAYS ---

@Preview(name = "System • Account • Perfil", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewSystemAccount() {
    GalleryIndividualFrame("Conta • Perfil do Usuário", "Premium Layout") {
        AccountContent(
            accountInfo = GalleryMocks.userInfo, 
            profileImageUrl = null, 
            deviceId = "GALLERY-DEVICE-ID", 
            ipAddress = "192.168.0.1", 
            serverUrl = "http://iptv.server",
            profiles = listOf(GalleryMocks.userProfile), 
            activeProfile = GalleryMocks.userProfile, 
            appSettings = GalleryMocks.appSettings,
            onBack = {}, 
            onRefresh = {}, 
            onUpdateProfilePic = {}, 
            onLogout = {}, 
            onAddProfile = {_,_ ->}, 
            onDeleteProfile = {}, 
            onSelectProfile = {}, 
            onSwitchAccount = {}, 
            onRemoveAccount = {}
        )
    }
}

@Preview(name = "System • Settings • Premium Redesign", widthDp = 1280, heightDp = 720)
@Composable
fun PreviewSystemSettings() {
    GalleryIndividualFrame("Configurações • Premium Redesign", "Two-Pane Layout") {
        SettingsScreen(
            mainViewModel = hiltViewModel(), // In a real app we'd mock this, but here it's for gallery reference
            onBack = {},
            onNavigateToAccount = {}
        )
    }
}

@Preview(name = "Lab • Settings • Phone Portrait", widthDp = 360, heightDp = 800)
@Composable
fun PreviewLabSettingsPhone() {
    MeusCanaisTheme {
        ResponsiveLayout {
            SettingsScreen(
                mainViewModel = hiltViewModel(),
                onBack = {},
                onNavigateToAccount = {}
            )
        }
    }
}

@Preview(name = "System • Downloads • Lista", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewSystemDownloads() {
    GalleryIndividualFrame("Downloads • Biblioteca Offline", "Horizontal Grid") {
        DownloadsContent(
            downloads = emptyList(), // Use empty for now to show state
            onPlay = {_,_,_,_,_,_ ->}, 
            onDelete = {}, 
            isExpanded = true
        )
    }
}

@Preview(name = "System • MultiView • Grid", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewSystemMultiView() {
    GalleryIndividualFrame("Multi-View • 4 Telas", "TV Context") {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(4) { index ->
                PlayerSlot(
                    index = index,
                    player = null,
                    channel = if (index == 0) XtreamStream(name = "HBO HD") else null,
                    isFocused = index == 0,
                    onFocus = {},
                    onAdd = {},
                    onRemove = {}
                )
            }
        }
    }
}

@Preview(name = "System • Global • Loading", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewSystemLoading() {
    GalleryIndividualFrame("Sistema • Loading Branded", "Horizontal Loading") {
        BrandedLoadingScreen("Sincronizando biblioteca...")
    }
}

@Preview(name = "System • Global • Exit Dialog", widthDp = 1000, heightDp = 600)
@Composable
fun PreviewSystemExit() {
    GalleryIndividualFrame("Sistema • Confirmação de Saída", "Exit Dialog") {
        ExitConfirmDialog({}, {})
    }
}

// --- UTILS ---

@Composable
private fun SyncLayoutMock(progress: AppSyncProgress, isTv: Boolean) {
    val tokens = AppDesignSystem
    PortalBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTv) tokens.spacing.giant * 2 else tokens.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = tokens.spacing.huge)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(if (isTv) 80.dp else 56.dp),
                    tint = tokens.colors.primary
                )
                Spacer(modifier = Modifier.width(tokens.spacing.large))
                Column {
                    Text(
                        text = "SINCRONIZAÇÃO",
                        style = if (isTv) tokens.typography.display else tokens.typography.headline,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "ATUALIZANDO SUA BIBLIOTECA",
                        style = tokens.typography.body,
                        color = tokens.colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isTv) 4 else 2),
                modifier = Modifier.fillMaxWidth(if (isTv) 0.85f else 1f),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.large),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium)
            ) {
                item { SyncItem("CONTA", progress.accountStatus) }
                item { SyncItem("CANAIS", progress.liveStatus) }
                item { SyncItem("FILMES", progress.vodStatus) }
                item { SyncItem("SÉRIES", progress.seriesStatus) }
            }

            Spacer(modifier = Modifier.height(tokens.spacing.huge))
            
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .clip(CircleShape),
                color = tokens.colors.primary,
                trackColor = Color.White.copy(alpha = 0.05f)
            )
        }
    }
}
