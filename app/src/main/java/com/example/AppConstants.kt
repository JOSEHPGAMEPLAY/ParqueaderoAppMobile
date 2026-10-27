package com.example

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Single source of truth for global constants, app versioning, and shared defaults.
 * Follows SOLID principles for centralized configuration management.
 */
object AppConstants {
    /**
     * Current application version sourced from BuildConfig.
     */
    val APP_VERSION: String = BuildConfig.VERSION_NAME

    // Network & Backend Defaults
    /**
     * URL base por defecto de la API. Se define en `app/build.gradle.kts` y puede
     * sobreescribirse al compilar con `-PapiBaseUrl=...` o `API_BASE_URL`.
     */
    val DEFAULT_API_BASE_URL: String = BuildConfig.DEFAULT_API_BASE_URL
    const val DEFAULT_GITHUB_RELEASE_URL = "https://api.github.com/repos/JOSEHPGAMEPLAY/ParqueaderoAppMobile/releases/latest"

    // SharedPreferences Keys
    const val PREF_IS_REGISTERED = "is_registered"
    const val PREF_REGISTERED_USERNAME = "registered_username"
    const val PREF_JWT_TOKEN = "jwt_token"
    const val PREF_API_BASE_URL = "api_base_url"
    const val PREF_GITHUB_RELEASE_URL = "github_release_url"

    // Rate Keys & Default Values (in COP)
    const val DEFAULT_RATE_WITH_TICKET_HOUR = 2000
    const val DEFAULT_RATE_WITH_TICKET_FRACTION = 1000
    const val DEFAULT_RATE_NO_TICKET_HOUR = 4000
    const val DEFAULT_RATE_NO_TICKET_FRACTION = 2000

    const val PREF_RATE_WITH_TICKET_HOUR = "rate_with_ticket_hour"
    const val PREF_RATE_WITH_TICKET_FRACTION = "rate_with_ticket_fraction"
    const val PREF_RATE_NO_TICKET_HOUR = "rate_no_ticket_hour"
    const val PREF_RATE_NO_TICKET_FRACTION = "rate_no_ticket_fraction"

    // Moneda y locale por defecto: coinciden con el AppConfig del backend.
    const val DEFAULT_CURRENCY_CODE = "COP"
    const val DEFAULT_LOCALE_TAG = "es-CO"

    /** El peso colombiano no usa centavos; el resto de monedas se muestran con dos decimales. */
    private const val ZERO_DECIMAL_CURRENCY = "COP"
    private const val ZERO_DECIMALS = 0
    private const val STANDARD_DECIMALS = 2

    @Volatile
    private var activeCurrencyCode: String = DEFAULT_CURRENCY_CODE

    @Volatile
    private var activeLocaleTag: String = DEFAULT_LOCALE_TAG

    /**
     * Formateador vigente. Se reemplaza completo al reconfigurar la moneda y cada lectura
     * formatea bajo el monitor del formateador obtenido, por lo que es seguro entre hilos
     * (NumberFormat no es thread-safe).
     */
    @Volatile
    private var currencyFormatter: NumberFormat =
        createCurrencyFormatter(DEFAULT_CURRENCY_CODE, DEFAULT_LOCALE_TAG)

    /** Moneda ISO-4217 aplicada al formateador. */
    val currencyCode: String
        get() = activeCurrencyCode

    /** Locale BCP-47 aplicado al formateador. */
    val localeTag: String
        get() = activeLocaleTag

    /**
     * Aplica la moneda y el locale recibidos desde el AppConfig central.
     * Cualquier valor ausente o inválido cae al formato seguro: COP / es-CO.
     * Llamar con los mismos valores vigentes no recrea el formateador.
     */
    fun configureCurrency(currencyCode: String?, localeTag: String?) {
        val safeLocaleTag = normalizeLocaleTag(localeTag)
        val safeCurrencyCode = normalizeCurrencyCode(currencyCode)
        if (safeCurrencyCode == activeCurrencyCode && safeLocaleTag == activeLocaleTag) return

        // Se publican primero los valores descriptivos y al final el formateador,
        // que es el único que consume formatCurrency.
        activeCurrencyCode = safeCurrencyCode
        activeLocaleTag = safeLocaleTag
        currencyFormatter = createCurrencyFormatter(safeCurrencyCode, safeLocaleTag)
    }

    /**
     * Formats monetary values consistently across the entire application,
     * using the currency and locale configured from the web panel.
     */
    fun formatCurrency(amount: Double): String {
        val formatter = currencyFormatter
        return synchronized(formatter) { formatter.format(amount) }
    }

    private fun normalizeLocaleTag(localeTag: String?): String {
        val raw = localeTag?.trim().orEmpty()
        if (raw.isEmpty()) return DEFAULT_LOCALE_TAG
        return try {
            val locale = Locale.forLanguageTag(raw)
            if (locale.language.isBlank() || locale.language.equals("und", ignoreCase = true)) {
                DEFAULT_LOCALE_TAG
            } else {
                locale.toLanguageTag()
            }
        } catch (e: Exception) {
            DEFAULT_LOCALE_TAG
        }
    }

    private fun normalizeCurrencyCode(currencyCode: String?): String {
        val raw = currencyCode?.trim()?.uppercase(Locale.ROOT).orEmpty()
        if (raw.isEmpty()) return DEFAULT_CURRENCY_CODE
        return try {
            Currency.getInstance(raw)
            raw
        } catch (e: IllegalArgumentException) {
            DEFAULT_CURRENCY_CODE
        } catch (e: Exception) {
            DEFAULT_CURRENCY_CODE
        }
    }

    private fun createCurrencyFormatter(currencyCode: String, localeTag: String): NumberFormat {
        val locale = Locale.forLanguageTag(localeTag)
        val formatter = NumberFormat.getCurrencyInstance(locale)
        try {
            formatter.currency = Currency.getInstance(currencyCode)
        } catch (e: IllegalArgumentException) {
            // Se conserva la moneda implícita del locale.
        }
        val decimals = if (currencyCode == ZERO_DECIMAL_CURRENCY) ZERO_DECIMALS else STANDARD_DECIMALS
        formatter.minimumFractionDigits = decimals
        formatter.maximumFractionDigits = decimals
        formatter.isGroupingUsed = true
        return formatter
    }
}
