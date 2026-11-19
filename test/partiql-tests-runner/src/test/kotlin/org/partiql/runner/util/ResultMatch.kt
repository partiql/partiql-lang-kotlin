package org.partiql.runner.util

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
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
        return compares(a, e, comparator)
    }
    return deepMatches(a, e, comparator)
}

/**
 * [comparator] equality. A result that holds a value of type JSON, when the expected value holds none, makes the
 * comparator raise FEATURE_NOT_SUPPORTED, which is not a match. Any other error is a real failure and is rethrown.
 */
private fun compares(a: Datum, e: Datum, comparator: Comparator<Datum>): Boolean = try {
    comparator.compare(a, e) == 0
} catch (ex: PRuntimeException) {
    if (ex.error.code() == PError.FEATURE_NOT_SUPPORTED) false else throw ex
}

private fun Datum.lowerVariant(): Datum = if (type.code() == PType.VARIANT && !isNull) lower() else this

private fun Datum.holdsJson(): Boolean {
    if (isNull || isMissing) return false
    return when (type.code()) {
        PType.JSON -> true
        PType.ARRAY, PType.BAG -> any { it.lowerVariant().holdsJson() }
        PType.STRUCT, PType.ROW -> fields.asSequence().any { it.value.lowerVariant().holdsJson() }
        @Suppress("DEPRECATION")
        PType.MAP -> entries.asSequence().any { it.value.lowerVariant().holdsJson() }
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
        PType.ARRAY -> a.type.code() == PType.ARRAY &&
            orderedMatches(a.toList(), e.toList()) { x, y -> deepMatches(x, y, comparator) }
        PType.BAG -> a.type.code() == PType.BAG && elementsMatch(a.toList(), e.toList(), comparator)
        PType.STRUCT, PType.ROW -> (a.type.code() == PType.STRUCT || a.type.code() == PType.ROW) &&
            fieldsMatch(a.fields.asSequence().toList(), e.fields.asSequence().toList(), comparator)
        @Suppress("DEPRECATION")
        PType.MAP -> a.type.code() == PType.MAP &&
            unorderedMatches(a.entries.asSequence().toList(), e.entries.asSequence().toList()) { x, y ->
                compares(x.key, y.key, comparator) && deepMatches(x.value, y.value, comparator)
            }
        else -> compares(a, e, comparator)
    }
}

/**
 * A bag's elements. Those without JSON compare as one bag with [comparator], as they did before; the greedy match
 * is used only for elements that hold JSON, where matching is an equivalence.
 */
private fun elementsMatch(a: List<Datum>, e: List<Datum>, comparator: Comparator<Datum>): Boolean {
    val (eJson, eRest) = e.partition { it.holdsJson() }
    val (aJson, aRest) = a.partition { it.holdsJson() }
    return compares(Datum.bag(aRest), Datum.bag(eRest), comparator) &&
        unorderedMatches(aJson, eJson) { x, y -> deepMatches(x, y, comparator) }
}

/**
 * A tuple's fields, split as [elementsMatch] splits a bag's elements.
 */
private fun fieldsMatch(a: List<Field>, e: List<Field>, comparator: Comparator<Datum>): Boolean {
    val (eJson, eRest) = e.partition { it.value.holdsJson() }
    val (aJson, aRest) = a.partition { it.value.holdsJson() }
    return compares(Datum.struct(aRest), Datum.struct(eRest), comparator) &&
        unorderedMatches(aJson, eJson) { x, y -> x.name == y.name && deepMatches(x.value, y.value, comparator) }
}

/**
 * Both values have type JSON and are not SQL NULL.
 */
@Suppress("DEPRECATION")
private fun jsonMatches(a: Datum, e: Datum): Boolean {
    if (a.type.code() != PType.JSON || e.type.code() != PType.JSON) return false
    if (a.jsonKind != e.jsonKind) return false
    return when (val kind = a.jsonKind) {
        JsonKind.NULL -> true
        JsonKind.SCALAR -> scalarMatches(a.jsonScalar, e.jsonScalar)
        JsonKind.ARRAY -> orderedMatches(a.toList(), e.toList(), ::jsonMatches)
        JsonKind.OBJECT -> unorderedMatches(a.fields.asSequence().toList(), e.fields.asSequence().toList()) { x, y ->
            x.name == y.name && jsonMatches(x.value, y.value)
        }
        else -> error("Unknown SQL/JSON item kind: $kind")
    }
}

/**
 * Scalars match when they have the same type and the same value. A DECIMAL must also have the same scale, and a
 * datetime must match in every field, including its offset. PLK stores no negative zero (DatumDouble), so -0e0
 * and 0e0 match, although the schema doc says they differ.
 */
private fun scalarMatches(a: Datum, e: Datum): Boolean {
    if (a.type.code() != e.type.code()) return false
    return when (val code = a.type.code()) {
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
        else -> error("Not an SQL/JSON scalar type: ${PType.of(code)}")
    }
}

private fun <T> orderedMatches(a: List<T>, e: List<T>, matches: (T, T) -> Boolean): Boolean =
    a.size == e.size && a.indices.all { matches(a[it], e[it]) }

/**
 * Matches two lists as multisets, taking the first unused match. That is enough only when [matches] is an
 * equivalence, as it is for JSON parts.
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
