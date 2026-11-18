package org.partiql.runner.util

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.JsonKind

/**
 * Whether an actual result matches an expected value. When the expected value holds no value of type JSON, the
 * result compares with [comparator], as before. Otherwise it is matched with the rules for `$json` in
 * docs/partiql-tests-schema-proposal.md in partiql-tests, because [comparator] does not compare JSON.
 *
 * Only [expect] is searched for JSON: [actual] can be a lazy result, and iterating it runs the query again.
 */
internal fun resultMatches(actual: Datum, expect: Datum, comparator: Comparator<Datum>): Boolean {
    val a = actual.lowerVariant()
    val e = expect.lowerVariant()
    if (!e.holdsJson()) {
        return try {
            comparator.compare(a, e) == 0
        } catch (ex: PRuntimeException) {
            // The result holds a value of type JSON, and the expected value does not.
            if (ex.error.code() == PError.FEATURE_NOT_SUPPORTED) false else throw ex
        }
    }
    return deepMatches(a, e, comparator)
}

private fun Datum.lowerVariant(): Datum = if (type.code() == PType.VARIANT && !isNull) lower() else this

private fun Datum.holdsJson(): Boolean {
    if (isNull || isMissing) return false
    return when (type.code()) {
        PType.JSON -> true
        PType.ARRAY, PType.BAG -> any { it.lowerVariant().holdsJson() }
        PType.STRUCT, PType.ROW -> fields.asSequence().any { it.value.lowerVariant().holdsJson() }
        else -> false
    }
}

private fun deepMatches(actual: Datum, expect: Datum, comparator: Comparator<Datum>): Boolean {
    val a = actual.lowerVariant()
    val e = expect.lowerVariant()
    if (a.isMissing || e.isMissing) return a.isMissing && e.isMissing
    // An SQL NULL of type JSON is written as a plain null, so nulls match whatever their type.
    if (a.isNull || e.isNull) return a.isNull && e.isNull
    if (a.type.code() == PType.JSON || e.type.code() == PType.JSON) return jsonMatches(a, e)
    return when (e.type.code()) {
        PType.ARRAY -> a.type.code() == PType.ARRAY && orderedMatches(a.toList(), e.toList()) { x, y -> deepMatches(x, y, comparator) }
        PType.BAG -> a.type.code() == PType.BAG && unorderedMatches(a.toList(), e.toList()) { x, y -> deepMatches(x, y, comparator) }
        PType.STRUCT, PType.ROW -> (a.type.code() == PType.STRUCT || a.type.code() == PType.ROW) &&
            unorderedMatches(a.fields.asSequence().toList(), e.fields.asSequence().toList()) { x, y ->
                x.name == y.name && deepMatches(x.value, y.value, comparator)
            }
        else -> comparator.compare(a, e) == 0
    }
}

/**
 * Both values have type JSON and are not SQL NULL.
 */
@Suppress("DEPRECATION")
private fun jsonMatches(a: Datum, e: Datum): Boolean {
    if (a.type.code() != PType.JSON || e.type.code() != PType.JSON) return false
    if (a.jsonKind != e.jsonKind) return false
    return when (a.jsonKind) {
        JsonKind.NULL -> true
        JsonKind.SCALAR -> scalarMatches(a.jsonScalar, e.jsonScalar)
        JsonKind.ARRAY -> orderedMatches(a.toList(), e.toList(), ::jsonMatches)
        JsonKind.OBJECT -> unorderedMatches(a.fields.asSequence().toList(), e.fields.asSequence().toList()) { x, y ->
            x.name == y.name && jsonMatches(x.value, y.value)
        }
        else -> false
    }
}

/**
 * Scalars match when they have the same type and the same value. A DECIMAL must also have the same scale,
 * and a datetime must match in every field, including its offset. PLK stores no negative zero, so -0e0 and
 * 0e0 match.
 */
private fun scalarMatches(a: Datum, e: Datum): Boolean {
    if (a.type.code() != e.type.code()) return false
    return when (a.type.code()) {
        PType.STRING -> a.string == e.string
        PType.BOOL -> a.boolean == e.boolean
        PType.INTEGER -> a.int == e.int
        PType.BIGINT -> a.long == e.long
        PType.DECIMAL -> a.bigDecimal == e.bigDecimal
        PType.DOUBLE -> a.double == e.double
        PType.DATE -> a.localDate == e.localDate
        PType.TIME -> a.localTime == e.localTime
        PType.TIMEZ -> a.offsetTime == e.offsetTime
        PType.TIMESTAMP -> a.localDateTime == e.localDateTime
        PType.TIMESTAMPZ -> a.offsetDateTime == e.offsetDateTime
        else -> false
    }
}

private fun <T> orderedMatches(a: List<T>, e: List<T>, matches: (T, T) -> Boolean): Boolean =
    a.size == e.size && a.indices.all { matches(a[it], e[it]) }

/**
 * Matches two lists as multisets. [matches] is an equivalence, so taking the first unused match is enough.
 */
private fun <T> unorderedMatches(a: List<T>, e: List<T>, matches: (T, T) -> Boolean): Boolean {
    if (a.size != e.size) return false
    val used = BooleanArray(a.size)
    for (x in e) {
        val i = a.indices.firstOrNull { !used[it] && matches(a[it], x) } ?: return false
        used[i] = true
    }
    return true
}
