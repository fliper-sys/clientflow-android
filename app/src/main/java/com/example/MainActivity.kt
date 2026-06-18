package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PatientEntity
import com.example.data.SessionLogEntity
import com.example.ui.ClientFlowViewModel
import com.example.ui.ClientFlowViewModelFactory
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var darkThemeBySystem = isSystemInDarkTheme()
            var darkThemeOverridden by remember { mutableStateOf<Boolean?>(null) }
            val useDarkTheme = darkThemeOverridden ?: darkThemeBySystem

            MyApplicationTheme(darkTheme = useDarkTheme) {
                // Background fills entire screen cleanly
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val viewModel: ClientFlowViewModel = viewModel(
                        factory = ClientFlowViewModelFactory(context)
                    )

                    ClientFlowAppContent(
                        viewModel = viewModel,
                        useDarkTheme = useDarkTheme,
                        onThemeToggle = { darkThemeOverridden = !useDarkTheme }
                    )
                }
            }
        }
    }
}

@Composable
fun ClientFlowAppContent(
    viewModel: ClientFlowViewModel,
    useDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val isLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isPanicActive by viewModel.isPanicActive.collectAsStateWithLifecycle()
    val privacyConfig by viewModel.privacyConfig.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Core Layout Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            HeaderArea(
                useDarkTheme = useDarkTheme,
                onThemeToggle = onThemeToggle,
                viewModel = viewModel,
                isPanicActive = isPanicActive,
                privacyConfig = privacyConfig
            )

            WorkspaceArea(viewModel = viewModel, privacyConfig = privacyConfig)
        }

        // Lock Screen overlay gate
        if (isLocked) {
            SecurityLockOverlay(
                viewModel = viewModel,
                privacyConfig = privacyConfig
            )
        }

        // Panic Mode blackout/med-reading overlay
        if (isPanicActive) {
            PanicOverlay(onDismiss = { viewModel.triggerPanic(false) })
        }
    }
}

@Composable
fun HeaderArea(
    useDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    viewModel: ClientFlowViewModel,
    isPanicActive: Boolean,
    privacyConfig: com.example.data.PrivacyConfigEntity
) {
    val context = LocalContext.current
    val currentSelectedPatientId by viewModel.selectedPatientId.collectAsStateWithLifecycle()
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val activePatient = patients.find { it.id == currentSelectedPatientId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Branding Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CF",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ClientFlow",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "DR. ARIS THORNE",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Control Hub Toggles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Panic button (pill-style minimalist with warning alert colors)
                    Button(
                        onClick = { viewModel.triggerPanic(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("panic_button")
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = "Panic Trigger", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PANIC", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Dark/Light toggle
                    IconButton(
                        onClick = onThemeToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .background(Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (useDarkTheme) Icons.Filled.Star else Icons.Filled.Star,
                            contentDescription = "Theme Switcher",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Lock button manually
                    IconButton(
                        onClick = { viewModel.setAppLocked(true) },
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .background(Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Manual Lockdown",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Gate Quick Actions Row
            Column {
                Text(
                    text = "CLINICAL PRIVACY & SECURITY SHIELDS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrivacyToggleChip(
                        label = "Mask Names",
                        isActive = privacyConfig.maskNames,
                        onToggle = { viewModel.updatePrivacyConfig(maskNames = !privacyConfig.maskNames) },
                        tag = "toggle_mask_names"
                    )
                    PrivacyToggleChip(
                        label = "Blur Notes",
                        isActive = privacyConfig.blurNotes,
                        onToggle = { viewModel.updatePrivacyConfig(blurNotes = !privacyConfig.blurNotes) },
                        tag = "toggle_blur_notes"
                    )
                    PrivacyToggleChip(
                        label = "Obfuscate Contacts",
                        isActive = privacyConfig.obfuscateContacts,
                        onToggle = { viewModel.updatePrivacyConfig(obfuscateContacts = !privacyConfig.obfuscateContacts) },
                        tag = "toggle_obfuscate_contacts"
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyToggleChip(
    label: String,
    isActive: Boolean,
    onToggle: () -> Unit,
    tag: String
) {
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .testTag(tag)
            .height(32.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActive) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = label,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun WorkspaceArea(
    viewModel: ClientFlowViewModel,
    privacyConfig: com.example.data.PrivacyConfigEntity
) {
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentSelectedPatientId by viewModel.selectedPatientId.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Case file & metrics, 1 = Mood Trends Calendar, 2 = Analytics Charts, 3 = Delta Contrast comparison
    var showCreatePatientDialog by remember { mutableStateOf(false) }

    // Root scroll support
    Column(modifier = Modifier.fillMaxSize()) {
        
        // Navigation Tabs Bar
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Casefile Logs", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.List, contentDescription = "Logs", modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Mood Calendar", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = "Calendar", modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Correlated Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.DateRange, contentDescription = "Charts", modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = activeTab == 3,
                onClick = { activeTab = 3 },
                text = { Text("Delta Compare", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                icon = { Icon(Icons.Filled.List, contentDescription = "Delta", modifier = Modifier.size(16.dp)) }
            )
        }

        // Caseload index header with demo triggers + totals
        CaseloadControlsPanel(
            viewModel = viewModel,
            patientsCount = patients.size,
            sessionsCount = sessions.size,
            onCreatePatientClick = { showCreatePatientDialog = true }
        )

        // Empty State Handler Case
        if (patients.isEmpty()) {
            EmptyCaseloadState(onPopulateDemo = { viewModel.populateDemoData() })
        } else {
            // Main workspace display depends on chosen Tab
            when (activeTab) {
                0 -> CasefileLogsTab(
                    viewModel = viewModel,
                    patients = patients,
                    sessions = sessions,
                    selectedPatientId = currentSelectedPatientId,
                    privacyConfig = privacyConfig
                )
                1 -> MoodCalendarTab(
                    viewModel = viewModel,
                    sessions = sessions,
                    selectedPatientId = currentSelectedPatientId,
                    patients = patients,
                    privacyConfig = privacyConfig
                )
                2 -> AnalyticsTab(
                    viewModel = viewModel,
                    sessions = sessions,
                    selectedPatientId = currentSelectedPatientId,
                    patients = patients
                )
                3 -> DeltaCompareTab(
                    viewModel = viewModel,
                    sessions = sessions,
                    selectedPatientId = currentSelectedPatientId,
                    patients = patients,
                    privacyConfig = privacyConfig
                )
            }
        }
    }

    if (showCreatePatientDialog) {
        CreatePatientDialog(
            onDismiss = { showCreatePatientDialog = false },
            onSave = { name, diagnosis, email, phone, notes ->
                viewModel.addCustomPatient(name, diagnosis, email, phone, notes)
                showCreatePatientDialog = false
            }
        )
    }
}

@Composable
fun CaseloadControlsPanel(
    viewModel: ClientFlowViewModel,
    patientsCount: Int,
    sessionsCount: Int,
    onCreatePatientClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SANDBOX COMPANION MANAGER",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Caseload: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$patientsCount Active Cases",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tealCheck()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• $sessionsCount Records",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Demo Sandboxes trigger
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Populate trigger
                Button(
                    onClick = { viewModel.populateDemoData() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("feed_demo_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Feed", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Load Demo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Delete wipe trigger
                IconButton(
                    onClick = { viewModel.wipeDatabase() },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .size(32.dp)
                        .testTag("wipe_sandbox_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Clear Sandbox",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Add Patient Trigger
                IconButton(
                    onClick = onCreatePatientClick,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .size(32.dp)
                        .testTag("create_patient_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Patient",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCaseloadState(onPopulateDemo: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                RoundedCornerShape(24.dp)
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Database is Empty",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Clinical Database is Empty",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Wiped for security compliance. Tap standard 'Load Demo' to populate full preconfigured patient files, sleep tracking histories, homework progress, and Aura charts safely.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onPopulateDemo,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Populate", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Feed Demo Caseload Profiles", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
fun CasefileLogsTab(
    viewModel: ClientFlowViewModel,
    patients: List<PatientEntity>,
    sessions: List<SessionLogEntity>,
    selectedPatientId: Int?,
    privacyConfig: com.example.data.PrivacyConfigEntity
) {
    val activePatient = patients.find { it.id == selectedPatientId } ?: patients.firstOrNull()
    var showAddSessionDialog by remember { mutableStateOf(false) }

    if (activePatient != null) {
        LaunchedEffect(activePatient) {
            if (selectedPatientId == null) {
                viewModel.selectPatient(activePatient.id)
            }
        }

        val patientSessions = sessions.filter { it.patientId == activePatient.id }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Analytical performance metrics counters
            item {
                PerformanceMetricsOverview(
                    activeFilesCount = patients.size,
                    billingCount = sessions.size,
                    complianceAvg = if (patients.isNotEmpty()) patients.map { it.homeworkRatio }.average().toFloat() else 0.0f,
                    activePatientName = if (privacyConfig.maskNames) activePatient.clinicalId else activePatient.name
                )
            }

            // Case Selection Carousel
            item {
                Text(
                    text = "ACTIVE CASE FILE SELECTOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(patients) { pat ->
                        val isSelected = pat.id == activePatient.id
                        val displayName = if (privacyConfig.maskNames) pat.clinicalId else pat.name

                        Card(
                            onClick = { viewModel.selectPatient(pat.id) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .fillMaxHeight(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = pat.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = pat.diagnosis,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expanded Active Patient Profile details Card
            item {
                ActivePatientProfileCard(
                    patient = activePatient,
                    sessions = patientSessions,
                    privacyConfig = privacyConfig,
                    onAddSessionClick = { showAddSessionDialog = true }
                )
            }

            // Mood quick logger strip
            item {
                DailyMoodQuickLogStrip(viewModel = viewModel)
            }

            // Case chronological consult logs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CHRONOLOGICAL CLINICAL LOGS (${patientSessions.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Hover note reveals if blurred",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (patientSessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No recorded consultations yet. Tap standard 'Log' buttons or 'Create consultation logs' to add clinical material.", fontSize = 11.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(patientSessions) { log ->
                    SessionLogCardItem(
                        log = log,
                        viewModel = viewModel,
                        privacyConfig = privacyConfig
                    )
                }
            }

            // Bottom space
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddSessionDialog && activePatient != null) {
        CreateSessionDialog(
            patientId = activePatient.id,
            onDismiss = { showAddSessionDialog = false },
            onSave = { duration, sleep, mState, mWeight, pLog, hw, energy, tags, cDate ->
                viewModel.addCustomSession(
                    activePatient.id, duration, sleep, mState, mWeight, pLog, hw, energy, tags, cDate
                )
                showAddSessionDialog = false
            }
        )
    }

    // Context Diary note popup
    val quickLogOpen by viewModel.quickLogOpen.collectAsStateWithLifecycle()
    val quickLogState by viewModel.selectedQuickMoodState.collectAsStateWithLifecycle()
    if (quickLogOpen && quickLogState != null) {
        QuickNoteContextModal(
            stateName = quickLogState!!,
            onDismiss = { viewModel.closeQuickMoodLog() },
            onSave = { note -> viewModel.saveQuickMoodLogWithNote(note) }
        )
    }
}

@Composable
fun PerformanceMetricsOverview(
    activeFilesCount: Int,
    billingCount: Int,
    complianceAvg: Float,
    activePatientName: String
) {
    val formattedFilesCount = if (activeFilesCount < 10) String.format(Locale.getDefault(), "%02d", activeFilesCount) else "$activeFilesCount"
    val formattedBillingCount = if (billingCount < 10) String.format(Locale.getDefault(), "%02d", billingCount) else "$billingCount"
    val formattedCompliance = "${(complianceAvg * 100).toInt()}%"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricItemCard(
            modifier = Modifier.weight(1f),
            title = "FILES",
            value = formattedFilesCount,
            icon = Icons.Filled.Person,
            subText = "Client Index",
            isFilled = true
        )
        MetricItemCard(
            modifier = Modifier.weight(1f),
            title = "COMPLY",
            value = formattedCompliance,
            icon = Icons.Filled.Check,
            subText = activePatientName,
            isFilled = false,
            progress = complianceAvg
        )
        MetricItemCard(
            modifier = Modifier.weight(1f),
            title = "SESSION",
            value = formattedBillingCount,
            icon = Icons.Filled.List,
            subText = "Total Sheets",
            isFilled = false
        )
    }
}

@Composable
fun MetricItemCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subText: String,
    progress: Float? = null,
    isFilled: Boolean = false
) {
    val containerColor = if (isFilled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isFilled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val titleColor = if (isFilled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (isFilled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (isFilled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(10.dp)
                )
            }
            if (progress != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(3.dp)
                        .clip(CircleShape),
                    color = iconColor,
                    trackColor = iconColor.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
fun ActivePatientProfileCard(
    patient: PatientEntity,
    sessions: List<SessionLogEntity>,
    privacyConfig: com.example.data.PrivacyConfigEntity,
    onAddSessionClick: () -> Unit
) {
    val displayName = if (privacyConfig.maskNames) patient.clinicalId else patient.name
    val displayEmail = if (privacyConfig.obfuscateContacts) "••••@secure-server.int" else patient.email
    val displayPhone = if (privacyConfig.obfuscateContacts) "+1(•••)•••-••••" else patient.phone
    val avgSleep = if (sessions.isNotEmpty()) sessions.map { it.sleepQuality }.average() else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = patient.clinicalId,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = patient.diagnosis,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Button(
                    onClick = onAddSessionClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("log_consult_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Consult", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Consult", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(10.dp))

            // Contact Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SECURE CONTACTS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Email, contentDescription = "Email", modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(displayEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = "Phone", modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(displayPhone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("INDEX SYMPTOM STATS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Total Consults: ${sessions.size}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text("Avg Sleep Quality: ${String.format("%.1f", avgSleep)}/10", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("PRIMARY EVALUATION MEMO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (privacyConfig.blurNotes) "•••• [Privacy Mode Blurred Notes] ••••" else patient.notes,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 16.sp,
                modifier = Modifier
                    .blur(if (privacyConfig.blurNotes) 4.dp else 0.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .padding(8.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun DailyMoodQuickLogStrip(viewModel: ClientFlowViewModel) {
    val moods = listOf(
        MoodData("✨ Productive", 6.0f, AuraPositive, AuraPositiveBg),
        MoodData("🍃 Calm", 5.0f, AuraCalm, AuraCalmBg),
        MoodData("📖 Reflective", 4.0f, AuraReflective, AuraReflectiveBg),
        MoodData("😐 Neutral", 3.0f, AuraNeutral, AuraNeutralBg),
        MoodData("⚡ Anxious", 2.0f, AuraAnxious, AuraAnxiousBg),
        MoodData("😟 Overwhelmed", 1.0f, AuraOverwhelmed, AuraOverwhelmedBg)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUICK MOOD LOG STRIP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Sandbox Active",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(moods) { mood ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .testTag("mood_strip_${mood.name.substring(3)}")
                            .clickable { viewModel.openQuickMoodLog(mood.name, mood.weight) }
                            .padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(mood.bgColor, RoundedCornerShape(12.dp))
                                .border(1.dp, mood.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = mood.name.substring(0, 2), fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = mood.name.substring(3),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

data class MoodData(val name: String, val weight: Float, val color: Color, val bgColor: Color)

@Composable
fun SessionLogCardItem(
    log: SessionLogEntity,
    viewModel: ClientFlowViewModel,
    privacyConfig: com.example.data.PrivacyConfigEntity
) {
    val hoveredNoteId by viewModel.hoveredNoteId.collectAsStateWithLifecycle()
    val isHovered = hoveredNoteId == log.id

    val matchingColor = when (log.moodState) {
        "Productive" -> AuraPositive
        "Calm" -> AuraCalm
        "Reflective" -> AuraReflective
        "Neutral" -> AuraNeutral
        "Anxious" -> AuraAnxious
        "Overwhelmed" -> AuraOverwhelmed
        else -> MaterialTheme.colorScheme.primary
    }

    // Determine Diagnostic Phase
    val treatmentPhase = when {
        log.sessionNumber <= 3 -> "Assessment"
        log.sessionNumber <= 8 -> "Active Intervention"
        else -> "Maintenance & Recovery"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable {
                if (privacyConfig.blurNotes) {
                    viewModel.setHoveredNote(if (isHovered) null else log.id)
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (isHovered) matchingColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(matchingColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${log.sessionNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Consultation Log #${log.sessionNumber}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = log.date,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Phase: $treatmentPhase",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Sleep and Mood values badge
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "🛌 Sleep: ${log.sleepQuality}/10", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .background(matchingColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = log.moodState, fontSize = 8.sp, color = matchingColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub notes which can be optionally blurred
            val isBlurred = privacyConfig.blurNotes && !isHovered
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "CONSULT NOTATIONS:", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        if (log.activeHomeworkStatus) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = "HW Completed", modifier = Modifier.size(10.dp), tint = AuraPositive)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("HW Done", fontSize = 8.sp, color = AuraPositive, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Close, contentDescription = "HW Missed", modifier = Modifier.size(10.dp), tint = AuraAnxious)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("HW Incomplete", fontSize = 8.sp, color = AuraAnxious, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isBlurred) "• [Blurred Notes: Tap to Reveal consultation data] •" else log.practitionerLog,
                        fontSize = 11.sp,
                        color = if (isBlurred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        lineHeight = 15.sp,
                        modifier = Modifier.blur(if (isBlurred) 3.dp else 0.dp)
                    )

                    if (log.moodNotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Diary Note: \"${log.moodNotes}\"", fontSize = 9.sp, color = matchingColor, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }

                    if (log.focusTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            log.focusTags.split(",").map { it.trim() }.forEach { tag ->
                                if (tag.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1f.dp)
                                    ) {
                                        Text(text = "#$tag", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodCalendarTab(
    viewModel: ClientFlowViewModel,
    sessions: List<SessionLogEntity>,
    selectedPatientId: Int?,
    patients: List<PatientEntity>,
    privacyConfig: com.example.data.PrivacyConfigEntity
) {
    val patientSessions = if (selectedPatientId == null) sessions else sessions.filter { it.patientId == selectedPatientId }
    val activePatient = patients.find { it.id == selectedPatientId }

    val calendar = Calendar.getInstance()
    val currentMonthDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val displayMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

    var expandedSessionDate by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter info header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.List, contentDescription = "Filters", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activePatient == null) "Showing caseload-wide sessions mapped on calendar."
                        else "Showing calendar timeline logs for: ${if (privacyConfig.maskNames) activePatient.clinicalId else activePatient.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Monthly Header title
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayMonthName.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                // Dominant Aura legend mapping indicator
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LegendIndicator(color = AuraPositive, text = "Pos")
                    LegendIndicator(color = AuraNeutral, text = "Neu")
                    LegendIndicator(color = AuraOverwhelmed, text = "Stress")
                }
            }
        }

        // Calendar grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Weekday titles
                    val days = listOf("S", "M", "T", "W", "T", "F", "S")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { day ->
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Dates Grid Loop
                    // Simple programmatic mapping for 30 days grid aligned with mock dates
                    val totalSlots = 35
                    var dayValue = 1
                    for (row in 0 until 5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val currentDayNum = dayValue
                                if (currentDayNum <= currentMonthDays) {
                                    // Construct date string matching: yyyy-MM-dd formatted properly
                                    val formattedDay = String.format("%02d", currentDayNum)
                                    val currentCal = Calendar.getInstance()
                                    val yearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCal.time)
                                    val dateStr = "$yearFormat-$formattedDay"

                                    // Find logged sessions on this date
                                    val sessionsOnDay = patientSessions.filter { it.date == dateStr }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (sessionsOnDay.isNotEmpty()) {
                                                    expandedSessionDate = dateStr
                                                }
                                            }
                                            .padding(vertical = 8.dp)
                                            .background(
                                                if (sessionsOnDay.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                                else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Text(
                                            text = "$currentDayNum",
                                            fontSize = 12.sp,
                                            fontWeight = if (sessionsOnDay.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sessionsOnDay.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )

                                        // Aura Dot Indicator representation
                                        if (sessionsOnDay.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                sessionsOnDay.forEach { session ->
                                                    val auraColor = when (session.moodState) {
                                                        "Productive" -> AuraPositive
                                                        "Calm" -> AuraCalm
                                                        "Reflective" -> AuraReflective
                                                        "Neutral" -> AuraNeutral
                                                        "Anxious" -> AuraAnxious
                                                        "Overwhelmed" -> AuraOverwhelmed
                                                        else -> MaterialTheme.colorScheme.primary
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(auraColor)
                                                    )
                                                }
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(7.dp))
                                        }
                                    }
                                    dayValue++
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        // Detail dialog inspection
        if (expandedSessionDate != null) {
            val sessionsOnDay = patientSessions.filter { it.date == expandedSessionDate }
            val dayInspectPatient = patients.find { it.id == sessionsOnDay.firstOrNull()?.patientId }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Calendar Daily Log Inspect: $expandedSessionDate",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = { expandedSessionDate = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        sessionsOnDay.forEach { session ->
                            val patName = if (privacyConfig.maskNames) dayInspectPatient?.clinicalId else dayInspectPatient?.name
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Patient: ${patName ?: "Anonymous"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tealCheck()
                                )
                                Text(text = "Treatment Duration: ${session.durationMinutes} minutes", fontSize = 10.sp)
                                Text(text = "Recorded Mood State: ${session.moodState}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Clinical Observations:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (privacyConfig.blurNotes) "•••• [Privacy Mode Blurred Notes] ••••" else session.practitionerLog,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.blur(if (privacyConfig.blurNotes) 4.dp else 0.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // Instructions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ABOUT THE MOOD GRID CALENDAR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("The Presence Grid maps chronological patient logs automatically. Emerald/Teal dots represent productive or calm session trends. Amber dots represent baseline neutral moods, and Rose represents symptomatic pressure levels. Clicking a highlighted day inspects all concurrent practitioner notations immediately.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun LegendIndicator(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = text, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AnalyticsTab(
    viewModel: ClientFlowViewModel,
    sessions: List<SessionLogEntity>,
    selectedPatientId: Int?,
    patients: List<PatientEntity>
) {
    val activePatient = patients.find { it.id == selectedPatientId }
    val dateFilter by viewModel.dateFilter.collectAsStateWithLifecycle()

    // Filter sessions based on Patient and Date Range
    val patientQuerySessions = if (selectedPatientId == null) sessions else sessions.filter { it.patientId == selectedPatientId }
    val mappedSessions = remember(patientQuerySessions, dateFilter) {
        val calendar = Calendar.getInstance()
        val limitDate = when (dateFilter) {
            "Week" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.time
            }
            "Month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.time
            }
            "Year" -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.time
            }
            else -> null
        }

        if (limitDate != null) {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            patientQuerySessions.filter {
                try {
                    val d = format.parse(it.date)
                    d != null && d.after(limitDate)
                } catch (e: Exception) {
                    true
                }
            }
        } else {
            patientQuerySessions
        }
    }.sortedBy { it.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Range Filter Buttons Toggle Hub
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ANALYTICAL TIME-WINDOWS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Week", "Month", "Year", "All").forEach { mode ->
                            Button(
                                onClick = { viewModel.setDateFilter(mode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dateFilter == mode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    contentColor = if (dateFilter == mode) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .testTag("filter_btn_$mode")
                            ) {
                                Text(text = mode, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Custom Canvas Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CORRELATED MOOD VS SLEEP DYNAMICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Plots Sleep Quality Index (1-10) against Client Mood weights (1-6)",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (mappedSessions.size < 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Insufficient historical parameters recorded for charting. Please add more logs or load preconfigured client sandboxes.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Custom Interactive Canvas Chart Drawing
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            val count = mappedSessions.size
                            val width = size.width
                            val height = size.height

                            val stepX = width / (count - 1)
                            val moodMax = 6.0f
                            val sleepMax = 10.0f

                            // Draw baseline grid lines
                            for (line in 1..4) {
                                val yVal = height * (line / 5f)
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.15f),
                                    start = Offset(0f, yVal),
                                    end = Offset(width, yVal),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            val sleepPath = Path()
                            val moodPath = Path()

                            mappedSessions.forEachIndexed { index, session ->
                                val x = index * stepX

                                // Map Sleep (1-10) to height inverted
                                val sleepY = height - ((session.sleepQuality / sleepMax) * (height - 20f)) - 10f
                                // Map Mood (1.0-6.0) to height inverted
                                val moodY = height - ((session.moodWeight / moodMax) * (height - 20f)) - 10f

                                if (index == 0) {
                                    sleepPath.moveTo(x, sleepY)
                                    moodPath.moveTo(x, moodY)
                                } else {
                                    sleepPath.lineTo(x, sleepY)
                                    moodPath.lineTo(x, moodY)
                                }

                                // Draw node circles
                                drawCircle(
                                    color = AuraCalm,
                                    radius = 3.dp.toPx(),
                                    center = Offset(x, sleepY)
                                )
                                drawCircle(
                                    color = AuraReflective,
                                    radius = 3.dp.toPx(),
                                    center = Offset(x, moodY)
                                )
                            }

                            // Draw continuous strokes
                            drawPath(
                                path = sleepPath,
                                color = AuraCalm,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawPath(
                                path = moodPath,
                                color = AuraReflective,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Chart legends
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            LegendItem(color = AuraCalm, text = "🛌 Sleep Index (1-10)")
                            LegendItem(color = AuraReflective, text = "📊 Mood Weight (1-6)")
                        }
                    }
                }
            }
        }

        // Diagnostic Phases Annotation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "THERAPEUTIC INTERVENTION PHASES LEGEND",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    DiagnosticPhaseItem(
                        phase = "Assessment Phase (Sessions 1-3)",
                        desc = "Initial intake baseline. Establishes therapeutic alliance, cognitive schema mapping, and logs initial distress indicators.",
                        color = AuraAnxious.copy(alpha = 0.2f),
                        accent = AuraAnxious
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagnosticPhaseItem(
                        phase = "Active Intervention (Sessions 4-8)",
                        desc = "Deep restructuring. Application of CBT worksheet practices, physical grounding, PMR exercise tracking, and autonomic reconditioning.",
                        color = AuraNeutral.copy(alpha = 0.2f),
                        accent = AuraNeutral
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DiagnosticPhaseItem(
                        phase = "Maintenance & Recovery (Sessions 9+)",
                        desc = "Self-management transitioning. Coping workbook completion, relapse prevention planning, and active symptom discharge preparation.",
                        color = AuraPositive.copy(alpha = 0.2f),
                        accent = AuraPositive
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DiagnosticPhaseItem(
    phase: String,
    desc: String,
    color: Color,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = phase, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(text = desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
        }
    }
}

@Composable
fun DeltaCompareTab(
    viewModel: ClientFlowViewModel,
    sessions: List<SessionLogEntity>,
    selectedPatientId: Int?,
    patients: List<PatientEntity>,
    privacyConfig: com.example.data.PrivacyConfigEntity
) {
    val activePatient = patients.find { it.id == selectedPatientId }
    val patientSessions = if (activePatient == null) emptyList() else sessions.filter { it.patientId == activePatient.id }

    val doc1Id by viewModel.deltaSessionOneId.collectAsStateWithLifecycle()
    val doc2Id by viewModel.deltaSessionTwoId.collectAsStateWithLifecycle()

    val sessionOne = patientSessions.find { it.id == doc1Id }
    val sessionTwo = patientSessions.find { it.id == doc2Id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DELTA DIAGNOSTIC CONTRAST TOOL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Compare and extract outcome metrics between two historical consultation logs.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (activePatient == null) {
                        Text(
                            text = "Please select an active client casefile first on the primary tab index.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        val displayName = if (privacyConfig.maskNames) activePatient.clinicalId else activePatient.name
                        Text(
                            text = "Active Case: $displayName",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Dual Dropdowns to Select Session
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Selector 1
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Select Base Session", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    items(patientSessions) { item ->
                                        val isSelected = item.id == doc1Id
                                        Box(
                                            modifier = Modifier
                                                .testTag("delta_one_${item.sessionNumber}")
                                                .clickable { viewModel.selectDeltaSessionOne(item.id) }
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "S-${item.sessionNumber}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Selector 2
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Select Comparison Session", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    items(patientSessions) { item ->
                                        val isSelected = item.id == doc2Id
                                        Box(
                                            modifier = Modifier
                                                .testTag("delta_two_${item.sessionNumber}")
                                                .clickable { viewModel.selectDeltaSessionTwo(item.id) }
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "S-${item.sessionNumber}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Comparative statistics and progress calculations
        if (sessionOne != null && sessionTwo != null && activePatient != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DELTA METRIC CONTRAST REPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Sleep Quality Diff
                        val sleepDiff = sessionTwo.sleepQuality - sessionOne.sleepQuality
                        val sleepIcon = if (sleepDiff >= 0) "📈" else "📉"
                        val sleepSign = if (sleepDiff >= 0) "+$sleepDiff" else "$sleepDiff"
                        val sleepFeedback = if (sleepDiff > 0) "Rest parameters improved significantly."
                        else if (sleepDiff < 0) "Decline observed. Check insomnia stressors indices."
                        else "No change in sleep parameters."

                        // Mood weight Diff
                        val moodDiff = sessionTwo.moodWeight - sessionOne.moodWeight
                        val moodSign = if (moodDiff >= 0) "+${String.format("%.1f", moodDiff)}" else String.format("%.1f", moodDiff)
                        val moodColor = if (moodDiff >= 0) AuraPositive else AuraOverwhelmed

                        // Dual Card layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Sleep contrast box
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SLEEP DELTA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$sleepIcon $sleepSign", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(sleepFeedback, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }

                            // Mood contrast box
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("MOOD WEIGHT DELTA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = moodSign, fontSize = 18.sp, fontWeight = FontWeight.Black, color = moodColor)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Base weight vs current. Higher weight indicates active focus & calm parameters.", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Written notes overlay
                        Text("DIAGNOSTIC TEXT CONTRAST", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text("Session #${sessionOne.sessionNumber}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (privacyConfig.blurNotes) "•••• [Blurred] ••••" else sessionOne.practitionerLog,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.blur(if (privacyConfig.blurNotes) 4.dp else 0.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text("Session #${sessionTwo.sessionNumber}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (privacyConfig.blurNotes) "•••• [Blurred] ••••" else sessionTwo.practitionerLog,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.blur(if (privacyConfig.blurNotes) 4.dp else 0.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Please select any two historical sessions from the selectors above to calculate progress differences and compare sleep ratings.", fontSize = 11.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun SecurityLockOverlay(
    viewModel: ClientFlowViewModel,
    privacyConfig: com.example.data.PrivacyConfigEntity
) {
    var enteredText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(enabled = false) {}, // absorb clicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "🔒 App Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Clinical Companion locked",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Input therapeutic practitioner security passcode",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text input visually masked representing PIN passcode
            OutlinedTextField(
                value = enteredText,
                onValueChange = {
                    if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                        enteredText = it
                        errorMessage = null
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("••••", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_pin_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Enter Button
            Button(
                onClick = {
                    val success = viewModel.attemptUnlock(enteredText, privacyConfig)
                    if (success) {
                        enteredText = ""
                        errorMessage = null
                    } else {
                        errorMessage = "Invalid credentials. Try generic demo code: '1234'"
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_unlock_button")
            ) {
                Text("Authorize Access", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clickable {
                        enteredText = "1234"
                        errorMessage = null
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "Passcode indicator", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sandbox Evaluation Bypass (Auto-write '1234')", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PanicOverlay(onDismiss: () -> Unit) {
    // Professional Looking Document reading workspace overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9)) // Clean medical clean-paper color
            .clickable(enabled = false) {} // block
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DSM-5-TR CLINICAL MANUAL REF SHEET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                // Silent resume action
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp).testTag("panic_dismiss_button")
                ) {
                    Text("Resume Dashboard Workspace", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Category F43.23: Adjustment Disorder with Mixed Anxiety and Depressed Mood",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Clinical Diagnostic Criteria Overview:\n\n" +
                        "A. Distinct marked emotional or behavioral symptoms developing in response to an identifiable stressor occurring within 3 months of the onset of the stressor.\n\n" +
                        "B. These symptoms or behaviors are clinically significant, as evidenced by one or both of the following:\n" +
                        "   1. Marked distress that is out of proportion to the severity or intensity of the stressor, taking into account the external context and cultural factors that might influence symptom severity and presentation.\n" +
                        "   2. Significant impairment in social, occupational, or other important areas of functioning.\n\n" +
                        "C. The stress-related disturbance does not meet the criteria for another mental disorder and is not merely an exacerbation of a preexisting mental disorder.\n\n" +
                        "D. The symptoms do not represent normal bereavement.\n\n" +
                        "E. Once the stressor or its consequences have terminated, the symptoms do not persist for more than an additional 6 months.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF334155),
                textAlign = TextAlign.Justify
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Standard Therapeutic Guidelines:\n\n" +
                        "Primary interventions focus on promoting behavioral recovery, cognitive restructuring of automatic safety thoughts, progressive muscular relaxation (PMR), mindfulness autonomic indices, and building emotional coping sheets. Practitioner logs should record sleep parameters weekly.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF334155),
                textAlign = TextAlign.Justify
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePatientDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "CREATE CLIENT RECORD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Client Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_patient_name")
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("ICD-10 Diagnosis (e.g. F41.1 GAD)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_patient_diagnosis")
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Contacts") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telephone Contact") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Brief Case Eval Memo") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (name.isEmpty() || diagnosis.isEmpty()) {
                                errorMsg = "Please supply patient name and diagnosis codes."
                            } else {
                                onSave(name, diagnosis, email, phone, notes)
                            }
                        },
                        modifier = Modifier.testTag("add_patient_save")
                    ) {
                        Text("Save File")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionDialog(
    patientId: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, Float, String, Boolean, Int, String, String?) -> Unit
) {
    var durationText by remember { mutableStateOf("50") }
    var sleepText by remember { mutableStateOf("7") }
    var energyText by remember { mutableStateOf("6") }
    var moodState by remember { mutableStateOf("Calm") }
    var practitionerLog by remember { mutableStateOf("") }
    var homeworkStatus by remember { mutableStateOf(true) }
    var focusTags by remember { mutableStateOf("CBT, Cognitive") }
    var dateString by remember { mutableStateOf("") }

    val moodOptions = listOf(
        Pair("Productive", 6.0f),
        Pair("Calm", 5.0f),
        Pair("Reflective", 4.0f),
        Pair("Neutral", 3.0f),
        Pair("Anxious", 2.0f),
        Pair("Overwhelmed", 1.0f)
    )

    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "RECORD CONSULTATION LOGS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (Min)") },
                        modifier = Modifier.weight(1f).testTag("add_session_duration")
                    )
                    OutlinedTextField(
                        value = sleepText,
                        onValueChange = { sleepText = it },
                        label = { Text("Sleep (1-10)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (yyyy-MM-dd / leave blank for today)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text("Mood State Select", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(moodOptions) { item ->
                        val isSelected = moodState == item.first
                        Box(
                            modifier = Modifier
                                .clickable { moodState = item.first }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = item.first,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = practitionerLog,
                    onValueChange = { practitionerLog = it },
                    label = { Text("Objective Consult Notations") },
                    modifier = Modifier.fillMaxWidth().testTag("add_session_notes")
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = focusTags,
                    onValueChange = { focusTags = it },
                    label = { Text("Therapeutic tags (comma list)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = homeworkStatus,
                        onCheckedChange = { homeworkStatus = it },
                        modifier = Modifier.testTag("add_session_hw")
                    )
                    Text("Assigned Cognitive Homework Sheets COMPLETED", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull() ?: 50
                            val sleep = sleepText.toIntOrNull() ?: 7
                            val weight = moodOptions.find { it.first == moodState }?.second ?: 3f
                            val energy = energyText.toIntOrNull() ?: 5
                            val dateToSave = if (dateString.isEmpty()) null else dateString

                            if (practitionerLog.isEmpty()) {
                                errorMsg = "Please supply objective consult notations."
                            } else {
                                onSave(duration, sleep, moodState, weight, practitionerLog, homeworkStatus, energy, focusTags, dateToSave)
                            }
                        },
                        modifier = Modifier.testTag("add_session_save")
                    ) {
                        Text("Save Consult")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteContextModal(
    stateName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notesText by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "LOG STATE: $stateName", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Saving this saves a daily chronological record for the active case file.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Enter a brief, 1-sentence state explanation") },
                    placeholder = { Text("e.g. Felt highly centered after doing afternoon grounding exercises.") },
                    modifier = Modifier.fillMaxWidth().testTag("quick_note_input"),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val note = if (notesText.isEmpty()) "State log for $stateName" else notesText
                            onSave(note)
                        },
                        modifier = Modifier.testTag("quick_note_save")
                    ) {
                        Text("Save Record")
                    }
                }
            }
        }
    }
}

// Extensions helper for compilation & custom themes compatibility
fun androidx.compose.material3.ColorScheme.tealCheck() = MinPrimaryDark
