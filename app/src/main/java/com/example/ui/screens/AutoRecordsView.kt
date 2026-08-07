package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.AppConstants
import com.example.ScanResult
import com.example.ScanUiState
import com.example.ScanViewModel
import com.example.ui.components.CameraPreview
import com.example.ui.components.DetailItem
import com.example.ui.components.HistoricalDetailRow
import com.example.ui.components.NotesFeedView
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun AutoRecordsView(
    viewModel: ScanViewModel,
    context: Context,
    cameraPermissionState: PermissionState,
    galleryLauncher: ActivityResultLauncher<String>,
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
    val collapsedDates = remember { mutableStateMapOf<String, Boolean>() }

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
                            text = AppConstants.formatCurrency(totalRevenue),
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
                val isCollapsed = collapsedDates[dateHeader] ?: false
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { collapsedDates[dateHeader] = !isCollapsed }
                            .padding(top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateHeader,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = if (isCollapsed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                            contentDescription = if (isCollapsed) "Expandir" else "Colapsar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isCollapsed) {
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
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Vehicle photo thumbnail in front of the plate badge
                                        if (!scan.imagePath.isNullOrBlank()) {
                                            AsyncImage(
                                                model = File(scan.imagePath),
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
                                                    text = AppConstants.formatCurrency(scan.paidAmount),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

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
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), modifier = Modifier.padding(bottom = 8.dp))

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
                                                value = AppConstants.formatCurrency(scan.paidAmount ?: 0.0)
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
                                                        NotesFeedView(
                                                            notesStr = scan.notes,
                                                            onDeleteComment = { comment -> viewModel.deleteNoteComment(scan, comment) }
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            setInlineNotesEditId(scan.id)
                                                            itemNotesValue = ""
                                                        },
                                                        modifier = Modifier.size(32.dp).testTag("edit_notes_btn")
                                                    ) {
                                                        Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }

                                        // Action buttons for expanded item
                                        if (scan.status == "ACTIVE") {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onCheckoutClick(scan) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    modifier = Modifier.weight(1f).height(38.dp).testTag("checkout_action_btn_${scan.plateText}"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Icon(Icons.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Dar Salida (Cobrar)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                }

                                                FilledTonalButton(
                                                    onClick = {
                                                        viewModel.deleteScan(scan)
                                                        Toast.makeText(context, "Registro eliminado permanentemente del dispositivo.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ),
                                                    modifier = Modifier.weight(1f).height(38.dp).testTag("delete_single_scan_btn"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                ) {
                                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Eliminar Registro", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
}
