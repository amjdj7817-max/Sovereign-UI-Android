package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Post
import com.example.data.User
import com.example.ui.InstagramViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: InstagramViewModel,
    isDetailView: Boolean = false,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedUser by viewModel.selectedUserProfile.collectAsState()
    val selectedPosts by viewModel.selectedUserPosts.collectAsState()
    val allPosts by viewModel.allPosts.collectAsState()

    // Determine which user context we are rendering
    val user = if (isDetailView) selectedUser else currentUser

    // Filter posts that belong to the active user profile
    val userPosts = if (isDetailView) {
        selectedPosts
    } else {
        allPosts.filter { it.userId == "me" }
    }

    Scaffold(
        topBar = {
            ProfileTopAppBar(
                user = user,
                isDetailView = isDetailView,
                onBackClick = { viewModel.navigateBack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag(if (isDetailView) "profile_detail_screen" else "my_profile_screen")
    ) { innerPadding ->
        if (user == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = InstaBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Profile Bio and Stats Section
                ProfileHeaderSection(
                    user = user,
                    isMe = !isDetailView,
                    onFollowClick = { viewModel.followUser(user.id) },
                    onMessageClick = { viewModel.navigateToActiveChat(user.id) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Determine whether user is allowed to view posts
                val isFollowing = user.isFollowing
                val isPrivate = user.isPrivate
                val canViewContent = !isPrivate || isFollowing || !isDetailView

                if (canViewContent) {
                    // Normal Tabs Grid Selector
                    ProfileTabsAndGrid(posts = userPosts)
                } else {
                    // Private Account lock overlay matches Instagram specification
                    PrivateAccountPlaceholder()
                }
            }
        }
    }
}

@Composable
fun ProfileTopAppBar(
    user: User?,
    isDetailView: Boolean,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDetailView) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (user?.isPrivate == true) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Private Account",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = user?.username ?: "instagram_user",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun ProfileHeaderSection(
    user: User,
    isMe: Boolean,
    onFollowClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded Avatar with fine spacing
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            // Statistics Grid (Accurate posts, followers, following counters)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfileStatItem(count = user.postsCount, label = "Posts")
                ProfileStatItem(count = user.followersCount, label = "Followers")
                ProfileStatItem(count = user.followingCount, label = "Following")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full Name and Bio details
        Text(
            text = user.fullName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (isMe) {
                "🎬 Building interactive apps in Android\n📌 Pixel Perfect Craftsman\n📩 DM for collabs"
            } else {
                "Digital Creator & Photographer 📸\nExploring natural depths and sharing lifestyle recipes."
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons Row correctly aligned side-by-side
        if (isMe) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = "Edit profile", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = "Share profile", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val isFollowing = user.isFollowing
            val isRequested = user.isFollowRequested

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFollowClick,
                    modifier = Modifier.weight(1f).height(36.dp).testTag("follow_profile_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else InstaBlue
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    val btnText = when {
                        isFollowing -> "Following"
                        isRequested -> "Requested"
                        else -> "Follow"
                    }
                    Text(
                        text = btnText,
                        color = if (isFollowing) MaterialTheme.colorScheme.onBackground else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onMessageClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("message_profile_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(text = "Message", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(), // displays exactly '0' if zero matching specifications
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = InstaTextGray
        )
    }
}

@Composable
fun ProfileTabsAndGrid(posts: List<Post>) {
    var selectedTabIdx by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Tab row icons (Grid, Reels)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { selectedTabIdx = 0 },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Grid layout",
                    tint = if (selectedTabIdx == 0) MaterialTheme.colorScheme.onBackground else InstaTextGray
                )
            }

            IconButton(
                onClick = { selectedTabIdx = 1 },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmarks layout",
                    tint = if (selectedTabIdx == 1) MaterialTheme.colorScheme.onBackground else InstaTextGray
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        if (posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.GridOn,
                        contentDescription = null,
                        tint = InstaTextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No posts yet",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        } else {
            // Beautiful grid lists
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                items(posts) { post ->
                    Box(modifier = Modifier.aspectRatio(1f)) {
                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (post.isVideo) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateAccountPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This Account is Private",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Follow this account to see their photos and videos.",
            fontSize = 13.sp,
            color = InstaTextGray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}
