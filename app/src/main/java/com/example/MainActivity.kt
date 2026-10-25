package com.example

import android.Manifest
import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.components.UpdateCheckerDialog
import com.example.ui.screens.AutoRecordsView
import com.example.ui.screens.DeviceRegistrationView
import com.example.ui.screens.RatesView
import com.example.ui.screens.SimulationView
import com.example.ui.screens.SynchronizationView
import com.example.ui.screens.TeamView
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

enum class ScreenSection {
    CAR_RECORDS,
    TEAM,
    SYNCHRONIZATION,
    RATES,
    SIMULATION
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
    val remoteConfig by viewModel.remoteConfig.collectAsStateWithLifecycle()
    val configUpdatedAt by viewModel.configUpdatedAt.collectAsStateWithLifecycle()
    val configError by viewModel.configError.collectAsStateWithLifecycle()
    val isConfigLoading by viewModel.isConfigLoading.collectAsStateWithLifecycle()
    val collaborativeMode by viewModel.collaborativeMode.collectAsStateWithLifecycle()

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

    // Group history by day to optimize rendering
    val groupedHistory = remember(filteredHistory) {
        filteredHistory.groupBy { scan ->
            SimpleDateFormat("EEEE, d 'de' MMMM, yyyy", Locale("es", "ES")).format(Date(scan.timestamp))
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

                HorizontalDivider(
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
                    label = { Text("Equipo en Vivo", fontWeight = FontWeight.Bold) },
                    selected = currentSection == ScreenSection.TEAM,
                    onClick = {
                        currentSection = ScreenSection.TEAM
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Filled.Groups, contentDescription = null) },
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
                    label = { Text("Tarifas y Reglas", fontWeight = FontWeight.Bold) },
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

                // Sidebar Footer
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (collaborativeMode)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (collaborativeMode) Icons.Filled.Groups else Icons.Filled.Person,
                                contentDescription = null,
                                tint = if (collaborativeMode)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (collaborativeMode) "Equipo compartido activo" else "Solo este celular",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (collaborativeMode)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) {
        val appTitle = when (currentSection) {
            ScreenSection.CAR_RECORDS -> "Registros de Autos"
            ScreenSection.TEAM -> "Equipo en Vivo"
            ScreenSection.SYNCHRONIZATION -> "Sincronización Web"
            ScreenSection.RATES -> "Tarifas y Reglas"
            ScreenSection.SIMULATION -> "Panel de Simulación"
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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
                                text = appTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                    ScreenSection.TEAM -> {
                        TeamView(viewModel = viewModel)
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
                            config = remoteConfig,
                            updatedAt = configUpdatedAt,
                            configError = configError,
                            isRefreshing = isConfigLoading,
                            onRefresh = { viewModel.refreshRemoteConfig() }
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
                            model = File(vehicle.imagePath),
                            contentDescription = "Foto de Placa",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

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

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

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
                            AppConstants.formatCurrency(calculatedAmount),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

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
                        Toast.makeText(context, "Cobro registrado de ${AppConstants.formatCurrency(calculatedAmount)}. Vehículo retirado.", Toast.LENGTH_LONG).show()
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
