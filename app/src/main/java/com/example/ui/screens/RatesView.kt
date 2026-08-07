package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppConstants
import com.example.ScanViewModel

@Composable
fun RatesView(
    viewModel: ScanViewModel,
    context: Context,
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
    keyboardController: SoftwareKeyboardController?
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
                    val wh = editWithHour.toIntOrNull() ?: AppConstants.DEFAULT_RATE_WITH_TICKET_HOUR
                    val wf = editWithFraction.toIntOrNull() ?: AppConstants.DEFAULT_RATE_WITH_TICKET_FRACTION
                    val nh = editNoHour.toIntOrNull() ?: AppConstants.DEFAULT_RATE_NO_TICKET_HOUR
                    val nf = editNoFraction.toIntOrNull() ?: AppConstants.DEFAULT_RATE_NO_TICKET_FRACTION
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
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 4.dp))
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

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
                            text = AppConstants.formatCurrency(simFeeResult),
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
