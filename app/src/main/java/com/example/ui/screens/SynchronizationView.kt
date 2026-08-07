package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.ScanViewModel

@Composable
fun SynchronizationView(
    viewModel: ScanViewModel,
    context: Context
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
                        placeholder = { Text(AppConstants.DEFAULT_API_BASE_URL) },
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
                        text = "🔄 Actualizaciones del Sistema (v${AppConstants.APP_VERSION})",
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
                                viewModel.checkForUpdates(currentVersion = AppConstants.APP_VERSION, forceShowSimulated = false)
                                Toast.makeText(context, "Consultando actualizaciones en GitHub...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("check_github_updates_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Buscar Ahora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.checkForUpdates(currentVersion = AppConstants.APP_VERSION, forceShowSimulated = true)
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
                        Text(text = "${Build.MANUFACTURER} ${Build.MODEL}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sistema Operativo:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Text(text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
