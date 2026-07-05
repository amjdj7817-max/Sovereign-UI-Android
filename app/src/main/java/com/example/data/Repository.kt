package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class Repository(private val context: Context) {

    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "instagram_clone_db"
    )
    .fallbackToDestructiveMigration()
    .build()

    private val userDao = db.userDao()
    private val postDao = db.postDao()
    private val commentDao = db.commentDao()
    private val storyDao = db.storyDao()
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()
    private val activityLogDao = db.activityLogDao()

    // Expose Flows
    val currentUserFlow: Flow<User?> = userDao.getUserByIdFlow("me")
    val allPosts: Flow<List<Post>> = postDao.getAllPostsFlow()
    val reels: Flow<List<Post>> = postDao.getReelsFlow()
    val activeStories: Flow<List<Story>> = storyDao.getActiveStoriesFlow()
    val chats: Flow<List<Chat>> = chatDao.getAllChatsFlow()
    val activityLogs: Flow<List<ActivityLog>> = activityLogDao.getActivityLogsFlow()
    val totalUnreadMessages: Flow<Int?> = chatDao.getTotalUnreadCountFlow()

    init {
        // Seed initial data in background
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfNeeded()
        }
    }

    suspend fun getUserProfileFlow(userId: String): Flow<User?> {
        return userDao.getUserByIdFlow(userId)
    }

    suspend fun getPostById(postId: String): Post? = withContext(Dispatchers.IO) {
        postDao.getPostById(postId)
    }

    fun getPostsByUserId(userId: String): Flow<List<Post>> {
        return postDao.getPostsByUserIdFlow(userId)
    }

    fun getCommentsForPost(postId: String): Flow<List<Comment>> {
        return commentDao.getCommentsForPostFlow(postId)
    }

    fun getMessagesForChat(chatId: String): Flow<List<Message>> {
        return messageDao.getMessagesForChatFlow(chatId)
    }

    // Toggle Post Like
    suspend fun toggleLike(postId: String) = withContext(Dispatchers.IO) {
        val post = postDao.getPostById(postId) ?: return@withContext
        val newIsLiked = !post.isLiked
        val diff = if (newIsLiked) 1 else -1
        postDao.updatePostLike(postId, newIsLiked, diff)
    }

    // Add Comment
    suspend fun addComment(postId: String, text: String, username: String, avatarUrl: String) = withContext(Dispatchers.IO) {
        val comment = Comment(
            id = UUID.randomUUID().toString(),
            postId = postId,
            username = username,
            userAvatarUrl = avatarUrl,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        commentDao.insertComment(comment)
        postDao.incrementCommentsCount(postId)
    }

    // Send Message
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        text: String = "",
        mediaUrl: String? = null,
        isVoiceNote: Boolean = false,
        voiceNoteWaveform: String? = null,
        voiceNoteDurationSec: Int = 0,
        attachedPostLink: String? = null
    ) = withContext(Dispatchers.IO) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            text = text,
            mediaUrl = mediaUrl,
            isVoiceNote = isVoiceNote,
            voiceNoteWaveform = voiceNoteWaveform,
            voiceNoteDurationSec = voiceNoteDurationSec,
            attachedPostLink = attachedPostLink,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)

        // Update the last message in chat, increment unread if from friend
        val chat = chatDao.getChatById(chatId) ?: return@withContext
        val lastMsgPreview = when {
            isVoiceNote -> "🎵 Voice note (${voiceNoteDurationSec}s)"
            mediaUrl != null -> "📷 Photo attachment"
            attachedPostLink != null -> "🔗 Shared a post link"
            else -> text
        }
        val unreadDiff = if (senderId == "me") 0 else 1
        chatDao.updateLastMessage(chatId, lastMsgPreview, System.currentTimeMillis(), unreadDiff)
    }

    // Add a Friend / Follow User
    suspend fun followUser(userId: String) = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext
        if (user.isFollowing) {
            // Unfollow
            userDao.updateUser(user.copy(isFollowing = false, followersCount = user.followersCount - 1))
            val me = userDao.getUserById("me") ?: return@withContext
            userDao.updateUser(me.copy(followingCount = Math.max(0, me.followingCount - 1)))
        } else {
            if (user.isPrivate) {
                // Request Follow
                userDao.updateUser(user.copy(isFollowRequested = true))
                
                // Add activity log
                val me = userDao.getUserById("me") ?: return@withContext
                activityLogDao.insertActivityLog(
                    ActivityLog(
                        id = UUID.randomUUID().toString(),
                        username = user.username,
                        avatarUrl = user.avatarUrl,
                        actionText = "requested to follow you.",
                        timestamp = System.currentTimeMillis(),
                        canFollowBack = false
                    )
                )
            } else {
                // Follow immediately
                userDao.updateUser(user.copy(isFollowing = true, followersCount = user.followersCount + 1))
                val me = userDao.getUserById("me") ?: return@withContext
                userDao.updateUser(me.copy(followingCount = me.followingCount + 1))

                // Check if a chat exists, if not, create one!
                val chatExist = chatDao.getChatById(userId)
                if (chatExist == null) {
                    val chat = Chat(
                        id = userId,
                        participantId = userId,
                        participantName = user.fullName,
                        participantAvatar = user.avatarUrl,
                        lastMessage = "You followed each other! Say hello 👋",
                        timestamp = System.currentTimeMillis(),
                        unreadCount = 0,
                        isOnline = true
                    )
                    chatDao.insertChat(chat)
                }

                // Add activity log
                activityLogDao.insertActivityLog(
                    ActivityLog(
                        id = UUID.randomUUID().toString(),
                        username = user.username,
                        avatarUrl = user.avatarUrl,
                        actionText = "started following you.",
                        timestamp = System.currentTimeMillis(),
                        canFollowBack = true,
                        isFollowingBack = false
                    )
                )
            }
        }
    }

    // Approve Follow Request (For Private Account)
    suspend fun approveFollowRequest(userId: String) = withContext(Dispatchers.IO) {
        val user = userDao.getUserById(userId) ?: return@withContext
        userDao.updateUser(user.copy(isFollowRequested = false, isFollowing = true, followersCount = user.followersCount + 1))
        val me = userDao.getUserById("me") ?: return@withContext
        userDao.updateUser(me.copy(followingCount = me.followingCount + 1))
    }

    // Follow back action from Activity log
    suspend fun followBack(logId: String, username: String) = withContext(Dispatchers.IO) {
        activityLogDao.updateFollowBackStatus(logId, true)
        // Find user by username and set isFollowing = true
        val users = userDao.getAllUsersFlow().first()
        val user = users.find { it.username == username }
        if (user != null) {
            userDao.updateUser(user.copy(isFollowing = true))
            val me = userDao.getUserById("me") ?: return@withContext
            userDao.updateUser(me.copy(followingCount = me.followingCount + 1))
        }
    }

    // Create a Post (Gallery Upload)
    suspend fun createPost(mediaUrl: String, caption: String, isVideo: Boolean) = withContext(Dispatchers.IO) {
        val me = userDao.getUserById("me") ?: return@withContext
        val postId = UUID.randomUUID().toString()
        val newPost = Post(
            id = postId,
            userId = me.id,
            username = me.username,
            userAvatarUrl = me.avatarUrl,
            mediaUrl = mediaUrl,
            isVideo = isVideo,
            caption = caption,
            likesCount = 0,
            commentsCount = 0,
            isLiked = false,
            timestamp = System.currentTimeMillis(),
            videoDurationMs = if (isVideo) 12000L else 0L,
            viewsCount = if (isVideo) 1 else 0
        )
        postDao.insertPost(newPost)
        // Update post count
        userDao.updateUser(me.copy(postsCount = me.postsCount + 1))
    }

    // Create a Story
    suspend fun createStory(mediaUrl: String) = withContext(Dispatchers.IO) {
        val me = userDao.getUserById("me") ?: return@withContext
        val storyId = UUID.randomUUID().toString()
        val newStory = Story(
            id = storyId,
            userId = me.id,
            username = me.username,
            userAvatarUrl = me.avatarUrl,
            mediaUrl = mediaUrl,
            durationSec = 15,
            isArchived = false,
            timestamp = System.currentTimeMillis()
        )
        storyDao.insertStory(newStory)
    }

    // Mark Chat as Read
    suspend fun markChatAsRead(chatId: String) = withContext(Dispatchers.IO) {
        chatDao.markChatAsRead(chatId)
    }

    // Seed Data
    private suspend fun seedInitialDataIfNeeded() {
        // Check if current user exists
        val me = userDao.getUserById("me")
        if (me != null) return // Already seeded

        // 1. Create Current User ("me") - Starts as new account, following specification:
        // "Accurate counter for posts, followers, and following. If zero, display '0' exactly like the reference UI."
        // Let's set it up as a pure 0-0-0 new account!
        val meUser = User(
            id = "me",
            username = "alex_creative",
            fullName = "Alex Mercer",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
            postsCount = 0,
            followersCount = 0,
            followingCount = 0,
            isPrivate = false
        )
        userDao.insertUser(meUser)

        // 2. Create other accounts (potential friends/followings)
        val friends = listOf(
            User(
                id = "sara_cooks",
                username = "sara_cooks",
                fullName = "Sara Croft (Chef)",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                postsCount = 42,
                followersCount = 1850,
                followingCount = 320,
                isPrivate = false,
                isFollowing = false
            ),
            User(
                id = "marcus_clicks",
                username = "marcus_clicks",
                fullName = "Marcus Vance",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                postsCount = 120,
                followersCount = 5900,
                followingCount = 412,
                isPrivate = true, // Private account to show follow request flow!
                isFollowing = false
            ),
            User(
                id = "travel_lily",
                username = "travel_lily",
                fullName = "Lily Chen",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                postsCount = 89,
                followersCount = 12400,
                followingCount = 890,
                isPrivate = false,
                isFollowing = false
            )
        )

        for (friend in friends) {
            userDao.insertUser(friend)
        }

        // 3. Insert beautiful mock posts
        val initialPosts = listOf(
            Post(
                id = "post_1",
                userId = "travel_lily",
                username = "travel_lily",
                userAvatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                mediaUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop&q=80",
                isVideo = false,
                caption = "Waking up to this sunrise in Santorini! Living the dream 🌅✨ #traveldiary #greece #wanderlust",
                likesCount = 1432,
                commentsCount = 2,
                isLiked = false,
                timestamp = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
            ),
            Post(
                id = "post_2",
                userId = "sara_cooks",
                username = "sara_cooks",
                userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                mediaUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=800&auto=format&fit=crop&q=80",
                isVideo = false,
                caption = "Smoked slow-cooked BBQ beef brisket. Fork tender and packing serious hickory flavor! Recipe coming in stories tonight 🥩🔥 #bbq #brisket #foodie",
                likesCount = 892,
                commentsCount = 1,
                isLiked = true,
                timestamp = System.currentTimeMillis() - 3600000 * 5 // 5 hours ago
            ),
            // REELS (Videos)
            Post(
                id = "reel_1",
                userId = "travel_lily",
                username = "travel_lily",
                userAvatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                mediaUrl = "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800&auto=format&fit=crop&q=80", // Beautiful boat travel image
                isVideo = true,
                caption = "Rowing through the emerald waters of Lake Di Braies, Italy. The air is so crisp! 🇮🇹🛶 #dolomites #reels #traveling",
                likesCount = 24500,
                commentsCount = 3,
                isLiked = false,
                timestamp = System.currentTimeMillis() - 3600000 * 8,
                videoDurationMs = 12000L,
                viewsCount = 98200
            ),
            Post(
                id = "reel_2",
                userId = "marcus_clicks",
                username = "marcus_clicks",
                userAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
                mediaUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&auto=format&fit=crop&q=80", // Beautiful Yosemite waterfall
                isVideo = true,
                caption = "Yosemite in full force. Standing next to this giant waterfall makes you feel so small ⛰️💧 #waterfall #naturelovers #reels",
                likesCount = 18900,
                commentsCount = 1,
                isLiked = false,
                timestamp = System.currentTimeMillis() - 3600000 * 12,
                videoDurationMs = 15000L,
                viewsCount = 74100
            ),
            Post(
                id = "reel_3",
                userId = "sara_cooks",
                username = "sara_cooks",
                userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                mediaUrl = "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=800&auto=format&fit=crop&q=80", // Pizza close up
                isVideo = true,
                caption = "The perfect Neapolitan pizza bubble crust rise! 72-hour cold ferment dough is the secret 🍕🔥 #pizza #cookingreels #baking",
                likesCount = 32100,
                commentsCount = 2,
                isLiked = false,
                timestamp = System.currentTimeMillis() - 3600000 * 18,
                videoDurationMs = 9000L,
                viewsCount = 125000
            )
        )

        postDao.insertPosts(initialPosts)

        // 4. Insert initial comments
        commentDao.insertComment(Comment("c1", "post_1", "sara_cooks", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80", "Santorini is high on my bucket list! Absolutely stunning photography!"))
        commentDao.insertComment(Comment("c2", "post_1", "marcus_clicks", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80", "The lighting in this shot is perfect, what camera set up?"))
        commentDao.insertComment(Comment("c3", "post_2", "travel_lily", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80", "Omg please drop the recipe ASAP, my mouth is watering! 🤤"))
        
        commentDao.insertComment(Comment("cr1", "reel_1", "sara_cooks", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80", "Water looks unreal! Unbelievable location."))
        commentDao.insertComment(Comment("cr2", "reel_1", "marcus_clicks", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80", "The mirror reflection on the water is crazy. Perfect composure."))
        commentDao.insertComment(Comment("cr3", "reel_1", "alex_creative", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80", "Adding this to my dream itinerary!"))

        commentDao.insertComment(Comment("cr4", "reel_2", "travel_lily", "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80", "Yosemite is magical, lovely capture!"))
        commentDao.insertComment(Comment("cr5", "reel_3", "alex_creative", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80", "Look at that crust puffing! Amazing!"))
        commentDao.insertComment(Comment("cr6", "reel_3", "marcus_clicks", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80", "Wish I could smell this through the screen!"))

        // 5. Insert gorgeous active stories (Unsplash images with faces/scenes)
        val initialStories = listOf(
            Story(
                id = "story_1",
                userId = "sara_cooks",
                username = "sara_cooks",
                userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                mediaUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=80", // Slicing dough
                durationSec = 15,
                timestamp = System.currentTimeMillis() - 3600000 * 1
            ),
            Story(
                id = "story_2",
                userId = "travel_lily",
                username = "travel_lily",
                userAvatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                mediaUrl = "https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=500&auto=format&fit=crop&q=80", // Travel luggage map
                durationSec = 15,
                timestamp = System.currentTimeMillis() - 3600000 * 3
            )
        )
        for (story in initialStories) {
            storyDao.insertStory(story)
        }

        // 6. Insert initial activity logs (to show the Activity Log UI working beautifully)
        val logs = listOf(
            ActivityLog(
                id = "log_1",
                username = "sara_cooks",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
                actionText = "started following you.",
                timestamp = System.currentTimeMillis() - 3600000 * 2,
                canFollowBack = true,
                isFollowingBack = false
            ),
            ActivityLog(
                id = "log_2",
                username = "travel_lily",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150&auto=format&fit=crop&q=80",
                actionText = "started following you.",
                timestamp = System.currentTimeMillis() - 3600000 * 4,
                canFollowBack = true,
                isFollowingBack = false
            )
        )
        for (log in logs) {
            activityLogDao.insertActivityLog(log)
        }

        // Note: Chats start empty for our new account to strictly satisfy "New accounts show 'No friends yet'. Adding a friend dynamically updates the UI."
        // Once the user "Follows" a friend (e.g. sara_cooks or travel_lily), they are added as a friend, a Chat is automatically created, and the chat list updates!
    }
}
