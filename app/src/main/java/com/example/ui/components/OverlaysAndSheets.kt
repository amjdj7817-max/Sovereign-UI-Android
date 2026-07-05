package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Comment
import com.example.data.Post
import com.example.data.Story
import com.example.data.User
import com.example.ui.InstagramViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun StoryViewerOverlay(
    stories: List<Story>,
    currentIndex: Int,
    onClose: () -> Unit,
    onSendQuickReply: (text: String, storyLink: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentIndex < 0 || currentIndex >= stories.size) return
    val story = stories[currentIndex]

    var replyText by remember { mutableStateOf("") }
    var currentProgress by remember { mutableStateOf(0.0f) }
    var isPaused by remember { mutableStateOf(false) }

    // Story progress simulation (15s duration)
    LaunchedEffect(currentIndex, isPaused) {
        if (!isPaused) {
            val steps = 150
            val stepDelay = 100L
            while (currentProgress < 1.0f) {
                delay(stepDelay)
                currentProgress += (1.0f / steps)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("story_viewer_overlay")
    ) {
        // High fidelity visual story media background
        AsyncImage(
            model = story.mediaUrl,
            contentDescription = "Story Media",
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val width = size.width
                            if (offset.x < width / 3) {
                                // Tap left (would navigate previous in ViewModel but handled simply here)
                            } else {
                                // Tap right (would navigate next in ViewModel)
                            }
                        }
                    )
                },
            contentScale = ContentScale.Crop
        )

        // Top semi-transparent gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
        ) {
            // Story progression indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val progress = when {
                        index < currentIndex -> 1.0f
                        index == currentIndex -> currentProgress
                        else -> 0.0f
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            // User Info header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = story.userAvatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = story.username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "15s clip",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close story",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom reply bar with Quick Message attachment logic
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
                .padding(16.dp)
        ) {
            // Quick reply template buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("🔥", "😍", "👏", "😂", "😮", "❤️").forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable {
                                onSendQuickReply(emoji, story.mediaUrl)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            // Text input box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Send message...", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 50.dp)
                        .testTag("story_reply_input"),
                    shape = RoundedCornerShape(25.dp)
                )

                if (replyText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSendQuickReply(replyText, story.mediaUrl)
                            replyText = ""
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(InstaBlue)
                            .testTag("story_reply_send")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send Reply",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like Story",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onSendQuickReply("Liked your story! ❤️", story.mediaUrl) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    comments: List<Comment>,
    onDismiss: () -> Unit,
    onPostComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newCommentText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = InstaTextGray) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxHeight(0.75f).testTag("comments_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        ) {
            // Header
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            if (comments.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ModeComment,
                            contentDescription = null,
                            tint = InstaTextGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No comments yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Start the conversation.",
                            fontSize = 12.sp,
                            color = InstaTextGray
                        )
                    }
                }
            } else {
                // Comments list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(comments) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = comment.userAvatarUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.username,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "3h", // Hardcoded mock relative time
                                        fontSize = 11.sp,
                                        color = InstaTextGray
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like comment",
                                tint = InstaTextGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            // Text input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(InstaBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "A", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(10.dp))

                TextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("Add a comment...", color = InstaTextGray, fontSize = 13.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field")
                )

                if (newCommentText.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onPostComment(newCommentText)
                            newCommentText = ""
                        },
                        modifier = Modifier.testTag("comment_submit_btn")
                    ) {
                        Text(text = "Post", color = InstaBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    postId: String,
    onDismiss: () -> Unit,
    viewModel: InstagramViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val friendsList by viewModel.chats.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = InstaTextGray) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxHeight(0.6f).testTag("share_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Share",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            // Direct share capabilities
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // WhatsApp
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Sharing to WhatsApp...", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "WhatsApp", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }

                // Copy Link
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Copy Link", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }

                // Add to Story
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        viewModel.sharePostToStory(postId)
                        Toast.makeText(context, "Added to Story!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(InstaPurple, InstaPink, InstaOrange))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Add to Story", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Send in Chat",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (friendsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Follow friends to share posts with them!", color = InstaTextGray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(friendsList) { friend ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = friend.participantAvatar,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = friend.participantName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Send to chat link",
                                    fontSize = 11.sp,
                                    color = InstaTextGray
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.sharePostToFriend(friend.id, postId)
                                    Toast.makeText(context, "Post shared with ${friend.participantName}!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = InstaBlue),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(text = "Send", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Media item structure for Gallery
data class GalleryMedia(
    val url: String,
    val isVideo: Boolean,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryMockSheet(
    onDismiss: () -> Unit,
    onSubmit: (url: String, caption: String, isVideo: Boolean) -> Unit,
    mode: String = "POST",
    modifier: Modifier = Modifier
) {
    var selectedMediaUrl by remember { mutableStateOf("") }
    var selectedIsVideo by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // A collection of gorgeous mock media templates in different aspect ratios and video subjects
    val mockMediaList = remember {
        listOf(
            GalleryMedia(
                url = "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=600&auto=format&fit=crop&q=80",
                isVideo = false,
                description = "Forest sunlight (Photo)"
            ),
            GalleryMedia(
                url = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=600&auto=format&fit=crop&q=80",
                isVideo = true,
                description = "Beautiful Forest Stream (Video)"
            ),
            GalleryMedia(
                url = "https://images.unsplash.com/photo-1472214222541-d510753a8707?w=600&auto=format&fit=crop&q=80",
                isVideo = false,
                description = "Golden landscape (Photo)"
            ),
            GalleryMedia(
                url = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=600&auto=format&fit=crop&q=80",
                isVideo = true,
                description = "Misty mountains (Video)"
            ),
            GalleryMedia(
                url = "https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=600&auto=format&fit=crop&q=80",
                isVideo = false,
                description = "Autumn pathway (Photo)"
            ),
            GalleryMedia(
                url = "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=600&auto=format&fit=crop&q=80",
                isVideo = true,
                description = "Coffee pour-over (Video)"
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = InstaTextGray) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxHeight(0.85f).testTag("gallery_mock_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel", color = MaterialTheme.colorScheme.onBackground)
                }

                Text(
                    text = if (mode == "POST") "New Post" else "New Story",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = {
                        if (selectedMediaUrl.isNotBlank()) {
                            onSubmit(selectedMediaUrl, captionText, selectedIsVideo)
                        }
                    },
                    enabled = selectedMediaUrl.isNotBlank()
                ) {
                    Text(
                        text = "Next",
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMediaUrl.isNotBlank()) InstaBlue else InstaTextGray
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            if (selectedMediaUrl.isNotBlank()) {
                // Media preview & caption input
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(200.dp)
                            .aspectRatio(if (mode == "POST") 1f else 9 / 16f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = selectedMediaUrl,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (selectedIsVideo) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    if (mode == "POST") {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            placeholder = { Text("Write a caption...", color = InstaTextGray, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gallery_caption_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { selectedMediaUrl = "" }) {
                        Text(text = "Change selection", color = InstaBlue)
                    }
                }
            } else {
                // Media Grid picker
                Text(
                    text = "Select from Camera Roll",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = InstaTextGray,
                    modifier = Modifier.padding(16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    items(mockMediaList) { media ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    selectedMediaUrl = media.url
                                    selectedIsVideo = media.isVideo
                                }
                        ) {
                            AsyncImage(
                                model = media.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            if (media.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Video marker",
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatProfileCardOverlay(
    userId: String,
    viewModel: InstagramViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usersList by viewModel.chats.collectAsState()
    var userProfile by remember { mutableStateOf<User?>(null) }
    var userPosts by remember { mutableStateOf<List<Post>>(emptyList()) }

    // Fetch details reactively
    LaunchedEffect(userId) {
        val detailUser = viewModel.selectedUserProfile.value
        userProfile = detailUser
        userPosts = viewModel.selectedUserPosts.value
    }

    // Modal Sheet Overlay
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle(color = InstaTextGray) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxHeight(0.5f).testTag("chat_profile_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card Top Content
            AsyncImage(
                model = userProfile?.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = userProfile?.fullName ?: "Instagram User",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "@${userProfile?.username ?: "instagram_user"}",
                fontSize = 13.sp,
                color = InstaTextGray,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Counters matching specification: "Accurate, zero-based counting"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileStatColumn(count = userProfile?.postsCount ?: 0, label = "posts")
                ProfileStatColumn(count = userProfile?.followersCount ?: 0, label = "followers")
                ProfileStatColumn(count = userProfile?.followingCount ?: 0, label = "following")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Follow Request approval indicator/controls
            val isFollowing = userProfile?.isFollowing == true
            val isRequested = userProfile?.isFollowRequested == true
            val isPrivate = userProfile?.isPrivate == true

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        userProfile?.id?.let { viewModel.followUser(it) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else InstaBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val btnText = when {
                        isFollowing -> "Following"
                        isRequested -> "Requested"
                        else -> "Follow"
                    }
                    Text(
                        text = btnText,
                        color = if (isFollowing) MaterialTheme.colorScheme.onBackground else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Message", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileStatColumn(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
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
