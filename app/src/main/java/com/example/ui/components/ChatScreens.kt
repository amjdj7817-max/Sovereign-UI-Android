package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Chat
import com.example.data.Message
import com.example.ui.InstagramViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    viewModel: InstagramViewModel,
    modifier: Modifier = Modifier
) {
    val chats by viewModel.chats.collectAsState()

    Scaffold(
        topBar = {
            ChatListTopBar(onBackClick = { viewModel.navigateBack() })
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("chat_list_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (chats.isEmpty()) {
                // "No friends yet" state-based view strictly conforming to specifications
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No friends yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Follow other creators from the home screen to add them as friends and start messaging! 👋",
                            fontSize = 13.sp,
                            color = InstaTextGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                // Dynamic chats list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(chats, key = { it.id }) { chat ->
                        ChatItemRow(
                            chat = chat,
                            onClick = { viewModel.navigateToActiveChat(chat.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = "Messages",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "New message",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun ChatItemRow(
    chat: Chat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(54.dp)) {
            AsyncImage(
                model = chat.participantAvatar,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.participantName,
                fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                fontSize = 13.sp,
                color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onBackground else InstaTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (chat.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(InstaBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chat.unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ActiveChatScreen(
    viewModel: InstagramViewModel,
    modifier: Modifier = Modifier
) {
    val activeChatId by viewModel.activeChatId.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val chat = chats.find { it.id == activeChatId }
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Voice & Video calling states
    var showCallOverlay by remember { mutableStateOf(false) }
    var callIsVideo by remember { mutableStateOf(false) }

    // Scroll thread to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (chat == null) return

    Scaffold(
        topBar = {
            ActiveChatTopBar(
                chat = chat,
                onBackClick = { viewModel.navigateBack() },
                onProfileClick = {
                    // Triggers the "View Profile Overlay" within chat
                    viewModel.openChatProfileCardOverlay(chat.participantId)
                },
                onVoiceCall = {
                    callIsVideo = false
                    showCallOverlay = true
                },
                onVideoCall = {
                    callIsVideo = true
                    showCallOverlay = true
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().testTag("active_chat_window")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages thread list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Profile greeting card inside chat
                    item {
                        ChatThreadHeaderCard(
                            chat = chat,
                            onViewProfile = { viewModel.openChatProfileCardOverlay(chat.participantId) }
                        )
                    }

                    items(messages, key = { it.id }) { message ->
                        val isMe = message.senderId == "me"
                        MessageBubble(message = message, isMe = isMe)
                    }
                }

                // Chat Input bar featuring: Simulated voice note recorder, photo attachment, normal texts
                ChatInputFooter(
                    textValue = messageText,
                    onValueChange = { messageText = it },
                    onSend = {
                        viewModel.sendTextMessage(messageText)
                        messageText = ""
                    },
                    viewModel = viewModel
                )
            }

            // High Fidelity Voice/Video Call Controls Center Overlay
            AnimatedVisibility(
                visible = showCallOverlay,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                CallSimulationOverlay(
                    chat = chat,
                    isVideo = callIsVideo,
                    onEndCall = { showCallOverlay = false }
                )
            }
        }
    }
}

@Composable
fun ActiveChatTopBar(
    chat: Chat,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // Tapping avatar or participant name triggers ProfileCardOverlay
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onProfileClick)
                .testTag("chat_user_header_click")
        ) {
            AsyncImage(
                model = chat.participantAvatar,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = chat.participantName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Active now",
                    fontSize = 11.sp,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        // Video and Voice calling actions
        IconButton(onClick = onVoiceCall) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Voice Call",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        IconButton(onClick = onVideoCall) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Call",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun ChatThreadHeaderCard(
    chat: Chat,
    onViewProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = chat.participantAvatar,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = chat.participantName,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Instagram",
            fontSize = 12.sp,
            color = InstaTextGray,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onViewProfile,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp).testTag("chat_view_profile_btn")
        ) {
            Text(
                text = "View Profile",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isMe: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isMe) InstaBlue else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 260.dp)
        ) {
            Column {
                if (message.isVoiceNote) {
                    // Voice Note Waveform representation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play voice note",
                            tint = if (isMe) Color.White else MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )

                        // Render waveform
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val waveBars = message.voiceNoteWaveform?.split(",")?.map { it.toFloatOrNull() ?: 0.5f } ?: listOf(0.3f, 0.7f, 0.5f, 0.8f, 0.2f, 0.6f)
                            waveBars.take(15).forEach { amp ->
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height((amp * 20).coerceIn(4f, 20f).dp)
                                        .background(
                                            if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }

                        Text(
                            text = "0:${String.format("%02d", message.voiceNoteDurationSec)}",
                            fontSize = 11.sp,
                            color = if (isMe) Color.White.copy(alpha = 0.8f) else InstaTextGray
                        )
                    }
                } else if (message.mediaUrl != null) {
                    // Photo Attachment message
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = message.text,
                            color = if (isMe) Color.White else MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Normal Text message
                    Text(
                        text = message.text,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChatInputFooter(
    textValue: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    viewModel: InstagramViewModel
) {
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val recordingDuration by viewModel.recordingDurationSec.collectAsState()
    val recordingWaveform by viewModel.recordingWaveform.collectAsState()
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isRecordingVoice) {
            // Active Voice Recording Waveform UI panel
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Red.copy(alpha = 0.1f))
                    .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Text(
                        text = "Recording 0:${String.format("%02d", recordingDuration)}",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dynamic live wave amplitudes indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    recordingWaveform.takeLast(10).forEach { amp ->
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height((amp * 16).coerceIn(3f, 16f).dp)
                                .background(Color.Red)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cancel",
                        color = InstaTextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.cancelRecordingVoice() }
                    )
                    Text(
                        text = "Send",
                        color = InstaBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.stopAndSendVoiceNote() }
                    )
                }
            }
        } else {
            // Normal Typing and Attachments footer
            // Photo Attachment shortcut button
            IconButton(
                onClick = {
                    // Send a mock gorgeous travel picture instantly using clean ViewModel action
                    viewModel.sendPhotoMessage("https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500&auto=format&fit=crop&q=80")
                    Toast.makeText(context, "Photo attached from camera roll!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Photo,
                    contentDescription = "Attach image",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            OutlinedTextField(
                value = textValue,
                onValueChange = onValueChange,
                placeholder = { Text("Message...", color = InstaTextGray, fontSize = 14.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(max = 50.dp)
                    .testTag("chat_message_input")
            )

            if (textValue.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(InstaBlue)
                        .testTag("chat_message_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send text",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Microphone icon launches Voice Waveform recorder
                IconButton(
                    onClick = {
                        viewModel.startRecordingVoice()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("voice_note_mic_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record voice note",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallSimulationOverlay(
    chat: Chat,
    isVideo: Boolean,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calling duration ticker
    var secondsRunning by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secondsRunning++
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(InstaBlack)
            .testTag("call_simulation_overlay")
    ) {
        if (isVideo) {
            // Immersive background visual simulating video camera frame
            AsyncImage(
                model = "https://images.unsplash.com/photo-1544025162-d76694265947?w=1000&auto=format&fit=crop&q=80",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Translucent screen filter
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Caller profile
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = chat.participantAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = chat.participantName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Text(
                    text = if (secondsRunning > 3) {
                        "Connected 0:${String.format("%02d", secondsRunning - 3)}"
                    } else {
                        "Ringing..."
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Call Controls Centered Panel exactly matching standard call applications
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Microphone Mute Button
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // End Call Button (Red circle centered)
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .testTag("end_call_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Video toggle / Flip camera Button
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speaker",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
