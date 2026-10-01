package com.meuscanais.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.meuscanais.R
import com.meuscanais.core.ui.components.buttons.AppButton
import com.meuscanais.core.ui.components.navigation.AppHeader
import com.meuscanais.core.ui.components.navigation.AppSidebar
import com.meuscanais.core.ui.components.states.LoadingState
import com.meuscanais.core.ui.components.settings.*
import com.meuscanais.core.ui.theme.*
import com.meuscanais.ui.dashboard.PortalBackground
import com.meuscanais.ui.viewmodel.MainViewModel
import com.meuscanais.ui.viewmodel.settings.SettingsViewModel
import com.meuscanais.ui.viewmodel.library.LibraryViewModel
import com.meuscanais.util.NetworkDiagnostics
import com.meuscanais.util.rememberWindowInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay

/**
 * CONFIGURAÇÕES - CINEMATIC IMMERSION
 */

enum class SettingsSection(val titleRes: Int, val icon: ImageVector) {
    ACCOUNT(R.string.account_label, Icons.Rounded.AccountCircle),
    APPEARANCE(R.string.app_settings_section, Icons.Rounded.Palette),
    PLAYBACK(R.string.player_settings_section, Icons.Rounded.PlayCircle),
    SECURITY(R.string.parental_control_label, Icons.Rounded.Security),
    MAINTENANCE(R.string.reset_database_label, Icons.Rounded.SettingsBackupRestore)
}

enum class SettingsCategory(val labelRes: Int, val icon: ImageVector, val section: SettingsSection) {
    // CONTA
    ACCOUNT(R.string.account_label, Icons.Rounded.ManageAccounts, SettingsSection.ACCOUNT),
    SIGN_OUT(R.string.logout_label, Icons.AutoMirrored.Rounded.Logout, SettingsSection.ACCOUNT),
    
    // APARÊNCIA
    LANGUAGE(R.string.language_label, Icons.Rounded.Language, SettingsSection.APPEARANCE),
    THEME(R.string.oled_theme_label, Icons.Rounded.DarkMode, SettingsSection.APPEARANCE),
    ZOOM(R.string.ui_zoom_label, Icons.Rounded.ZoomIn, SettingsSection.APPEARANCE),
    
    // REPRODUÇÃO
    PLAYER_ENGINE(R.string.video_quality_section, Icons.Rounded.SettingsInputComponent, SettingsSection.PLAYBACK),
    BUFFER(R.string.buffer_strategy_label, Icons.Rounded.Memory, SettingsSection.PLAYBACK),
    AUTO_PLAY(R.string.auto_play_label, Icons.AutoMirrored.Rounded.PlaylistPlay, SettingsSection.PLAYBACK),
    DIAGNOSTICS(R.string.show_diagnostics_label, Icons.Rounded.Analytics, SettingsSection.PLAYBACK),
    
    // SEGURANÇA
    PARENTAL(R.string.parental_control_label, Icons.Rounded.Lock, SettingsSection.SECURITY),
    HIDE_LOCKED(R.string.hide_blocked_label, Icons.Rounded.VisibilityOff, SettingsSection.SECURITY),
    
    // MANUTENÇÃO
    UPDATE(R.string.force_sync_button, Icons.Rounded.Sync, SettingsSection.MAINTENANCE),
    APP_UPDATES(R.string.app_updates_title, Icons.Rounded.SystemUpdate, SettingsSection.MAINTENANCE),
    DNS_TESTER(R.string.dns_tester_title, Icons.Rounded.Dns, SettingsSection.MAINTENANCE),
    CLEAR_CACHE(R.string.clear_cache_label, Icons.Rounded.Brush, SettingsSection.MAINTENANCE),
    CLEAR_CATALOG(R.string.reset_database_label, Icons.Rounded.DeleteSweep, SettingsSection.MAINTENANCE),
    SPEED_TEST(R.string.test_now_button, Icons.Rounded.Speed, SettingsSection.MAINTENANCE)
}

@UnstableApi
@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToDnsTester: () -> Unit = {},
    onNavigateToUpdates: () -> Unit = {}
) {
    val settings by settingsViewModel.settings.collectAsState()
    val dbStats by mainViewModel.dbStats.collectAsState()
    val credentials = mainViewModel.credentials
    val windowInfo = rememberWindowInfo()
    val tokens = AppDesignSystem
    val responsive = tokens.responsive
    val scope = rememberCoroutineScope()
    
    var selectedSection by remember { mutableStateOf(SettingsSection.ACCOUNT) }
    var detailCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    val sidebarFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    val isCompact = windowInfo.screenWidth < 800.dp

    BackHandler(enabled = detailCategory != null || showResetDialog) {
        if (detailCategory != null) detailCategory = null
        else if (showResetDialog) showResetDialog = false
    }

    LaunchedEffect(Unit) {
        mainViewModel.refreshStats()
        delay(500)
        try { sidebarFocusRequester.requestFocus() } catch (_: Exception) {}
    }

    PortalBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = stringResource(R.string.settings_nav_title),
                subtitle = stringResource(R.string.settings_nav_subtitle),
                onBack = onBack
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // SIDEBAR (Only on wide screens)
                if (!isCompact) {
                    AppSidebar(
                        items = SettingsSection.entries,
                        selectedItemPredicate = { it == selectedSection },
                        itemLabel = { stringResource(it.titleRes) },
                        onItemClick = { selectedSection = it },
                        focusRequester = sidebarFocusRequester,
                        nextFocusRequester = contentFocusRequester
                    )
                }

                // CONTENT AREA
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = responsive.dp(tokens.spacing.extraLarge), vertical = responsive.dp(tokens.spacing.large))
                ) {
                    val displaySections = if (isCompact) SettingsSection.entries else listOf(selectedSection)
                    
                    displaySections.forEach { section ->
                        if (isCompact) {
                            Text(
                                text = stringResource(section.titleRes).uppercase(),
                                style = tokens.typography.label.copy(fontSize = responsive.sp(14.sp)),
                                color = tokens.colors.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                modifier = Modifier.padding(bottom = responsive.dp(tokens.spacing.large), top = responsive.dp(tokens.spacing.extraLarge))
                            )
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isCompact) Arrangement.Start else Arrangement.Start,
                            verticalArrangement = Arrangement.spacedBy(responsive.dp(tokens.spacing.extraLarge))
                        ) {
                            SettingsCategory.entries.filter { it.section == section }.forEachIndexed { index, category ->
                                SettingsGridCard(
                                    label = stringResource(category.labelRes),
                                    icon = category.icon,
                                    isDestructive = category == SettingsCategory.SIGN_OUT || category == SettingsCategory.CLEAR_CATALOG,
                                    modifier = Modifier
                                        .padding(end = responsive.dp(tokens.spacing.medium))
                                        .then(if (index == 0 && !isCompact) Modifier.focusRequester(contentFocusRequester) else Modifier),
                                    onClick = {
                                        when (category) {
                                            SettingsCategory.SIGN_OUT -> mainViewModel.logout()
                                            SettingsCategory.UPDATE -> mainViewModel.loadData(force = true)
                                            SettingsCategory.APP_UPDATES -> onNavigateToUpdates()
                                            SettingsCategory.DNS_TESTER -> onNavigateToDnsTester()
                                            SettingsCategory.CLEAR_CACHE -> mainViewModel.clearCache()
                                            SettingsCategory.CLEAR_CATALOG -> mainViewModel.clearCatalogData()
                                            else -> detailCategory = category
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(responsive.dp(tokens.spacing.giant)))
                    
                    // FOOTER (Stats)
                    if (!isCompact) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = tokens.colors.surface.copy(alpha = 0.1f),
                            shape = tokens.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(responsive.dp(tokens.spacing.large)),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VERSÃO 1.0 PREMIUM",
                                    style = tokens.typography.caption.copy(fontSize = responsive.sp(10.sp)),
                                    color = tokens.colors.textSecondary.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "BIBLIOTECA: ${dbStats.first} CANAIS | ${dbStats.second} FILMES | ${dbStats.third} SÉRIES",
                                    style = tokens.typography.caption.copy(fontSize = responsive.sp(10.sp)),
                                    color = tokens.colors.textSecondary.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialogs
    if (detailCategory != null) {
        val cat = detailCategory!!
        AlertDialog(
            onDismissRequest = { detailCategory = null },
            containerColor = tokens.colors.backgroundSecondary,
            shape = tokens.shapes.extraLarge,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(cat.icon, null, tint = tokens.colors.primary, modifier = Modifier.size(responsive.dp(32.dp)))
                    Spacer(Modifier.width(responsive.dp(tokens.spacing.large)))
                    Text(
                        text = stringResource(cat.labelRes).uppercase(),
                        style = tokens.typography.headline.copy(fontSize = responsive.sp(tokens.typography.headline.fontSize)),
                        fontWeight = FontWeight.Black,
                        color = tokens.colors.textPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = responsive.dp(tokens.spacing.medium)),
                    verticalArrangement = Arrangement.spacedBy(responsive.dp(tokens.spacing.small))
                ) {
                    when (cat) {
                        SettingsCategory.ACCOUNT -> {
                            SettingsActionItem(
                                label = "CONTA ATIVA: ${credentials?.username ?: "NENHUM"}",
                                icon = Icons.Rounded.AccountCircle,
                                onClick = onNavigateToAccount
                            )
                            SettingsActionItem(
                                label = "GERENCIAR PERFIS E CONEXÕES",
                                icon = Icons.Rounded.People,
                                onClick = onNavigateToAccount
                            )
                        }
                        SettingsCategory.LANGUAGE -> {
                            val codes = mapOf("pt" to "PORTUGUÊS", "en" to "ENGLISH", "es" to "ESPAÑOL")
                            codes.forEach { (code, name) ->
                                SettingsActionItem(
                                    label = name,
                                    onClick = { settingsViewModel.updateLanguage(code); detailCategory = null },
                                    trailingText = if (settings.language == code) "ATIVO" else null
                                )
                            }
                        }
                        SettingsCategory.THEME -> {
                            SettingsToggle(
                                label = "TEMA OLED BLACK",
                                checked = settings.useOledTheme,
                                description = "Fundo preto puro para economia de bateria e contraste infinito.",
                                onCheckedChange = { settingsViewModel.updateUseOledTheme(it) }
                            )
                        }
                        SettingsCategory.ZOOM -> {
                            Text("TAMANHO DA INTERFACE", color = tokens.colors.primary, style = tokens.typography.label, fontWeight = FontWeight.Black)
                            val options = listOf(0.8f to "PEQUENO", 1.0f to "PADRÃO", 1.2f to "GRANDE", 1.5f to "EXTRA")
                            options.forEach { (zoom, label) ->
                                SettingsActionItem(
                                    label = label,
                                    onClick = { settingsViewModel.updateUiZoom(zoom) },
                                    trailingText = if (settings.uiZoom == zoom) "ATIVO" else null
                                )
                            }
                        }
                        SettingsCategory.PLAYER_ENGINE -> {
                            Text("MOTOR DE REPRODUÇÃO", color = tokens.colors.primary, style = tokens.typography.label, fontWeight = FontWeight.Black)
                            listOf("EXO" to "EXOPLAYER (RECOMENDADO)", "VLC" to "VLC ENGINE (EXTERNO)").forEach { (id, label) ->
                                SettingsActionItem(
                                    label = label,
                                    onClick = { settingsViewModel.updatePlayerEngine(id) },
                                    trailingText = if (settings.playerEngine == id) "ATIVO" else null
                                )
                            }
                        }
                        SettingsCategory.BUFFER -> {
                            Text("ESTRATÉGIA DE CARREGAMENTO", color = tokens.colors.primary, style = tokens.typography.label, fontWeight = FontWeight.Black)
                            listOf("Stable" to "ESTÁVEL (EQUILIBRADO)", "Fast" to "RÁPIDO (MENOR LATÊNCIA)", "Safe" to "SEGURO (MAIOR CACHE)").forEach { (id, label) ->
                                SettingsActionItem(
                                    label = label,
                                    onClick = { settingsViewModel.updateBufferStrategy(id) },
                                    trailingText = if (settings.bufferStrategy == id) "ATIVO" else null
                                )
                            }
                        }
                        SettingsCategory.AUTO_PLAY -> {
                            SettingsToggle(
                                label = "AUTO-REPRODUÇÃO",
                                checked = settings.autoPlayEnabled,
                                description = "Iniciar próximo episódio ou canal automaticamente.",
                                onCheckedChange = { settingsViewModel.updateAutoPlay(it) }
                            )
                        }
                        SettingsCategory.DIAGNOSTICS -> {
                            SettingsToggle(
                                label = "ESTATÍSTICAS DE REDE",
                                checked = settings.showDiagnostics,
                                description = "Exibir velocidade e buffer durante a reprodução.",
                                onCheckedChange = { settingsViewModel.updateShowDiagnostics(it) }
                            )
                        }
                        SettingsCategory.PARENTAL -> {
                            var pinInput by remember { mutableStateOf(settings.appPin ?: "") }
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 4) pinInput = it },
                                label = { Text("PIN DE 4 DÍGITOS") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = tokens.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = tokens.colors.primary,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(Modifier.height(responsive.dp(tokens.spacing.medium)))
                            AppButton(text = "SALVAR NOVO PIN", onClick = { settingsViewModel.updatePin(pinInput.ifEmpty { null }); detailCategory = null }, modifier = Modifier.fillMaxWidth())
                        }
                        SettingsCategory.HIDE_LOCKED -> {
                            SettingsToggle(
                                label = "OCULTAR BLOQUEADOS",
                                checked = settings.hideBlockedCategories,
                                description = "Categorias protegidas por PIN não aparecerão no catálogo.",
                                onCheckedChange = { settingsViewModel.updateHideBlockedCategories(it) }
                            )
                        }
                        SettingsCategory.SPEED_TEST -> {
                            var testResult by remember { mutableStateOf<String?>(null) }
                            var testing by remember { mutableStateOf(false) }
                            if (testing) {
                                LoadingState(message = "ANALISANDO CONEXÃO...")
                            } else {
                                testResult?.let {
                                    Text(
                                        text = it,
                                        color = tokens.colors.primary,
                                        style = tokens.typography.title,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(Modifier.height(responsive.dp(tokens.spacing.medium)))
                                AppButton(
                                    text = "INICIAR TESTE",
                                    onClick = {
                                        testing = true
                                        scope.launch {
                                            val ping = credentials?.let { NetworkDiagnostics.measurePing(it.baseUrl) } ?: -1
                                            val speed = credentials?.let { NetworkDiagnostics.measureDownloadSpeed(it.baseUrl) } ?: 0.0
                                            testResult = "PING: ${ping}MS\nVELOCIDADE: %.2f MBPS".format(speed)
                                            testing = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailCategory = null }) {
                    Text("CONCLUÍDO", color = tokens.colors.primary, fontWeight = FontWeight.Black)
                }
            }
        )
    }
}
