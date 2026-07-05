package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.*
import com.example.ui.theme.*

@Composable
fun MainScreen(
    viewModel: InstagramViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Global sheets state
    val commentsPostId by viewModel.commentsPostId.collectAsState()
    val sharePostId by viewModel.sharePostId.collectAsState()
    val gallerySheetOpen by viewModel.gallerySheetOpen.collectAsState()
    val galleryMode by viewModel.galleryMode.collectAsState()
    val storyViewerIndex by viewModel.storyViewerIndex.collectAsState()
    val chatProfileCardOverlayUserId by viewModel.chatProfileCardOverlayUserId.collectAsState()

    val commentsList by viewModel.activeComments.collectAsState()
    val storiesList by viewModel.activeStories.collectAsState()

    // Determine whether to display the bottom bar (hidden in deep screens like chat or story view)
    val showBottomBar = screenState == ScreenState.MAIN_TABS && storyViewerIndex == -1

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                InstagramBottomBar(
                    currentTab = currentTab,
                    onTabSelect = { viewModel.selectTab(it) },
                    userAvatarUrl = currentUser?.avatarUrl ?: ""
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("main_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            // Main Frame Switcher with animated crossfade
            AnimatedContent(
                targetState = screenState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_navigation"
            ) { state ->
                when (state) {
                    ScreenState.MAIN_TABS -> {
                        when (currentTab) {
                            NavTab.HOME -> FeedScreen(viewModel = viewModel)
                            NavTab.SEARCH -> SearchScreen(viewModel = viewModel)
                            NavTab.REELS -> ReelsScreen(viewModel = viewModel)
                            NavTab.PROFILE -> ProfileScreen(viewModel = viewModel, isDetailView = false)
                            else -> Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                    ScreenState.CHAT_LIST -> ChatListScreen(viewModel = viewModel)
                    ScreenState.ACTIVE_CHAT -> ActiveChatScreen(viewModel = viewModel)
                    ScreenState.ACTIVITY_LOG -> ActivityLogScreen(viewModel = viewModel)
                    ScreenState.USER_PROFILE_DETAIL -> ProfileScreen(viewModel = viewModel, isDetailView = true)
                }
            }

            // Global comments sheet overlay (Video continues playing in background!)
            if (commentsPostId != null) {
                CommentsBottomSheet(
                    postId = commentsPostId!!,
                    comments = commentsList,
                    onDismiss = { viewModel.closeCommentsSheet() },
                    onPostComment = { text -> viewModel.postComment(commentsPostId!!, text) }
                )
            }

            // Global sharing sheet overlay
            if (sharePostId != null) {
                ShareSheet(
                    postId = sharePostId!!,
                    onDismiss = { viewModel.closeShareSheet() },
                    viewModel = viewModel
                )
            }

            // Global mock gallery sheet
            if (gallerySheetOpen) {
                GalleryMockSheet(
                    onDismiss = { viewModel.closeGallerySheet() },
                    onSubmit = { url, caption, isVideo ->
                        viewModel.handleGalleryUpload(url, caption, isVideo)
                    },
                    mode = galleryMode
                )
            }

            // Fullscreen story viewer overlay
            if (storyViewerIndex != -1) {
                StoryViewerOverlay(
                    stories = storiesList,
                    currentIndex = storyViewerIndex,
                    onClose = { viewModel.closeStoryViewer() },
                    onSendQuickReply = { text, link ->
                        viewModel.sendQuickMessageStory(text, link)
                    }
                )
            }

            // Chat view profile details card overlay
            if (chatProfileCardOverlayUserId != null) {
                ChatProfileCardOverlay(
                    userId = chatProfileCardOverlayUserId!!,
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeChatProfileCardOverlay() }
                )
            }
        }
    }
}

@Composable
fun InstagramBottomBar(
    currentTab: NavTab,
    onTabSelect: (NavTab) -> Unit,
    userAvatarUrl: String
) {
    // Standard M3 Navigation bar with custom Instagram minimal theme and layout
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            .testTag("instagram_bottom_navigation")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Home
            IconButton(
                onClick = { onTabSelect(NavTab.HOME) },
                modifier = Modifier.testTag("tab_home")
            ) {
                Icon(
                    imageVector = if (currentTab == NavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab 2: Search
            IconButton(
                onClick = { onTabSelect(NavTab.SEARCH) },
                modifier = Modifier.testTag("tab_search")
            ) {
                Icon(
                    imageVector = if (currentTab == NavTab.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab 3: Create (+)
            IconButton(
                onClick = { onTabSelect(NavTab.ADD) },
                modifier = Modifier.testTag("tab_add")
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddBox,
                    contentDescription = "Create post",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Tab 4: Reels (Vertical play / Library icon)
            IconButton(
                onClick = { onTabSelect(NavTab.REELS) },
                modifier = Modifier.testTag("tab_reels")
            ) {
                Icon(
                    imageVector = if (currentTab == NavTab.REELS) Icons.Outlined.Slideshow else Icons.Outlined.Slideshow,
                    contentDescription = "Reels",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Tab 5: Profile circle avatar icon matching Instagram reference
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (currentTab == NavTab.PROFILE) 1.5.dp else 0.dp,
                        color = if (currentTab == NavTab.PROFILE) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onTabSelect(NavTab.PROFILE) }
                    .testTag("tab_profile"),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = "My Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(InstaBlue)
                    )
                }
            }
        }
    }
}
