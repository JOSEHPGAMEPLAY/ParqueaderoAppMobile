package com.example

import android.content.SharedPreferences
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.math.floor

/**
 * Configuración central administrada desde el panel web.
 * Se descarga con GET /api/config y se cachea para seguir operando offline.
 */

@JsonClass(generateAdapter = true)
data class RemoteRateConfig(
    @Json(name = "hour") val hour: Int = AppConstants.DEFAULT_RATE_WITH_TICKET_HOUR,
    @Json(name = "fraction") val fraction: Int = AppConstants.DEFAULT_RATE_WITH_TICKET_FRACTION
)

@JsonClass(generateAdapter = true)
data class RemoteBillingConfig(
    @Json(name = "minimumHours") val minimumHours: Int = 1,
    @Json(name = "fractionThresholdMinutes") val fractionThresholdMinutes: Int = 15,
    @Json(name = "freeMinutes") val freeMinutes: Int = 0
)

@JsonClass(generateAdapter = true)
data class RemoteMobileConfig(
    @Json(name = "collaborativeSyncEnabled") val collaborativeSyncEnabled: Boolean = true,
    @Json(name = "syncIntervalSeconds") val syncIntervalSeconds: Int = 30
)

@JsonClass(generateAdapter = true)
data class RemoteRates(
    @Json(name = "withTicket") val withTicket: RemoteRateConfig = RemoteRateConfig(),
    @Json(name = "withoutTicket") val withoutTicket: RemoteRateConfig = RemoteRateConfig(
        hour = AppConstants.DEFAULT_RATE_NO_TICKET_HOUR,
        fraction = AppConstants.DEFAULT_RATE_NO_TICKET_FRACTION
    )
)

@JsonClass(generateAdapter = true)
data class AppRemoteConfig(
    @Json(name = "key") val key: String = "global",
    @Json(name = "version") val version: Int = 0,
    @Json(name = "currency") val currency: String = "COP",
    @Json(name = "locale") val locale: String = "es-CO",
    @Json(name = "timezone") val timezone: String = "America/Bogota",
    @Json(name = "billing") val billing: RemoteBillingConfig = RemoteBillingConfig(),
    @Json(name = "rates") val rates: RemoteRates = RemoteRates(),
    @Json(name = "mobile") val mobile: RemoteMobileConfig = RemoteMobileConfig()
)

@JsonClass(generateAdapter = true)
data class AppConfigEnvelope(
    @Json(name = "config") val config: AppRemoteConfig
)

/**
 * Regla de facturación compartida con el backend:
 * horas completas + fracción solo si el residuo supera el umbral configurado.
 */
fun AppRemoteConfig.calculateFee(
    entryTimeMs: Long,
    exitTimeMs: Long,
    hasTicket: Boolean
): Double {
    if (exitTimeMs <= entryTimeMs) return 0.0

    val rate = if (hasTicket) rates.withTicket else rates.withoutTicket
    val billableMinutes =
        (exitTimeMs - entryTimeMs).toDouble() / 60000.0 - billing.freeMinutes
    if (billableMinutes <= 0) return 0.0

    val fullHours = floor(billableMinutes / 60.0).toInt()
    val remainder = billableMinutes - fullHours * 60.0
    val minimumHours = billing.minimumHours.coerceAtLeast(0)

    if (fullHours < minimumHours) {
        return (minimumHours * rate.hour).toDouble()
    }

    val fraction = if (remainder > billing.fractionThresholdMinutes) rate.fraction else 0
    return (fullHours * rate.hour + fraction).toDouble()
}

/**
 * Persistencia local de la última configuración válida recibida del servidor.
 */
object RemoteConfigStore {
    private const val PREFS_KEY_CONFIG = "remote_config_json"
    private const val PREFS_KEY_UPDATED_AT = "remote_config_updated_at"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(AppRemoteConfig::class.java)

    fun load(prefs: SharedPreferences): AppRemoteConfig {
        val json = prefs.getString(PREFS_KEY_CONFIG, null) ?: return AppRemoteConfig()
        return try {
            adapter.fromJson(json) ?: AppRemoteConfig()
        } catch (e: Exception) {
            AppRemoteConfig()
        }
    }

    fun save(prefs: SharedPreferences, config: AppRemoteConfig, updatedAt: Long) {
        try {
            prefs.edit()
                .putString(PREFS_KEY_CONFIG, adapter.toJson(config))
                .putLong(PREFS_KEY_UPDATED_AT, updatedAt)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("RemoteConfigStore", "No se pudo guardar la configuración", e)
        }
    }

    fun lastUpdatedAt(prefs: SharedPreferences): Long =
        prefs.getLong(PREFS_KEY_UPDATED_AT, 0L)
}
