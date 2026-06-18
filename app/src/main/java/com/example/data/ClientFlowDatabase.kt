package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clinicalId: String,
    val name: String,
    val diagnosis: String,
    val email: String,
    val phone: String,
    val notes: String = "",
    val homeworkRatio: Float = 0.0f,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "session_logs")
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val sessionNumber: Int,
    val date: String,             // e.g. "2026-06-18"
    val durationMinutes: Int,
    val sleepQuality: Int,        // 1 to 10
    val moodWeight: Float,        // 1.0 to 6.0
    val moodState: String,        // "Productive", "Calm", "Reflective", "Neutral", "Anxious", "Overwhelmed"
    val moodNotes: String = "",
    val practitionerLog: String = "",
    val activeHomeworkStatus: Boolean = false,
    val energyLevel: Int = 5,     // 1 to 10
    val focusTags: String = ""    // e.g., "CBT, ACT"
)

@Entity(tableName = "privacy_config")
data class PrivacyConfigEntity(
    @PrimaryKey val id: Int = 1,
    val pin: String = "1234",
    val pinEnabled: Boolean = true,
    val isLocked: Boolean = true,
    val maskNames: Boolean = false,
    val obfuscateContacts: Boolean = false,
    val blurNotes: Boolean = false
)

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Query("DELETE FROM patients WHERE id = :id")
    suspend fun deletePatient(id: Int)

    @Query("DELETE FROM patients")
    suspend fun clearAllPatients()
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM session_logs ORDER BY date DESC, id DESC")
    fun getAllSessions(): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs WHERE patientId = :patientId ORDER BY sessionNumber ASC")
    fun getSessionsForPatient(patientId: Int): Flow<List<SessionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionLogEntity): Long

    @Update
    suspend fun updateSession(session: SessionLogEntity)

    @Query("DELETE FROM session_logs WHERE id = :id")
    suspend fun deleteSession(id: Int)

    @Query("DELETE FROM session_logs WHERE patientId = :patientId")
    suspend fun deleteSessionsForPatient(patientId: Int)

    @Query("DELETE FROM session_logs")
    suspend fun clearAllSessions()
}

@Dao
interface PrivacyConfigDao {
    @Query("SELECT * FROM privacy_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<PrivacyConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: PrivacyConfigEntity)
}

@Database(entities = [PatientEntity::class, SessionLogEntity::class, PrivacyConfigEntity::class], version = 1, exportSchema = false)
abstract class ClientFlowDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun sessionDao(): SessionDao
    abstract fun privacyConfigDao(): PrivacyConfigDao
}
