package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isPrivate: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowRequested: Boolean = false
)

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val userAvatarUrl: String,
    val mediaUrl: String,
    val isVideo: Boolean = false,
    val caption: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val videoDurationMs: Long = 15000L,
    val viewsCount: Int = 1240
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String,
    val postId: String,
    val username: String,
    val userAvatarUrl: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val userAvatarUrl: String,
    val mediaUrl: String,
    val durationSec: Int = 15,
    val isArchived: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey val id: String,
    val participantId: String,
    val participantName: String,
    val participantAvatar: String,
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val text: String = "",
    val mediaUrl: String? = null,
    val isVoiceNote: Boolean = false,
    val voiceNoteWaveform: String? = null, // JSON string of amplitude floats
    val voiceNoteDurationSec: Int = 0,
    val attachedPostLink: String? = null, // Post ID if attached
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey val id: String,
    val username: String,
    val avatarUrl: String,
    val actionText: String, // "started following you"
    val timestamp: Long = System.currentTimeMillis(),
    val canFollowBack: Boolean = false,
    val isFollowingBack: Boolean = false
)
