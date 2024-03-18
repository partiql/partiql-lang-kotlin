package org.partiql.shape

import org.partiql.shape.visitor.ShapePrinter
import org.partiql.shape.visitor.ShapeVisitor
import org.partiql.types.AnyOfType
import org.partiql.types.CollectionType
import org.partiql.types.ListType
import org.partiql.types.SexpType
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
public sealed interface PShape : ShapeNode {

    public val type: PartiQLType

    public val constraints: Set<Constraint>

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
            constraint: Constraint,
            metas: Set<Meta> = emptySet()
        ): PShape {
            return Base(type, constraint, metas)
        }

        @JvmStatic
        @JvmOverloads
        @Deprecated("Should we allow this?")
        public fun of(
            type: PartiQLType,
            constraints: Set<Constraint> = emptySet(),
            metas: Set<Meta> = emptySet()
        ): PShape {
            val newConstraints = when (type) {
                is MissingType -> constraints + setOf(NotNull)
                else -> constraints
            }
            return Base(type, newConstraints, metas)
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
                0 -> Base(AnyType)
                1 -> shapes.first()
                else -> {
                    val flattened = shapes.flatMap { it.allShapes() }.toSet()
                    val type = flattened.first().type.let { first ->
                        when (flattened.all { it.type == first }) {
                            true -> first
                            false -> AnyType
                        }
                    }
                    Base(type, constraints = setOf(AnyOf(flattened)))
                }
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
            return when {
                this.isUnion() -> this.getAnyOf().shapes
                else -> setOf(this)
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.getAnyOf(): AnyOf {
            val anyOf = this.constraints.filterIsInstance<AnyOf>()
            return when (anyOf.size) {
                0 -> error("None found!")
                1 -> anyOf.first()
                else -> error("Expected one AnyOf, but ${anyOf.size} found.")
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
            return when {
                this.isUnion() -> this.allShapes().all { it.isSpecificType<T>() }
                else -> this.isSpecificType<T>()
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public inline fun <reified T> PShape.canBeType(): Boolean {
            return when {
                this.isUnion() -> this.allShapes().any { it.isSpecificType<T>() }
                else -> this.isSpecificType<T>()
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public inline fun <reified T> PShape.mayBeType(): Boolean {
            if (this.isType<T>()) {
                return true
            }
            return when {
                this.isUnion() -> this.allShapes().any { it.isType<T>() }
                else -> false
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.getFirstAndOnlyFields(): Fields? {
            val fields = this.constraints.filterIsInstance<Fields>()
            return when (fields.size) {
                1 -> fields.first()
                else -> null // TODO: Error or null?
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.getSingleElement(): Element? {
            val elements = this.constraints.filterIsInstance<Element>()
            return when (elements.size) {
                1 -> elements.first()
                else -> null // TODO: Error or null?
            }
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.getElement(): Element {
            val default = Element(of(AnyType))
            return this.getSingleElement() ?: default
        }

        @JvmStatic
        @Deprecated("Should we allow this?")
        public fun PShape.setElement(shape: PShape): PShape {
            val constraints = this.constraints.filterNot { it is Element } + setOf(Element(shape))
            return this.copy(constraints = constraints.toSet())
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
            if (this.canBeType<NullType>()) {
                return true
            }
            return when (this.constraints.size) {
                0 -> true
                else -> this.constraints.any { it.canBeNull() }
            }
        }

        @Deprecated("Double-check this")
        public fun Constraint.canBeNull(): Boolean {
            return when (this) {
                is NotNull -> false
                is AnyOf -> this.shapes.any { it.isNullable() }
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
            val constraints = this.constraints.filterNot { it is NotNull }.map { c ->
                when (c) {
                    is AnyOf -> {
                        val shapes = c.shapes.map { it.asNullable() }.toSet()
                        AnyOf(shapes)
                    }
                    else -> c
                }
            }.toSet()
            return of(
                type = this.type,
                constraints = constraints,
                metas = this.metas
            )
        }

        @Deprecated("Double-check this")
        public fun PShape.asOptional(): PShape {
            return when (this.type) {
                is MissingType -> this
                else -> PShape.anyOf(
                    setOf(
                        this,
                        PShape.of(MissingType)
                    ),
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
            constraints: Set<Constraint>? = null,
            metas: Set<Meta>? = null
        ): PShape {
            val c = constraints ?: this.constraints
            val t = type ?: this.type
            val m = metas ?: this.metas
            return Base(t, c, m)
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
                        of(
                            type = pType,
                            constraints = setOf(
                                Fields(
                                    fields = fields,
                                    isClosed = type.contentClosed,
                                    // TODO: isOrdered = type.constraints.contains(TupleConstraint.Ordered)
                                ),
                                NotNull
                            )
                        )
                    }
                    is CollectionType -> {
                        val element = type.elementType
                        val pType = when (type) {
                            is org.partiql.types.BagType -> BagType
                            is ListType -> ArrayType
                            is SexpType -> ArrayType
                        }
                        PShape.of(
                            type = pType,
                            constraints = setOf(
                                Element(fromStaticType(element)),
                                NotNull
                            )
                        )
                    }
                    is org.partiql.types.NullType -> of(PartiQLType.fromSingleType(type))
                    else -> of(
                        PartiQLType.fromSingleType(type),
                        constraints = setOf(NotNull)
                    )
                }
                is org.partiql.types.AnyType -> of(AnyType)
                is AnyOfType -> {
                    val flattened = type.flatten().allTypes
                    val types = when (flattened.any { it is org.partiql.types.NullType }) {
                        true -> flattened.filterNot { it is org.partiql.types.NullType }.map { child ->
                            fromStaticType(child).asNullable()
                        }
                        false -> flattened.map { child ->
                            fromStaticType(child)
                        }
                    }.toSet()
                    anyOf(types)
                }
            }
        }

        public fun PShape.isUnion(): Boolean = this.constraints.any { it.isUnion() }

        public fun Constraint.isUnion(): Boolean = when (this) {
            is AnyOf -> true
            else -> false
        }
    }

    private data class Base(
        override val type: PartiQLType,
        override val constraints: Set<Constraint> = emptySet(),
        override val metas: Set<Meta> = emptySet()
    ) : PShape {

        // TODO: On init, add each constructor at a time to make sure everything is clean.

        public constructor(type: PartiQLType, constraint: Constraint, metas: Set<Meta> = emptySet()) : this(type, setOf(constraint), metas)
        override fun validate(): ValidationResult {
            constraints.forEach { constraint ->
                val result = constraint.validate(type)
                if (result is ValidationResult.Failure) {
                    return result
                }
            }
            return ValidationResult.Success
        }

        override fun toString(): String {
            return ShapePrinter.stringify(this)
        }

        override fun <R, C> accept(visitor: ShapeVisitor<R, C>, ctx: C): R {
            return visitor.visitShape(this, ctx)
        }
    }
}
