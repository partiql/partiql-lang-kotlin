package org.partiql.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType

private const val UNSPECIFIED_LENGTH = "UNSPECIFIED_LENGTH"
private const val UNSPECIFIED_PRECISION = "UNSPECIFIED_PRECISION"
private const val UNSPECIFIED_SCALE = "UNSPECIFIED_SCALE"

/**
 * Tests that constructed PType instances have the correct metas.
 */
class PTypeMetaTest {
    private fun PType.assertUnspecifiedLength() {
        assertEquals(this.metas[UNSPECIFIED_LENGTH], true)
    }

    private fun PType.assertUnspecifiedPrecision() {
        assertEquals(this.metas[UNSPECIFIED_PRECISION], true)
    }

    private fun PType.assertUnspecifiedScale() {
        assertEquals(this.metas[UNSPECIFIED_SCALE], true)
    }

    @Test
    fun `test decimal has no scale and no precision`() {
        val decimal = PType.decimal()
        decimal.assertUnspecifiedPrecision()
        decimal.assertUnspecifiedScale()
    }

    @Test
    fun `test decimal has no scale`() {
        val decimal = PType.decimal(10)
        decimal.assertUnspecifiedScale()
    }

    @Test
    fun `test numeric has no scale and no precision`() {
        val numeric = PType.numeric()
        numeric.assertUnspecifiedPrecision()
        numeric.assertUnspecifiedScale()
    }

    @Test
    fun `test numeric has no scale`() {
        val numeric = PType.numeric(10)
        numeric.assertUnspecifiedScale()
    }

    @Test
    fun `test varchar has no length`() {
        val varchar = PType.varchar()
        varchar.assertUnspecifiedLength()
    }

    @Test
    fun `test character has no length`() {
        val char = PType.character()
        char.assertUnspecifiedLength()
    }

    @Test
    fun `test clob has no length`() {
        val clob = PType.clob()
        clob.assertUnspecifiedLength()
    }

    @Test
    fun `test blob has no length`() {
        val blob = PType.blob()
        blob.assertUnspecifiedLength()
    }

    @Test
    fun `test time has no precision`() {
        val time = PType.time()
        time.assertUnspecifiedPrecision()
    }

    @Test
    fun `test timez has no precision`() {
        val timez = PType.timez()
        timez.assertUnspecifiedPrecision()
    }

    @Test
    fun `test timestamp has no precision`() {
        val timestamp = PType.timestamp()
        timestamp.assertUnspecifiedPrecision()
    }

    @Test
    fun `test timestampz has no precision`() {
        val timestampz = PType.timestampz()
        timestampz.assertUnspecifiedPrecision()
    }
}
