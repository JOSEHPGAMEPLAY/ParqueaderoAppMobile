package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.ScanViewModel
import com.example.TeamRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TeamView(viewModel: ScanViewModel) {
    val context = LocalContext.current
    val config by viewModel.remoteConfig.collectAsStateWithLifecycle()
    val collaborativeMode by viewModel.collaborativeMode.collectAsStateWithLifecycle()
    val teamRecords by viewModel.teamRecords.collectAsStateWithLifecycle()
    val lastSync by viewModel.teamLastSync.collectAsStateWithLifecycle()
    val teamError by viewModel.teamError.collectAsStateWithLifecycle()

    var checkoutTarget by remember { mutableStateOf<TeamRecord?>(null) }
    var checkoutHasTicket by remember { mutableStateOf(true) }

    val adminAllowsCollaboration = config.mobile.collaborativeSyncEnabled

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (collaborativeMode)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (collaborativeMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Modo colaborativo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    if (collaborativeMode) "Activo: compartiendo registros"
                                    else "Inactivo: trabajo 100% offline",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = collaborativeMode,
                            onCheckedChange = { viewModel.setCollaborativeMode(it) },
                            enabled = adminAllowsCollaboration
                        )
                    }

                    Text(
                        text = if (adminAllowsCollaboration) {
                            "Al activarlo, este celular envía sus registros pendientes y muestra " +
                                "los vehículos activos de los demás celulares vinculados. " +
                                "Mientras esté desactivado, la app funciona sin conexión."
                        } else {
                            "El administrador desactivó el modo colaborativo desde la web. " +
                                "Puedes seguir registrando vehículos de forma offline."
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lastSync > 0) {
                                "Última actualización: " + SimpleDateFormat(
                                    "HH:mm:ss",
                                    Locale.getDefault()
                                ).format(Date(lastSync))
                            } else {
                                "Sin sincronizar todavía"
                            },
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { viewModel.refreshTeam() },
                            enabled = collaborativeMode
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Actualizar", fontSize = 11.sp)
                        }
                    }

                    if (!teamError.isNullOrBlank()) {
                        Text(
                            teamError!!,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (!collaborativeMode) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Activa el modo colaborativo para ver los vehículos del equipo.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (teamRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.LocalParking,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No hay vehículos activos en el equipo.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    "Vehículos activos del equipo (${teamRecords.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(teamRecords, key = { it.plateNumber }) { record ->
                TeamRecordCard(
                    record = record,
                    onCheckout = {
                        checkoutTarget = record
                        checkoutHasTicket = record.hasTicket
                    }
                )
            }
        }
    }

    if (checkoutTarget != null) {
        val record = checkoutTarget!!
        val now = System.currentTimeMillis()
        val fee = viewModel.calculateParkingFee(
            record.entryTimeMillis,
            now,
            checkoutHasTicket
        )

        AlertDialog(
            onDismissRequest = { checkoutTarget = null },
            title = { Text("Cobrar ${record.plateNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Este vehículo fue registrado desde ${record.deviceModel ?: "otro celular"} " +
                            "por ${record.operatorName ?: "otro operador"}.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Entrada", fontSize = 12.sp)
                        Text(
                            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                                .format(Date(record.entryTimeMillis)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tipo de cobro", fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = checkoutHasTicket,
                                onClick = { checkoutHasTicket = true },
                                label = { Text("Con tiquete", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = !checkoutHasTicket,
                                onClick = { checkoutHasTicket = false },
                                label = { Text("Sin tiquete", fontSize = 10.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Text(
                            AppConstants.formatCurrency(fee),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        "El servidor recalcula el valor con la tarifa central al confirmar.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = record
                        checkoutTarget = null
                        viewModel.checkoutRemoteVehicle(target, checkoutHasTicket) { price ->
                            val message = if (price != null) {
                                "Cobro registrado: ${AppConstants.formatCurrency(price)}"
                            } else {
                                "No se pudo cobrar ${target.plateNumber}"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Confirmar cobro", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { checkoutTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TeamRecordCard(record: TeamRecord, onCheckout: () -> Unit) {
    val now = System.currentTimeMillis()
    val minutes = ((now - record.entryTimeMillis) / 60000).coerceAtLeast(0)
    val hours = minutes / 60
    val rest = minutes % 60

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        record.plateNumber,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (record.hasTicket) "Con tiquete" else "Sin tiquete",
                            fontSize = 9.sp
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Entrada: " + SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(record.entryTimeMillis)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (hours > 0) "${hours}h ${rest}m" else "${rest}min",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.operatorName ?: "Operador",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = record.deviceModel ?: "Celular",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onCheckout,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Filled.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cobrar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
