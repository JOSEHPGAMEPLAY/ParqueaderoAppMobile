package com.example

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
    const val DEFAULT_API_BASE_URL = "http://10.0.2.2:3000/"
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

    /**
     * Formats monetary values consistently across the entire application.
     */
    fun formatCurrency(amount: Double): String {
        return "$${String.format("%,.0f", amount)} COP"
    }
}
