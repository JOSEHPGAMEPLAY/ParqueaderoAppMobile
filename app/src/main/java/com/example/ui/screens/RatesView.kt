package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AppConstants
import com.example.AppRemoteConfig
import com.example.ScanViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Vista de tarifas de solo lectura: la configuración se administra desde el
 * panel web y se descarga al dispositivo. Se mantiene una copia local para
 * seguir facturando sin conexión.
 */
@Composable
fun RatesView(
    viewModel: ScanViewModel,
    config: AppRemoteConfig,
    updatedAt: Long,
    configError: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    var simDurationMin by remember { mutableStateOf(135) }
    var simHasTicket by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Tarifas centralizadas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "El administrador define las tarifas y reglas desde la página web. " +
                            "Todos los celulares usan la misma configuración y guardan una copia " +
                            "para trabajar sin internet.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Versión de configuración: ${if (config.version > 0) config.version else "local"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (updatedAt > 0) {
                                    "Actualizada: " + SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(Date(updatedAt))
                                } else {
                                    "Aún no se ha descargado del servidor"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    Icons.Filled.CloudSync,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Actualizar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!configError.isNullOrBlank()) {
                        Text(
                            configError,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            RateCard(
                title = "Tarifas CON tiquete",
                subtitle = "Vehículos con tiquete o código de barras",
                hour = config.rates.withTicket.hour,
                fraction = config.rates.withTicket.fraction,
                accent = MaterialTheme.colorScheme.primary
            )
        }

        item {
            RateCard(
                title = "Tarifas SIN tiquete",
                subtitle = "Ingreso manual sin tiquete",
                hour = config.rates.withoutTicket.hour,
                fraction = config.rates.withoutTicket.fraction,
                accent = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Regla de cobro",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    RuleRow(
                        "Horas mínimas",
                        "${config.billing.minimumHours} hora(s)"
                    )
                    RuleRow(
                        "Umbral de fracción",
                        "${config.billing.fractionThresholdMinutes} minutos"
                    )
                    RuleRow(
                        "Minutos de cortesía",
                        "${config.billing.freeMinutes} minutos"
                    )
                    Text(
                        text = "Ejemplo: con un umbral de ${config.billing.fractionThresholdMinutes} min, " +
                            "una estadía de 2 h ${config.billing.fractionThresholdMinutes} min cobra 2 horas. " +
                            "Al superar el umbral se suma la fracción.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.MonetizationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Simulador de cobro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = "Comprueba la regla configurada sin afectar registros reales.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tiempo de estadía:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val hours = simDurationMin / 60
                        val minutes = simDurationMin % 60
                        Text(
                            text = if (hours > 0) "${hours}h ${minutes}m ($simDurationMin min)"
                            else "$simDurationMin min",
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { simHasTicket = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (simHasTicket) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (simHasTicket) Color.Black
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Con tiquete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { simHasTicket = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!simHasTicket) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!simHasTicket) Color.Black
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sin tiquete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                    )

                    val entryTimeSim = System.currentTimeMillis() - (simDurationMin * 60 * 1000L)
                    val simFee = viewModel.calculateParkingFee(
                        entryTimeSim,
                        System.currentTimeMillis(),
                        simHasTicket
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Valor simulado:", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            text = AppConstants.formatCurrency(simFee),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    val applied = if (simHasTicket) config.rates.withTicket else config.rates.withoutTicket
                    Text(
                        text = "Aplicado: ${AppConstants.formatCurrency(applied.hour.toDouble())} por hora " +
                            "y ${AppConstants.formatCurrency(applied.fraction.toDouble())} por fracción.",
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
private fun RateCard(
    title: String,
    subtitle: String,
    hour: Int,
    fraction: Int,
    accent: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadOnlyAmount("Hora completa", hour, Modifier.weight(1f))
                ReadOnlyAmount("Fracción", fraction, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReadOnlyAmount(label: String, amount: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            AppConstants.formatCurrency(amount.toDouble()),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RuleRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
