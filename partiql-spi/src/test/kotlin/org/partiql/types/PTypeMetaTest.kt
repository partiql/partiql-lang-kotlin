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
    private fun PType.assertHasLengthMeta() {
        assertEquals(this.metas[UNSPECIFIED_LENGTH], true)
    }

    private fun PType.assertHasPrecisionMeta() {
        assertEquals(this.metas[UNSPECIFIED_PRECISION], true)
    }

    private fun PType.assertHasScaleMeta() {
        assertEquals(this.metas[UNSPECIFIED_SCALE], true)
    }

    @Test
    fun `test decimal has no scale and no precision`() {
        val decimal = PType.decimal()
        decimal.assertHasPrecisionMeta()
        decimal.assertHasScaleMeta()
    }

    @Test
    fun `test decimal has no scale`() {
        val decimal = PType.decimal(10)
        decimal.assertHasScaleMeta()
    }

    @Test
    fun `test numeric has no scale and no precision`() {
        val numeric = PType.numeric()
        numeric.assertHasPrecisionMeta()
        numeric.assertHasScaleMeta()
    }

    @Test
    fun `test numeric has no scale`() {
        val numeric = PType.numeric(10)
        numeric.assertHasScaleMeta()
    }

    @Test
    fun `test varchar has no length`() {
        val varchar = PType.varchar()
        varchar.assertHasLengthMeta()
    }

    @Test
    fun `test character has no length`() {
        val char = PType.character()
        char.assertHasLengthMeta()
    }

    @Test
    fun `test clob has no length`() {
        val clob = PType.clob()
        clob.assertHasLengthMeta()
    }

    @Test
    fun `test blob has no length`() {
        val blob = PType.blob()
        blob.assertHasLengthMeta()
    }

    @Test
    fun `test time has no precision`() {
        val time = PType.time()
        time.assertHasPrecisionMeta()
    }

    @Test
    fun `test timez has no precision`() {
        val timez = PType.timez()
        timez.assertHasPrecisionMeta()
    }

    @Test
    fun `test timestamp has no precision`() {
        val timestamp = PType.timestamp()
        timestamp.assertHasPrecisionMeta()
    }

    @Test
    fun `test timestampz has no precision`() {
        val timestampz = PType.timestampz()
        timestampz.assertHasPrecisionMeta()
    }
}
