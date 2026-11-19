package org.partiql.runner.util

import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.value.io.DatumIonReaderBuilder
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Matching an actual result with an expected value that holds `$json`.
 */
@Suppress("DEPRECATION")
class ResultMatchTest {

    private val comparator = Datum.comparator(true, true)

    private fun read(ion: String): Datum = DatumIonReaderBuilder.standard().build(loadSingleElement(ion)).read()

    private fun matches(actual: String, expect: String) = resultMatches(read(actual), read(expect), comparator)

    @Test
    fun objectsMatchInAnyOrderAndCountRepeats() {
        assertTrue(matches("\$json::{ a: 1, b: 2 }", "\$json::{ b: 2, a: 1 }"))
        assertTrue(matches("\$json::{ a: 1, a: 1 }", "\$json::{ a: 1, a: 1 }"))
        assertFalse(matches("\$json::{ a: 1, a: 1 }", "\$json::{ a: 1 }"))
        assertFalse(matches("\$json::{ a: 1 }", "\$json::{ a: 1, a: 1 }"))
    }

    @Test
    fun arraysMatchInOrder() {
        assertTrue(matches("\$json::[1, 2]", "\$json::[1, 2]"))
        assertFalse(matches("\$json::[1, 2]", "\$json::[2, 1]"))
    }

    @Test
    fun numbersMatchByTypeValueAndScale() {
        assertTrue(matches("\$json::7", "\$json::7"))
        assertFalse(matches("\$json::7", "\$json::7.0"))
        assertFalse(matches("\$json::7", "\$json::3000000000"))
        assertFalse(matches("\$json::10.5", "\$json::10.50"))
        assertFalse(matches("\$json::1e0", "\$json::1.0"))
    }

    @Test
    fun timesMatchInEveryFieldIncludingTheOffset() {
        val t = "\$json::\$time::{ hour: 2, minute: 30, second: 59., offset: %s }"
        assertTrue(matches(t.format("-480"), t.format("-480")))
        assertFalse(matches(t.format("-480"), t.format("0")))
        assertFalse(matches(t.format("0"), t.format("null")))
    }

    @Test
    fun nullsMatchOnlyTheirOwnKind() {
        assertFalse(matches("\$json::null", "null"))
        assertFalse(matches("null", "\$json::null"))
        assertTrue(resultMatches(Datum.nullValue(PType.json()), read("null"), comparator))
    }

    @Test
    fun missingMatchesOnlyMissing() {
        assertTrue(matches("\$bag::[\$missing::null, \$json::1]", "\$bag::[\$json::1, \$missing::null]"))
        assertFalse(matches("\$bag::[null, \$json::1]", "\$bag::[\$missing::null, \$json::1]"))
    }

    @Test
    fun aJsonValueNeverMatchesAnotherType() {
        assertFalse(matches("{ a: 1 }", "\$json::{ a: 1 }"))
        assertFalse(matches("\$json::\"x\"", "\"x\""))
    }

    @Test
    fun aResultWithJsonDoesNotMatchAnExpectedValueWithout() {
        // The comparator raises FEATURE_NOT_SUPPORTED, which counts as no match.
        assertFalse(matches("\$json::1", "1"))
        assertFalse(matches("\$bag::[\$json::1]", "\$bag::[1]"))
    }

    @Test
    fun otherComparatorErrorsAreNotHidden() {
        val failing = Comparator<Datum> { _, _ ->
            throw PRuntimeException(PError(PError.INTERNAL_ERROR, Severity.ERROR(), PErrorKind.EXECUTION(), null, null))
        }
        assertThrows<PRuntimeException> { resultMatches(read("1"), read("1"), failing) }
    }

    @Test
    fun aStructMatchesARowWithTheSameFields() {
        val row = Datum.row(Field.of("a", Datum.jsonScalar(Datum.integer(1))), Field.of("b", Datum.integer(2)))
        assertTrue(resultMatches(row, read("{ b: 2, a: \$json::1 }"), comparator))
        assertFalse(resultMatches(row, read("{ b: 3, a: \$json::1 }"), comparator))
    }

    @Test
    fun containersOfJsonMatchDeeply() {
        assertTrue(matches("\$bag::[{ a: \$json::[1] }, 2]", "\$bag::[2, { a: \$json::[1] }]"))
        assertFalse(matches("\$bag::[{ a: \$json::[1] }]", "\$bag::[{ a: \$json::[2] }]"))
        assertTrue(matches("[1, \$json::2]", "[1, \$json::2]"))
    }

    @Test
    fun jsonInsideAMapValue() {
        val m = "\$map::string::dynamic::[[\"k\", \$json::{ a: %s }]]"
        assertTrue(matches(m.format("1"), m.format("1")))
        assertFalse(matches(m.format("1"), m.format("2")))
    }
}
