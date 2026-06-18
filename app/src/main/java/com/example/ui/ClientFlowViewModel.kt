package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClientFlowViewModel(context: Context) : ViewModel() {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        ClientFlowDatabase::class.java,
        "clientflow_database"
    ).fallbackToDestructiveMigration().build()

    private val repository = ClientFlowRepository(db)

    // Central Data Flows
    val patients: StateFlow<List<PatientEntity>> = repository.allPatients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<SessionLogEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privacyConfig: StateFlow<PrivacyConfigEntity> = repository.privacyConfig
        .map { it ?: PrivacyConfigEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrivacyConfigEntity())

    // UI Interactive States
    private val _selectedPatientId = MutableStateFlow<Int?>(null)
    val selectedPatientId: StateFlow<Int?> = _selectedPatientId.asStateFlow()

    private val _dateFilter = MutableStateFlow("All") // "Week", "Month", "Year", "All"
    val dateFilter: StateFlow<String> = _dateFilter.asStateFlow()

    private val _isAppLocked = MutableStateFlow(true)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isPanicActive = MutableStateFlow(false)
    val isPanicActive: StateFlow<Boolean> = _isPanicActive.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    private val _hoveredNoteId = MutableStateFlow<Int?>(null)
    val hoveredNoteId: StateFlow<Int?> = _hoveredNoteId.asStateFlow()

    // Quick Mood Log Strip States
    private val _quickLogOpen = MutableStateFlow(false)
    val quickLogOpen: StateFlow<Boolean> = _quickLogOpen.asStateFlow()

    private val _selectedQuickMoodState = MutableStateFlow<String?>(null)
    val selectedQuickMoodState: StateFlow<String?> = _selectedQuickMoodState.asStateFlow()

    private val _selectedQuickMoodWeight = MutableStateFlow(3.0f)
    val selectedQuickMoodWeight: StateFlow<Float> = _selectedQuickMoodWeight.asStateFlow()

    // Delta Tool Session selection
    private val _deltaSessionOneId = MutableStateFlow<Int?>(null)
    val deltaSessionOneId: StateFlow<Int?> = _deltaSessionOneId.asStateFlow()

    private val _deltaSessionTwoId = MutableStateFlow<Int?>(null)
    val deltaSessionTwoId: StateFlow<Int?> = _deltaSessionTwoId.asStateFlow()

    init {
        // Observe privacyConfig lock status on launch
        viewModelScope.launch {
            privacyConfig.collect { config ->
                // Initial check for locking
                if (!config.pinEnabled) {
                    _isAppLocked.value = false
                }
            }
        }
    }

    fun selectPatient(patientId: Int?) {
        _selectedPatientId.value = patientId
        // Automatically reset session select in Delta tool
        _deltaSessionOneId.value = null
        _deltaSessionTwoId.value = null
    }

    fun setDateFilter(filter: String) {
        _dateFilter.value = filter
    }

    fun setHoveredNote(noteId: Int?) {
        _hoveredNoteId.value = noteId
    }

    fun selectDate(date: String?) {
        _selectedDate.value = date
    }

    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }

    fun attemptUnlock(enteredPin: String, config: PrivacyConfigEntity): Boolean {
        return if (enteredPin == config.pin) {
            _isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun triggerPanic(active: Boolean) {
        _isPanicActive.value = active
    }

    fun openQuickMoodLog(stateName: String, weight: Float) {
        _selectedQuickMoodState.value = stateName
        _selectedQuickMoodWeight.value = weight
        _quickLogOpen.value = true
    }

    fun closeQuickMoodLog() {
        _quickLogOpen.value = false
        _selectedQuickMoodState.value = null
    }

    fun selectDeltaSessionOne(id: Int?) {
        _deltaSessionOneId.value = id
    }

    fun selectDeltaSessionTwo(id: Int?) {
        _deltaSessionTwoId.value = id
    }

    fun updatePrivacyConfig(
        maskNames: Boolean? = null,
        obfuscateContacts: Boolean? = null,
        blurNotes: Boolean? = null,
        pin: String? = null,
        pinEnabled: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = privacyConfig.value
            val next = current.copy(
                maskNames = maskNames ?: current.maskNames,
                obfuscateContacts = obfuscateContacts ?: current.obfuscateContacts,
                blurNotes = blurNotes ?: current.blurNotes,
                pin = pin ?: current.pin,
                pinEnabled = pinEnabled ?: current.pinEnabled
            )
            repository.savePrivacyConfig(next)
        }
    }

    fun saveQuickMoodLogWithNote(note: String) {
        val patientId = _selectedPatientId.value
        val stateName = _selectedQuickMoodState.value
        val weight = _selectedQuickMoodWeight.value

        if (patientId != null && stateName != null) {
            viewModelScope.launch {
                // Find next session number for this patient
                val patientSessions = db.sessionDao().getSessionsForPatient(patientId).first()
                val nextSessionNum = patientSessions.size + 1
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val currentDateStr = dateFormat.format(Date())

                val newSession = SessionLogEntity(
                    patientId = patientId,
                    sessionNumber = nextSessionNum,
                    date = currentDateStr,
                    durationMinutes = 45, // default session duration
                    sleepQuality = 5,    // neutral sleep default
                    moodWeight = weight,
                    moodState = stateName,
                    moodNotes = note,
                    practitionerLog = "Logged via custom granular daily mood logging strip. Focus: $note",
                    activeHomeworkStatus = false,
                    energyLevel = 5,
                    focusTags = "Mood Log"
                )
                repository.insertSession(newSession)
                updatePatientHomeworkRatio(patientId)
                closeQuickMoodLog()
            }
        }
    }

    fun addCustomSession(
        patientId: Int,
        duration: Int,
        sleep: Int,
        moodState: String,
        moodWeight: Float,
        practitionerLog: String,
        activeHomeworkStatus: Boolean,
        energyLevel: Int,
        focusTags: String,
        customDate: String? = null
    ) {
        viewModelScope.launch {
            val patientSessions = db.sessionDao().getSessionsForPatient(patientId).first()
            val nextSessionNum = patientSessions.size + 1
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = customDate ?: dateFormat.format(Date())

            val newSession = SessionLogEntity(
                patientId = patientId,
                sessionNumber = nextSessionNum,
                date = dateStr,
                durationMinutes = duration,
                sleepQuality = sleep,
                moodWeight = moodWeight,
                moodState = moodState,
                practitionerLog = practitionerLog,
                activeHomeworkStatus = activeHomeworkStatus,
                energyLevel = energyLevel,
                focusTags = focusTags
            )
            repository.insertSession(newSession)
            updatePatientHomeworkRatio(patientId)
        }
    }

    fun addCustomPatient(
        name: String,
        diagnosis: String,
        email: String,
        phone: String,
        notes: String
    ) {
        viewModelScope.launch {
            val count = patients.value.size
            val clinicCode = "F-CODE-${count + 11}"
            val newPatient = PatientEntity(
                clinicalId = clinicCode,
                name = name,
                diagnosis = diagnosis,
                email = email,
                phone = phone,
                notes = notes,
                homeworkRatio = 0.0f
            )
            val newId = repository.insertPatient(newPatient)
            _selectedPatientId.value = newId.toInt()
        }
    }

    private suspend fun updatePatientHomeworkRatio(patientId: Int) {
        val patientSessions = db.sessionDao().getSessionsForPatient(patientId).first()
        if (patientSessions.isNotEmpty()) {
            val completed = patientSessions.count { it.activeHomeworkStatus }
            val ratio = completed.toFloat() / patientSessions.size.toFloat()
            // Find current patient detail
            val currentPatients = repository.allPatients.first()
            val patient = currentPatients.find { it.id == patientId }
            if (patient != null) {
                repository.updatePatient(patient.copy(homeworkRatio = ratio))
            }
        }
    }

    fun wipeDatabase() {
        viewModelScope.launch {
            repository.clearAllPatients()
            _selectedPatientId.value = null
            _deltaSessionOneId.value = null
            _deltaSessionTwoId.value = null
            _selectedDate.value = null
        }
    }

    fun populateDemoData() {
        viewModelScope.launch {
            repository.clearAllPatients()

            // Patient 1: Diagnostic Phase - Assessment
            val patient1Id = repository.insertPatient(PatientEntity(
                clinicalId = "F-CODE-10",
                name = "Jonathan Ryder",
                diagnosis = "F43.21 Post-Traumatic Stress Disorder",
                email = "jryder.clinical@secure.net",
                phone = "+1-555-0101",
                notes = "Exhibits hypervigilance post industrial accident. Intermittent sleep disturbances with recurring nightmare cycle. Standard CBT exposure logs started.",
                homeworkRatio = 0.5f
            )).toInt()

            // Patient 2: Diagnostic Phase - Active Intervention
            val patient2Id = repository.insertPatient(PatientEntity(
                clinicalId = "F-CODE-25",
                name = "Sarah Jenkins",
                diagnosis = "F41.1 Generalized Anxiety Disorder",
                email = "sjenkins.patient@secure.net",
                phone = "+1-555-4422",
                notes = "High-achieving finance executive. Worry cycles center heavily on somatic tension and perfectionism. Practicing diaphragmatic breathing and cognitive restructuring homework sheets.",
                homeworkRatio = 0.8f
            )).toInt()

            // Patient 3: Diagnostic Phase - Maintenance
            val patient3Id = repository.insertPatient(PatientEntity(
                clinicalId = "F-CODE-99",
                name = "David Alastair",
                diagnosis = "F32.9 Major Depressive Disorder (In Remission)",
                email = "dalastair.secure@secure.net",
                phone = "+1-555-9000",
                notes = "Longterm patient transitioning to self-management. Maintains exceptional therapy workbook homework logs. Occasional mild dips during seasonal pressure indices.",
                homeworkRatio = 1.0f
            )).toInt()

            // Add Sessions for Patient 1: Assessment (2 sessions)
            repository.insertSession(SessionLogEntity(
                patientId = patient1Id,
                sessionNumber = 1,
                date = getPastDateString(10),
                durationMinutes = 60,
                sleepQuality = 3,
                moodWeight = 2.0f,
                moodState = "Anxious",
                moodNotes = "Acute flashback indices after evening triggers.",
                practitionerLog = "Initial intake completed. Alliance established. Patient reports significant avoidance behavior and persistent dreams. Diagnosed trauma symptoms mapped.",
                activeHomeworkStatus = false,
                energyLevel = 3,
                focusTags = "CBT, Trauma"
            ))
            repository.insertSession(SessionLogEntity(
                patientId = patient1Id,
                sessionNumber = 2,
                date = getPastDateString(3),
                durationMinutes = 50,
                sleepQuality = 4,
                moodWeight = 1.0f,
                moodState = "Overwhelmed",
                moodNotes = "Job restructuring triggered situational stress.",
                practitionerLog = "Session 2: Psychoeducation on autonomic responses provided. Patient introduced to standard stress container workbook. Complied with initial grounding task.",
                activeHomeworkStatus = true,
                energyLevel = 2,
                focusTags = "Grounding, ACT"
            ))

            // Add Sessions for Patient 2: Active Interventions (6 sessions)
            repository.insertSession(SessionLogEntity(
                patientId = patient2Id,
                sessionNumber = 1,
                date = getPastDateString(30),
                durationMinutes = 50,
                sleepQuality = 4,
                moodWeight = 2.0f,
                moodState = "Anxious",
                moodNotes = "Quarterly financial reporting anxiety peak.",
                practitionerLog = "Intake completed. Primary stress triggers identified in corporate setting. Restlessness and high muscle tension reported.",
                activeHomeworkStatus = false,
                energyLevel = 4,
                focusTags = "CBT, Anxiety"
            ))
            repository.insertSession(SessionLogEntity(
                patientId = patient2Id,
                sessionNumber = 2,
                date = getPastDateString(25),
                durationMinutes = 50,
                sleepQuality = 5,
                moodWeight = 3.0f,
                moodState = "Neutral",
                moodNotes = "Quiet weekend relaxed baseline slightly.",
                practitionerLog = "Session 2: Downward arrow technique employed to isolate core competence core beliefs. Structured decatastrophizing exercise mapped.",
                activeHomeworkStatus = true,
                energyLevel = 5,
                focusTags = "CBT, Cognitive"
            ))
            repository.insertSession(SessionLogEntity(
                patientId = patient2Id,
                sessionNumber = 3,
                date = getPastDateString(18),
                durationMinutes = 50,
                sleepQuality = 6,
                moodWeight = 4.0f,
                moodState = "Reflective",
                moodNotes = "Journaling daily yields clearer clinical patterns.",
                practitionerLog = "Session 3: Schema therapy concepts reviewed. Explored high standards protector modes. Homework: journaling daily stressors.",
                activeHomeworkStatus = true,
                energyLevel = 6,
                focusTags = "Schema, Mindfulness"
            ))
            repository.insertSession(SessionLogEntity(
                patientId = patient2Id,
                sessionNumber = 4,
                date = getPastDateString(12),
                durationMinutes = 55,
                sleepQuality = 5,
                moodWeight = 2.0f,
                moodState = "Anxious",
                moodNotes = "Somatic chest tight sensations elevated.",
                practitionerLog = "Session 4: Physical somatic check-ins. Dissected hyperventilation panic spirals. Reviewed deep breathing mechanics. Complied with workbook instructions.",
                activeHomeworkStatus = true,
                energyLevel = 4,
                focusTags = "Somatic, ACT"
            ))
            repository.insertSession(SessionLogEntity(
                patientId = patient2Id,
                sessionNumber = 5,
                date = getPastDateString(6),
                durationMinutes = 45,
                sleepQuality = 7,
                moodWeight = 5.0f,
                moodState = "Calm",
                moodNotes = "Excellent response to progressive relaxation guidelines.",
                practitionerLog = "Session 5: Progressive Muscle Relaxation (PMR) guide. Patient achieved notable muscle release. Reported lower anticipatory anxiety levels.",
                activeHomeworkStatus = true,
                energyLevel = 7,
                focusTags = "Relaxation, PMR"
            ))
            repository.insertSession(SessionLogEntity(
                patientId = patient2Id,
                sessionNumber = 6,
                date = getPastDateString(1),
                durationMinutes = 50,
                sleepQuality = 8,
                moodWeight = 6.0f,
                moodState = "Productive",
                moodNotes = "Achieved work goals while sustaining inner boundaries.",
                practitionerLog = "Session 6: Boundary assertive training. Analyzed corporate email triggers. Patient displays highly functional CBT skills integration.",
                activeHomeworkStatus = true,
                energyLevel = 8,
                focusTags = "Assertiveness, CBT"
            ))

            // Add Sessions for Patient 3: Maintenance & Recovery (10 sessions)
            // We'll add 10 sessions spanning 70 days. This maps the progression indices beautifully!
            val states = listOf("Neutral", "Reflective", "Neutral", "Anxious", "Calm", "Reflective", "Calm", "Productive", "Calm", "Productive")
            val weights = listOf(3.0f, 4.0f, 3.0f, 2.0f, 5.0f, 4.0f, 5.0f, 6.0f, 5.0f, 6.0f)
            val sleepScores = listOf(5, 5, 6, 4, 7, 7, 8, 8, 9, 9)
            val focusList = listOf("CBT, Sleep", "Mindfulness", "Acceptance", "Triggers", "ACT, Values", "Behavioral Act", "Self Compassion", "Resilience", "Discharge prep", "Self-Management")

            for (i in 1..10) {
                repository.insertSession(SessionLogEntity(
                    patientId = patient3Id,
                    sessionNumber = i,
                    date = getPastDateString(70 - (i * 7)),
                    durationMinutes = 50,
                    sleepQuality = sleepScores[i - 1],
                    moodWeight = weights[i - 1],
                    moodState = states[i - 1],
                    moodNotes = "Automatic milestone check-in index for session $i.",
                    practitionerLog = "Session $i clinical documentation. Focus topic: ${focusList[i - 1]}. Patient demonstrated full autonomous coping, and completed self-management workbook homework sheets.",
                    activeHomeworkStatus = true,
                    energyLevel = sleepScores[i - 1],
                    focusTags = focusList[i - 1]
                ))
            }

            // Update ratios in DB
            updatePatientHomeworkRatio(patient1Id)
            updatePatientHomeworkRatio(patient2Id)
            updatePatientHomeworkRatio(patient3Id)

            // Select Ryder as starting active case so the dashboards is fully populated with life
            _selectedPatientId.value = patient1Id
        }
    }

    private fun getPastDateString(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(cal.time)
    }
}

class ClientFlowViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientFlowViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientFlowViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
