package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Post
import com.example.data.Story
import com.example.data.User
import com.example.ui.InstagramViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    viewModel: InstagramViewModel,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.allPosts.collectAsState()
    val stories by viewModel.activeStories.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadCountFlow by viewModel.totalUnreadMessages.collectAsState()
    val unreadCount = unreadCountFlow ?: 0

    Scaffold(
        topBar = {
            FeedTopAppBar(
                unreadCount = unreadCount,
                onDirectMessageClick = { viewModel.navigateToChatList() },
                onActivityClick = { viewModel.navigateToActivityLog() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("feed_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Stories Section
            item {
                StoriesRow(
                    currentUser = currentUser,
                    stories = stories,
                    onAddStoryClick = { viewModel.openGallerySheet("STORY") },
                    onStoryClick = { index -> viewModel.openStoryViewer(index) }
                )
                Divider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    thickness = 0.5.dp
                )
            }

            // Feed Posts
            items(posts, key = { it.id }) { post ->
                PostItem(
                    post = post,
                    viewModel = viewModel,
                    onLikeToggle = { viewModel.toggleLike(post.id) },
                    onCommentsClick = { viewModel.openCommentsSheet(post.id) },
                    onShareClick = { viewModel.openShareSheet(post.id) },
                    onUserClick = { viewModel.navigateToUserProfile(post.userId) }
                )
            }
        }
    }
}

@Composable
fun FeedTopAppBar(
    unreadCount: Int,
    onDirectMessageClick: () -> Unit,
    onActivityClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pixel-matched classic elegant branding text
        Text(
            text = "Instagram",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Activity log (Heart icon)
            IconButton(
                onClick = onActivityClick,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            // Direct message paper-plane icon with red notification dot + unread count
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onDirectMessageClick)
                    .testTag("dm_paperplane_icon")
            ) {
                Icon(
                    imageVector = Icons.Outlined.MailOutline,
                    contentDescription = "Direct Messaging",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxSize()
                )

                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 5.dp, y = (-5).dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .testTag("dm_unread_badge")
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StoriesRow(
    currentUser: User?,
    stories: List<Story>,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add Your Story Column
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(onClick = onAddStoryClick).testTag("add_story_button")
            ) {
                Box(modifier = Modifier.size(68.dp)) {
                    AsyncImage(
                        model = currentUser?.avatarUrl,
                        contentDescription = "My avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(InstaBlue)
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Add Story",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your Story",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }

        // Friends Stories Cards
        itemsIndexed(stories) { index, story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(index) }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        // Gradient ring showing unviewed story
                        .background(
                            Brush.linearGradient(
                                colors = listOf(InstaPurple, InstaPink, InstaOrange, InstaYellow)
                            )
                        )
                        .padding(2.5.dp) // Ring thickness spacing
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(2.dp) // inner border
                ) {
                    AsyncImage(
                        model = story.userAvatarUrl,
                        contentDescription = story.username,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.username,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(68.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    viewModel: InstagramViewModel,
    onLikeToggle: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onUserClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isHeartVisible by remember { mutableStateOf(false) }
    var heartScale by remember { mutableStateOf(0.0f) }

    // Custom animation for heart scale pop
    val animatedHeartScale by animateFloatAsState(
        targetValue = heartScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heart_pop"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_item_${post.id}")
            .padding(bottom = 12.dp)
    ) {
        // User info row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.userAvatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onUserClick),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onUserClick)
            ) {
                Text(
                    text = post.username,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Suggested post", // mock detail
                    fontSize = 11.sp,
                    color = InstaTextGray
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Post Media with double-tap-to-like pop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (!post.isLiked) {
                                onLikeToggle()
                            }
                            coroutineScope.launch {
                                isHeartVisible = true
                                heartScale = 1.3f
                                delay(400)
                                heartScale = 0.0f
                                delay(100)
                                isHeartVisible = false
                            }
                        }
                    )
                }
        ) {
            AsyncImage(
                model = post.mediaUrl,
                contentDescription = "Post Media",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (post.isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = "Video post",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Animated double tap heart overlay
            if (isHeartVisible) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(100.dp)
                        .scale(animatedHeartScale)
                )
            }
        }

        // Row of buttons (Like, Comment, Share)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Heart button with spring toggle like animation
            IconButton(
                onClick = onLikeToggle,
                modifier = Modifier.testTag("like_button_${post.id}")
            ) {
                if (post.isLiked) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Unlike",
                        tint = Color.Red,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            IconButton(
                onClick = onCommentsClick,
                modifier = Modifier.testTag("comment_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.ModeComment,
                    contentDescription = "Comment",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(23.dp)
                )
            }

            IconButton(
                onClick = onShareClick,
                modifier = Modifier.testTag("share_button_${post.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Send,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        // Details column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            // Likes Count
            Text(
                text = "${post.likesCount} likes",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Caption
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(post.username)
                    }
                    append(" ")
                    append(post.caption)
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 18.sp
            )

            // Dynamic Comments shortcut triggering Bottom sheet
            if (post.commentsCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "View all ${post.commentsCount} comments",
                    color = InstaTextGray,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable(onClick = onCommentsClick)
                        .testTag("view_all_comments_trigger_${post.id}")
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "1 hour ago", // simulated
                fontSize = 10.sp,
                color = InstaTextGray
            )
        }
    }
}
