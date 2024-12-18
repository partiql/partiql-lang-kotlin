package org.partiql.spi.value.ion

import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Test
import org.partiql.spi.value.Datum
import org.partiql.spi.value.DatumWriter
import org.partiql.types.PType
import java.math.BigDecimal

/**
 * Round-trip tests for encoding PartiQL values in Ion; this currently uses all decorators.
 */
class IonStreamTest {

    /**
     * Apply all directions and round-trip.
     */
    private fun case(ion: String, datum: Datum) {
        // assertRead(ion, datum)
        assertWrite(ion, datum)
        // assertRoundTrip(datum)
    }

    @Test
    fun testNull() {
        case("null", Datum.nullValue())
        case("'bool'::null", Datum.nullValue(PType.bool()))
        case("'decimal(2,0)'::null", Datum.nullValue(PType.decimal(2, 0)))
    }

    @Test
    fun testBool() {
        case("'bool'::null", Datum.nullValue(PType.bool()))
        case("'bool'::true", Datum.bool(true))
        case("'bool'::false", Datum.bool(false))
    }

    @Test
    fun testNumbers() {
        // tinyint
        case("'tinyint'::42", Datum.tinyint(42))
        case("'tinyint'::-42", Datum.tinyint(-42))
        // smallint
        case("'smallint'::42", Datum.smallint(42))
        case("'smallint'::-42", Datum.smallint(-42))
        // int
        case("'int'::42", Datum.integer(42))
        case("'int'::-42", Datum.integer(-42))
        // bigint
        case("'bigint'::42", Datum.bigint(42))
        case("'bigint'::-42", Datum.bigint(-42))
        // decimal
        case("'decimal(3,1)'::10.5", Datum.decimal(BigDecimal("10.5"), 3, 1))
        case("'decimal(3,1)'::-10.5", Datum.decimal(BigDecimal("-10.5"), 3, 1))
        // real
        case("'real'::3.14e0", Datum.real(3.14f))
        case("'real'::-3.14e0", Datum.real(-3.14f))
        // double
        case("'double'::3.1415e0", Datum.doublePrecision(3.1415))
        case("'double'::-3.1415e0", Datum.doublePrecision(-3.1415))
    }

    @Test
    fun testText() {
        // char
        case("'char(1)'::\"a\"", Datum.character("a", 1))
        case("'char(3)'::\"abc\"", Datum.character("abc", 3))
        // varchar
        case("'varchar(3)'::\"abc\"", Datum.varchar("abc", 3))
        case("'varchar(5)'::\"abc  \"", Datum.varchar("abc  ", 5))
        // string
        case("'string'::\"hello\"", Datum.string("hello"))
    }

    @Test
    fun testLob() {
        // clob
        case("'clob(7)'::{{\"goodbye\"}}", Datum.clob("goodbye".toByteArray(), 7))
        // blob
        case("'blob(5)'::{{aGVsbG8=}}", Datum.blob("hello".toByteArray(), 5))
    }

    @Test
    fun testDatetime() {
        // TODO blocked on https://github.com/partiql/partiql-lang-kotlin/pull/1656
    }

    @Test
    fun testArray() {
        // DYNAMIC ARRAY
        case(
            "'array<dynamic>'::[int::1, int::2, int::3]",
            Datum.array(listOf(Datum.integer(1), Datum.integer(2), Datum.integer(3)))
        )
        // INT ARRAY (should omit element types)
        case(
            "'array<int>'::[1,2,3]",
            Datum.array(listOf(Datum.integer(1), Datum.integer(2), Datum.integer(3)), PType.integer())
        )
        // ARRAY<ARRAY<INT>>
    }

    /**
     * Assert ion -> datum via IonSource (PSource).
     */
    private fun assertRead(ion: String, datum: Datum) {
        TODO()
    }

    /**
     * Assert datum -> ion via IonSink (PSink).
     *
     * @param ion
     * @param datum
     */
    private fun assertWrite(ion: String, datum: Datum) {
        assertEquals(ion, write(datum))
    }

    /**
     * Assert round-trip datum->ion->datum with no loss of information.
     *
     * @param datum
     */
    private fun assertRoundTrip(datum: Datum) {
        val e: Datum = datum
        val a: Datum = read(write(e))
        assertEquals(e, a)
    }

    private fun write(datum: Datum): String {
        val sb = StringBuilder()
        val sink = IonSink.text(sb, elisions = IntArray(0))
        val writer = DatumWriter(sink)
        writer.write(datum)
        return sb.toString()
    }

    private fun read(ion: String): Datum {
        // val source = IonSource.decorated().build(ion)
        // val reader = DatumReader(source)
        // return reader.read()
        return Datum.nullValue()
    }

    /**
     * Assert ion elements are equal.
     */
    private fun assertEquals(expected: String, actual: String) {
        val e = loadSingleElement(expected)
        val a = loadSingleElement(actual)
        if (e != a) {
            throw AssertionError("Expected: $expected, Actual: $actual")
        }
    }

    /**
     * Assert ion elements are equal.
     */
    private fun assertEquals(expected: Datum, actual: Datum) {
        val comparator = Datum.comparator()
        if (comparator.compare(expected, actual) != 0) {
            val e = write(expected)
            val a = write(actual)
            throw AssertionError("Expected: $e, Actual: $a")
        }
    }
}
