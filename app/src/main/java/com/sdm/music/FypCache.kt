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

// ---- DATABASE ----
@Database(entities = [FypCacheEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fypCacheDao(): FypCacheDao

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
