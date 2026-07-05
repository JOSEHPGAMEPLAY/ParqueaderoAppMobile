package com.example

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_results")
data class ScanResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plateText: String,
    val country: String? = null,
    val state: String? = null,
    val vehicleBrand: String? = null,
    val vehicleColor: String? = null,
    val confidence: Int? = null,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePath: String? = null,
    val exitTime: Long? = null,
    val hasTicket: Boolean = true,
    val paidAmount: Double? = null,
    val status: String = "ACTIVE", // "ACTIVE" = inside lot, "COMPLETED" = exited
    val isSynced: Boolean = false,
    val remoteId: String? = null
)

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResult): Long

    @Update
    suspend fun updateScan(scan: ScanResult)

    @Delete
    suspend fun deleteScan(scan: ScanResult)

    @Query("DELETE FROM scan_results WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("UPDATE scan_results SET plateText = :newPlate, isSynced = 0 WHERE id = :id")
    suspend fun updatePlateTextById(id: Long, newPlate: String)

    @Query("DELETE FROM scan_results")
    suspend fun deleteAllScans()
}

@Database(entities = [ScanResult::class], version = 4, exportSchema = false)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao

    companion object {
        @Volatile
        private var INSTANCE: ScanDatabase? = null

        fun getDatabase(context: Context): ScanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanDatabase::class.java,
                    "scan_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ScanResultRepository(private val dao: ScanResultDao) {
    val allScans: Flow<List<ScanResult>> = dao.getAllScans()

    suspend fun insert(scan: ScanResult): Long = dao.insertScan(scan)

    suspend fun update(scan: ScanResult) = dao.updateScan(scan)

    suspend fun delete(scan: ScanResult) = dao.deleteScan(scan)

    suspend fun deleteById(id: Long) = dao.deleteScanById(id)

    suspend fun updatePlateTextById(id: Long, newPlate: String) = dao.updatePlateTextById(id, newPlate)

    suspend fun deleteAll() = dao.deleteAllScans()
}
