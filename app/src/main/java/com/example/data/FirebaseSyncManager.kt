package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"

    private val _syncStatus = MutableStateFlow<String>("Ready")
    val syncStatus = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow<Boolean>(false)
    val isSyncing = _isSyncing.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val projectId = try { com.example.BuildConfig.FIREBASE_PROJECT_ID } catch (e: Throwable) { "" }
            val apiKey = try { com.example.BuildConfig.FIREBASE_API_KEY } catch (e: Throwable) { "" }
            val appId = try { com.example.BuildConfig.FIREBASE_APP_ID } catch (e: Throwable) { "" }

            if (FirebaseApp.getApps(context).isEmpty()) {
                val resolvedProjectId = if (projectId.isNotEmpty() && projectId != "clinical-companion-demo") projectId else "clinical-companion-demo"
                val resolvedApiKey = if (apiKey.isNotEmpty() && apiKey != "AIzaSyD-clinicalcompaniondemo12345") apiKey else "AIzaSyD-clinicalcompaniondemo12345"
                val resolvedAppId = if (appId.isNotEmpty() && appId != "1:1234567890:android:abcdef123456") appId else "1:100000000000:android:e6fde089201f"

                val options = FirebaseOptions.Builder()
                    .setProjectId(resolvedProjectId)
                    .setApiKey(resolvedApiKey)
                    .setApplicationId(resolvedAppId)
                    .build()

                FirebaseApp.initializeApp(context.applicationContext, options)
                Log.d(TAG, "Programmatic Firebase initialized successfully.")
            }

            // Configure offline-persistence settings for Firestore
            val firestore = FirebaseFirestore.getInstance()
            val cacheSettings = PersistentCacheSettings.newBuilder().build()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            firestore.firestoreSettings = settings

            isInitialized = true
            _syncStatus.value = "Connected (Offline-Ready)"
            Log.d(TAG, "Firestore configured for offline-first replication.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed programmatic Firebase initialization: ${e.localizedMessage}")
            _syncStatus.value = "Local Mode Active (No Cloud Config)"
        }
    }

    private fun getFirestoreInstance(): FirebaseFirestore? {
        if (!isInitialized) return null
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun savePatientToCloud(patient: PatientEntity) = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext
        try {
            _syncStatus.value = "Saving Patient: ${patient.name}"
            val docId = "patient_${patient.id}"
            firestore.collection("patients")
                .document(docId)
                .set(patient.toMap())
                .await()
            _syncStatus.value = "Synced: ${patient.name}"
        } catch (e: Exception) {
            Log.e(TAG, "Error saving patient to cloud: ${e.localizedMessage}")
            _syncStatus.value = "Saved Locally (Sync Pending Conn)"
        }
    }

    suspend fun saveSessionToCloud(session: SessionLogEntity) = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext
        try {
            _syncStatus.value = "Saving Session: #${session.sessionNumber}"
            val docId = "session_${session.id}"
            firestore.collection("session_logs")
                .document(docId)
                .set(session.toMap())
                .await()
            _syncStatus.value = "Synced Session #${session.sessionNumber}"
        } catch (e: Exception) {
            Log.e(TAG, "Error saving session to cloud: ${e.localizedMessage}")
            _syncStatus.value = "Saved Locally (Sync Pending Conn)"
        }
    }

    suspend fun deletePatientFromCloud(patientId: Int) = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext
        try {
            _syncStatus.value = "Deleting Patient..."
            val docId = "patient_$patientId"
            firestore.collection("patients").document(docId).delete().await()

            // Also delete associated sessions from cloud
            val querySnapshot = firestore.collection("session_logs")
                .whereEqualTo("patientId", patientId)
                .get()
                .await()
            for (doc in querySnapshot.documents) {
                doc.reference.delete()
            }
            _syncStatus.value = "Patient Deleted (Synced)"
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting patient from cloud: ${e.localizedMessage}")
            _syncStatus.value = "Deleted Locally"
        }
    }

    suspend fun deleteSessionFromCloud(sessionId: Int) = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance() ?: return@withContext
        try {
            _syncStatus.value = "Deleting Session..."
            val docId = "session_$sessionId"
            firestore.collection("session_logs").document(docId).delete().await()
            _syncStatus.value = "Session Deleted"
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting session: ${e.localizedMessage}")
        }
    }

    suspend fun executeFullBilateralSync(db: ClientFlowDatabase) = withContext(Dispatchers.IO) {
        val firestore = getFirestoreInstance()
        if (firestore == null) {
            _syncStatus.value = "Need Cloud Config to Sync"
            return@withContext
        }

        if (_isSyncing.value) return@withContext
        _isSyncing.value = true
        _syncStatus.value = "Initiating Cloud Sync..."

        try {
            val patientDao = db.patientDao()
            val sessionDao = db.sessionDao()

            // 1. Upload Local to Cloud
            _syncStatus.value = "Pushing local database to Firestore..."
            val localPatients = patientDao.getAllPatients().firstList()
            for (patient in localPatients) {
                firestore.collection("patients").document("patient_${patient.id}").set(patient.toMap()).await()
            }

            val localSessions = sessionDao.getAllSessions().firstList()
            for (session in localSessions) {
                firestore.collection("session_logs").document("session_${session.id}").set(session.toMap()).await()
            }

            // 2. Fetch from Cloud and Merge (Download newer)
            _syncStatus.value = "Downloading cloud updates..."
            val cloudPatientsSnapshot = firestore.collection("patients").get().await()
            for (doc in cloudPatientsSnapshot.documents) {
                val data = doc.data
                if (data != null) {
                    val p = data.toPatientEntity()
                    patientDao.insertPatient(p)
                }
            }

            val cloudSessionsSnapshot = firestore.collection("session_logs").get().await()
            for (doc in cloudSessionsSnapshot.documents) {
                val data = doc.data
                if (data != null) {
                    val s = data.toSessionLogEntity()
                    sessionDao.insertSession(s)
                }
            }

            _syncStatus.value = "Cloud Sync Complete!"
        } catch (e: Exception) {
            Log.e(TAG, "Full bi-lateral sync error: ${e.localizedMessage}")
            _syncStatus.value = "Sync Interrupted (Offline Supported)"
        } finally {
            _isSyncing.value = false
        }
    }

    // Helper Flow extension to get first emission list
    private suspend fun <T> kotlinx.coroutines.flow.Flow<List<T>>.firstList(): List<T> {
        return this.first()
    }

    // Entity Serializers
    private fun PatientEntity.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "clinicalId" to clinicalId,
        "name" to name,
        "diagnosis" to diagnosis,
        "email" to email,
        "phone" to phone,
        "notes" to notes,
        "homeworkRatio" to homeworkRatio,
        "createdAt" to createdAt
    )

    private fun SessionLogEntity.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "patientId" to patientId,
        "sessionNumber" to sessionNumber,
        "date" to date,
        "durationMinutes" to durationMinutes,
        "sleepQuality" to sleepQuality,
        "moodWeight" to moodWeight,
        "moodState" to moodState,
        "moodNotes" to moodNotes,
        "practitionerLog" to practitionerLog,
        "activeHomeworkStatus" to activeHomeworkStatus,
        "energyLevel" to energyLevel,
        "focusTags" to focusTags
    )

    private fun Map<String, Any>.toPatientEntity(): PatientEntity = PatientEntity(
        id = (this["id"] as? Long)?.toInt() ?: 0,
        clinicalId = this["clinicalId"] as? String ?: "",
        name = this["name"] as? String ?: "",
        diagnosis = this["diagnosis"] as? String ?: "",
        email = this["email"] as? String ?: "",
        phone = this["phone"] as? String ?: "",
        notes = this["notes"] as? String ?: "",
        homeworkRatio = (this["homeworkRatio"] as? Double)?.toFloat() ?: (this["homeworkRatio"] as? Float) ?: 0.0f,
        createdAt = (this["createdAt"] as? Long) ?: System.currentTimeMillis()
    )

    private fun Map<String, Any>.toSessionLogEntity(): SessionLogEntity = SessionLogEntity(
        id = (this["id"] as? Long)?.toInt() ?: 0,
        patientId = (this["patientId"] as? Long)?.toInt() ?: 0,
        sessionNumber = (this["sessionNumber"] as? Long)?.toInt() ?: 1,
        date = this["date"] as? String ?: "",
        durationMinutes = (this["durationMinutes"] as? Long)?.toInt() ?: 45,
        sleepQuality = (this["sleepQuality"] as? Long)?.toInt() ?: 5,
        moodWeight = (this["moodWeight"] as? Double)?.toFloat() ?: (this["moodWeight"] as? Float) ?: 3.0f,
        moodState = this["moodState"] as? String ?: "Neutral",
        moodNotes = this["moodNotes"] as? String ?: "",
        practitionerLog = this["practitionerLog"] as? String ?: "",
        activeHomeworkStatus = this["activeHomeworkStatus"] as? Boolean ?: false,
        energyLevel = (this["energyLevel"] as? Long)?.toInt() ?: 5,
        focusTags = this["focusTags"] as? String ?: ""
    )
}
