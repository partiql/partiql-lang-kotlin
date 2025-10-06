package org.partiql.planner.internal.typer

import org.partiql.spi.catalog.Identifier
import org.partiql.spi.types.PType

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
) : PType(_delegate.code()) {
    init {
        this.metas = HashMap(_delegate.metas)
    }

    fun getDelegate(): PType = _delegate
    override fun getFields(): MutableCollection<PTypeField> {
        return _delegate.fields.map { field ->
            when (field) {
                is PTypeField -> field
                else -> PTypeField(field.name, CompilerType(field.type))
            }
        }.toMutableList()
    }

    override fun getLength(): Long {
        return _delegate.length
    }

    override fun getIntervalCode(): Int {
        return _delegate.getIntervalCode()
    }

    override fun getPrecision(): Int = _delegate.precision
    override fun getScale(): Int = _delegate.scale
    override fun getFractionalPrecision(): Int {
        return _delegate.getFractionalPrecision()
    }
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

    internal class PTypeField(
        private val _name: String,
        private val _type: CompilerType
    ) : org.partiql.spi.types.PTypeField {
        override fun getName(): String = _name
        override fun getType(): CompilerType = _type

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is org.partiql.spi.types.PTypeField) return false
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
        return this.code() in setOf(
            INTEGER,
            NUMERIC,
            BIGINT,
            TINYINT,
            SMALLINT,
            REAL,
            DOUBLE,
            DECIMAL,
        )
    }

    /**
     * Assumes that the type is either a struct or row.
     *
     * @return null when the field definitely does not exist; dynamic when the type cannot be determined
     */
    internal fun getSymbol(field: String): Pair<Identifier.Simple, CompilerType>? {
        if (this.code() == STRUCT) {
            return Identifier.Simple.regular(field) to CompilerType(dynamic())
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
            true -> Identifier.Simple.regular(field) to type
            false -> Identifier.Simple.delimited(ids.first()) to type
        }
    }

    internal companion object {

        @JvmStatic
        internal fun anyOf(types: Collection<CompilerType>): CompilerType {
            if (types.isEmpty()) {
                return CompilerType(dynamic())
            }
            val unique = types.toSet()
            return when (unique.size) {
                1 -> unique.first()
                else -> CompilerType(dynamic())
            }
        }
    }
}
