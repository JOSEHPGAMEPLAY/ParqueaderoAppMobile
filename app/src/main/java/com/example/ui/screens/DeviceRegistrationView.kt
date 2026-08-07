package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.ScanViewModel

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
                    placeholder = { Text(AppConstants.DEFAULT_API_BASE_URL) },
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
                    Text(text = "${Build.MANUFACTURER} ${Build.MODEL}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Sistema Operativo:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text(text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
