package com.mapaurbano.mobile.feature.map

import com.mapaurbano.mobile.core.model.MapPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapCoordinatesTest {
    @Test
    fun `accepts a valid Mendoza coordinate`() {
        val point = MapPoint(0.5f, 0.5f, -32.8895, -68.8458, "Centro")

        assertTrue(point.hasUsableCoordinates())
    }

    @Test
    fun `rejects backend placeholder and out of range coordinates`() {
        val placeholder = MapPoint(0.5f, 0.5f, 0.0, 0.0, "Sin ubicación")
        val invalid = MapPoint(0.5f, 0.5f, -120.0, -68.8458, "Inválida")

        assertFalse(placeholder.hasUsableCoordinates())
        assertFalse(invalid.hasUsableCoordinates())
    }
}
