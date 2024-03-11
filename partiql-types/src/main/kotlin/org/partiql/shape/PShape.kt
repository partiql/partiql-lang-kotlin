package org.partiql.shape

import org.partiql.shape.constraints.Constraint
import org.partiql.shape.constraints.Element
import org.partiql.shape.constraints.Fields
import org.partiql.shape.constraints.Multiple
import org.partiql.shape.constraints.None
import org.partiql.shape.constraints.NotNull
import org.partiql.types.AnyOfType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.AnyType
import org.partiql.value.ArrayType
import org.partiql.value.BagType
import org.partiql.value.CharType
import org.partiql.value.CharVarType
import org.partiql.value.CharVarUnboundedType
import org.partiql.value.MissingType
import org.partiql.value.NullType
import org.partiql.value.PartiQLType

/**
 * TODO: Do we support validation of values eventually?
 */
public sealed interface PShape {

    public val type: PartiQLType

    public val constraint: Constraint

    @Deprecated("This is an example of how we can gather metadata.")
    public val metas: Set<Meta>

    public fun validate(): ValidationResult

    /**
     * TODO: Do we support this?
     */
    @Deprecated(
        "This is an example of how we can gather other descriptors such as PRIMARY KEY, etc. " +
            "It is distinct from Constraints."
    )
    public sealed interface Meta

    public sealed interface ValidationResult {

        public object Success : ValidationResult

        public data class Failure(
            val errors: List<org.partiql.shape.errors.Error>
        ) : ValidationResult {

            public constructor(vararg errors: org.partiql.shape.errors.Error) : this(errors.toList())
        }
    }

    public companion object {

        @JvmStatic
        @JvmOverloads
        @Deprecated("Should we allow this?")
        public fun of(
            type: PartiQLType,
            constraint: Constraint = None,
            metas: Set<Meta> = emptySet()
        ): PShape {
            return Single(type, constraint, metas)
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        @JvmName("anyOfTypes")
        public fun anyOf(types: Set<PartiQLType>): PShape {
            val shapes = types.map { type -> of(type) }.toSet()
            return anyOf(shapes)
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        @JvmName("anyOfShapes")
        public fun anyOf(shapes: Set<PShape>): PShape {
            return when (shapes.size) {
                0 -> Single(AnyType)
                1 -> shapes.first()
                else -> Union.of(
                    shapes = shapes
                )
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun anyOf(vararg types: PartiQLType): PShape {
            return anyOf(types.toSet())
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun anyOf(vararg shapes: PShape): PShape {
            return anyOf(shapes.toSet())
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.allShapes(): Set<PShape> {
            return when (this) {
                is Union -> this.shapes
                else -> setOf(this)
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.allTypes(): Set<PartiQLType> {
            val shapes = this.allShapes()
            return shapes.map { it.type }.toSet()
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public inline fun <reified T> PShape.isSpecificType(): Boolean {
            return this.type is T
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public inline fun <reified T> PShape.isType(): Boolean {
            return when (this) {
                is Union -> this.shapes.all { it.isSpecificType<T>() }
                else -> this.isSpecificType<T>()
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public inline fun <reified T> PShape.mayBeType(): Boolean {
            if (this.isType<T>()) {
                return true
            }
            return when (this) {
                is Union -> this.shapes.any { it.isType<T>() }
                else -> false
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.getFirstAndOnlyFields(): Fields? {
            return when (val constraint = this.constraint) {
                is Multiple -> constraint.getFirstAndOnlyFields()
                is Fields -> constraint
                else -> null
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.getSingleElement(): Element? {
            return when (val constraint = this.constraint) {
                is Multiple -> constraint.getSingleElement()
                is Element -> constraint
                else -> null
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.getElement(): Element {
            val default = Element(of(AnyType))
            return when (val constraint = this.constraint) {
                is Multiple -> constraint.getSingleElement() ?: default
                is Element -> constraint
                else -> default
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.setElement(shape: PShape): PShape {
            val constraint = when (val c = this.constraint) {
                is Multiple -> {
                    val constraints = c.constraints.filterNot { it is Element }.toSet() + setOf(Element(shape))
                    Multiple.of(constraints)
                }
                is Element -> Element(shape)
                else -> Multiple.of(setOf(c, Element(shape)))
            }
            return this.copy(constraint = constraint)
        }

        @Deprecated("Should we allow this?")
        private fun Multiple.getSingleElement(): Element? {
            return this.constraints.filterIsInstance<Element>().let { elements ->
                when (elements.size) {
                    1 -> elements.first()
                    else -> null
                }
            }
        }

        @Deprecated("Should we allow this?")
        private fun Multiple.getFirstAndOnlyFields(): Fields? {
            return this.constraints.filterIsInstance<Fields>().let { fields ->
                when (fields.size) {
                    1 -> fields.first()
                    else -> null
                }
            }
        }

        @Deprecated("Double-check this")
        // TODO: Allow types to be nullable?
        public fun PShape.isNullable(): Boolean {
            if (this.type is NullType) {
                return true
            }
            return when (val c = this.constraint) {
                is Multiple -> c.constraints.none { it is NotNull }
                is NotNull -> false
                else -> true
            }
        }

        @Deprecated("Double-check this")
        public fun PShape.isText(): Boolean = this.isType<CharType>() ||
            this.isType<CharVarType>() ||
            this.isType<CharVarUnboundedType>()

        @Deprecated("Double-check this")
        public fun PartiQLType.isText(): Boolean = this is CharType || this is CharVarType || this is CharVarUnboundedType

        @Deprecated("Double-check this")
        public fun PShape.asNullable(): PShape {
            val constraint = when (val c = this.constraint) {
                is NotNull -> None
                is Multiple -> Multiple.of(
                    c.constraints.filterNot {
                        it is NotNull
                    }.toSet()
                )
                else -> c
            }
            return of(
                type = this.type,
                constraint = constraint,
                metas = this.metas
            )
        }

        @Deprecated("Double-check this")
        public fun PShape.asOptional(): PShape {
            return when (this.type) {
                is MissingType -> this
                else -> Union.of(
                    shapes = setOf(
                        this,
                        Single(MissingType)
                    ),
                    type = AnyType
                )
            }
        }

        @JvmStatic
        @Deprecated("Double-check this")
        public fun PShape.isMissable(): Boolean {
            return this.mayBeType<MissingType>()
        }

        @JvmStatic
        @Deprecated("Double-check this")
        public fun PShape.isCollection(): Boolean {
            return this.isType<BagType>() || this.isType<ArrayType>()
        }

        @Deprecated("Double-check this")
        public fun PShape.copy(
            type: PartiQLType? = null,
            constraint: Constraint? = null,
            metas: Set<Meta>? = null
        ): PShape = when (this) {
            is Union -> {
                val c = constraint ?: this.constraint
                val t = type ?: this.type
                val m = metas ?: this.metas
                this.copy(constraint = c, type = t, metas = m)
            }
            is Single -> {
                val c = constraint ?: this.constraint
                val t = type ?: this.type
                val m = metas ?: this.metas
                this.copy(constraint = c, type = t, metas = m)
            }
            else -> error("This should not have occurred, but to compile, this check is required.")
        }

        /**
         * Converts [StaticType] to [PShape]
         */
        @Deprecated("Should not be used")
        public fun fromStaticType(type: StaticType): PShape {
            return when (type) {
                is SingleType -> when (type) {
                    is StructType -> {
                        val pType = PartiQLType.fromSingleType(type)
                        val fields = type.fields.map { Fields.Field(it.key, fromStaticType(it.value)) }
                        Single(
                            type = pType,
                            constraint = Fields(
                                fields = fields,
                                isClosed = type.contentClosed,
                                isOrdered = type.constraints.contains(TupleConstraint.Ordered)
                            )
                        )
                    }
                    else -> of(PartiQLType.fromSingleType(type))
                }
                is org.partiql.types.AnyType -> of(AnyType)
                is AnyOfType -> {
                    val types = type.flatten().allTypes.map { child ->
                        fromStaticType(child)
                    }.toSet()
                    Union.of(types)
                }
            }
        }
    }
}
