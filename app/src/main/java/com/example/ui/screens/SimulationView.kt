package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.ScanViewModel

@Composable
fun SimulationView(
    viewModel: ScanViewModel,
    context: Context,
    keyboardController: SoftwareKeyboardController?
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

        // 2. Simulador de Tiempo y Alteración de Registros Activos
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
                            valueRange = 5f..1440f,
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
                            text = AppConstants.formatCurrency(simFeeResult),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val applyHVal = if (simHasTicket) rateWithTicketHour else rateNoTicketHour
                    val applyFVal = if (simHasTicket) rateWithTicketFraction else rateNoTicketFraction
                    Text(
                        text = "Ecuación aplicada: ${AppConstants.formatCurrency(applyHVal.toDouble())} por hora " +
                            "y ${AppConstants.formatCurrency(applyFVal.toDouble())} por fracción.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}
