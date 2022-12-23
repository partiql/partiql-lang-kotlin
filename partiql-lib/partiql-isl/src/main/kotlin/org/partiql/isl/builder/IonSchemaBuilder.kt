package org.partiql.isl.builder

import com.amazon.ionelement.api.TimestampElement
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.MutableList
import kotlin.jvm.JvmStatic
import org.partiql.isl.Bounds
import org.partiql.isl.Constraint
import org.partiql.isl.Field
import org.partiql.isl.Footer
import org.partiql.isl.Header
import org.partiql.isl.Import
import org.partiql.isl.IonSchemaNode
import org.partiql.isl.Measure
import org.partiql.isl.Range
import org.partiql.isl.Ref
import org.partiql.isl.Schema
import org.partiql.isl.TimestampPrecision
import org.partiql.isl.Type
import org.partiql.isl.UserReservedFields
import org.partiql.isl.Value
import org.partiql.isl.Version

/**
 * The Builder is inside this private final class for DSL aesthetics
 */
public class IonSchema private constructor() {
  @Suppress("ClassName")
  public class Builder(
    private val factory: IonSchemaFactory
  ) {
    public fun schema(
      version: Version? = null,
      header: Header? = null,
      definitions: MutableList<Type> = mutableListOf(),
      footer: Footer? = null,
      block: _Schema.() -> Unit = {}
    ): Schema {
      val b = _Schema(version, header, definitions, footer)
      b.block()
      return factory.schema(version = b.version!!, header = b.header!!, definitions =
          b.definitions!!, footer = b.footer!!)
    }

    public fun header(
      imports: MutableList<Import> = mutableListOf(),
      userReservedFields: UserReservedFields? = null,
      block: _Header.() -> Unit = {}
    ): Header {
      val b = _Header(imports, userReservedFields)
      b.block()
      return factory.header(imports = b.imports!!, userReservedFields = b.userReservedFields!!)
    }

    public fun userReservedFields(
      header: MutableList<String> = mutableListOf(),
      type: MutableList<String> = mutableListOf(),
      footer: MutableList<String> = mutableListOf(),
      block: _UserReservedFields.() -> Unit = {}
    ): UserReservedFields {
      val b = _UserReservedFields(header, type, footer)
      b.block()
      return factory.userReservedFields(header = b.header!!, type = b.type!!, footer = b.footer!!)
    }

    public fun footer(block: _Footer.() -> Unit = {}): Footer {
      val b = _Footer()
      b.block()
      return factory.footer()
    }

    public fun importSchema(id: String? = null, block: _ImportSchema.() -> Unit = {}):
        Import.Schema {
      val b = _ImportSchema(id)
      b.block()
      return factory.importSchema(id = b.id!!)
    }

    public fun importType(
      id: String? = null,
      type: Type? = null,
      block: _ImportType.() -> Unit = {}
    ): Import.Type {
      val b = _ImportType(id, type)
      b.block()
      return factory.importType(id = b.id!!, type = b.type!!)
    }

    public fun importTypeAlias(
      id: String? = null,
      type: Type? = null,
      alias: String? = null,
      block: _ImportTypeAlias.() -> Unit = {}
    ): Import.TypeAlias {
      val b = _ImportTypeAlias(id, type, alias)
      b.block()
      return factory.importTypeAlias(id = b.id!!, type = b.type!!, alias = b.alias!!)
    }

    public fun type(
      name: String? = null,
      constraints: MutableList<Constraint> = mutableListOf(),
      block: _Type.() -> Unit = {}
    ): Type {
      val b = _Type(name, constraints)
      b.block()
      return factory.type(name = b.name!!, constraints = b.constraints!!)
    }

    public fun refType(
      name: String? = null,
      nullable: Boolean? = null,
      block: _RefType.() -> Unit = {}
    ): Ref.Type {
      val b = _RefType(name, nullable)
      b.block()
      return factory.refType(name = b.name!!, nullable = b.nullable!!)
    }

    public fun refInline(
      constraints: MutableList<Constraint> = mutableListOf(),
      nullable: Boolean? = null,
      block: _RefInline.() -> Unit = {}
    ): Ref.Inline {
      val b = _RefInline(constraints, nullable)
      b.block()
      return factory.refInline(constraints = b.constraints!!, nullable = b.nullable!!)
    }

    public fun refImport(
      schema: String? = null,
      type: String? = null,
      nullable: Boolean? = null,
      block: _RefImport.() -> Unit = {}
    ): Ref.Import {
      val b = _RefImport(schema, type, nullable)
      b.block()
      return factory.refImport(schema = b.schema!!, type = b.type!!, nullable = b.nullable!!)
    }

    public fun constraintRange(range: Range? = null, block: _ConstraintRange.() -> Unit = {}):
        Constraint.Range {
      val b = _ConstraintRange(range)
      b.block()
      return factory.constraintRange(range = b.range!!)
    }

    public fun constraintAllOf(types: MutableList<Ref> = mutableListOf(),
        block: _ConstraintAllOf.() -> Unit = {}): Constraint.AllOf {
      val b = _ConstraintAllOf(types)
      b.block()
      return factory.constraintAllOf(types = b.types!!)
    }

    public fun constraintAnyOf(types: MutableList<Ref> = mutableListOf(),
        block: _ConstraintAnyOf.() -> Unit = {}): Constraint.AnyOf {
      val b = _ConstraintAnyOf(types)
      b.block()
      return factory.constraintAnyOf(types = b.types!!)
    }

    public fun constraintAnnotations(block: _ConstraintAnnotations.() -> Unit = {}):
        Constraint.Annotations {
      val b = _ConstraintAnnotations()
      b.block()
      return factory.constraintAnnotations()
    }

    public fun constraintLengthEquals(
      measure: Measure? = null,
      length: Int? = null,
      block: _ConstraintLengthEquals.() -> Unit = {}
    ): Constraint.Length.Equals {
      val b = _ConstraintLengthEquals(measure, length)
      b.block()
      return factory.constraintLengthEquals(measure = b.measure!!, length = b.length!!)
    }

    public fun constraintLengthRange(
      measure: Measure? = null,
      range: Range.Int? = null,
      block: _ConstraintLengthRange.() -> Unit = {}
    ): Constraint.Length.Range {
      val b = _ConstraintLengthRange(measure, range)
      b.block()
      return factory.constraintLengthRange(measure = b.measure!!, range = b.range!!)
    }

    public fun constraintContains(values: MutableList<Value.Ion> = mutableListOf(),
        block: _ConstraintContains.() -> Unit = {}): Constraint.Contains {
      val b = _ConstraintContains(values)
      b.block()
      return factory.constraintContains(values = b.values!!)
    }

    public fun constraintElement(
      type: Ref? = null,
      distinct: Boolean? = null,
      block: _ConstraintElement.() -> Unit = {}
    ): Constraint.Element {
      val b = _ConstraintElement(type, distinct)
      b.block()
      return factory.constraintElement(type = b.type!!, distinct = b.distinct!!)
    }

    public fun constraintExponentEquals(`value`: Int? = null, block: _ConstraintExponentEquals.() ->
        Unit = {}): Constraint.Exponent.Equals {
      val b = _ConstraintExponentEquals(value)
      b.block()
      return factory.constraintExponentEquals(value = b.value!!)
    }

    public fun constraintExponentRange(range: Range.Int? = null,
        block: _ConstraintExponentRange.() -> Unit = {}): Constraint.Exponent.Range {
      val b = _ConstraintExponentRange(range)
      b.block()
      return factory.constraintExponentRange(range = b.range!!)
    }

    public fun constraintFieldNames(
      type: Ref? = null,
      distinct: Boolean? = null,
      block: _ConstraintFieldNames.() -> Unit = {}
    ): Constraint.FieldNames {
      val b = _ConstraintFieldNames(type, distinct)
      b.block()
      return factory.constraintFieldNames(type = b.type!!, distinct = b.distinct!!)
    }

    public fun constraintFields(
      closed: Boolean? = null,
      fields: MutableList<Field> = mutableListOf(),
      block: _ConstraintFields.() -> Unit = {}
    ): Constraint.Fields {
      val b = _ConstraintFields(closed, fields)
      b.block()
      return factory.constraintFields(closed = b.closed!!, fields = b.fields!!)
    }

    public fun constraintIeee754Float(format: Constraint.Ieee754Float.Format? = null,
        block: _ConstraintIeee754Float.() -> Unit = {}): Constraint.Ieee754Float {
      val b = _ConstraintIeee754Float(format)
      b.block()
      return factory.constraintIeee754Float(format = b.format!!)
    }

    public fun constraintNot(type: Ref? = null, block: _ConstraintNot.() -> Unit = {}):
        Constraint.Not {
      val b = _ConstraintNot(type)
      b.block()
      return factory.constraintNot(type = b.type!!)
    }

    public fun constraintOneOf(types: MutableList<Ref> = mutableListOf(),
        block: _ConstraintOneOf.() -> Unit = {}): Constraint.OneOf {
      val b = _ConstraintOneOf(types)
      b.block()
      return factory.constraintOneOf(types = b.types!!)
    }

    public fun constraintOrderedElements(types: MutableList<Ref> = mutableListOf(),
        block: _ConstraintOrderedElements.() -> Unit = {}): Constraint.OrderedElements {
      val b = _ConstraintOrderedElements(types)
      b.block()
      return factory.constraintOrderedElements(types = b.types!!)
    }

    public fun constraintPrecisionEquals(`value`: Int? = null,
        block: _ConstraintPrecisionEquals.() -> Unit = {}): Constraint.Precision.Equals {
      val b = _ConstraintPrecisionEquals(value)
      b.block()
      return factory.constraintPrecisionEquals(value = b.value!!)
    }

    public fun constraintPrecisionRange(range: Range.Int? = null,
        block: _ConstraintPrecisionRange.() -> Unit = {}): Constraint.Precision.Range {
      val b = _ConstraintPrecisionRange(range)
      b.block()
      return factory.constraintPrecisionRange(range = b.range!!)
    }

    public fun constraintRegex(
      pattern: String? = null,
      flags: Constraint.Regex.Flags? = null,
      block: _ConstraintRegex.() -> Unit = {}
    ): Constraint.Regex {
      val b = _ConstraintRegex(pattern, flags)
      b.block()
      return factory.constraintRegex(pattern = b.pattern!!, flags = b.flags!!)
    }

    public fun constraintTimestampOffset(pattern: String? = null,
        block: _ConstraintTimestampOffset.() -> Unit = {}): Constraint.TimestampOffset {
      val b = _ConstraintTimestampOffset(pattern)
      b.block()
      return factory.constraintTimestampOffset(pattern = b.pattern!!)
    }

    public fun constraintTimestampPrecisionEquals(`value`: TimestampPrecision? = null,
        block: _ConstraintTimestampPrecisionEquals.() -> Unit = {}):
        Constraint.TimestampPrecision.Equals {
      val b = _ConstraintTimestampPrecisionEquals(value)
      b.block()
      return factory.constraintTimestampPrecisionEquals(value = b.value!!)
    }

    public fun constraintTimestampPrecisionRange(range: Range.TimestampPrecision? = null,
        block: _ConstraintTimestampPrecisionRange.() -> Unit = {}):
        Constraint.TimestampPrecision.Range {
      val b = _ConstraintTimestampPrecisionRange(range)
      b.block()
      return factory.constraintTimestampPrecisionRange(range = b.range!!)
    }

    public fun constraintType(type: Ref? = null, block: _ConstraintType.() -> Unit = {}):
        Constraint.Type {
      val b = _ConstraintType(type)
      b.block()
      return factory.constraintType(type = b.type!!)
    }

    public fun constraintValidValues(values: MutableList<Value> = mutableListOf(),
        block: _ConstraintValidValues.() -> Unit = {}): Constraint.ValidValues {
      val b = _ConstraintValidValues(values)
      b.block()
      return factory.constraintValidValues(values = b.values!!)
    }

    public fun `field`(
      name: String? = null,
      type: Ref? = null,
      block: _Field.() -> Unit = {}
    ): Field {
      val b = _Field(name, type)
      b.block()
      return factory.field(name = b.name!!, type = b.type!!)
    }

    public fun valueIon(`value`: Value.Ion? = null, block: _ValueIon.() -> Unit = {}): Value.Ion {
      val b = _ValueIon(value)
      b.block()
      return factory.valueIon(value = b.value!!)
    }

    public fun valueRange(`value`: Constraint.Range? = null, block: _ValueRange.() -> Unit = {}):
        Value.Range {
      val b = _ValueRange(value)
      b.block()
      return factory.valueRange(value = b.value!!)
    }

    public fun rangeInt(
      lower: Int? = null,
      upper: Int? = null,
      bounds: Bounds? = null,
      block: _RangeInt.() -> Unit = {}
    ): Range.Int {
      val b = _RangeInt(lower, upper, bounds)
      b.block()
      return factory.rangeInt(lower = b.lower!!, upper = b.upper!!, bounds = b.bounds!!)
    }

    public fun rangeNumber(
      lower: Double? = null,
      upper: Double? = null,
      bounds: Bounds? = null,
      block: _RangeNumber.() -> Unit = {}
    ): Range.Number {
      val b = _RangeNumber(lower, upper, bounds)
      b.block()
      return factory.rangeNumber(lower = b.lower!!, upper = b.upper!!, bounds = b.bounds!!)
    }

    public fun rangeTimestamp(
      lower: TimestampElement? = null,
      upper: TimestampElement? = null,
      bounds: Bounds? = null,
      block: _RangeTimestamp.() -> Unit = {}
    ): Range.Timestamp {
      val b = _RangeTimestamp(lower, upper, bounds)
      b.block()
      return factory.rangeTimestamp(lower = b.lower!!, upper = b.upper!!, bounds = b.bounds!!)
    }

    public fun rangeTimestampPrecision(
      lower: TimestampPrecision? = null,
      upper: TimestampPrecision? = null,
      bounds: Bounds? = null,
      block: _RangeTimestampPrecision.() -> Unit = {}
    ): Range.TimestampPrecision {
      val b = _RangeTimestampPrecision(lower, upper, bounds)
      b.block()
      return factory.rangeTimestampPrecision(lower = b.lower!!, upper = b.upper!!, bounds =
          b.bounds!!)
    }

    public class _Schema(
      public var version: Version? = null,
      public var header: Header? = null,
      public var definitions: MutableList<Type> = mutableListOf(),
      public var footer: Footer? = null
    )

    public class _Header(
      public var imports: MutableList<Import> = mutableListOf(),
      public var userReservedFields: UserReservedFields? = null
    )

    public class _UserReservedFields(
      public var header: MutableList<String> = mutableListOf(),
      public var type: MutableList<String> = mutableListOf(),
      public var footer: MutableList<String> = mutableListOf()
    )

    public class _Footer

    public class _ImportSchema(
      public var id: String? = null
    )

    public class _ImportType(
      public var id: String? = null,
      public var type: Type? = null
    )

    public class _ImportTypeAlias(
      public var id: String? = null,
      public var type: Type? = null,
      public var alias: String? = null
    )

    public class _Type(
      public var name: String? = null,
      public var constraints: MutableList<Constraint> = mutableListOf()
    )

    public class _RefType(
      public var name: String? = null,
      public var nullable: Boolean? = null
    )

    public class _RefInline(
      public var constraints: MutableList<Constraint> = mutableListOf(),
      public var nullable: Boolean? = null
    )

    public class _RefImport(
      public var schema: String? = null,
      public var type: String? = null,
      public var nullable: Boolean? = null
    )

    public class _ConstraintRange(
      public var range: Range? = null
    )

    public class _ConstraintAllOf(
      public var types: MutableList<Ref> = mutableListOf()
    )

    public class _ConstraintAnyOf(
      public var types: MutableList<Ref> = mutableListOf()
    )

    public class _ConstraintAnnotations

    public class _ConstraintLengthEquals(
      public var measure: Measure? = null,
      public var length: Int? = null
    )

    public class _ConstraintLengthRange(
      public var measure: Measure? = null,
      public var range: Range.Int? = null
    )

    public class _ConstraintContains(
      public var values: MutableList<Value.Ion> = mutableListOf()
    )

    public class _ConstraintElement(
      public var type: Ref? = null,
      public var distinct: Boolean? = null
    )

    public class _ConstraintExponentEquals(
      public var `value`: Int? = null
    )

    public class _ConstraintExponentRange(
      public var range: Range.Int? = null
    )

    public class _ConstraintFieldNames(
      public var type: Ref? = null,
      public var distinct: Boolean? = null
    )

    public class _ConstraintFields(
      public var closed: Boolean? = null,
      public var fields: MutableList<Field> = mutableListOf()
    )

    public class _ConstraintIeee754Float(
      public var format: Constraint.Ieee754Float.Format? = null
    )

    public class _ConstraintNot(
      public var type: Ref? = null
    )

    public class _ConstraintOneOf(
      public var types: MutableList<Ref> = mutableListOf()
    )

    public class _ConstraintOrderedElements(
      public var types: MutableList<Ref> = mutableListOf()
    )

    public class _ConstraintPrecisionEquals(
      public var `value`: Int? = null
    )

    public class _ConstraintPrecisionRange(
      public var range: Range.Int? = null
    )

    public class _ConstraintRegex(
      public var pattern: String? = null,
      public var flags: Constraint.Regex.Flags? = null
    )

    public class _ConstraintTimestampOffset(
      public var pattern: String? = null
    )

    public class _ConstraintTimestampPrecisionEquals(
      public var `value`: TimestampPrecision? = null
    )

    public class _ConstraintTimestampPrecisionRange(
      public var range: Range.TimestampPrecision? = null
    )

    public class _ConstraintType(
      public var type: Ref? = null
    )

    public class _ConstraintValidValues(
      public var values: MutableList<Value> = mutableListOf()
    )

    public class _Field(
      public var name: String? = null,
      public var type: Ref? = null
    )

    public class _ValueIon(
      public var `value`: Value.Ion? = null
    )

    public class _ValueRange(
      public var `value`: Constraint.Range? = null
    )

    public class _RangeInt(
      public var lower: Int? = null,
      public var upper: Int? = null,
      public var bounds: Bounds? = null
    )

    public class _RangeNumber(
      public var lower: Double? = null,
      public var upper: Double? = null,
      public var bounds: Bounds? = null
    )

    public class _RangeTimestamp(
      public var lower: TimestampElement? = null,
      public var upper: TimestampElement? = null,
      public var bounds: Bounds? = null
    )

    public class _RangeTimestampPrecision(
      public var lower: TimestampPrecision? = null,
      public var upper: TimestampPrecision? = null,
      public var bounds: Bounds? = null
    )
  }

  public companion object {
    @JvmStatic
    public fun <T : IonSchemaNode> build(factory: IonSchemaFactory = IonSchemaFactory.DEFAULT,
        block: Builder.() -> T) = Builder(factory).block()

    @JvmStatic
    public fun <T : IonSchemaNode> create(block: IonSchemaFactory.() -> T) =
        IonSchemaFactory.DEFAULT.block()
  }
}
