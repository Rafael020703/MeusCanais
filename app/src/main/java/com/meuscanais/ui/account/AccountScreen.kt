package com.meuscanais.ui.account

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.meuscanais.util.AppVersionProvider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.meuscanais.R
import com.meuscanais.core.ui.components.buttons.*
import com.meuscanais.core.ui.components.cards.AppCard
import com.meuscanais.core.ui.components.navigation.AppHeader
import com.meuscanais.core.ui.components.settings.SettingsActionItem
import com.meuscanais.core.ui.components.settings.SettingsStatCard
import com.meuscanais.core.ui.theme.*
import com.meuscanais.core.ui.components.common.adaptiveFocus
import com.meuscanais.data.model.UserInfo
import com.meuscanais.data.repository.SettingsRepository
import com.meuscanais.domain.model.UserProfile
import com.meuscanais.ui.dashboard.PortalBackground
import com.meuscanais.ui.viewmodel.MainViewModel
import com.meuscanais.ui.viewmodel.settings.SettingsViewModel
import com.meuscanais.util.NetworkUtils
import com.meuscanais.util.rememberWindowInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun AccountScreen(
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val profileImageUrl by viewModel.profileImageUrl.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.refreshAccountInfo()
        viewModel.refreshStats()
    }
    
    val context = LocalContext.current
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }
    val ipAddress = remember { NetworkUtils.getIPAddress(true) }
    val credentials = viewModel.credentials

    PortalBackground {
        AccountContent(
            accountInfo = accountInfo,
            profileImageUrl = profileImageUrl,
            deviceId = deviceId,
            ipAddress = ipAddress,
            serverUrl = credentials?.baseUrl ?: "N/A",
            profiles = profiles,
            activeProfile = activeProfile,
            appSettings = appSettings,
            onBack = onBack,
            onRefresh = { viewModel.refreshAccountInfo() },
            onUpdateProfilePic = { url -> viewModel.updateProfilePicture(url) },
            onLogout = { viewModel.logout(); onBack() },
            onAddProfile = { name, isChild -> viewModel.addProfile(name, isChild = isChild) },
            onDeleteProfile = { id -> viewModel.deleteProfile(id) },
            onSelectProfile = { viewModel.selectProfile(it) },
            onSwitchAccount = { viewModel.switchAccount(it) },
            onRemoveAccount = { viewModel.removeAccount(it) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun AccountContent(
    accountInfo: UserInfo?,
    profileImageUrl: String?,
    deviceId: String,
    ipAddress: String,
    serverUrl: String,
    profiles: List<UserProfile>,
    activeProfile: UserProfile?,
    appSettings: SettingsRepository.AppSettings,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onUpdateProfilePic: (String) -> Unit,
    onLogout: () -> Unit,
    onAddProfile: (String, Boolean) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onSelectProfile: (UserProfile) -> Unit,
    onSwitchAccount: (Int) -> Unit,
    onRemoveAccount: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    val tokens = AppDesignSystem
    val windowInfo = rememberWindowInfo()
    val isExpanded = windowInfo.isExpanded
    
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val expiryTimestamp = accountInfo?.expDate?.toLongOrNull() ?: 0L
    val isExpiringSoon = expiryTimestamp > 0 && (expiryTimestamp * 1000 - currentTime < 24 * 60 * 60 * 1000)
    val isExpiringCritical = expiryTimestamp > 0 && (expiryTimestamp * 1000 - currentTime < 60 * 60 * 1000)

    val timeLeft = remember(expiryTimestamp, currentTime) {
        if (expiryTimestamp <= 0L || expiryTimestamp > 4000000000L) "ILIMITADO"
        else {
            val diff = expiryTimestamp * 1000 - currentTime
            if (diff <= 0) "EXPIRADO"
            else if (diff < 24 * 60 * 60 * 1000) {
                val h = diff / (1000 * 60 * 60)
                val m = (diff / (1000 * 60)) % 60
                val s = (diff / 1000) % 60
                String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
            } else {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(expiryTimestamp * 1000))
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "critical")
    val alpha by if (isExpiringCritical) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            title = stringResource(R.string.account_nav_title),
            subtitle = stringResource(R.string.account_nav_subtitle),
            onBack = onBack,
            actions = {
                AppIconButton(icon = Icons.Rounded.Refresh, onClick = onRefresh)
                AppIconButton(
                    icon = Icons.AutoMirrored.Rounded.Logout, 
                    onClick = onLogout,
                    tint = tokens.colors.error
                )
            }
        )

        if (accountInfo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tokens.colors.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        horizontal = if (isExpanded) tokens.spacing.extraLarge * 2 else tokens.spacing.large, 
                        vertical = tokens.spacing.large
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(), 
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Profile Column
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        modifier = Modifier.width(if (isExpanded) 320.dp else 240.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isExpanded) 200.dp else 160.dp)
                                .clip(CircleShape)
                                .background(tokens.colors.surface.copy(alpha = 0.2f))
                                .clickable { showAvatarDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize().padding(tokens.spacing.small),
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(4.dp, tokens.colors.primary)
                            ) {
                                if (!profileImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = profileImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountCircle, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(if (isExpanded) 140.dp else 100.dp), 
                                        tint = tokens.colors.primary
                                    )
                                }
                            }
                            
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).padding(tokens.spacing.medium).size(48.dp),
                                shape = CircleShape,
                                color = tokens.colors.primary,
                                contentColor = Color.Black,
                                border = BorderStroke(2.dp, Color.Black)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(tokens.spacing.large))
                        
                        Text(
                            text = (accountInfo.username ?: "USUÁRIO").uppercase(),
                            style = tokens.typography.headline,
                            fontWeight = FontWeight.Black,
                            color = tokens.colors.textPrimary,
                            letterSpacing = 1.sp
                        )
                        
                        val status = accountInfo.status?.lowercase() ?: ""
                        val statusColor = if (status == "active") tokens.colors.success else tokens.colors.error
                        
                        Surface(
                            modifier = Modifier.padding(top = tokens.spacing.medium),
                            color = statusColor.copy(alpha = 0.1f),
                            shape = tokens.shapes.extraLarge,
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = (accountInfo.status ?: "UNKNOWN").uppercase(),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                style = tokens.typography.label,
                                fontWeight = FontWeight.Black,
                                color = statusColor,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.width(64.dp))
                    }

                    // Details Column
                    Column(modifier = Modifier.weight(1f).padding(top = tokens.spacing.large)) {
                        SectionHeader("PERFIS DE USUÁRIO")
                        Spacer(modifier = Modifier.height(tokens.spacing.medium))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(), 
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium)
                        ) {
                            profiles.forEach { profile ->
                                ProfileCard(
                                    profile = profile,
                                    isActive = profile.id == activeProfile?.id,
                                    onClick = { onSelectProfile(profile) },
                                    onDelete = { onDeleteProfile(profile.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (profiles.size < 4) {
                                AddProfileCard(
                                    onClick = { showAddProfileDialog = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(tokens.spacing.giant))

                        SectionHeader("CONTAS IPTV")
                        Spacer(modifier = Modifier.height(tokens.spacing.medium))

                        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
                            appSettings.accounts.forEachIndexed { index, account ->
                                val isActive = index == appSettings.currentAccountIndex
                                Surface(
                                    onClick = { onSwitchAccount(index) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .adaptiveFocus(shape = tokens.shapes.medium),
                                    color = if (isActive) tokens.colors.primary.copy(alpha = 0.05f) else Color.Transparent,
                                    shape = tokens.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = tokens.spacing.large, vertical = tokens.spacing.medium),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CloudQueue, 
                                            contentDescription = null,
                                            tint = if (isActive) tokens.colors.primary else tokens.colors.textSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(tokens.spacing.large))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(account.username.uppercase(), color = tokens.colors.textPrimary, fontWeight = FontWeight.Black, style = tokens.typography.title)
                                            Text(account.baseUrl, color = tokens.colors.textSecondary.copy(alpha = 0.5f), style = tokens.typography.caption)
                                        }
                                        
                                        if (isActive) {
                                            Text("ATIVO", color = tokens.colors.primary, style = tokens.typography.label, fontWeight = FontWeight.Black)
                                        } else {
                                            AppIconButton(
                                                icon = Icons.Rounded.Delete,
                                                onClick = { onRemoveAccount(index) },
                                                tint = tokens.colors.error.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            AppSecondaryButton(
                                text = "ADICIONAR NOVA CONTA",
                                icon = Icons.Rounded.Add,
                                onClick = onLogout,
                                modifier = Modifier.fillMaxWidth().padding(top = tokens.spacing.medium)
                            )
                        }

                        Spacer(modifier = Modifier.height(tokens.spacing.giant))

                        SectionHeader("INFORMAÇÕES DE ACESSO")
                        Spacer(modifier = Modifier.height(tokens.spacing.medium))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
                            SettingsStatCard(
                                label = "CONEXÕES",
                                value = "${accountInfo.activeCons ?: "0"}/${accountInfo.maxConnections ?: "0"}",
                                icon = Icons.Rounded.Dns,
                                modifier = Modifier.weight(1f)
                            )
                            SettingsStatCard(
                                label = if (isExpiringSoon) "VENCENDO EM" else "DATA DE EXPIRAÇÃO",
                                value = timeLeft,
                                icon = if (isExpiringSoon) Icons.Rounded.Timer else Icons.Rounded.CalendarToday,
                                modifier = Modifier.weight(2f),
                                isHighlight = true,
                                highlightColor = if (isExpiringSoon) tokens.colors.error.copy(alpha = alpha) else tokens.colors.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(tokens.spacing.giant))

                        SectionHeader("DETALHES DO SISTEMA")
                        Spacer(modifier = Modifier.height(tokens.spacing.medium))
                        
                        Surface(
                            color = tokens.colors.surface.copy(alpha = 0.1f),
                            shape = tokens.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(tokens.spacing.large), verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
                                val context = LocalContext.current
                                val appVersion = remember(context) { AppVersionProvider.getFormattedVersionName(context).uppercase() }
                                SystemInfoRow("URL DO SERVIDOR", serverUrl, Icons.Rounded.CloudQueue)
                                SystemInfoRow("ENDEREÇO IP", ipAddress, Icons.Rounded.Public)
                                SystemInfoRow("ID DO DISPOSITIVO", deviceId, Icons.Rounded.Fingerprint)
                                SystemInfoRow("VERSÃO DO APP", "$appVersion PREMIUM", Icons.Rounded.Info)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(tokens.spacing.giant * 2))
            }
        }
    }

    if (showAvatarDialog) {
        AvatarSelectionDialog(
            currentAvatar = profileImageUrl,
            onAvatarSelected = { onUpdateProfilePic(it); showAvatarDialog = false },
            onDismiss = { showAvatarDialog = false }
        )
    }

    if (showAddProfileDialog) {
        AddProfileDialog(
            onAdd = { name, isChild -> onAddProfile(name, isChild); showAddProfileDialog = false },
            onDismiss = { showAddProfileDialog = false }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val tokens = AppDesignSystem
    Text(
        text = title, 
        style = tokens.typography.label, 
        fontWeight = FontWeight.Black, 
        color = tokens.colors.primary, 
        letterSpacing = 2.sp
    )
}

@Composable
fun ProfileCard(
    profile: UserProfile,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = AppDesignSystem
    var isFocused by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(140.dp)
            .adaptiveFocus(
                shape = tokens.shapes.large,
                onFocus = { isFocused = it }
            ),
        color = if (isActive || isFocused) tokens.colors.primary.copy(alpha = 0.1f) else tokens.colors.surface.copy(alpha = 0.2f),
        shape = tokens.shapes.large,
        border = BorderStroke(1.dp, if (isActive || isFocused) tokens.colors.primary else Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(tokens.spacing.medium)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!profile.iconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile.iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = if (profile.isChild) Icons.Rounded.Face else Icons.Rounded.Person,
                        contentDescription = null,
                        tint = if (isActive || isFocused) tokens.colors.primary else tokens.colors.textSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(tokens.spacing.small))
                Text(
                    text = profile.name.uppercase(),
                    style = tokens.typography.title,
                    fontWeight = FontWeight.Black,
                    color = if (isActive || isFocused) tokens.colors.primary else tokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (profile.isChild) {
                    Text("INFANTIL", style = tokens.typography.caption, color = tokens.colors.secondary, fontWeight = FontWeight.Black)
                }
            }
            
            if (profile.id != "default") {
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    AppIconButton(
                        icon = Icons.Rounded.Delete, 
                        onClick = onDelete,
                        tint = tokens.colors.error.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddProfileCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = AppDesignSystem
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(140.dp)
            .adaptiveFocus(
                shape = tokens.shapes.large,
                onFocus = { isFocused = it }
            ),
        color = if (isFocused) tokens.colors.primary.copy(alpha = 0.05f) else tokens.colors.surface.copy(alpha = 0.1f),
        shape = tokens.shapes.large,
        border = BorderStroke(1.dp, if (isFocused) tokens.colors.primary else Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add, 
                contentDescription = null,
                tint = if (isFocused) tokens.colors.primary else tokens.colors.textSecondary, 
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(tokens.spacing.small))
            Text(
                text = "NOVO PERFIL", 
                style = tokens.typography.label, 
                color = if (isFocused) tokens.colors.primary else tokens.colors.textSecondary, 
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun AddProfileDialog(onAdd: (String, Boolean) -> Unit, onDismiss: () -> Unit) {
    val tokens = AppDesignSystem
    var name by remember { mutableStateOf("") }
    var isChild by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("NOVO PERFIL", style = tokens.typography.headline, fontWeight = FontWeight.Black, color = tokens.colors.primary) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("NOME DO PERFIL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = tokens.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(tokens.spacing.large))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isChild, onCheckedChange = { isChild = it }, colors = CheckboxDefaults.colors(checkedColor = tokens.colors.primary))
                    Spacer(Modifier.width(tokens.spacing.small))
                    Text("PERFIL INFANTIL (RESTRITO)", style = tokens.typography.body, color = tokens.colors.textPrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            AppButton(text = "CRIAR", onClick = { if (name.isNotBlank()) onAdd(name, isChild) })
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = tokens.colors.textSecondary) }
        },
        containerColor = tokens.colors.backgroundSecondary,
        shape = tokens.shapes.extraLarge
    )
}

@Composable
fun AvatarSelectionDialog(
    currentAvatar: String?,
    onAvatarSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = AppDesignSystem
    val avatars = listOf(
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Aneka",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Milo",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Luna",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Oscar",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Zoe",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Jasper",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Bella",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Shadow",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Coco",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Simba",
        "https://api.dicebear.com/7.x/avataaars/svg?seed=Nala"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.colors.backgroundSecondary,
        shape = tokens.shapes.extraLarge,
        title = { 
            Column {
                Text("ESCOLHA SEU AVATAR", style = tokens.typography.headline, fontWeight = FontWeight.Black, color = tokens.colors.textPrimary)
                Text("PERSONALIZE SUA IDENTIDADE", style = tokens.typography.caption, color = tokens.colors.primary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(tokens.spacing.large))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
                    modifier = Modifier.heightIn(max = 350.dp)
                ) {
                    items(avatars.size) { index ->
                        val url = avatars[index]
                        val isSelected = currentAvatar == url
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) tokens.colors.primary.copy(alpha = 0.1f) else tokens.colors.surface.copy(alpha = 0.1f))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) tokens.colors.primary else Color.White.copy(alpha = 0.05f),
                                    shape = CircleShape
                                )
                                .clickable { onAvatarSelected(url) },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(tokens.spacing.small),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(tokens.spacing.large))
                var customUrl by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("OU USE UMA URL DIRETA") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = tokens.shapes.medium,
                    trailingIcon = {
                        IconButton(onClick = { if (customUrl.isNotBlank()) onAvatarSelected(customUrl) }) {
                            Icon(Icons.Rounded.Check, null, tint = tokens.colors.primary)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tokens.colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("FECHAR", color = tokens.colors.textSecondary) }
        }
    )
}

@Composable
fun SystemInfoRow(label: String, value: String, icon: ImageVector) {
    val tokens = AppDesignSystem
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tokens.colors.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(tokens.spacing.large))
        Column {
            Text(text = label, style = tokens.typography.caption, color = tokens.colors.textSecondary, fontWeight = FontWeight.Bold)
            Text(text = value.uppercase(), style = tokens.typography.body, color = tokens.colors.textPrimary, fontWeight = FontWeight.Black)
        }
    }
}
