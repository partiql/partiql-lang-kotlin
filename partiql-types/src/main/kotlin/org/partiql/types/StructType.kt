package org.partiql.types

/**
 * Describes a PartiQL Struct.
 *
 * @param fields the key-value pairs of the struct
 * @param contentClosed when true, denotes that no other attributes may be present
 * @param primaryKeyFields fields designated as primary keys
 * @param constraints set of constraints applied to the Struct
 * @param metas meta-data
 */
public data class StructType(
    val fields: List<Field> = listOf(),
    // `TupleConstraint` already has `Open` constraint which overlaps with `contentClosed`.
    // In addition, `primaryKeyFields` must not exist on the `StructType` as `PrimaryKey`
    // is a property of collection of tuples. As we have plans to define PartiQL types in
    // more details it's foreseeable to have an refactor of our types in future and have a
    // new definition of this type as `Tuple`. See the following issue for more details:
    // https://github.com/partiql/partiql-spec/issues/49
    // TODO remove `contentClosed` and `primaryKeyFields` if after finalizing our type specification we're
    // still going with `StructType`.
    val contentClosed: Boolean = false,
    val primaryKeyFields: List<String> = listOf(),
    val constraints: Set<TupleConstraint> = setOf(),
    override val metas: Map<String, Any> = mapOf(),
) : SingleType() {

    public constructor(
        fields: Map<String, StaticType>,
        contentClosed: Boolean = false,
        primaryKeyFields: List<String> = listOf(),
        constraints: Set<TupleConstraint> = setOf(),
        metas: Map<String, Any> = mapOf(),
    ) : this(
        fields.map { Field(it.key, it.value) },
        contentClosed,
        primaryKeyFields,
        constraints,
        metas
    )

    /**
     * The key-value pair of a StructType, where the key represents the name of the field and the value represents
     * its [StaticType]. Note: multiple [Field]s within a [StructType] may contain the same [key], and therefore,
     * multiple same-named keys may refer to distinct [StaticType]s. To determine the [StaticType]
     * of a reference to a field, especially in the case of duplicates, it depends on the ordering of the [StructType]
     * (denoted by the presence of [TupleConstraint.Ordered] in the [StructType.constraints]).
     * - If ORDERED: the PartiQL specification says to grab the first encountered matching field.
     * - If UNORDERED: it is implementation-defined. However, gather all possible types, merge them using [AnyOfType].
     */
    public data class Field(
        val key: String,
        val value: StaticType
    )

    override fun flatten(): StaticType = this

    override val allTypes: List<StaticType>
        get() = listOf(this)

    override fun toString(): String {
        val firstSeveral = fields.take(3).joinToString { "${it.key}: ${it.value}" }
        return when {
            fields.size <= 3 -> "struct($firstSeveral, $constraints)"
            else -> "struct($firstSeveral, ... and ${fields.size - 3} other field(s), $constraints)"
        }
    }
}