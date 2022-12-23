package org.partiql.isl.builder

import com.amazon.ionelement.api.TimestampElement
import kotlin.Boolean
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import org.partiql.isl.Bounds
import org.partiql.isl.Constraint
import org.partiql.isl.Field
import org.partiql.isl.Footer
import org.partiql.isl.Header
import org.partiql.isl.Import
import org.partiql.isl.Measure
import org.partiql.isl.Range
import org.partiql.isl.Ref
import org.partiql.isl.Schema
import org.partiql.isl.TimestampPrecision
import org.partiql.isl.Type
import org.partiql.isl.UserReservedFields
import org.partiql.isl.Value
import org.partiql.isl.Version

public abstract class IonSchemaFactory {
  public open fun schema(
    version: Version,
    header: Header,
    definitions: List<Type>,
    footer: Footer
  ) = Schema(version, header, definitions, footer)

  public open fun header(imports: List<Import>, userReservedFields: UserReservedFields) =
      Header(imports, userReservedFields)

  public open fun userReservedFields(
    header: List<String>,
    type: List<String>,
    footer: List<String>
  ) = UserReservedFields(header, type, footer)

  public open fun footer() = Footer()

  public open fun importSchema(id: String) = Import.Schema(id)

  public open fun importType(id: String, type: Type) = Import.Type(id, type)

  public open fun importTypeAlias(
    id: String,
    type: Type,
    alias: String
  ) = Import.TypeAlias(id, type, alias)

  public open fun type(name: String, constraints: List<Constraint>) = Type(name, constraints)

  public open fun refType(name: String, nullable: Boolean) = Ref.Type(name, nullable)

  public open fun refInline(constraints: List<Constraint>, nullable: Boolean) =
      Ref.Inline(constraints, nullable)

  public open fun refImport(
    schema: String,
    type: String,
    nullable: Boolean
  ) = Ref.Import(schema, type, nullable)

  public open fun constraintRange(range: Range) = Constraint.Range(range)

  public open fun constraintAllOf(types: List<Ref>) = Constraint.AllOf(types)

  public open fun constraintAnyOf(types: List<Ref>) = Constraint.AnyOf(types)

  public open fun constraintAnnotations() = Constraint.Annotations()

  public open fun constraintLengthEquals(measure: Measure, length: Int) =
      Constraint.Length.Equals(measure, length)

  public open fun constraintLengthRange(measure: Measure, range: Range.Int) =
      Constraint.Length.Range(measure, range)

  public open fun constraintContains(values: List<Value.Ion>) = Constraint.Contains(values)

  public open fun constraintElement(type: Ref, distinct: Boolean) = Constraint.Element(type,
      distinct)

  public open fun constraintExponentEquals(`value`: Int) = Constraint.Exponent.Equals(value)

  public open fun constraintExponentRange(range: Range.Int) = Constraint.Exponent.Range(range)

  public open fun constraintFieldNames(type: Ref, distinct: Boolean) = Constraint.FieldNames(type,
      distinct)

  public open fun constraintFields(closed: Boolean, fields: List<Field>) = Constraint.Fields(closed,
      fields)

  public open fun constraintIeee754Float(format: Constraint.Ieee754Float.Format) =
      Constraint.Ieee754Float(format)

  public open fun constraintNot(type: Ref) = Constraint.Not(type)

  public open fun constraintOneOf(types: List<Ref>) = Constraint.OneOf(types)

  public open fun constraintOrderedElements(types: List<Ref>) = Constraint.OrderedElements(types)

  public open fun constraintPrecisionEquals(`value`: Int) = Constraint.Precision.Equals(value)

  public open fun constraintPrecisionRange(range: Range.Int) = Constraint.Precision.Range(range)

  public open fun constraintRegex(pattern: String, flags: Constraint.Regex.Flags) =
      Constraint.Regex(pattern, flags)

  public open fun constraintTimestampOffset(pattern: String) = Constraint.TimestampOffset(pattern)

  public open fun constraintTimestampPrecisionEquals(`value`: TimestampPrecision) =
      Constraint.TimestampPrecision.Equals(value)

  public open fun constraintTimestampPrecisionRange(range: Range.TimestampPrecision) =
      Constraint.TimestampPrecision.Range(range)

  public open fun constraintType(type: Ref) = Constraint.Type(type)

  public open fun constraintValidValues(values: List<Value>) = Constraint.ValidValues(values)

  public open fun `field`(name: String, type: Ref) = Field(name, type)

  public open fun valueIon(`value`: Value.Ion) = Value.Ion(value)

  public open fun valueRange(`value`: Constraint.Range) = Value.Range(value)

  public open fun rangeInt(
    lower: Int,
    upper: Int,
    bounds: Bounds
  ) = Range.Int(lower, upper, bounds)

  public open fun rangeNumber(
    lower: Double,
    upper: Double,
    bounds: Bounds
  ) = Range.Number(lower, upper, bounds)

  public open fun rangeTimestamp(
    lower: TimestampElement,
    upper: TimestampElement,
    bounds: Bounds
  ) = Range.Timestamp(lower, upper, bounds)

  public open fun rangeTimestampPrecision(
    lower: TimestampPrecision,
    upper: TimestampPrecision,
    bounds: Bounds
  ) = Range.TimestampPrecision(lower, upper, bounds)

  public companion object {
    public val DEFAULT: IonSchemaFactory = object : IonSchemaFactory() {}
  }
}
