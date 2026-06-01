package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val handle: String,
    val avatarUrl: String,
    val bio: String,
    val isAi: Boolean,
    val followersCount: Int,
    val followingCount: Int,
    val trustScore: Int,
    val isVerified: Boolean = false,
    val verificationExpiry: Long? = null
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: String,
    val content: String,
    val mediaUrl: String? = null,
    val mediaType: String? = null, // "IMAGE", "VIDEO" or null
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val trustScore: Int = 85,
    val sourceName: String = "nOG News Feed",
    val isTrend: Boolean = false,
    val isArchived: Boolean = false,
    val category: String? = null
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val authorId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val replyToCommentId: Int? = null,
    val replyToAuthorName: String? = null
)

@Entity(tableName = "followers")
data class FollowerEntity(
    @PrimaryKey val id: String, // format: "userId_targetId"
    val userId: String,
    val targetId: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String, // "LIKE", "COMMENT", "TREND", "SYSTEM"
    val postId: Int? = null
)

@Entity(tableName = "analytics")
data class AnalyticsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val metricType: String, // "POST_CLICK", "LIKE_CLICK", "COMMENT_POST", "NOG_QUERY", "NOTIFICATION_OPEN", "FEED_SCROLL"
    val count: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SocialDao {
    // Users
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Posts
    @Query("SELECT * FROM posts ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentPosts(limit: Int): List<PostEntity>

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: Int): PostEntity?

    @Query("SELECT * FROM posts WHERE isTrend = 1 ORDER BY timestamp DESC")
    fun getTrendingPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedPostsFlow(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("SELECT * FROM posts WHERE LOWER(content) = LOWER(:content) LIMIT 1")
    suspend fun getPostByContent(content: String): PostEntity?

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: Int)

    @Query("DELETE FROM posts WHERE id NOT IN (SELECT id FROM posts ORDER BY timestamp DESC LIMIT 200)")
    suspend fun pruneOldPosts()

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    // Comments
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPostFlow(postId: Int): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    // Followers
    @Query("SELECT * FROM followers WHERE userId = :userId")
    fun getFollowingForUserFlow(userId: String): Flow<List<FollowerEntity>>

    @Query("SELECT * FROM followers WHERE targetId = :targetId")
    fun getFollowersForUserFlow(targetId: String): Flow<List<FollowerEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM followers WHERE userId = :userId AND targetId = :targetId)")
    suspend fun isFollowing(userId: String, targetId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follower: FollowerEntity)

    @Query("DELETE FROM followers WHERE userId = :userId AND targetId = :targetId")
    suspend fun deleteFollow(userId: String, targetId: String)

    @Query("SELECT * FROM followers WHERE userId = :userId")
    suspend fun getFollowingForUser(userId: String): List<FollowerEntity>

    @Query("SELECT * FROM followers WHERE targetId = :targetId")
    suspend fun getFollowersForUser(targetId: String): List<FollowerEntity>

    @Query("DELETE FROM followers WHERE userId = :userId")
    suspend fun deleteAllFollowingForUser(userId: String)

    @Query("DELETE FROM followers WHERE targetId = :targetId")
    suspend fun deleteAllFollowersForUser(targetId: String)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    // Analytics
    @Query("SELECT * FROM analytics")
    fun getAllAnalyticsFlow(): Flow<List<AnalyticsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: AnalyticsEntity)
}

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        CommentEntity::class,
        FollowerEntity::class,
        NotificationEntity::class,
        AnalyticsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun socialDao(): SocialDao
}
