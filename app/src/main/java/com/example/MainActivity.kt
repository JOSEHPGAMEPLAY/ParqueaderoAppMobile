package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: ScanViewModel = viewModel(
                    factory = ScanViewModelFactory(application)
                )

                // Root-level update checking overlay dialog
                UpdateCheckerDialog(viewModel = viewModel)

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = viewModel(factory = ScanViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Observe flows from ViewModel
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val currentCapturedBitmap by viewModel.currentBitmap.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val isRegistered by viewModel.isRegistered.collectAsStateWithLifecycle()

    if (!isRegistered) {
        DeviceRegistrationView(
            viewModel = viewModel,
            modifier = modifier
        )
        return
    }

    var editedPlateText by remember { mutableStateOf("") }
    var currentSection by rememberSaveable { mutableStateOf(ScreenSection.CAR_RECORDS) }

    LaunchedEffect(scanState) {
        if (scanState is ScanUiState.Success) {
            editedPlateText = (scanState as ScanUiState.Success).info.plateText ?: ""
        }
    }

    val rateWithTicketHour by viewModel.rateWithTicketHour.collectAsStateWithLifecycle()
    val rateWithTicketFraction by viewModel.rateWithTicketFraction.collectAsStateWithLifecycle()
    val rateNoTicketHour by viewModel.rateNoTicketHour.collectAsStateWithLifecycle()
    val rateNoTicketFraction by viewModel.rateNoTicketFraction.collectAsStateWithLifecycle()

    // Parking rates text states
    var editWithHour by remember { mutableStateOf("") }
    var editWithFraction by remember { mutableStateOf("") }
    var editNoHour by remember { mutableStateOf("") }
    var editNoFraction by remember { mutableStateOf("") }

    LaunchedEffect(rateWithTicketHour, rateWithTicketFraction, rateNoTicketHour, rateNoTicketFraction) {
        editWithHour = rateWithTicketHour.toString()
        editWithFraction = rateWithTicketFraction.toString()
        editNoHour = rateNoTicketHour.toString()
        editNoFraction = rateNoTicketFraction.toString()
    }

    // Checkout dialogue targets
    var checkoutVehicleTarget by remember { mutableStateOf<ScanResult?>(null) }
    var checkoutHasTicket by remember { mutableStateOf(true) }

    // Permissions state
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.processImage(bitmap, context)
                } else {
                    Toast.makeText(context, "No se pudo cargar la imagen seleccionada.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir la foto: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Interactive simulator input states
    var simPlateText by remember { mutableStateOf("LKM 519") }
    var simCountry by remember { mutableStateOf("Colombia") }
    var simState by remember { mutableStateOf("Bogotá D.C.") }

    // Preset list of mock plates for quick testing (very helpful for sandbox setups)
    val presets = listOf(
        Triple("LKM 519", "Colombia", "Bogotá D.C."),
        Triple("HJU 223", "Colombia", "Medellín"),
        Triple("AB 123 CD", "Venezuela", "Caracas"),
        Triple("FTG 848", "Colombia", "Cali"),
        Triple("HJD 984", "Colombia", "Medellín")
    )

    // Segmented Input Tabs (0 = Escanear Camera/Gallery, 1 = Simulator / Fees)
    var selectedTab by remember { mutableStateOf(0) }
    var isCameraActive by remember { mutableStateOf(false) }

    // Dialog flags
    var showClearConfirm by remember { mutableStateOf(false) }
    var expandedVehicleId by remember { mutableStateOf<Long?>(null) }
    var inlineNotesEditId by remember { mutableStateOf<Long?>(null) }
    var editedNotesValue by remember { mutableStateOf("") }

    // Manual registration dialog state
    var showManualDialog by remember { mutableStateOf(false) }
    var manualPlate by remember { mutableStateOf("") }
    var manualCountry by remember { mutableStateOf("Colombia") }
    var manualState by remember { mutableStateOf("Bogotá D.C.") }
    var manualBrand by remember { mutableStateOf("") }
    var manualColor by remember { mutableStateOf("") }
    var manualNotes by remember { mutableStateOf("") }

    // Filtered history based on query text
    val filteredHistory = remember(scanHistory, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            scanHistory
        } else {
            scanHistory.filter { scan ->
                scan.plateText.contains(searchQuery, ignoreCase = true) ||
                        (scan.country?.contains(searchQuery, ignoreCase = true) == true) ||
                        (scan.state?.contains(searchQuery, ignoreCase = true) == true) ||
                        (scan.vehicleBrand?.contains(searchQuery, ignoreCase = true) == true) ||
                        (scan.vehicleColor?.contains(searchQuery, ignoreCase = true) == true) ||
                        NotesParser.parse(scan.notes).any { it.message.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // Group history by day to optimize rendering, lookup, and visual scan times
    val groupedHistory = remember(filteredHistory) {
        filteredHistory.groupBy { scan ->
            SimpleDateFormat("EEEE, d 'de' MMMM, yyyy", java.util.Locale("es", "ES")).format(Date(scan.timestamp))
        }
    }

    // Force active camera state if permission is newly granted
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && isCameraActive) {
            // Keep active
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val registeredUsername by viewModel.registeredUsername.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Sidebar Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Parking",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Gestión de Parqueadero",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Registros de Autos", fontWeight = FontWeight.Bold) },
                    selected = currentSection == ScreenSection.CAR_RECORDS,
                    onClick = {
                        currentSection = ScreenSection.CAR_RECORDS
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Sincronización Web", fontWeight = FontWeight.Bold) },
                    selected = currentSection == ScreenSection.SYNCHRONIZATION,
                    onClick = {
                        currentSection = ScreenSection.SYNCHRONIZATION
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Filled.Sync, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Configurar Tarifas", fontWeight = FontWeight.Bold) },
                    selected = currentSection == ScreenSection.RATES,
                    onClick = {
                        currentSection = ScreenSection.RATES
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Filled.Payments, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                NavigationDrawerItem(
                    label = { Text("Panel de Simulación", fontWeight = FontWeight.Bold) },
                    selected = currentSection == ScreenSection.SIMULATION,
                    onClick = {
                        currentSection = ScreenSection.SIMULATION
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Filled.Science, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Sidebar Footer (Web integration state)
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ESTADO DE CONEXIÓN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (registeredUsername.isNotEmpty()) 
                                        Color(0xFFE8F5E9) 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (registeredUsername.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFF757575))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (registeredUsername.isNotEmpty()) "Vinculado con Web" else "Modo Local / Autónomo",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (registeredUsername.isNotEmpty()) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) {
        val app_title = when (currentSection) {
            ScreenSection.CAR_RECORDS -> "Registros de Autos"
            ScreenSection.SYNCHRONIZATION -> "Sincronización Web"
            ScreenSection.RATES -> "Tarifas de Parqueadero"
            ScreenSection.SIMULATION -> "Panel de Simulación"
            else -> "Smart Parking"
        }

        Scaffold(
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_hamburger_car_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DirectionsCar,
                                    contentDescription = "Abrir menú de navegación",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = app_title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (registeredUsername.isNotEmpty()) 
                                        Color(0xFFE8F5E9) 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (registeredUsername.isNotEmpty()) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    tint = if (registeredUsername.isNotEmpty()) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (registeredUsername.isNotEmpty()) "WEB" else "OFFLINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (registeredUsername.isNotEmpty()) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentSection) {
                    ScreenSection.CAR_RECORDS -> {
                        AutoRecordsView(
                            viewModel = viewModel,
                            context = context,
                            cameraPermissionState = cameraPermissionState,
                            galleryLauncher = galleryLauncher,
                            isCameraActive = isCameraActive,
                            setCameraActive = { isCameraActive = it },
                            editedPlateText = editedPlateText,
                            setEditedPlateText = { editedPlateText = it },
                            groupedHistory = groupedHistory,
                            scanHistory = scanHistory,
                            searchQuery = searchQuery,
                            onClearHistoryClick = { showClearConfirm = true },
                            onManualClick = { showManualDialog = true },
                            expandedVehicleId = expandedVehicleId,
                            setExpandedVehicleId = { expandedVehicleId = it },
                            inlineNotesEditId = inlineNotesEditId,
                            setInlineNotesEditId = { inlineNotesEditId = it },
                            editedNotesValue = editedNotesValue,
                            setEditedNotesValue = { editedNotesValue = it },
                            onCheckoutClick = { scan ->
                                checkoutVehicleTarget = scan
                                checkoutHasTicket = true
                            }
                        )
                    }
                    ScreenSection.SYNCHRONIZATION -> {
                        SynchronizationView(
                            viewModel = viewModel,
                            context = context
                        )
                    }
                    ScreenSection.RATES -> {
                        RatesView(
                            viewModel = viewModel,
                            context = context,
                            editWithHour = editWithHour,
                            setEditWithHour = { editWithHour = it },
                            editWithFraction = editWithFraction,
                            setEditWithFraction = { editWithFraction = it },
                            editNoHour = editNoHour,
                            setEditNoHour = { editNoHour = it },
                            editNoFraction = editNoFraction,
                            setEditNoFraction = { editNoFraction = it },
                            rateWithTicketHour = rateWithTicketHour,
                            rateWithTicketFraction = rateWithTicketFraction,
                            rateNoTicketHour = rateNoTicketHour,
                            rateNoTicketFraction = rateNoTicketFraction,
                            keyboardController = keyboardController
                        )
                    }
                    ScreenSection.SIMULATION -> {
                        SimulationView(
                            viewModel = viewModel,
                            context = context,
                            keyboardController = keyboardController
                        )
                    }
                }
            }
        }
    }

    if (false) {
        LazyColumn(
            modifier = modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. Header / Dashboard Branding
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Escáner de Placas",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Reconocimiento y análisis de matrículas",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 1b. Métricas Operativas (Diseño Basado en Métricas)
        item {
            val activeCount = scanHistory.count { it.status == "ACTIVE" }
            val completedCount = scanHistory.count { it.status == "COMPLETED" }
            val totalRevenue = scanHistory.sumOf { it.paidAmount ?: 0.0 }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vehículos Adentro
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "En Parqueadero",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$activeCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Salidas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Salidas Hoy",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$completedCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Ingreso Total
                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Payment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Total Recaudado (App)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$${String.format("%,.0f", totalRevenue)} COP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // 1c. Panel de Integración en la Nube (Sincronización Web REST)
        item {
            val registeredUsername by viewModel.registeredUsername.collectAsStateWithLifecycle()
            val apiBaseUrl by viewModel.apiBaseUrl.collectAsStateWithLifecycle()
            val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
            val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
            val syncStatusError by viewModel.syncStatusError.collectAsStateWithLifecycle()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("sync_panel_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Vinculado con la Web: $registeredUsername",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Base API: $apiBaseUrl",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Sync button
                        Button(
                            onClick = { viewModel.syncWithBackend() },
                            enabled = !isSyncing,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("sync_now_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "Sincronizar",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sincronizar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Explanation line
                    Text(
                        text = "Un solo toque para consolidar reportes de turnos, actualizar ingresos por día y enviar comentarios offline.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        lineHeight = 11.sp
                    )

                    // Message status
                    AnimatedVisibility(visible = syncStatusMessage.isNotEmpty() || syncStatusError) {
                        val badgeColor = if (syncStatusError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        Surface(
                            color = badgeColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (syncStatusError) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = syncStatusMessage,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Desvincular Dispositivo",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clickable { viewModel.unregisterDevice() }
                                .padding(vertical = 2.dp)
                                .testTag("unlink_device_button")
                        )
                    }
                }
            }
        }

        // 2. Control Tabs (Escanear / Cámara, Tarifas/Prueba)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant

                    // Camera & Gallery Tab Button (Unified)
                    Button(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("tab_camera"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 0) activeColor else inactiveColor,
                            contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escanear Placa", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Simulator Tab Button
                    Button(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_simulator"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedTab == 1) activeColor else inactiveColor,
                            contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tarifas/Prueba", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 2b. Manual License Plate Entry Button
        item {
            Button(
                onClick = { 
                    manualPlate = ""
                    manualBrand = ""
                    manualColor = ""
                    manualNotes = ""
                    showManualDialog = true 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("open_manual_entry_dialog_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = "Ingreso Manual",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ingresar Placa Manualmente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 3. Operational Scanner Board Content
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (selectedTab == 1) Modifier.wrapContentHeight() else Modifier.height(310.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().then(if (selectedTab == 1) Modifier.wrapContentHeight() else Modifier.fillMaxSize())) {
                    when (selectedTab) {
                        0 -> {
                            // Unified Camera & Gallery Board
                            if (isCameraActive) {
                                if (cameraPermissionState.status.isGranted) {
                                    var cameraError by remember { mutableStateOf<String?>(null) }
                                    if (cameraError == null) {
                                        CameraPreview(
                                            modifier = Modifier.fillMaxSize(),
                                            onClose = { isCameraActive = false },
                                            onBitmapCaptured = { bitmap ->
                                                viewModel.processImage(bitmap, context)
                                            },
                                            onError = { err ->
                                                cameraError = err.localizedMessage ?: "Error de cámara desconocido"
                                            }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Error,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Error al iniciar cámara:",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = cameraError!!,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.padding(top = 16.dp)
                                                ) {
                                                    Button(
                                                        onClick = { cameraError = null },
                                                    ) {
                                                        Text("Reintentar")
                                                    }
                                                    OutlinedButton(
                                                        onClick = { isCameraActive = false },
                                                    ) {
                                                        Text("Cerrar")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Requiere Permiso para activar cámara
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoCamera,
                                                contentDescription = "Camera Permission Needed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Permiso Requerido",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Text(
                                                text = "Para activar la cámara del escáner en vivo, concede el permiso.",
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { cameraPermissionState.launchPermissionRequest() },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Permitir", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { isCameraActive = false },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Cancelar", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Camera inactivo: panel alternativo unificado cámara + galería
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PhotoCamera,
                                            contentDescription = "Escáner Listo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Escáner Local Offline",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Captura con la cámara o selecciona una imagen de tu galería para procesar la placa de forma instantánea 100% on-device (Colombia/Venezuela).",
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { isCameraActive = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(1.1f).testTag("activate_camera_button")
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Cámara En Vivo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Button(
                                                onClick = { galleryLauncher.launch("image/*") },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.weight(0.9f).testTag("select_gallery_button")
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Galería", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Configuración de Tarifas
                                Text(
                                    text = "⚙️ Tarifas del Parqueadero (COP)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                // CARD SECTOR: TARIFA CON TICKET
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "🎟️ Tarifas CON TICKET (Lectura Física/Código)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = editWithHour,
                                                onValueChange = { editWithHour = it.filter { c -> c.isDigit() } },
                                                label = { Text("Valor por Hora Completa", fontSize = 11.sp) },
                                                prefix = { Text("$ ", fontSize = 12.sp) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = editWithFraction,
                                                onValueChange = { editWithFraction = it.filter { c -> c.isDigit() } },
                                                label = { Text("Valor por Media Hora (Fracción)", fontSize = 11.sp) },
                                                prefix = { Text("$ ", fontSize = 12.sp) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                
                                // CARD SECTOR: TARIFA SIN TICKET
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "✏️ Tarifas SIN TICKET (Ingreso/Registro Manual)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = editNoHour,
                                                onValueChange = { editNoHour = it.filter { c -> c.isDigit() } },
                                                label = { Text("Valor por Hora Completa", fontSize = 11.sp) },
                                                prefix = { Text("$ ", fontSize = 12.sp) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = editNoFraction,
                                                onValueChange = { editNoFraction = it.filter { c -> c.isDigit() } },
                                                label = { Text("Valor por Media Hora (Fracción)", fontSize = 11.sp) },
                                                prefix = { Text("$ ", fontSize = 12.sp) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        val wh = editWithHour.toIntOrNull() ?: 2000
                                        val wf = editWithFraction.toIntOrNull() ?: 1000
                                        val nh = editNoHour.toIntOrNull() ?: 4000
                                        val nf = editNoFraction.toIntOrNull() ?: 2000
                                        viewModel.updateRates(wh, wf, nh, nf)
                                        keyboardController?.hide()
                                        Toast.makeText(context, "Tarifas del Parqueadero actualizadas.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    Text("Guardar Tarifas", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(4.dp))

                                // 2. Simulador de Patentes
                                Text(
                                    text = "🚗 Simular Entrada de Vehículo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                OutlinedTextField(
                                    value = simPlateText,
                                    onValueChange = { simPlateText = it.uppercase().take(15) },
                                    label = { Text("Matrícula (Ej. LKM 519)") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_sim_plate_text"),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Next
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    var simCountryDropdownExpanded by remember { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { simCountryDropdownExpanded = true }
                                    ) {
                                        OutlinedTextField(
                                            value = simCountry,
                                            onValueChange = {},
                                            readOnly = true,
                                            enabled = false,
                                            label = { Text("País") },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Filled.ArrowDropDown,
                                                    contentDescription = "Desplegar países"
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("input_sim_country")
                                        )
                                        DropdownMenu(
                                            expanded = simCountryDropdownExpanded,
                                            onDismissRequest = { simCountryDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Colombia 🇨🇴") },
                                                onClick = {
                                                    simCountry = "Colombia"
                                                    simState = "Bogotá D.C."
                                                    simCountryDropdownExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Venezuela 🇻🇪") },
                                                onClick = {
                                                    simCountry = "Venezuela"
                                                    simState = "Caracas"
                                                    simCountryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = simState,
                                        onValueChange = { simState = it.take(20) },
                                        label = { Text("Estado/Ciudad") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_sim_state"),
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = {
                                            keyboardController?.hide()
                                            viewModel.generateSimulatedPlateAndScan(simPlateText, simCountry, simState, context)
                                        })
                                    )
                                }

                                // Quick Presets Slider
                                Column {
                                    Text("Elegir preajuste rápido:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        presets.forEach { preset ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        simPlateText = preset.first
                                                        simCountry = preset.second
                                                        simState = preset.third
                                                        viewModel.generateSimulatedPlateAndScan(preset.first, preset.second, preset.third, context)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(preset.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        viewModel.generateSimulatedPlateAndScan(simPlateText, simCountry, simState, context)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("generate_scan_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.FlashOn, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Registrar Ingreso de Vehículo", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // 3. Configuración de Actualizaciones (GitHub)
                                Text(
                                    text = "🔄 Actualizaciones del Sistema",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val githubReleaseUrl by viewModel.githubReleaseUrl.collectAsStateWithLifecycle()
                                var inputGithubReleaseUrl by remember(githubReleaseUrl) { mutableStateOf(githubReleaseUrl) }

                                OutlinedTextField(
                                    value = inputGithubReleaseUrl,
                                    onValueChange = { inputGithubReleaseUrl = it },
                                    label = { Text("URL de la API de GitHub para Updates") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setup_github_release_url_input"),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Done
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateGithubReleaseUrl(inputGithubReleaseUrl)
                                            viewModel.checkForUpdates(currentVersion = "1.0", forceShowSimulated = false)
                                            Toast.makeText(context, "Consultando actualizaciones en GitHub...", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("check_github_updates_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Comprobar Ahora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.checkForUpdates(currentVersion = "1.0", forceShowSimulated = true)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("simulate_github_updates_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Simular (Demo)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Floating Loading overlay inside the Scanner View
                    if (scanState is ScanUiState.Processing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Leyendo Matrícula...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Procesando placa y registrando ingreso al parqueadero",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Live Scanned Result Showcase Area
        item {
            AnimatedVisibility(
                visible = scanState !is ScanUiState.Idle && scanState !is ScanUiState.Processing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                when (val state = scanState) {
                    is ScanUiState.Success -> {
                        val details = state.info
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_success_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Resultado de Lectura",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    // High contrast Confidence Chip
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Confianza: ${details.confidence ?: 90}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Render plate number with a realistic European metallic looking plate mockup
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Left Blue Flag Badge
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF0F4C81))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = Color.Yellow,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = (details.country?.take(2) ?: "ES").uppercase(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Large readable plate number
                                        Text(
                                            text = editedPlateText.uppercase(),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            letterSpacing = 1.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Live interactive Plate Editor
                                OutlinedTextField(
                                    value = editedPlateText,
                                    onValueChange = { newValue ->
                                        val uppercaseValue = newValue.uppercase()
                                        editedPlateText = uppercaseValue
                                        viewModel.updateLatestScanPlateText(uppercaseValue)
                                    },
                                    label = { Text("Corregir Matrícula Detectada", fontWeight = FontWeight.Bold) },
                                    placeholder = { Text("Ej. LKM 519") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("verify_plate_input_field"),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Done
                                     ),
                                     colors = OutlinedTextFieldDefaults.colors(
                                         focusedBorderColor = MaterialTheme.colorScheme.primary,
                                         focusedLabelColor = MaterialTheme.colorScheme.primary,
                                         unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                     )
                                 )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Grid details
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        DetailItem(label = "País", value = details.country ?: "No identificado", modifier = Modifier.weight(1f), icon = Icons.Filled.Public)
                                        DetailItem(label = "Estado", value = details.state ?: "No identificado", modifier = Modifier.weight(1f), icon = Icons.Filled.Map)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        DetailItem(label = "Marca", value = details.vehicleBrand ?: "No identificada", modifier = Modifier.weight(1f), icon = Icons.Filled.DirectionsCar)
                                        DetailItem(label = "Color", value = details.vehicleColor ?: "No identificado", modifier = Modifier.weight(1f), icon = Icons.Filled.Palette)
                                    }
                                }

                                if (!details.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Anomalías / Observaciones:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    NotesFeedView(notesStr = details.notes)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.dismissScanResult() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Cerrar")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.dismissScanResult()
                                            Toast.makeText(context, "Placa guardada correctamente.", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Continuar", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                    is ScanUiState.Error -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_error_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = "Ocurrió un error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No se completó la lectura",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { viewModel.dismissScanResult() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Entendido")
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // 5. Scanned History Section Label & Clear Option
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.ListAlt,
                        contentDescription = "Historial",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historial de Escaneos (${scanHistory.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (scanHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Limpiar historial",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Search text input bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Buscar por matrícula, país, color, marca...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_history_input"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // 6. List of Historical Entries
        if (filteredHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = "Sin registros",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Aún no hay códigos escaneados" else "No se encontraron coincidencias",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "Las matrículas detectadas por la cámara se registrarán automáticamente aquí." else "Intenta buscar usando otras palabras clave.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            groupedHistory.forEach { (dateHeader, scansInDay) ->
                item(key = "header_${dateHeader}") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 " + dateHeader.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val activeCount = scansInDay.count { it.status == "ACTIVE" }
                            val completedCount = scansInDay.count { it.status == "COMPLETED" }
                            val dayRevenue = scansInDay.sumOf { it.paidAmount ?: 0.0 }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Activos: $activeCount | Salidos: $completedCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "Recaudado: $${String.format("%,.0f", dayRevenue)} COP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                items(scansInDay, key = { it.id }) { scan ->
                    val isExpanded = expandedVehicleId == scan.id
                val isNotesEditing = inlineNotesEditId == scan.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_item_${scan.id}")
                        .clickable {
                            expandedVehicleId = if (isExpanded) null else scan.id
                            inlineNotesEditId = null // clear notes editing when toggling expansion
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Captured image thumbnail loaded via Coil
                            if (!scan.imagePath.isNullOrBlank()) {
                                AsyncImage(
                                    model = scan.imagePath,
                                    contentDescription = "Foto de Placa",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            } else {
                                // Fallback visual placeholder
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Pin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Main Text & tags info
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = scan.plateText,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    if (!scan.country.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = scan.country.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    // STATUS BADGE
                                    val isActive = scan.status == "ACTIVE"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isActive) Color(0xFFE57373).copy(alpha = 0.15f)
                                                else Color(0xFF81C784).copy(alpha = 0.15f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isActive) Color(0xFFE57373)
                                                else Color(0xFF81C784),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isActive) "PARQUEADO" else "RETIRADO",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isActive) Color(0xFFD32F2F) else Color(0xFF388E3C)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val carBrand = scan.vehicleBrand ?: "Desconocido"
                                    val carColor = scan.vehicleColor ?: "Desconocido"
                                    
                                    Icon(
                                        imageVector = Icons.Filled.DirectionsCar,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$carBrand • $carColor",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Interactive Entry/Exit Counters Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val entryFormatted = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(scan.timestamp))
                                    Text(
                                        text = "Ingreso: $entryFormatted",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )

                                    if (scan.status == "ACTIVE") {
                                        val minutesElapsed = (System.currentTimeMillis() - scan.timestamp) / (1000 * 60)
                                        val hr = minutesElapsed / 60
                                        val mn = minutesElapsed % 60
                                        val durationLabel = if (hr > 0) "${hr}h ${mn}m" else "${mn}min"
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "⏱️ $durationLabel",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        val exitFormatted = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(scan.exitTime ?: 0L))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Salida: $exitFormatted",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                if (scan.status == "COMPLETED") {
                                    Text(
                                        text = "💵 Cobrado: $${String.format("%,.0f", scan.paidAmount ?: 0.0)} COP" + if (scan.hasTicket == true) " (Con Ticket)" else " (Sin Ticket)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            // Interactive edit/delete action tray based on selection collapse
                            IconButton(
                                onClick = {
                                    expandedVehicleId = if (isExpanded) null else scan.id
                                    inlineNotesEditId = null
                                }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Mostrar detalles",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Expanded Panel for details and editing notes
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(8.dp))

                                // Detailed specifications
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    HistoricalDetailRow(label = "Estado / Región:", value = scan.state ?: "No indicado")
                                    HistoricalDetailRow(label = "Confianza de Escaneo:", value = "${scan.confidence ?: 90}%")
                                    HistoricalDetailRow(label = "Ubicación de Foto:", value = scan.imagePath?.substringAfterLast("/") ?: "N/D")
                                    
                                    val isActive = scan.status == "ACTIVE"
                                    HistoricalDetailRow(
                                        label = "Estado actual:", 
                                        value = if (isActive) "🔴 EN EL PARQUEADERO (Activo)" else "🟢 RETIRADO (Salida Registrada)"
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // INTEGRITY OF PARKING MANAGEMENT & ACTIONS
                                if (scan.status == "ACTIVE") {
                                    Text(
                                        text = "⭐ Acciones de Cobro y Salida:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    Button(
                                        onClick = {
                                            checkoutVehicleTarget = scan
                                            checkoutHasTicket = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Payment, contentDescription = null, tint = Color.Black)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("DAR SALIDA / CALCULAR COBRO", fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }

                                    // Sandbox Simulate controls
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "🧪 Herramientas de Prueba (Simular tiempos de ingreso):",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp)
                                    ) {
                                        val now = System.currentTimeMillis()
                                        listOf(
                                            Pair("Hace 30 Min", now - 30 * 60 * 1000L),
                                            Pair("Hace 1 Hora", now - 60 * 60 * 1000L),
                                            Pair("Hace 2.5 Horas", now - 150 * 60 * 1000L),
                                            Pair("Hace 12 Horas", now - 12 * 60 * 60 * 1000L)
                                        ).forEach { (label, mockEntryTime) ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        viewModel.updateEntryTime(scan, mockEntryTime)
                                                        Toast.makeText(context, "Tiempo simulado actualizado: $label", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Notes box & Edit Trigger
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Anotaciones / Notas Administrativas:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (!isNotesEditing) {
                                        IconButton(
                                            onClick = {
                                                inlineNotesEditId = scan.id
                                                editedNotesValue = ""
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Editar anotación",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                if (isNotesEditing) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editedNotesValue,
                                            onValueChange = { editedNotesValue = it },
                                            placeholder = { Text("ej. Vehículo de carga, residente, libre pase...") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("notes_edit_input_${scan.id}"),
                                            textStyle = TextStyle(fontSize = 12.sp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { inlineNotesEditId = null }) {
                                                Text("Cancelar", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.updateNotes(scan, editedNotesValue)
                                                    inlineNotesEditId = null
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                            ) {
                                                Text("Guardar", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                } else {
                                    NotesFeedView(notesStr = scan.notes)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick Delete button for specific item
                                Button(
                                    onClick = {
                                        viewModel.deleteScan(scan)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .testTag("delete_item_button_${scan.id}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Eliminar de Historial", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    } // Closes the if (false) bypass block

    // Modal Confirmation Dialog to clear all scans
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Vaciar Historial", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro/a de que deseas borrar permanentemente todos los registros escaneados del historial? Esta acción es irreversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                        Toast.makeText(context, "Historial vaciado correctamente.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Eliminar Todo", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = CardDefaults.cardColors().containerColor
        )
    }

    // Manual Registration Dialog Prompt
    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AddRoad,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Registro Manual de Vehículo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Ingrese los datos del vehículo para registrar su hora de ingreso al parqueadero hoy.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualPlate,
                        onValueChange = { manualPlate = it.uppercase().take(15) },
                        label = { Text("Matrícula/Placa *") },
                        placeholder = { Text("Ej. LKM 519") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_entry_plate_field")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var countryDropdownExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { countryDropdownExpanded = true }
                        ) {
                            OutlinedTextField(
                                value = manualCountry,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("País") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Desplegar países"
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_entry_country_field")
                            )
                            DropdownMenu(
                                expanded = countryDropdownExpanded,
                                onDismissRequest = { countryDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Colombia 🇨🇴") },
                                    onClick = {
                                        manualCountry = "Colombia"
                                        manualState = "Bogotá D.C."
                                        countryDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Venezuela 🇻🇪") },
                                    onClick = {
                                        manualCountry = "Venezuela"
                                        manualState = "Caracas"
                                        countryDropdownExpanded = false
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = manualState,
                            onValueChange = { manualState = it.take(20) },
                            label = { Text("Sede/Ciudad") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_entry_state_field")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualBrand,
                            onValueChange = { manualBrand = it.take(20) },
                            label = { Text("Marca") },
                            placeholder = { Text("Ej. Mazda") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_entry_brand_field")
                        )

                        OutlinedTextField(
                            value = manualColor,
                            onValueChange = { manualColor = it.take(20) },
                            label = { Text("Color") },
                            placeholder = { Text("Ej. Gris") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_entry_color_field")
                        )
                    }

                    OutlinedTextField(
                        value = manualNotes,
                        onValueChange = { manualNotes = it },
                        label = { Text("Observaciones / Notas") },
                        placeholder = { Text("Ej. Guardafango rayado, casco azul...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_entry_notes_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualPlate.trim().isBlank()) {
                            Toast.makeText(context, "Por favor ingrese la placa del vehículo.", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.registerManualPlate(
                                plateText = manualPlate,
                                country = manualCountry,
                                state = manualState,
                                vehicleBrand = manualBrand,
                                vehicleColor = manualColor,
                                notes = manualNotes
                            )
                            showManualDialog = false
                            Toast.makeText(context, "Vehículo registrado exitosamente.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Registrar Ingreso", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = CardDefaults.cardColors().containerColor
        )
    }

    if (checkoutVehicleTarget != null) {
        val vehicle = checkoutVehicleTarget!!
        val currentMs = System.currentTimeMillis()
        val entryTime = vehicle.timestamp
        val durationMs = currentMs - entryTime
        val durationMinutes = durationMs / (1000 * 60)
        
        val calculatedAmount = viewModel.calculateParkingFee(entryTime, currentMs, checkoutHasTicket)
        
        AlertDialog(
            onDismissRequest = { checkoutVehicleTarget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cobro y Registro de Salida", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Registrar salida del vehículo: ${vehicle.plateText}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    
                    if (!vehicle.imagePath.isNullOrBlank()) {
                        AsyncImage(
                            model = vehicle.imagePath,
                            contentDescription = "Foto de Placa",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hora Entrada:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(entryTime)),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tiempo Transcurrido:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val hr = durationMinutes / 60
                        val mn = durationMinutes % 60
                        Text(
                            text = if (hr > 0) "${hr}h ${mn}m" else "${mn}min",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Toggle between "Con Ticket" and "Sin Ticket"
                    Text("Tipo de Cobro:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { checkoutHasTicket = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (checkoutHasTicket) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (checkoutHasTicket) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Con Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { checkoutHasTicket = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!checkoutHasTicket) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!checkoutHasTicket) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sin Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL A PAGAR:", fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(
                            "$${String.format("%,.0f", calculatedAmount)} COP",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    // Details breakdown info
                    val appliedHourRate = if (checkoutHasTicket) rateWithTicketHour else rateNoTicketHour
                    val appliedFractionRate = if (checkoutHasTicket) rateWithTicketFraction else rateNoTicketFraction
                    Text(
                        text = "Tarifa parametrizada: $appliedHourRate COP / hora y $appliedFractionRate COP / media hora.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.checkoutVehicle(vehicle, checkoutHasTicket)
                        checkoutVehicleTarget = null
                        Toast.makeText(context, "Cobro registrado de $${String.format("%,.0f", calculatedAmount)} COP. Vehículo retirado.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Cobrar", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { checkoutVehicleTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun DetailItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HistoricalDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit,
    onError: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    LaunchedEffect(cameraProviderFuture, lifecycleOwner) {
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            onError(e)
        }
    }

    DisposableEffect(lifecycleOwner, cameraProviderFuture) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                // Ignore any exceptions during unbind on disposal
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Close/Turn Off Camera Button to reduce battery usage
        FilledTonalButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .height(36.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Desactivar cámara",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Apagar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Overlay focusing bracket via pure Compose DrawScope Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bracketWidth = w * 0.70f
            val bracketHeight = h * 0.35f
            val left = (w - bracketWidth) / 2f
            val top = (h - bracketHeight) / 2f
            val right = left + bracketWidth
            val bottom = top + bracketHeight

            val strokeWidthPx = 4.dp.toPx()
            val len = 24.dp.toPx()
            val primaryColor = Color(0xFFD0BCFF) // Geometric Balance Lavender accent

            // Top Left corner bracket
            drawLine(color = primaryColor, start = Offset(left, top), end = Offset(left + len, top), strokeWidth = strokeWidthPx)
            drawLine(color = primaryColor, start = Offset(left, top), end = Offset(left, top + len), strokeWidth = strokeWidthPx)
            
            // Top Right corner bracket
            drawLine(color = primaryColor, start = Offset(right, top), end = Offset(right - len, top), strokeWidth = strokeWidthPx)
            drawLine(color = primaryColor, start = Offset(right, top), end = Offset(right, top + len), strokeWidth = strokeWidthPx)
            
            // Bottom Left corner bracket
            drawLine(color = primaryColor, start = Offset(left, bottom), end = Offset(left + len, bottom), strokeWidth = strokeWidthPx)
            drawLine(color = primaryColor, start = Offset(left, bottom), end = Offset(left, bottom - len), strokeWidth = strokeWidthPx)
            
            // Bottom Right corner bracket
            drawLine(color = primaryColor, start = Offset(right, bottom), end = Offset(right - len, bottom), strokeWidth = strokeWidthPx)
            drawLine(color = primaryColor, start = Offset(right, bottom), end = Offset(right, bottom - len), strokeWidth = strokeWidthPx)
        }

        // Floating Action Button to shutter/capture
        FloatingActionButton(
            onClick = {
                imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val bitmap = image.toRotatedBitmap()
                            image.close()
                            if (bitmap != null) {
                                onBitmapCaptured(bitmap)
                            } else {
                                onError(NullPointerException("Se produjo un error al procesar los pixeles de la foto."))
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            onError(exception)
                        }
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .testTag("shutter_button"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Capturar foto de placa",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceRegistrationView(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputUrl by remember { mutableStateOf(viewModel.apiBaseUrl.value) }
    var inputUsername by remember { mutableStateOf("") }
    var inputMasterKey by remember { mutableStateOf("") }

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val syncStatusError by viewModel.syncStatusError.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aesthetic Top Logo / Illustration
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Configuración del Móvil",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Vincula este aplicativo de escáner con tu backend Express/Node para sincronizar tarifas, comentarios y reportes financieros.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Form Fields
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🔑 Credenciales de la Cuenta móvil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // API Base URL Field
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text("Base URL de la API Web") },
                    placeholder = { Text("http://10.0.2.2:3000/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_api_url_input"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(Icons.Filled.Link, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Username/Operator Field
                OutlinedTextField(
                    value = inputUsername,
                    onValueChange = { inputUsername = it },
                    label = { Text("Nombre de Operador / Usuario") },
                    placeholder = { Text("ej. admin_parqueadero") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_username_input"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                // Master Admin Password Key Field
                OutlinedTextField(
                    value = inputMasterKey,
                    onValueChange = { inputMasterKey = it },
                    label = { Text("Contraseña de Registro (Admin Key)") },
                    placeholder = { Text("Contraseña para el backend") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_password_input"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    leadingIcon = {
                        Icon(Icons.Filled.Key, contentDescription = null)
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Automatically Extracted Metadata Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📡 Datos del Móvil (Enviados por debajo):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Marca/Modelo:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(text = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sistema Operativo:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(text = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status messages
        AnimatedVisibility(visible = syncStatusMessage.isNotEmpty() || syncStatusError) {
            val statusColor = if (syncStatusError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            Surface(
                color = statusColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (syncStatusError) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = syncStatusMessage,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Action Buttons
        Button(
            onClick = {
                viewModel.updateApiBaseUrl(inputUrl)
                viewModel.registerDeviceAccount(inputUsername, inputMasterKey, simulateIfServerOffline = false)
            },
            enabled = !isSyncing && inputUsername.isNotBlank() && inputMasterKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("setup_register_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Registrar y Conectar por API Real", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                viewModel.updateApiBaseUrl(inputUrl)
                viewModel.registerDeviceAccount(inputUsername, inputMasterKey, simulateIfServerOffline = true)
            },
            enabled = !isSyncing && inputUsername.isNotBlank() && inputMasterKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("setup_simulate_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.CloudQueue, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Demostración Offline (Simular Registro)", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun UpdateCheckerDialog(
    viewModel: ScanViewModel
) {
    val showDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateAvailable.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (showDialog && updateInfo != null) {
        val info = updateInfo!!
        val apkAsset = info.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) } ?: info.assets.firstOrNull()
        val downloadUrl = apkAsset?.browserDownloadUrl ?: "https://github.com/usuario/repo/releases"

        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Actualización Disponible 🚀",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Una nueva versión de la aplicación de escaneo ha sido detectada:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Versión: ${info.tagName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = info.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (apkAsset != null) {
                                Text(
                                    text = "Archivo: ${apkAsset.name}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Text(
                        text = "¿Deseas descargar el instalador APK de la actualización ahora?",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissUpdateDialog()
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(downloadUrl)
                            ).apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "No se pudo abrir el navegador de descargas.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Descargar APK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissUpdateDialog() }
                ) {
                    Text("Más tarde")
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("github_update_dialog")
        )
    }
}

enum class ScreenSection {
    CAR_RECORDS,
    SYNCHRONIZATION,
    RATES,
    SIMULATION
}

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun AutoRecordsView(
    viewModel: ScanViewModel,
    context: android.content.Context,
    cameraPermissionState: com.google.accompanist.permissions.PermissionState,
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    isCameraActive: Boolean,
    setCameraActive: (Boolean) -> Unit,
    editedPlateText: String,
    setEditedPlateText: (String) -> Unit,
    groupedHistory: Map<String, List<ScanResult>>,
    scanHistory: List<ScanResult>,
    searchQuery: String,
    onClearHistoryClick: () -> Unit,
    onManualClick: () -> Unit,
    expandedVehicleId: Long?,
    setExpandedVehicleId: (Long?) -> Unit,
    inlineNotesEditId: Long?,
    setInlineNotesEditId: (Long?) -> Unit,
    editedNotesValue: String,
    setEditedNotesValue: (String) -> Unit,
    onCheckoutClick: (ScanResult) -> Unit
) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Operational Metrics Cards Row
        item {
            val activeCount = scanHistory.count { it.status == "ACTIVE" }
            val completedCount = scanHistory.count { it.status == "COMPLETED" }
            val totalRevenue = scanHistory.sumOf { it.paidAmount ?: 0.0 }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vehículos Adentro
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "En Parqueadero",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$activeCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Salidas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Salidas Hoy",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$completedCount",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Ingreso Total
                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Payment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Total Recaudado (App)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "$${String.format("%,.0f", totalRevenue)} COP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Unified Entry Interface (Cámara / Galería Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isCameraActive) 310.dp else 190.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isCameraActive) {
                        if (cameraPermissionState.status.isGranted) {
                            var cameraError by remember { mutableStateOf<String?>(null) }
                            if (cameraError == null) {
                                CameraPreview(
                                    modifier = Modifier.fillMaxSize(),
                                    onClose = { setCameraActive(false) },
                                    onBitmapCaptured = { bitmap ->
                                        viewModel.processImage(bitmap, context)
                                    },
                                    onError = { err ->
                                        cameraError = err.localizedMessage ?: "Error de cámara desconocido"
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Outlined.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Error al iniciar cámara:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = cameraError!!,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 12.dp)
                                        ) {
                                            Button(onClick = { cameraError = null }) {
                                                Text("Reintentar", fontSize = 11.sp)
                                            }
                                            OutlinedButton(onClick = { setCameraActive(false) }) {
                                                Text("Cerrar", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoCamera,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Permiso Requerido",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Para activar la cámara del escáner en vivo, concede el permiso.",
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                                            Text("Permitir", fontSize = 11.sp)
                                        }
                                        OutlinedButton(onClick = { setCameraActive(false) }) {
                                            Text("Cancelar", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Camera inactive: unified clean display
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = "Escáner Listo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Reconocimiento de Placas local",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Automático on-device para Colombia y Venezuela.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { setCameraActive(true) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("activate_camera_button")
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cámara En Vivo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).testTag("select_gallery_button")
                                    ) {
                                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Elegir de Galería", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manual Input Action Button
        item {
            Button(
                onClick = onManualClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .testTag("open_manual_entry_dialog_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ingresar Placa Manualmente", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Floating Processing Loader
        if (scanState is ScanUiState.Processing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Analizando matrícula...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live Captured Scanned Result Card (Success/Error)
        item {
            AnimatedVisibility(
                visible = scanState !is ScanUiState.Idle && scanState !is ScanUiState.Processing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                when (val state = scanState) {
                    is ScanUiState.Success -> {
                        val details = state.info
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_success_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Resultado de Lectura",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Confianza: ${details.confidence ?: 90}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Realistic license plate mockup
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                        .padding(vertical = 10.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF0F4C81))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = Color.Yellow,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = (details.country?.take(2) ?: "ES").uppercase(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Text(
                                            text = editedPlateText.uppercase(),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            letterSpacing = 1.5.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = editedPlateText,
                                    onValueChange = { newValue ->
                                        val uppercaseValue = newValue.uppercase()
                                        setEditedPlateText(uppercaseValue)
                                        viewModel.updateLatestScanPlateText(uppercaseValue)
                                    },
                                    label = { Text("Corregir Matrícula Detectada", fontWeight = FontWeight.Bold) },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("verify_plate_input_field")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        DetailItem(label = "País", value = details.country ?: "No identificado", modifier = Modifier.weight(1f), icon = Icons.Filled.Public)
                                        DetailItem(label = "Estado", value = details.state ?: "No identificado", modifier = Modifier.weight(1f), icon = Icons.Filled.Map)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        DetailItem(label = "Marca", value = details.vehicleBrand ?: "No identificada", modifier = Modifier.weight(1f), icon = Icons.Filled.DirectionsCar)
                                        DetailItem(label = "Color", value = details.vehicleColor ?: "No identificado", modifier = Modifier.weight(1f), icon = Icons.Filled.Palette)
                                    }
                                }

                                if (!details.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = details.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.dismissScanResult() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Descartar")
                                    }
                                    Button(
                                        onClick = { viewModel.dismissScanResult() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Confirmar", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }
                    is ScanUiState.Error -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scan_error_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("No se pudo completar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = state.message, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { viewModel.dismissScanResult() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Entendido")
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Section Title: Scanned History Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ListAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historial de Escaneos (${scanHistory.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (scanHistory.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistoryClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_history_button")
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Borrar historial", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // Filter search input bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Buscar por matrícula, país, color, marca...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_history_input"),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Grouped Scrolling Entries
        if (groupedHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Aún no hay códigos escaneados" else "No se encontraron coincidencias",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "Las matrículas detectadas por la cámara se registrarán automáticamente aquí." else "Intenta buscar usando otras palabras clave.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            groupedHistory.forEach { (dateHeader, scansInDay) ->
                item {
                    Text(
                        text = dateHeader,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(scansInDay, key = { it.id }) { scan ->
                    var itemNotesValue by remember(scan.notes) { mutableStateOf(scan.notes ?: "") }
                    val isExpanded = expandedVehicleId == scan.id
                    val isEditingNotes = inlineNotesEditId == scan.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("scan_record_card_${scan.plateText}")
                            .clickable { setExpandedVehicleId(if (isExpanded) null else scan.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (scan.status == "ACTIVE") {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ),
                        border = if (scan.status == "ACTIVE") BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Vehicle photo thumbnail in front of the plate badge
                                    if (!scan.imagePath.isNullOrBlank()) {
                                        AsyncImage(
                                            model = scan.imagePath,
                                            contentDescription = "Foto de Placa",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DirectionsCar,
                                                contentDescription = "Sin foto",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Metal style plate badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = scan.plateText.uppercase(),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = scan.country ?: "Colombia",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!scan.state.isNullOrEmpty()) {
                                                Text(
                                                    text = " (${scan.state})",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        val entryFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(scan.timestamp))
                                        val exitFormat = scan.exitTime?.let { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(it)) }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (scan.isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudQueue,
                                                contentDescription = if (scan.isSynced) "Sincronizado con la nube" else "Pendiente de sincronizar",
                                                tint = if (scan.isSynced) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = if (scan.isSynced) "Sincronizado" else "No Sincronizado",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (scan.isSynced) Color(0xFF2E7D32) else Color(0xFFE65100)
                                            )
                                        }

                                        Text(
                                            text = "Entrada: $entryFormat",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        if (exitFormat != null) {
                                            Text(
                                                text = "Salida: $exitFormat",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (scan.paidAmount != null && scan.paidAmount > 0.0) {
                                            Text(
                                                text = "Ganado: $${String.format("%,.0f", scan.paidAmount)} COP",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }

                                if (scan.status == "ACTIVE") {
                                    // Direct action button: Cobrar / Give Departure without opening details
                                    Button(
                                        onClick = { onCheckoutClick(scan) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("direct_checkout_${scan.plateText}"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ExitToApp,
                                                contentDescription = "Dar salida",
                                                tint = Color.Black,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Cobrar",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                } else {
                                    // Completed status tag
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Gray)
                                            )
                                            Text(
                                                text = "RETIRADO",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Expanded Section Area
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), modifier = Modifier.padding(bottom = 8.dp))

                                    HistoricalDetailRow(label = "Marca del Vehículo", value = scan.vehicleBrand ?: "No especificado")
                                    HistoricalDetailRow(label = "Color", value = scan.vehicleColor ?: "No especificado")

                                    val entryTimeFull = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(scan.timestamp))
                                    HistoricalDetailRow(label = "Fecha de Entrada", value = entryTimeFull)

                                    if (scan.status == "COMPLETED" && scan.exitTime != null) {
                                        val exitTimeFull = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(scan.exitTime))
                                        HistoricalDetailRow(label = "Fecha de Salida", value = exitTimeFull)

                                        val durationMin = (scan.exitTime - scan.timestamp) / (1000 * 60)
                                        val hours = durationMin / 60
                                        val mins = durationMin % 60
                                        HistoricalDetailRow(
                                            label = "Tiempo Total",
                                            value = if (hours > 0) "${hours}h ${mins}m" else "${mins}min"
                                        )

                                        HistoricalDetailRow(
                                            label = "Monto Cobrado",
                                            value = "$${String.format("%,.0f", scan.paidAmount ?: 0.0)} COP"
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Dynamic inline notes editor
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("Notas / Observaciones del Operador:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                        if (isEditingNotes) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = itemNotesValue,
                                                    onValueChange = { itemNotesValue = it },
                                                    modifier = Modifier.weight(1f).testTag("inline_notes_input"),
                                                    textStyle = TextStyle(fontSize = 11.sp),
                                                    singleLine = true
                                                )
                                                IconButton(
                                                    onClick = {
                                                        viewModel.updateNotes(scan, itemNotesValue)
                                                        setInlineNotesEditId(null)
                                                        Toast.makeText(context, "Nota actualizada.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(36.dp).testTag("save_notes_btn")
                                                ) {
                                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    NotesFeedView(notesStr = scan.notes)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        setInlineNotesEditId(scan.id)
                                                        itemNotesValue = "" // start with empty string to enter new note!
                                                    },
                                                    modifier = Modifier.size(32.dp).testTag("edit_notes_btn")
                                                ) {
                                                    Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    // Action buttons for expanded item (Departure - No simulation controls here!)
                                    if (scan.status == "ACTIVE") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onCheckoutClick(scan) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.fillMaxWidth().testTag("checkout_action_btn_${scan.plateText}"),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(4.dp)
                                            ) {
                                                Icon(Icons.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Dar Salida (Cobrar)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                    } else {
                                        // Delete single row from SQLite Database
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                viewModel.deleteScan(scan)
                                                Toast.makeText(context, "Registro eliminado permanentemente del dispositivo.", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("delete_single_scan_btn"),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Eliminar de Historial", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
    }
}

@Composable
fun SynchronizationView(
    viewModel: ScanViewModel,
    context: android.content.Context
) {
    val registeredUsername by viewModel.registeredUsername.collectAsStateWithLifecycle()
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val syncStatusError by viewModel.syncStatusError.collectAsStateWithLifecycle()

    var inputUrl by remember { mutableStateOf(apiBaseUrl) }
    var inputUsername by remember { mutableStateOf(registeredUsername) }
    var inputMasterKey by remember { mutableStateOf("") }

    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Connection Status Card Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (registeredUsername.isNotEmpty()) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (registeredUsername.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFFF44336))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (registeredUsername.isNotEmpty()) "Vinculado" else "Desconectado",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (registeredUsername.isNotEmpty()) {
                            Text(
                                text = "Desvincular",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { viewModel.unregisterDevice() }
                                    .testTag("unlink_device_button")
                            )
                        }
                    }

                    if (registeredUsername.isNotEmpty()) {
                        Text(
                            text = "Dispositivo enlazado para sincronizar de manera remota al operador $registeredUsername.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "URL Base: $apiBaseUrl",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Este dispositivo opera de forma autónoma. Configure el backend a continuación para respaldar y reportar datos.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Integration Synchronization Box
        if (registeredUsername.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_panel_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Sincronizador Web REST",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${scanHistory.size} registros locales disponibles",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { viewModel.syncWithBackend() },
                                enabled = !isSyncing,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("sync_now_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sincronizar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Status notification details
                        AnimatedVisibility(visible = syncStatusMessage.isNotEmpty() || syncStatusError) {
                            val badgeColor = if (syncStatusError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            Surface(
                                color = badgeColor.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (syncStatusError) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = syncStatusMessage,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Un solo toque consolida los turnos del operador en el panel central de reportes, actualiza montos online y respalda notas.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }

        // 3. Pairing Credentials Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "🔑 Registrar u Operar Dispositivo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = { Text("Base URL de la API Web") },
                        placeholder = { Text("http://10.0.2.2:3000/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_api_url_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = inputUsername,
                        onValueChange = { inputUsername = it },
                        label = { Text("Nombre de Operador / Usuario") },
                        placeholder = { Text("ej. admin_parqueadero") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_username_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = inputMasterKey,
                        onValueChange = { inputMasterKey = it },
                        label = { Text("Contraseña de Registro (Admin Key)") },
                        placeholder = { Text("Contraseña para el backend") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_password_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateApiBaseUrl(inputUrl)
                                viewModel.registerDeviceAccount(inputUsername, inputMasterKey, simulateIfServerOffline = false)
                            },
                            enabled = !isSyncing && inputUsername.isNotBlank() && inputMasterKey.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("setup_register_button")
                        ) {
                            Text("Enlazar API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.updateApiBaseUrl(inputUrl)
                                viewModel.registerDeviceAccount(inputUsername, inputMasterKey, simulateIfServerOffline = true)
                            },
                            enabled = !isSyncing && inputUsername.isNotBlank() && inputMasterKey.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("setup_simulate_button")
                        ) {
                            Icon(Icons.Filled.CloudQueue, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simular Enlace", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 4. GitHub Software Update Checker Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🔄 Actualizaciones del Sistema",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val githubReleaseUrl by viewModel.githubReleaseUrl.collectAsStateWithLifecycle()
                    var inputGithubReleaseUrl by remember(githubReleaseUrl) { mutableStateOf(githubReleaseUrl) }

                    OutlinedTextField(
                        value = inputGithubReleaseUrl,
                        onValueChange = { inputGithubReleaseUrl = it },
                        label = { Text("URL de la API de GitHub para Updates") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("setup_github_release_url_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateGithubReleaseUrl(inputGithubReleaseUrl)
                                viewModel.checkForUpdates(currentVersion = "1.0", forceShowSimulated = false)
                                Toast.makeText(context, "Consultando actualizaciones en GitHub...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("check_github_updates_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Buscar Ahora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.checkForUpdates(currentVersion = "1.0", forceShowSimulated = true)
                            },
                            modifier = Modifier.weight(1f).testTag("simulate_github_updates_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simular (Demo)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Hardware Info details panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📡 Datos del Móvil (Enviados por debajo):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Marca/Modelo:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Text(text = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sistema Operativo:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Text(text = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RatesView(
    viewModel: ScanViewModel,
    context: android.content.Context,
    editWithHour: String,
    setEditWithHour: (String) -> Unit,
    editWithFraction: String,
    setEditWithFraction: (String) -> Unit,
    editNoHour: String,
    setEditNoHour: (String) -> Unit,
    editNoFraction: String,
    setEditNoFraction: (String) -> Unit,
    rateWithTicketHour: Int,
    rateWithTicketFraction: Int,
    rateNoTicketHour: Int,
    rateNoTicketFraction: Int,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    // Pricing simulator local states
    var simDurationMin by remember { mutableStateOf(45) }
    var simHasTicket by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Header description
        item {
            Text(
                text = "Configure y compruebe los costos aplicados por hora y fracción de media hora de estacionamiento.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // TARIFA CON TICKET CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🎟️ Tarifas CON TICKET (Lectura Código de Barra/Físico)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editWithHour,
                            onValueChange = { setEditWithHour(it.filter { c -> c.isDigit() }) },
                            label = { Text("Valor Hora Completa", fontSize = 11.sp) },
                            prefix = { Text("$ ", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editWithFraction,
                            onValueChange = { setEditWithFraction(it.filter { c -> c.isDigit() }) },
                            label = { Text("Valor Media Hora (Frac)", fontSize = 11.sp) },
                            prefix = { Text("$ ", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // TARIFA SIN TICKET CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "✏️ Tarifas SIN TICKET (Ingreso Manual)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editNoHour,
                            onValueChange = { setEditNoHour(it.filter { c -> c.isDigit() }) },
                            label = { Text("Valor Hora Completa", fontSize = 11.sp) },
                            prefix = { Text("$ ", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editNoFraction,
                            onValueChange = { setEditNoFraction(it.filter { c -> c.isDigit() }) },
                            label = { Text("Valor Media Hora (Frac)", fontSize = 11.sp) },
                            prefix = { Text("$ ", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Action Guardar Tarifas Button
        item {
            Button(
                onClick = {
                    val wh = editWithHour.toIntOrNull() ?: 2000
                    val wf = editWithFraction.toIntOrNull() ?: 1000
                    val nh = editNoHour.toIntOrNull() ?: 4000
                    val nf = editNoFraction.toIntOrNull() ?: 2000
                    viewModel.updateRates(wh, wf, nh, nf)
                    keyboardController?.hide()
                    Toast.makeText(context, "Tarifas actualizadas localmente.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar Tarifas", fontWeight = FontWeight.Bold)
            }
        }

        // Space divider
        item {
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
        }

        // INTERACTIVE PRICING SIMULATOR
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calculadora y Simulador de Tarifas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = "Compruebe los cálculos de cobro sin necesidad de alterar datos reales. Ajuste el control para cambiar el tiempo transcurrido.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )

                    // Slider for duration minutes
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tiempo de estadía:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val displayH = simDurationMin / 60
                            val displayM = simDurationMin % 60
                            Text(
                                text = if (displayH > 0) "${displayH}h ${displayM}m ($simDurationMin min)" else "$simDurationMin min",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = simDurationMin.toFloat(),
                            onValueChange = { simDurationMin = it.toInt() },
                            valueRange = 5f..1440f, // From 5 mins up to 24 hours (1440 min)
                            steps = 0,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Ticket type select buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { simHasTicket = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (simHasTicket) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (simHasTicket) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Con Ticket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { simHasTicket = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!simHasTicket) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!simHasTicket) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sin Ticket (Manual)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Simulated Fee output rendering
                    val entryTimeSim = System.currentTimeMillis() - (simDurationMin * 60 * 1000L)
                    val simFeeResult = viewModel.calculateParkingFee(entryTimeSim, System.currentTimeMillis(), simHasTicket)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Valor Simulado:", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            text = "$${String.format("%,.0f", simFeeResult)} COP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Breakdown explanation
                    val applyHVal = if (simHasTicket) rateWithTicketHour else rateNoTicketHour
                    val applyFVal = if (simHasTicket) rateWithTicketFraction else rateNoTicketFraction
                    Text(
                        text = "Ecuación aplicada: $applyHVal COP por hora y $applyFVal COP por fracción de 30 mins.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SimulationView(
    viewModel: ScanViewModel,
    context: android.content.Context,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    // Leftovers/Presets and values for vehicle entry simulation
    var simPlateText by remember { mutableStateOf("") }
    var simCountry by remember { mutableStateOf("Colombia") }
    var simState by remember { mutableStateOf("Bogotá D.C.") }

    val presets = listOf(
        Triple("ABC 123", "Colombia", "Bogotá D.C."),
        Triple("MNO 987", "Venezuela", "Caracas"),
        Triple("COL 456", "Colombia", "Medellín"),
        Triple("VEN 789", "Venezuela", "Valencia"),
        Triple("TAX 111", "Colombia", "Cali")
    )

    // Pricing simulator local states
    var simDurationMin by remember { mutableStateOf(45) }
    var simHasTicket by remember { mutableStateOf(true) }

    // Rates values from VM state
    val rateWithTicketHour = viewModel.rateWithTicketHour.collectAsStateWithLifecycle().value
    val rateWithTicketFraction = viewModel.rateWithTicketFraction.collectAsStateWithLifecycle().value
    val rateNoTicketHour = viewModel.rateNoTicketHour.collectAsStateWithLifecycle().value
    val rateNoTicketFraction = viewModel.rateNoTicketFraction.collectAsStateWithLifecycle().value

    // Active cars state for time backdating simulation
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val activeCars = scanHistory.filter { it.status == "ACTIVE" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Entorno de Pruebas y Simulación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Agregue datos de prueba y realice simulaciones de tarifas de forma aislada sin afectar la operación real.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 1. Simular Entrada de Vehículo (Colombia / Venezuela)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "🚗 Simular Entrada de Vehículo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = simPlateText,
                        onValueChange = { simPlateText = it.uppercase().take(15) },
                        label = { Text("Matrícula (Ej. LKM 519)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sim_plate_text"),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var simCountryDropdownExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { simCountryDropdownExpanded = true }
                        ) {
                            OutlinedTextField(
                                value = simCountry,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("País") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Desplegar países"
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_sim_country")
                            )
                            DropdownMenu(
                                expanded = simCountryDropdownExpanded,
                                onDismissRequest = { simCountryDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Colombia 🇨🇴") },
                                    onClick = {
                                        simCountry = "Colombia"
                                        simState = "Bogotá D.C."
                                        simCountryDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Venezuela 🇻🇪") },
                                    onClick = {
                                        simCountry = "Venezuela"
                                        simState = "Caracas"
                                        simCountryDropdownExpanded = false
                                    }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = simState,
                            onValueChange = { simState = it.take(20) },
                            label = { Text("Estado/Ciudad") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_sim_state"),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                if (simPlateText.isNotBlank()) {
                                    viewModel.generateSimulatedPlateAndScan(simPlateText, simCountry, simState, context)
                                    simPlateText = ""
                                } else {
                                    Toast.makeText(context, "Por favor ingrese una matrícula.", Toast.LENGTH_SHORT).show()
                                }
                            })
                        )
                    }

                    // Quick Presets Slider
                    Column {
                        Text("Elegir preajuste rápido:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            simPlateText = preset.first
                                            simCountry = preset.second
                                            simState = preset.third
                                            viewModel.generateSimulatedPlateAndScan(preset.first, preset.second, preset.third, context)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(preset.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (simPlateText.isNotBlank()) {
                                viewModel.generateSimulatedPlateAndScan(simPlateText, simCountry, simState, context)
                                simPlateText = ""
                            } else {
                                Toast.makeText(context, "Por favor ingrese una matrícula.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_scan_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FlashOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Registrar Ingreso de Vehículo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. Simulador de Tiempo y Alteración de Registros Activos (Altering entry times)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "⏱️ Alterar Tiempo de Registros Activos (Pruebas)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Seleccione un auto que se encuentre actualmente en el parqueadero para simular que ingresó hace varias horas. Esto le permite probar cálculos de tarifas prolongadas.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )

                    if (activeCars.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No hay autos activos en el parqueadero actualmente.", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        var selectedCarId by remember { mutableStateOf<Long?>(null) }
                        var timeBackHours by remember { mutableStateOf(2) }

                        val selectedCar = activeCars.find { it.id == selectedCarId } ?: activeCars.first()
                        if (selectedCarId == null || activeCars.none { it.id == selectedCarId }) {
                            selectedCarId = selectedCar.id
                        }

                        var dropdownExpanded by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1.3f)) {
                                OutlinedTextField(
                                    value = "Placa: ${selectedCar.plateText.uppercase()}",
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = false,
                                    label = { Text("Vehículo Activo") },
                                    trailingIcon = {
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { dropdownExpanded = true })
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    activeCars.forEach { active ->
                                        DropdownMenuItem(
                                            text = { Text("Placa: ${active.plateText.uppercase()} (${active.vehicleBrand ?: "Sin Marca"})") },
                                            onClick = {
                                                selectedCarId = active.id
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = "Retroceder $timeBackHours h",
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Horas atrás") },
                                trailingIcon = {
                                    var hoursDropdownExpanded by remember { mutableStateOf(false) }
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.clickable { hoursDropdownExpanded = true })
                                    DropdownMenu(
                                        expanded = hoursDropdownExpanded,
                                        onDismissRequest = { hoursDropdownExpanded = false }
                                    ) {
                                        (1..24).forEach { h ->
                                            DropdownMenuItem(
                                                text = { Text("$h horas") },
                                                onClick = {
                                                    timeBackHours = h
                                                    hoursDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                val currentEntryTime = selectedCar.timestamp
                                val updatedTime = currentEntryTime - (timeBackHours * 3600 * 1000L)
                                viewModel.updateEntryTime(selectedCar, updatedTime)
                                Toast.makeText(context, "Hora de ingreso para ${selectedCar.plateText.uppercase()} modificada con éxito.", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Modificar Hora de Ingreso", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // 3. Calculadora y Simulador de Tarifas
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "💰 Calculadora de Cobros (Simulador)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = "Calcule costos exactos arrastrando los tiempos transcurridos sin alterar ningún registro de la base de datos.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )

                    // Slider for duration minutes
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tiempo transcurrido en el lote:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val displayH = simDurationMin / 60
                            val displayM = simDurationMin % 60
                            Text(
                                text = if (displayH > 0) "${displayH}h ${displayM}m ($simDurationMin min)" else "$simDurationMin min",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = simDurationMin.toFloat(),
                            onValueChange = { simDurationMin = it.toInt() },
                            valueRange = 5f..1440f, // From 5 mins up to 24 hours
                            steps = 0,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Ticket type select buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { simHasTicket = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (simHasTicket) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (simHasTicket) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Con Ticket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { simHasTicket = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!simHasTicket) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!simHasTicket) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sin Ticket (Manual)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Simulated Fee output rendering
                    val entryTimeSim = System.currentTimeMillis() - (simDurationMin * 60 * 1000L)
                    val simFeeResult = viewModel.calculateParkingFee(entryTimeSim, System.currentTimeMillis(), simHasTicket)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Valor Estimado:", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            text = "$${String.format("%,.0f", simFeeResult)} COP",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val applyHVal = if (simHasTicket) rateWithTicketHour else rateNoTicketHour
                    val applyFVal = if (simHasTicket) rateWithTicketFraction else rateNoTicketFraction
                    Text(
                        text = "Ecuación aplicada: $applyHVal COP por hora y $applyFVal COP por fracción de 30 mins.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NotesFeedView(notesStr: String?, modifier: Modifier = Modifier) {
    val comments = remember(notesStr) { NotesParser.parse(notesStr) }
    if (comments.isEmpty()) {
        Text(
            text = "Sin anotaciones añadidas. Haz clic en el ícono de edición para agregar detalles.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            modifier = modifier.padding(vertical = 4.dp)
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            comments.forEach { comment ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "👤 " + comment.authorName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val dateStr = remember(comment.timestamp) {
                                val sdf = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(comment.timestamp))
                            }
                            Text(
                                text = dateStr,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = comment.message,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

