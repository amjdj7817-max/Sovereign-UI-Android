package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityLog
import com.example.data.Chat
import com.example.data.Comment
import com.example.data.Message
import com.example.data.Post
import com.example.data.Repository
import com.example.data.Story
import com.example.data.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Random

enum class NavTab {
    HOME, SEARCH, ADD, REELS, PROFILE
}

enum class ScreenState {
    MAIN_TABS,
    CHAT_LIST,
    ACTIVE_CHAT,
    ACTIVITY_LOG,
    USER_PROFILE_DETAIL
}

class InstagramViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository(application)

    // Repository Flows
    val currentUser: StateFlow<User?> = repository.currentUserFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allPosts: StateFlow<List<Post>> = repository.allPosts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reels: StateFlow<List<Post>> = repository.reels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeStories: StateFlow<List<Story>> = repository.activeStories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chats: StateFlow<List<Chat>> = repository.chats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activityLogs: StateFlow<List<ActivityLog>> = repository.activityLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalUnreadMessages: StateFlow<Int?> = repository.totalUnreadMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Screen state and navigation
    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab = _currentTab.asStateFlow()

    private val _screenState = MutableStateFlow(ScreenState.MAIN_TABS)
    val screenState = _screenState.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId = _activeChatId.asStateFlow()

    private val _activeUserProfileId = MutableStateFlow<String?>(null)
    val activeUserProfileId = _activeUserProfileId.asStateFlow()

    // Dynamic user caching for profiled details
    private val _selectedUserProfile = MutableStateFlow<User?>(null)
    val selectedUserProfile = _selectedUserProfile.asStateFlow()

    private val _selectedUserPosts = MutableStateFlow<List<Post>>(emptyList())
    val selectedUserPosts = _selectedUserPosts.asStateFlow()

    // Bottom sheets & Overlays
    private val _commentsPostId = MutableStateFlow<String?>(null)
    val commentsPostId = _commentsPostId.asStateFlow()

    private val _storyViewerIndex = MutableStateFlow<Int>(-1) // -1 means closed
    val storyViewerIndex = _storyViewerIndex.asStateFlow()

    private val _sharePostId = MutableStateFlow<String?>(null)
    val sharePostId = _sharePostId.asStateFlow()

    private val _gallerySheetOpen = MutableStateFlow(false)
    val gallerySheetOpen = _gallerySheetOpen.asStateFlow()

    private val _galleryMode = MutableStateFlow("POST") // "POST" or "STORY"
    val galleryMode = _galleryMode.asStateFlow()

    // Playback Speed (1.0x, 1.5x, 2.0x) for active video
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed = _playbackSpeed.asStateFlow()

    // Draggable Progress/Timeline for currently playing video (normalized 0.0f to 1.0f)
    private val _playbackProgress = MutableStateFlow(0.0f)
    val playbackProgress = _playbackProgress.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying = _isPlaying.asStateFlow()

    // Comments list in bottom sheet
    private val _activeComments = MutableStateFlow<List<Comment>>(emptyList())
    val activeComments = _activeComments.asStateFlow()

    // Chat messages list
    private val _activeMessages = MutableStateFlow<List<Message>>(emptyList())
    val activeMessages = _activeMessages.asStateFlow()

    // Voice Note State
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice = _isRecordingVoice.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec = _recordingDurationSec.asStateFlow()

    private val _recordingWaveform = MutableStateFlow<List<Float>>(emptyList())
    val recordingWaveform = _recordingWaveform.asStateFlow()

    // Searching
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Profile detail overlay inside chat
    private val _chatProfileCardOverlayUserId = MutableStateFlow<String?>(null)
    val chatProfileCardOverlayUserId = _chatProfileCardOverlayUserId.asStateFlow()

    // Timer jobs
    private var storyTimerJob: Job? = null
    private var voiceTimerJob: Job? = null
    private var reelsProgressJob: Job? = null

    init {
        // Collect comments flow when post id changes
        viewModelScope.launch {
            commentsPostId.collect { postId ->
                if (postId != null) {
                    repository.getCommentsForPost(postId).collect {
                        _activeComments.value = it
                    }
                } else {
                    _activeComments.value = emptyList()
                }
            }
        }

        // Collect messages flow when active chat id changes
        viewModelScope.launch {
            activeChatId.collect { chatId ->
                if (chatId != null) {
                    repository.markChatAsRead(chatId)
                    repository.getMessagesForChat(chatId).collect {
                        _activeMessages.value = it
                    }
                } else {
                    _activeMessages.value = emptyList()
                }
            }
        }

        // Collect user profile when active profile id changes
        viewModelScope.launch {
            activeUserProfileId.collect { userId ->
                if (userId != null) {
                    repository.getUserProfileFlow(userId).collect { user ->
                        _selectedUserProfile.value = user
                    }
                } else {
                    _selectedUserProfile.value = null
                }
            }
        }

        // Collect user posts when active profile id changes
        viewModelScope.launch {
            activeUserProfileId.collect { userId ->
                if (userId != null) {
                    repository.getPostsByUserId(userId).collect { posts ->
                        _selectedUserPosts.value = posts
                    }
                } else {
                    _selectedUserPosts.value = emptyList()
                }
            }
        }

        // Start Reels Progress Simulator to drive the Progress Bar and Auto-play loops nicely!
        startReelsProgressSimulator()
    }

    // Navigation calls
    fun selectTab(tab: NavTab) {
        if (tab == NavTab.ADD) {
            openGallerySheet("POST")
        } else {
            _currentTab.value = tab
            _screenState.value = ScreenState.MAIN_TABS
            // Reset speed on tab switch
            _playbackSpeed.value = 1.0f
            if (tab == NavTab.REELS) {
                _isPlaying.value = true
            }
        }
    }

    fun navigateToChatList() {
        _screenState.value = ScreenState.CHAT_LIST
    }

    fun navigateToActiveChat(chatId: String) {
        _activeChatId.value = chatId
        _screenState.value = ScreenState.ACTIVE_CHAT
    }

    fun navigateToActivityLog() {
        _screenState.value = ScreenState.ACTIVITY_LOG
    }

    fun navigateToUserProfile(userId: String) {
        if (userId == "me") {
            _currentTab.value = NavTab.PROFILE
            _screenState.value = ScreenState.MAIN_TABS
        } else {
            _activeUserProfileId.value = userId
            _screenState.value = ScreenState.USER_PROFILE_DETAIL
        }
    }

    fun navigateBack() {
        when (_screenState.value) {
            ScreenState.ACTIVE_CHAT -> {
                _screenState.value = ScreenState.CHAT_LIST
                _activeChatId.value = null
            }
            ScreenState.CHAT_LIST, ScreenState.ACTIVITY_LOG, ScreenState.USER_PROFILE_DETAIL -> {
                _screenState.value = ScreenState.MAIN_TABS
                _activeUserProfileId.value = null
            }
            else -> {}
        }
    }

    // Engagement actions
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

    fun postComment(postId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val user = currentUser.value ?: return@launch
            repository.addComment(postId, text, user.username, user.avatarUrl)
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            repository.followUser(userId)
        }
    }

    fun followBack(logId: String, username: String) {
        viewModelScope.launch {
            repository.followBack(logId, username)
        }
    }

    // Sheets management
    fun openCommentsSheet(postId: String) {
        _commentsPostId.value = postId
    }

    fun closeCommentsSheet() {
        _commentsPostId.value = null
    }

    fun openShareSheet(postId: String) {
        _sharePostId.value = postId
    }

    fun closeShareSheet() {
        _sharePostId.value = null
    }

    fun openGallerySheet(mode: String = "POST") {
        _galleryMode.value = mode
        _gallerySheetOpen.value = true
    }

    fun closeGallerySheet() {
        _gallerySheetOpen.value = false
    }

    fun openChatProfileCardOverlay(userId: String) {
        _chatProfileCardOverlayUserId.value = userId
    }

    fun closeChatProfileCardOverlay() {
        _chatProfileCardOverlayUserId.value = null
    }

    // Story flow
    fun openStoryViewer(startIndex: Int) {
        _storyViewerIndex.value = startIndex
        _isPlaying.value = false // Pause background video
        startStoryProgressTimer()
    }

    fun closeStoryViewer() {
        storyTimerJob?.cancel()
        _storyViewerIndex.value = -1
        _isPlaying.value = true
    }

    private fun startStoryProgressTimer() {
        storyTimerJob?.cancel()
        storyTimerJob = viewModelScope.launch {
            // Simulated 15 second story progress. In a high fidelity mock we transition story by story!
            delay(15000)
            val stories = activeStories.value
            val nextIndex = _storyViewerIndex.value + 1
            if (nextIndex < stories.size) {
                _storyViewerIndex.value = nextIndex
                startStoryProgressTimer()
            } else {
                closeStoryViewer()
            }
        }
    }

    fun sendQuickMessageStory(text: String, storyLink: String) {
        val stories = activeStories.value
        val currentIndex = _storyViewerIndex.value
        if (currentIndex < 0 || currentIndex >= stories.size) return
        val story = stories[currentIndex]

        viewModelScope.launch {
            // Check if chat exists with the story owner
            val chat = chats.value.find { it.participantId == story.userId }
            val chatId = chat?.id ?: story.userId
            if (chat == null) {
                // Pre-create chat
                val user = repository.getUserProfileFlow(story.userId).first()
                if (user != null) {
                    repository.followUser(story.userId) // Ensure followed/friend chat creation!
                }
            }
            // Send quick text + link attach
            repository.sendMessage(
                chatId = chatId,
                senderId = "me",
                text = "$text (Attached Story link: $storyLink)"
            )
            closeStoryViewer()
        }
    }

    // Playback video controls
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun seekPlayback(progress: Float) {
        _playbackProgress.value = progress.coerceIn(0.0f, 1.0f)
    }

    private fun startReelsProgressSimulator() {
        reelsProgressJob?.cancel()
        reelsProgressJob = viewModelScope.launch {
            while (true) {
                delay(100)
                if (_isPlaying.value && _currentTab.value == NavTab.REELS && _storyViewerIndex.value == -1) {
                    // Drive progress based on current playback speed
                    val currentProgress = _playbackProgress.value
                    // 100ms interval -> standard duration 15s. Base increment: 0.1s / 15s = 0.0066f
                    val speedMultiplier = _playbackSpeed.value
                    val increment = (0.1f / 15.0f) * speedMultiplier
                    val nextProgress = currentProgress + increment
                    if (nextProgress >= 1.0f) {
                        _playbackProgress.value = 0.0f
                        // In an actual reels infinite scroll, we can just loop the current reel
                    } else {
                        _playbackProgress.value = nextProgress
                    }
                }
            }
        }
    }

    // Voice Note recorder simulation
    fun startRecordingVoice() {
        _isRecordingVoice.value = true
        _recordingDurationSec.value = 0
        _recordingWaveform.value = emptyList()
        val random = Random()

        voiceTimerJob?.cancel()
        voiceTimerJob = viewModelScope.launch {
            while (_isRecordingVoice.value) {
                delay(1000)
                _recordingDurationSec.value += 1
                // Add realistic amplitude floats between 0.1f and 1.0f
                val newWaveform = _recordingWaveform.value.toMutableList()
                for (i in 0..4) {
                    newWaveform.add(0.1f + random.nextFloat() * 0.9f)
                }
                _recordingWaveform.value = newWaveform
            }
        }
    }

    fun stopAndSendVoiceNote() {
        _isRecordingVoice.value = false
        voiceTimerJob?.cancel()
        val duration = _recordingDurationSec.value
        if (duration < 1) return // too short

        val chatId = _activeChatId.value ?: return
        val waveformString = _recordingWaveform.value.joinToString(",") { String.format("%.2f", it) }

        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                senderId = "me",
                isVoiceNote = true,
                voiceNoteWaveform = waveformString,
                voiceNoteDurationSec = duration
            )
        }
    }

    fun cancelRecordingVoice() {
        _isRecordingVoice.value = false
        voiceTimerJob?.cancel()
    }

    // Normal messaging
    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                senderId = "me",
                text = text
            )
        }
    }

    fun sendPhotoMessage(mediaUrl: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                senderId = "me",
                text = "Attached photo from camera roll 📸",
                mediaUrl = mediaUrl
            )
        }
    }

    // Attached sharing in messaging
    fun sharePostToFriend(chatId: String, postId: String) {
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                senderId = "me",
                attachedPostLink = postId
            )
            closeShareSheet()
        }
    }

    fun sharePostToStory(postId: String) {
        viewModelScope.launch {
            val post = repository.getPostById(postId) ?: return@launch
            repository.createStory(post.mediaUrl)
            closeShareSheet()
        }
    }

    // Posting gallery mock selection
    fun handleGalleryUpload(mediaUrl: String, caption: String, isVideo: Boolean) {
        viewModelScope.launch {
            if (_galleryMode.value == "POST") {
                repository.createPost(mediaUrl, caption, isVideo)
            } else {
                repository.createStory(mediaUrl)
            }
            closeGallerySheet()
        }
    }

    // Search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun onCleared() {
        super.onCleared()
        storyTimerJob?.cancel()
        voiceTimerJob?.cancel()
        reelsProgressJob?.cancel()
    }
}
