package org.partiql.spi.internal

import org.partiql.types.Field
import org.partiql.types.PType
import org.partiql.types.PType.Kind

/**
 * Important SQL Definitions:
 * - assignable: The characteristic of a data type that permits a value of that data type to be
 * assigned to a site of a specified data type.
 *
 * This came from the internal planner Coercion.kt
 *
 * TODO consider modeling with enums and/or additional optimizations.
 */
internal object SqlTypes {

    /**
     * Remaining coercions from SQL:1999:
     * - Values corresponding to the binary data type are mutually assignable.
     * - Values corresponding to the data types BIT and BIT VARYING are always mutually comparable and
     * are mutually assignable.
     * - Values of type interval are mutually assignable only if the source and target of the assignment are
     * both year-month intervals or if they are both day-time intervals.
     * - Values corresponding to user-defined types are discussed in Subclause 4.8.4, ‘‘User-defined type
     * comparison and assignment’’.
     */
    fun isAssignable(input: PType, target: PType): Boolean {
        return areAssignableNumberTypes(input, target) ||
            areAssignableTextTypes(input, target) ||
            areAssignableBooleanTypes(input, target) ||
            areAssignableDateTimeTypes(input, target) ||
            areAssignableCollectionTypes(input, target) ||
            areAssignableStructuralTypes(input, target) ||
            areAssignableDynamicTypes(target)
    }

    /**
     * NOT specified by SQL:1999. We assume that we can coerce a collection of one type to another if the subtype
     * of each collection is assignable.
     */
    private fun areAssignableCollectionTypes(input: PType, target: PType): Boolean {
        return input in SqlTypeFamily.COLLECTION && target in SqlTypeFamily.COLLECTION && isAssignable(input.typeParameter, target.typeParameter)
    }

    /**
     * NOT specified by SQL:1999. We assume that we can statically coerce anything to DYNAMIC. However, note that
     * CAST(<v> AS DYNAMIC) is NEVER inserted. We check for the use of DYNAMIC at function resolution. This is merely
     * for the [PType.getTypeParameter] and [PType.getFields]
     */
    private fun areAssignableDynamicTypes(target: PType): Boolean {
        return target.kind == Kind.DYNAMIC
    }

    /**
     * NOT completely specified by SQL:1999.
     *
     * From SQL:1999:
     * ```
     * Values corresponding to row types are mutually assignable if and only if both have the same degree
     * and every field in one row type is mutually assignable to the field in the same ordinal position of
     * the other row type. Values corresponding to row types are mutually comparable if and only if both
     * have the same degree and every field in one row type is mutually comparable to the field in the
     * same ordinal position of the other row type.
     * ```
     */
    private fun areAssignableStructuralTypes(input: PType, target: PType): Boolean {
        return when {
            input.kind == Kind.ROW && target.kind == Kind.ROW -> fieldsAreAssignable(input.fields.toList(), target.fields!!.toList())
            input.kind == Kind.STRUCT && target.kind == Kind.ROW -> true
            input.kind == Kind.ROW && target.kind == Kind.STRUCT -> true
            input.kind == Kind.STRUCT && target.kind == Kind.STRUCT -> true
            else -> false
        }
    }

    private fun fieldsAreAssignable(input: List<Field>, target: List<Field>): Boolean {
        if (input.size != target.size) { return false }
        val iIter = input.iterator()
        val tIter = target.iterator()
        while (iIter.hasNext()) {
            val iField = iIter.next()
            val tField = tIter.next()
            if (!isAssignable(iField.type, tField.type)) {
                return false
            }
        }
        return true
    }

    /**
     * This is a PartiQL extension. We assume that structs/rows with the same field names may be assignable
     * if all names match AND types are assignable.
     */
    private fun namedFieldsAreAssignableUnordered(input: List<Field>, target: List<Field>): Boolean {
        if (input.size != target.size) { return false }
        val inputSorted = input.sortedBy { it.name }
        val targetSorted = target.sortedBy { it.name }
        val iIter = inputSorted.iterator()
        val tIter = targetSorted.iterator()
        while (iIter.hasNext()) {
            val iField = iIter.next()
            val tField = tIter.next()
            if (iField.name != tField.name) {
                return false
            }
            if (!isAssignable(iField.type, tField.type)) {
                return false
            }
        }
        return true
    }

    /**
     * From SQL:1999:
     * ```
     * Values of the data types NUMERIC, DECIMAL, INTEGER, SMALLINT, FLOAT, REAL, and
     * DOUBLE PRECISION are numbers and are all mutually comparable and mutually assignable.
     * ```
     */
    private fun areAssignableNumberTypes(input: PType, target: PType): Boolean {
        return input in SqlTypeFamily.NUMBER && target in SqlTypeFamily.NUMBER
    }

    /**
     * From SQL:1999:
     * ```
     * Values corresponding to the data type boolean are always mutually comparable and are mutually
     * assignable.
     * ```
     */
    private fun areAssignableBooleanTypes(input: PType, target: PType): Boolean {
        return input.kind == Kind.BOOL && target.kind == Kind.BOOL
    }

    /**
     * From SQL:1999:
     * ```
     * Values corresponding to the data types CHARACTER, CHARACTER VARYING, and CHARACTER
     * LARGE OBJECT are mutually assignable if and only if they are taken from the same character
     * repertoire. (For this implementation, we shall assume that all text types share the same
     * character repertoire.)
     * ```
     */
    private fun areAssignableTextTypes(input: PType, target: PType): Boolean {
        return input in SqlTypeFamily.TEXT && target in SqlTypeFamily.TEXT
    }

    /**
     * From SQL:1999:
     * ```
     * Values of type datetime are mutually assignable only if the source and target of the assignment are
     * both of type DATE, or both of type TIME (regardless whether WITH TIME ZONE or WITHOUT
     * TIME ZONE is specified or implicit), or both of type TIMESTAMP (regardless whether WITH TIME
     * ZONE or WITHOUT TIME ZONE is specified or implicit)
     * ```
     */
    private fun areAssignableDateTimeTypes(input: PType, target: PType): Boolean {
        val i = input.kind
        val t = target.kind
        return when {
            i == Kind.DATE && t == Kind.DATE -> true
            (i == Kind.TIMEZ || i == Kind.TIME) && (t == Kind.TIMEZ || t == Kind.TIME) -> true
            (i == Kind.TIMESTAMPZ || i == Kind.TIMESTAMP) && (t == Kind.TIMESTAMPZ || t == Kind.TIMESTAMP) -> true
            else -> false
        }
    }
}
