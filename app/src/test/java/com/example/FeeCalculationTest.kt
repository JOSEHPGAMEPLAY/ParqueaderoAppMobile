package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Bloquea la regla de cobro compartida con el backend y la web:
 * horas completas + fracción solo cuando el residuo supera el umbral.
 */
class FeeCalculationTest {

    private val config = AppRemoteConfig(
        billing = RemoteBillingConfig(
            minimumHours = 1,
            fractionThresholdMinutes = 15,
            freeMinutes = 0
        ),
        rates = RemoteRates(
            withTicket = RemoteRateConfig(hour = 2000, fraction = 1000),
            withoutTicket = RemoteRateConfig(hour = 4000, fraction = 2000)
        )
    )

    private val entry = 1_700_000_000_000L

    private fun fee(hours: Int, minutes: Int, hasTicket: Boolean = true): Double =
        config.calculateFee(
            entry,
            entry + (hours * 60L + minutes) * 60_000L,
            hasTicket
        )

    @Test
    fun `exact hours charge only full hours`() {
        assertEquals(4000.0, fee(2, 0), 0.001)
    }

    @Test
    fun `remainder equal to threshold does not charge fraction`() {
        assertEquals(4000.0, fee(2, 15), 0.001)
    }

    @Test
    fun `remainder above threshold charges fraction`() {
        assertEquals(5000.0, fee(2, 16), 0.001)
    }

    @Test
    fun `less than minimum hours charges minimum`() {
        assertEquals(2000.0, fee(0, 30), 0.001)
    }

    @Test
    fun `zero duration charges nothing`() {
        assertEquals(0.0, config.calculateFee(entry, entry, true), 0.001)
    }

    @Test
    fun `without ticket uses its own rate table`() {
        assertEquals(8000.0, fee(2, 15, hasTicket = false), 0.001)
        assertEquals(10000.0, fee(2, 16, hasTicket = false), 0.001)
    }

    @Test
    fun `free minutes are discounted before billing`() {
        val withCourtesy = config.copy(
            billing = config.billing.copy(freeMinutes = 20)
        )
        // 2 h 15 min - 20 min = 1 h 55 min -> 1 hora + fracción
        assertEquals(
            3000.0,
            withCourtesy.calculateFee(entry, entry + 135 * 60_000L, true),
            0.001
        )
    }

    @Test
    fun `configurable threshold changes the billing point`() {
        val wide = config.copy(billing = config.billing.copy(fractionThresholdMinutes = 45))
        // 2 h 20 min: con umbral 45 no cobra fracción
        assertEquals(4000.0, wide.calculateFee(entry, entry + 140 * 60_000L, true), 0.001)
        // 2 h 46 min: supera el umbral de 45 y cobra fracción
        assertEquals(5000.0, wide.calculateFee(entry, entry + 166 * 60_000L, true), 0.001)
    }
}
