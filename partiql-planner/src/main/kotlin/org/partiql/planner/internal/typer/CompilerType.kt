package org.partiql.planner.internal.typer

import org.partiql.planner.catalog.Identifier
import org.partiql.types.PType
import org.partiql.types.PType.Kind

/**
 * This is largely just to show that the planner does not need to use [_delegate] ([PType]) directly. Using an
 * internal representation, we can leverage the APIs of [PType] while carrying some additional information such
 * as [isMissingValue].
 *
 * @property isNullValue denotes that the expression will always return the null value.
 * @property isMissingValue denotes that the expression will always return the missing value.
 */
internal class CompilerType(
    private val _delegate: PType,
    // Note: This is an experimental property.
    internal val isNullValue: Boolean = false,
    // Note: This is an experimental property.
    internal val isMissingValue: Boolean = false
) : PType {
    public fun getDelegate(): PType = _delegate
    override fun getKind(): Kind = _delegate.kind
    override fun getFields(): MutableCollection<Field> {
        return _delegate.fields.map { field ->
            when (field) {
                is Field -> field
                else -> Field(field.name, CompilerType(field.type))
            }
        }.toMutableList()
    }

    override fun getLength(): Int {
        return _delegate.length
    }

    override fun getPrecision(): Int = _delegate.precision
    override fun getScale(): Int = _delegate.scale
    override fun getTypeParameter(): CompilerType {
        return when (val p = _delegate.typeParameter) {
            is CompilerType -> p
            else -> CompilerType(p)
        }
    }

    override fun equals(other: Any?): Boolean {
        return _delegate == other
    }

    override fun hashCode(): Int {
        return _delegate.hashCode()
    }

    override fun toString(): String {
        return _delegate.toString()
    }

    internal class Field(
        private val _name: String,
        private val _type: CompilerType
    ) : org.partiql.types.Field {
        override fun getName(): String = _name
        override fun getType(): CompilerType = _type

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is org.partiql.types.Field) return false
            val nameMatches = _name == other.name
            val typeMatches = _type == other.type
            return nameMatches && typeMatches
        }

        override fun hashCode(): Int {
            var result = _name.hashCode()
            result = 31 * result + _type.hashCode()
            return result
        }
    }

    internal fun isNumeric(): Boolean {
        return this.kind in setOf(
            Kind.INT,
            Kind.INT_ARBITRARY,
            Kind.BIGINT,
            Kind.TINYINT,
            Kind.SMALLINT,
            Kind.REAL,
            Kind.DOUBLE_PRECISION,
            Kind.DECIMAL,
            Kind.DECIMAL_ARBITRARY
        )
    }

    /**
     * Assumes that the type is either a struct or row.
     *
     * @return null when the field definitely does not exist; dynamic when the type cannot be determined
     */
    internal fun getSymbol(field: String): Pair<Identifier.Part, CompilerType>? {
        if (this.kind == Kind.STRUCT) {
            return Identifier.Part.regular(field) to CompilerType(PType.typeDynamic())
        }
        val fields = this.fields.mapNotNull {
            when (it.name.equals(field, true)) {
                true -> it.name to it.type
                false -> null
            }
        }.ifEmpty { return null }
        val type = anyOf(fields.map { it.second })
        val ids = fields.map { it.first }.toSet()
        return when (ids.size > 1) {
            true -> Identifier.Part.regular(field) to type
            false -> Identifier.Part.delimited(ids.first()) to type
        }
    }

    internal companion object {

        @JvmStatic
        internal fun anyOf(types: Collection<CompilerType>): CompilerType {
            if (types.isEmpty()) {
                return CompilerType(PType.typeDynamic())
            }
            val unique = types.toSet()
            return when (unique.size) {
                1 -> unique.first()
                else -> CompilerType(PType.typeDynamic())
            }
        }
    }
}
