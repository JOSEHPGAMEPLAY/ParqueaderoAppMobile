package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La URL base por defecto vive en `BuildConfig.DEFAULT_API_BASE_URL` (inyectable con
 * `-PapiBaseUrl`), pero Retrofit exige que termine en "/". Este test evita que un build
 * con una URL mal formada llegue a producción.
 */
class DefaultApiBaseUrlTest {

    @Test
    fun `la url base por defecto no esta vacia`() {
        assertFalse(AppConstants.DEFAULT_API_BASE_URL.isBlank())
    }

    @Test
    fun `la url base por defecto termina en barra`() {
        assertTrue(AppConstants.DEFAULT_API_BASE_URL.endsWith("/"))
    }

    @Test
    fun `la url base por defecto es absoluta`() {
        assertTrue(
            "Debe iniciar con http o https",
            AppConstants.DEFAULT_API_BASE_URL.startsWith("http://") ||
                AppConstants.DEFAULT_API_BASE_URL.startsWith("https://")
        )
    }

    @Test
    fun `sync client arranca con la misma url que AppConstants`() {
        assertEquals(AppConstants.DEFAULT_API_BASE_URL, SyncClient.baseApiUrl)
    }
}
