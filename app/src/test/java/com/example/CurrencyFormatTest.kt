package com.example

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Bloquea el contrato del formateador monetario: moneda y locale vienen del
 * AppConfig central y cualquier valor inválido cae a COP / es-CO.
 */
class CurrencyFormatTest {

    @Before
    fun setUp() {
        AppConstants.configureCurrency(
            AppConstants.DEFAULT_CURRENCY_CODE,
            AppConstants.DEFAULT_LOCALE_TAG
        )
    }

    @After
    fun tearDown() {
        AppConstants.configureCurrency(
            AppConstants.DEFAULT_CURRENCY_CODE,
            AppConstants.DEFAULT_LOCALE_TAG
        )
    }

    @Test
    fun `default configuration uses COP without decimals`() {
        // en es-CO la coma es el separador decimal: si apareciera, habría centavos.
        val render = AppConstants.formatCurrency(1234.56)

        assertEquals("COP", AppConstants.currencyCode)
        assertFalse(render, render.contains(","))
        assertTrue(render, render.contains("235"))
    }

    @Test
    fun `COP rounds away the cents`() {
        val render = AppConstants.formatCurrency(1500.75)

        assertTrue(render, render.contains("1") && render.contains("501"))
        assertFalse(render, render.contains("75"))
    }

    @Test
    fun `other currencies keep two decimals`() {
        AppConstants.configureCurrency("USD", "en-US")

        val render = AppConstants.formatCurrency(12.5)

        assertEquals("USD", AppConstants.currencyCode)
        assertEquals("en-US", AppConstants.localeTag)
        assertTrue(render, render.endsWith("12.50"))
    }

    @Test
    fun `unknown currency falls back to COP`() {
        AppConstants.configureCurrency("XYZ", "es-CO")

        val render = AppConstants.formatCurrency(1234.56)

        assertEquals("COP", AppConstants.currencyCode)
        // COP no lleva centavos: en es-CO nunca debe aparecer la coma decimal.
        assertFalse(render, render.contains(","))
        assertTrue(render, render.contains("235"))
    }

    @Test
    fun `invalid locale falls back to es-CO`() {
        AppConstants.configureCurrency("COP", "%%%")

        assertEquals("es-CO", AppConstants.localeTag)
        assertEquals("COP", AppConstants.currencyCode)
    }

    @Test
    fun `null values fall back to defaults`() {
        AppConstants.configureCurrency(null, null)

        assertEquals("COP", AppConstants.currencyCode)
        assertEquals("es-CO", AppConstants.localeTag)
        assertTrue(AppConstants.formatCurrency(0.0).isNotEmpty())
    }
}
