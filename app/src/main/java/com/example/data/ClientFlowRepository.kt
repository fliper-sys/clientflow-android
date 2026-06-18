package com.example.data

import kotlinx.coroutines.flow.Flow

class ClientFlowRepository(private val db: ClientFlowDatabase) {
    private val patientDao = db.patientDao()
    private val sessionDao = db.sessionDao()
    private val configDao = db.privacyConfigDao()

    val allPatients: Flow<List<PatientEntity>> = patientDao.getAllPatients()
    val allSessions: Flow<List<SessionLogEntity>> = sessionDao.getAllSessions()
    val privacyConfig: Flow<PrivacyConfigEntity?> = configDao.getConfig()

    fun getSessionsForPatient(patientId: Int): Flow<List<SessionLogEntity>> {
        return sessionDao.getSessionsForPatient(patientId)
    }

    suspend fun insertPatient(patient: PatientEntity): Long {
        return patientDao.insertPatient(patient)
    }

    suspend fun updatePatient(patient: PatientEntity) {
        patientDao.updatePatient(patient)
    }

    suspend fun deletePatient(id: Int) {
        patientDao.deletePatient(id)
        sessionDao.deleteSessionsForPatient(id)
    }

    suspend fun clearAllPatients() {
        patientDao.clearAllPatients()
        sessionDao.clearAllSessions()
    }

    suspend fun insertSession(session: SessionLogEntity): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun deleteSession(id: Int) {
        sessionDao.deleteSession(id)
    }

    suspend fun updateSession(session: SessionLogEntity) {
        sessionDao.updateSession(session)
    }

    suspend fun savePrivacyConfig(config: PrivacyConfigEntity) {
        configDao.saveConfig(config)
    }
}
