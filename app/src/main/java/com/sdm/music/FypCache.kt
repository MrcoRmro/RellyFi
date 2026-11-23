package com.sdm.music

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ---- ENTIDAD ----
@Entity(tableName = "fyp_cache")
data class FypCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val artistKey: String,       // Artista limpio (clave de búsqueda)
    val videoId: String,
    val title: String,
    val thumbnail: String,
    val cachedAt: Long = System.currentTimeMillis()
)

// ---- DAO ----
@Dao
interface FypCacheDao {

    @Query("SELECT * FROM fyp_cache WHERE artistKey = :artistKey ORDER BY cachedAt DESC LIMIT 20")
    suspend fun getForArtist(artistKey: String): List<FypCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FypCacheEntity>)

    @Query("DELETE FROM fyp_cache WHERE artistKey = :artistKey")
    suspend fun deleteForArtist(artistKey: String)

    // Caché válido si tiene datos de menos de 24 horas
    @Query("SELECT COUNT(*) FROM fyp_cache WHERE artistKey = :artistKey AND cachedAt > :since")
    suspend fun countFreshEntries(artistKey: String, since: Long): Int
}

// ---- ENTIDAD DE DESCARGAS DE PLAYLIST ----
@Entity(tableName = "download_queue")
data class DownloadItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistName: String,
    val playlistUrl: String,
    val videoId: String,
    val videoUrl: String,
    val title: String,
    val thumbnail: String,
    val status: String = "PENDIENTE", // PENDIENTE, DESCARGANDO, COMPLETADA, FALLIDA
    val addedAt: Long = System.currentTimeMillis()
)

// ---- DAO DE DESCARGAS ----
@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_queue WHERE status = 'PENDIENTE' ORDER BY id ASC LIMIT 1")
    suspend fun getNextPending(): DownloadItemEntity?

    @Query("SELECT * FROM download_queue WHERE status = 'DESCARGANDO' LIMIT 1")
    suspend fun getCurrentlyDownloading(): DownloadItemEntity?

    @Query("SELECT * FROM download_queue ORDER BY id ASC")
    fun getAllFlow(): Flow<List<DownloadItemEntity>>

    @Query("SELECT COUNT(*) FROM download_queue WHERE status IN ('PENDIENTE', 'DESCARGANDO')")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'COMPLETADA'")
    fun getCompletedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM download_queue")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DownloadItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadItemEntity)

    @Update
    suspend fun update(item: DownloadItemEntity)

    @Query("UPDATE download_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("UPDATE download_queue SET status = 'PENDIENTE' WHERE status = 'DESCARGANDO'")
    suspend fun resetDownloadingToPending()

    @Query("DELETE FROM download_queue WHERE status = 'COMPLETADA'")
    suspend fun deleteCompleted()

    @Query("DELETE FROM download_queue")
    suspend fun clearAll()
}

// ---- DATABASE ----
@Database(entities = [FypCacheEntity::class, DownloadItemEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fypCacheDao(): FypCacheDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rellyfi_db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
