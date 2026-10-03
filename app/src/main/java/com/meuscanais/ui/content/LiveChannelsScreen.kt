package com.meuscanais.ui.content

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.meuscanais.R
import com.meuscanais.core.ui.components.buttons.AppIconButton
import com.meuscanais.core.ui.components.navigation.AppHeader
import com.meuscanais.core.ui.components.navigation.AppSidebar
import com.meuscanais.core.ui.components.states.EmptyState
import com.meuscanais.core.ui.components.states.LoadingState
import com.meuscanais.ui.dashboard.PortalBackground
import com.meuscanais.ui.viewmodel.CategoryViewModel
import com.meuscanais.ui.viewmodel.MainViewModel
import com.meuscanais.ui.content.components.ContentGrid
import com.meuscanais.ui.content.components.PinUnlockDialog
import com.meuscanais.ui.content.components.SortMenuDialog
import com.meuscanais.ui.viewmodel.settings.SettingsViewModel
import com.meuscanais.util.WindowSize
import com.meuscanais.util.rememberWindowInfo
import kotlinx.coroutines.delay

@UnstableApi
@Composable
fun LiveChannelsScreen(
    viewModel: MainViewModel,
    categoryViewModel: CategoryViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onPlay: (Int, String, String, String?, Map<String, Int>, String?) -> Unit,
    onCategoryClick: (String, String?) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val contentRows by categoryViewModel.currentContentRows.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isLoading by categoryViewModel.isLoading.collectAsStateWithLifecycle()
    val sortOrder by categoryViewModel.sortOrder.collectAsStateWithLifecycle()
    val blockedIds by viewModel.blockedCategoryIds.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    
    var showSortMenu by remember { mutableStateOf(false) }
    var pendingCategoryToUnlock by remember { mutableStateOf<String?>(null) }
    
    val windowInfo = rememberWindowInfo()
    val isCompact = windowInfo.widthSize == WindowSize.COMPACT
    
    val sidebarFocusRequester = remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }
    var wasPinDialogActive by remember { mutableStateOf(false) }

    val ids = remember(settings.hideBlockedCategories, blockedIds) {
        if (settings.hideBlockedCategories) blockedIds else emptySet()
    }

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(contentRows) {
        val loadedCatId = contentRows.firstOrNull()?.catId
        if (loadedCatId != null && selectedCategoryId != loadedCatId) {
            selectedCategoryId = loadedCatId
        }
    }

    var isInitialLoad by remember { mutableStateOf(true) }

    LaunchedEffect(isLoading) {
        if (!isLoading && isInitialLoad) {
            isInitialLoad = false
            delay(200)
            try { sidebarFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    LaunchedEffect(pendingCategoryToUnlock) {
        if (pendingCategoryToUnlock != null) {
            wasPinDialogActive = true
        } else if (wasPinDialogActive) {
            wasPinDialogActive = false
            delay(150)
            try {
                sidebarFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val handleCategorySelect: (String) -> Unit = { catId ->
        if (blockedIds.contains(catId) && !settings.hideBlockedCategories) {
            pendingCategoryToUnlock = catId
        } else {
            selectedCategoryId = catId
            viewModel.saveLastCategory("live", catId)
            categoryViewModel.loadContent("live", catId, ids)
        }
    }

    PortalBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            val activeCat = categories.find { cat -> 
                cat.categoryId == selectedCategoryId || contentRows.any { it.catId == cat.categoryId } 
            }
            AppHeader(
                title = stringResource(R.string.live_tv_nav_title),
                subtitle = activeCat?.categoryName ?: stringResource(R.string.all_categories_label),
                onBack = onBack,
                actions = {
                    AppIconButton(Icons.AutoMirrored.Rounded.Sort, { showSortMenu = true })
                    AppIconButton(Icons.Rounded.Search, onNavigateToSearch)
                }
            )

            Row(modifier = Modifier.fillMaxSize()) {
                if (!isCompact) {
                    AppSidebar(
                        items = categories,
                        selectedItemPredicate = { cat -> 
                            cat.categoryId == selectedCategoryId || (selectedCategoryId == null && contentRows.any { it.catId == cat.categoryId })
                        },
                        itemLabel = { it.categoryName ?: "" },
                        onItemClick = { cat ->
                            cat.categoryId?.let { catId ->
                                handleCategorySelect(catId)
                                if (!blockedIds.contains(catId) || settings.hideBlockedCategories) {
                                    try { gridFocusRequester.requestFocus() } catch (_: Exception) {}
                                }
                            }
                        },
                        onItemFocus = { cat ->
                            cat.categoryId?.let { catId ->
                                if (catId != selectedCategoryId) {
                                    handleCategorySelect(catId)
                                }
                            }
                        },
                        focusRequester = sidebarFocusRequester,
                        nextFocusRequester = gridFocusRequester
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        LoadingState(message = stringResource(R.string.msg_connecting))
                    } else if (contentRows.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.no_results_found),
                            description = "Tente outra categoria."
                        )
                    } else {
                        ContentGrid(
                            items = contentRows.flatMap { it.items },
                            gridFocusRequester = gridFocusRequester,
                            sidebarFocusRequester = sidebarFocusRequester,
                            onItemClick = { item ->
                                onPlay(item.id.toInt(), item.name, item.type.toString(), item.epgId, item.qualities, item.icon)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSortMenu) {
        SortMenuDialog(
            currentSort = sortOrder,
            onSortChanged = { categoryViewModel.onSortOrderChanged(it); showSortMenu = false },
            onDismiss = { showSortMenu = false }
        )
    }

    if (pendingCategoryToUnlock != null) {
        PinUnlockDialog(
            onDismiss = { pendingCategoryToUnlock = null },
            onConfirm = { pin ->
                if (pin == settings.appPin) {
                    val unlockedCat = pendingCategoryToUnlock!!
                    viewModel.unlockCategory(unlockedCat)
                    selectedCategoryId = unlockedCat
                    viewModel.saveLastCategory("live", unlockedCat)
                    categoryViewModel.loadContent("live", unlockedCat, ids)
                    pendingCategoryToUnlock = null
                }
            }
        )
    }
}
