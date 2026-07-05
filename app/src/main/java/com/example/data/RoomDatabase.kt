package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET followersCount = followersCount + 1 WHERE id = :userId")
    suspend fun incrementFollowers(userId: String)

    @Query("UPDATE users SET followersCount = MAX(0, followersCount - 1) WHERE id = :userId")
    suspend fun decrementFollowers(userId: String)
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE isVideo = 1 ORDER BY timestamp DESC")
    fun getReelsFlow(): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUserIdFlow(userId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): Post?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Query("UPDATE posts SET isLiked = :isLiked, likesCount = likesCount + :diff WHERE id = :postId")
    suspend fun updatePostLike(postId: String, isLiked: Boolean, diff: Int)

    @Query("UPDATE posts SET commentsCount = commentsCount + 1 WHERE id = :postId")
    suspend fun incrementCommentsCount(postId: String)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPostFlow(postId: String): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getActiveStoriesFlow(): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE userId = :userId AND isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedStoriesByUserIdFlow(userId: String): Flow<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Query("UPDATE stories SET isArchived = 1 WHERE id = :storyId")
    suspend fun archiveStory(storyId: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY timestamp DESC")
    fun getAllChatsFlow(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat)

    @Query("UPDATE chats SET lastMessage = :lastMsg, timestamp = :time, unreadCount = unreadCount + :unreadDiff WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMsg: String, time: Long, unreadDiff: Int)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatAsRead(chatId: String)

    @Query("SELECT SUM(unreadCount) FROM chats")
    fun getTotalUnreadCountFlow(): Flow<Int?>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getActivityLogsFlow(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("UPDATE activity_logs SET isFollowingBack = :isFollowingBack WHERE id = :logId")
    suspend fun updateFollowBackStatus(logId: String, isFollowingBack: Boolean)
}

@Database(
    entities = [
        User::class,
        Post::class,
        Comment::class,
        Story::class,
        Chat::class,
        Message::class,
        ActivityLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun storyDao(): StoryDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun activityLogDao(): ActivityLogDao
}
