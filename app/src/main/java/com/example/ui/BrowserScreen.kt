package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AdBlockerSheet
import com.example.ui.components.BookmarksHistoryDialog
import com.example.ui.components.BrowserWebView
import com.example.ui.components.CookieEditorDialog
import com.example.ui.components.HomeScreen
import com.example.ui.components.SettingsDialog
import com.example.ui.components.TabManagerSheet
import com.example.ui.theme.CyanPrimary
import com.example.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val isNightMode by viewModel.repository.isNightModeEnabled.collectAsState()
    val isAdBlocker by viewModel.repository.isAdBlockerEnabled.collectAsState()
    val isTextOnly by viewModel.repository.isTextOnlyMode.collectAsState()
    val bookmarks by viewModel.repository.bookmarks.collectAsState()

    // Sheet visibility
    val isTabManagerOpen by viewModel.isTabManagerOpen.collectAsState()
    val isMenuOpen by viewModel.isMenuOpen.collectAsState()
    val isCookieEditorOpen by viewModel.isCookieEditorOpen.collectAsState()
    val isAdBlockerSheetOpen by viewModel.isAdBlockerSheetOpen.collectAsState()
    val isBookmarksOpen by viewModel.isBookmarksHistoryOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val isFindInPageOpen by viewModel.isFindInPageOpen.collectAsState()

    // Address bar editing mode
    var isEditingAddress by remember { mutableStateOf(false) }
    var addressInput by remember { mutableStateOf("") }

    // Listen to toasts
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // Android Hardware / Gesture Back Handling
    BackHandler(enabled = true) {
        when {
            isEditingAddress -> isEditingAddress = false
            isFindInPageOpen -> viewModel.clearFind()
            isTabManagerOpen -> viewModel.isTabManagerOpen.value = false
            isMenuOpen -> viewModel.isMenuOpen.value = false
            isCookieEditorOpen -> viewModel.isCookieEditorOpen.value = false
            isAdBlockerSheetOpen -> viewModel.isAdBlockerSheetOpen.value = false
            isBookmarksOpen -> viewModel.isBookmarksHistoryOpen.value = false
            isSettingsOpen -> viewModel.isSettingsOpen.value = false
            currentTab?.canGoBack == true -> viewModel.goBack()
            currentTab?.isHome == false -> viewModel.goHome()
            tabs.size > 1 -> currentTab?.let { viewModel.closeTab(it.id) }
            else -> {
                // System default behavior will exit app
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    val isCurrentUrlBookmarked = remember(currentTab?.url, bookmarks) {
        currentTab?.let { viewModel.repository.isBookmarked(it.url) } == true
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Top Address Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingAddress) {
                        // In editing mode
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            placeholder = { Text("Search or type URL...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (addressInput.isNotBlank()) {
                                        viewModel.navigateTo(addressInput)
                                        isEditingAddress = false
                                    }
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { isEditingAddress = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("active_address_edit_field")
                        )
                    } else {
                        // Display Mode Address Bar
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable {
                                    addressInput = if (currentTab?.isHome == true) "" else currentTab?.url ?: ""
                                    isEditingAddress = true
                                }
                                .testTag("top_address_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Shield or SSL lock
                                if (currentTab?.isHome == true) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    // Shield with blocked count
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.isAdBlockerSheetOpen.value = true }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "AdBlocker Info",
                                            tint = if (isAdBlocker) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        if (currentTab != null && currentTab!!.blockedCount > 0) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "${currentTab!!.blockedCount}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CyanPrimary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // URL or Home title
                                Text(
                                    text = if (currentTab?.isHome == true) "Search or enter address" else currentTab?.url ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (currentTab?.isHome == true) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                // Right actions: Bookmark and Reload
                                if (currentTab?.isHome == false) {
                                    IconButton(
                                        onClick = {
                                            val url = currentTab?.url ?: ""
                                            val title = currentTab?.title ?: url
                                            if (url.isNotBlank()) {
                                                if (isCurrentUrlBookmarked) {
                                                    val b = bookmarks.find { it.url.equals(url, ignoreCase = true) }
                                                    if (b != null) viewModel.repository.removeBookmark(b.id)
                                                    viewModel.showToast("Bookmark removed")
                                                } else {
                                                    viewModel.repository.addBookmark(title, url)
                                                    viewModel.showToast("Bookmark added!")
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentUrlBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark",
                                            tint = if (isCurrentUrlBookmarked) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.reload() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reload",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Progress Bar
                AnimatedVisibility(
                    visible = currentTab?.isLoading == true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val progressFloat = (currentTab?.progress ?: 0) / 100f
                    LinearProgressIndicator(
                        progress = { progressFloat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = CyanPrimary,
                        trackColor = Color.Transparent
                    )
                }

                // Find in page bar
                AnimatedVisibility(visible = isFindInPageOpen) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = viewModel.findQuery.collectAsState().value,
                                onValueChange = { viewModel.performFind(it) },
                                placeholder = { Text("Find in page...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )
                            IconButton(onClick = { viewModel.clearFind() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Find")
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Via Browser's Signature Bottom Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("bottom_toolbar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(
                        onClick = { viewModel.goBack() },
                        enabled = currentTab?.canGoBack == true,
                        modifier = Modifier.testTag("nav_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (currentTab?.canGoBack == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                    // Forward Button
                    IconButton(
                        onClick = { viewModel.goForward() },
                        enabled = currentTab?.canGoForward == true,
                        modifier = Modifier.testTag("nav_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (currentTab?.canGoForward == true) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }

                    // Home Button
                    IconButton(
                        onClick = { viewModel.goHome() },
                        modifier = Modifier.testTag("nav_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (currentTab?.isHome == true) CyanPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Tabs Counter Button (e.g. [ 3 ])
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.5.dp, if (tabs.size > 1) CyanPrimary else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { viewModel.isTabManagerOpen.value = true }
                            .testTag("nav_tabs_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${tabs.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (tabs.size > 1) CyanPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Menu Button
                    IconButton(
                        onClick = { viewModel.isMenuOpen.value = true },
                        modifier = Modifier.testTag("nav_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            currentTab?.let { tab ->
                if (tab.isHome) {
                    HomeScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    BrowserWebView(
                        tab = tab,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Tab Manager Sheet
    if (isTabManagerOpen) {
        TabManagerSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.isTabManagerOpen.value = false }
        )
    }

    // Cookie Editor Dialog
    if (isCookieEditorOpen) {
        CookieEditorDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.isCookieEditorOpen.value = false }
        )
    }

    // Ad Blocker Sheet
    if (isAdBlockerSheetOpen) {
        AdBlockerSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.isAdBlockerSheetOpen.value = false }
        )
    }

    // Bookmarks & History Dialog
    if (isBookmarksOpen) {
        BookmarksHistoryDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.isBookmarksHistoryOpen.value = false }
        )
    }

    // Settings Dialog
    if (isSettingsOpen) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.isSettingsOpen.value = false }
        )
    }

    // Quick Action Menu Bottom Sheet (Via Style Menu)
    if (isMenuOpen) {
        val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.isMenuOpen.value = false },
            sheetState = menuSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("quick_menu_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Browser Tools",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { viewModel.isMenuOpen.value = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close Menu")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Grid of Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Row 1: Cookie Editor & Ad Blocker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MenuItemCard(
                            icon = Icons.Default.Cookie,
                            title = "Cookie Editor",
                            subtitle = "Copy JSON cookies",
                            badge = "JSON",
                            highlight = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.isMenuOpen.value = false
                                viewModel.loadCookiesForCurrentTab()
                                viewModel.isCookieEditorOpen.value = true
                            }
                        )
                        MenuItemCard(
                            icon = Icons.Default.Shield,
                            title = "Ad Blocker",
                            subtitle = if (isAdBlocker) "Active (${currentTab?.blockedCount ?: 0})" else "Disabled",
                            highlight = isAdBlocker,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.isMenuOpen.value = false
                                viewModel.isAdBlockerSheetOpen.value = true
                            }
                        )
                    }

                    // Row 2: Night Mode & Desktop Site
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MenuItemCard(
                            icon = Icons.Default.DarkMode,
                            title = "Night Mode",
                            subtitle = if (isNightMode) "Dark Web ON" else "Off",
                            highlight = isNightMode,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.toggleNightMode()
                            }
                        )
                        MenuItemCard(
                            icon = Icons.Default.DesktopWindows,
                            title = "Desktop Site",
                            subtitle = if (currentTab?.isDesktopMode == true) "Desktop ON" else "Mobile Mode",
                            highlight = currentTab?.isDesktopMode == true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.toggleDesktopMode()
                            }
                        )
                    }

                    // Row 3: Bookmarks/History & Text-Only Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MenuItemCard(
                            icon = Icons.Default.Bookmark,
                            title = "Bookmarks",
                            subtitle = "Saved & History",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.isMenuOpen.value = false
                                viewModel.isBookmarksHistoryOpen.value = true
                            }
                        )
                        MenuItemCard(
                            icon = Icons.Default.Image,
                            title = "Text Only",
                            subtitle = if (isTextOnly) "Fast Load ON" else "Images ON",
                            highlight = isTextOnly,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.toggleTextOnly()
                            }
                        )
                    }

                    // Row 4: Find in Page & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MenuItemCard(
                            icon = Icons.Default.FindInPage,
                            title = "Find in Page",
                            subtitle = "Search text",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.isMenuOpen.value = false
                                viewModel.isFindInPageOpen.value = true
                            }
                        )
                        MenuItemCard(
                            icon = Icons.Default.Share,
                            title = "Share Link",
                            subtitle = "Share current URL",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.isMenuOpen.value = false
                                val curUrl = currentTab?.url ?: ""
                                if (curUrl.isNotBlank()) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, curUrl)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Page"))
                                } else {
                                    viewModel.showToast("No active page to share")
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                // Settings Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.isMenuOpen.value = false
                            viewModel.isSettingsOpen.value = true
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Browser Settings",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (highlight) CyanPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            1.dp,
            if (highlight) CyanPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .height(68.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (highlight) CyanPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (highlight) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanPrimary)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (highlight) CyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
